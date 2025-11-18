package com.example.myapplication.admin.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Message
import com.example.myapplication.model.BroadcastMessage
import com.example.myapplication.model.Chat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class AdminMessagesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _broadcastSent = MutableLiveData<Boolean>()
    val broadcastSent: LiveData<Boolean> = _broadcastSent

    /**
     * Carga los mensajes de un chat específico
     */
    fun loadMessages(chatId: String) {
        db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminMessagesViewModel", "Error cargando mensajes: ${e.message}", e)
                    _messages.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messageList = mutableListOf<Message>()
                    for (doc in snapshot.documents) {
                        try {
                            val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                            if (message != null) {
                                messageList.add(message)
                            }
                        } catch (ex: Exception) {
                            Log.e("AdminMessagesViewModel", "Error parseando mensaje: ${ex.message}", ex)
                        }
                    }
                    _messages.value = messageList
                    Log.d("AdminMessagesViewModel", "Mensajes cargados: ${messageList.size}")
                }
            }
    }

    /**
     * Envía un mensaje desde el administrador a un usuario
     */
    fun sendMessage(
        chatId: String,
        senderEmail: String,
        senderName: String,
        content: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = Message(
            chatId = chatId,
            senderEmail = senderEmail,
            senderName = senderName,
            content = content,
            timestamp = Timestamp.now()
        )

        db.collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("AdminMessagesViewModel", "Mensaje enviado exitosamente")
                // Actualizar último mensaje del chat
                updateChatLastMessage(chatId, content)
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("AdminMessagesViewModel", "Error enviando mensaje: ${e.message}")
                onFailure(e)
            }
    }

    /**
     * Envía un mensaje a todos los usuarios
     */
    fun sendBroadcastMessage(
        adminEmail: String,
        adminName: String,
        content: String,
        userEmails: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userEmails.isEmpty()) {
            onFailure(Exception("No hay usuarios para enviar el mensaje"))
            return
        }

        val broadcastMessage = BroadcastMessage(
            senderEmail = adminEmail,
            senderName = adminName,
            content = content,
            timestamp = Timestamp.now(),
            recipients = userEmails
        )

        db.collection("broadcasts")
            .add(broadcastMessage)
            .addOnSuccessListener { docRef ->
                Log.d("AdminMessagesViewModel", "Mensaje difundido exitosamente")
                // Crear un mensaje individual en cada chat de soporte
                createMessagesForBroadcast(adminEmail, adminName, content, userEmails, onSuccess, onFailure)
            }
            .addOnFailureListener { e ->
                Log.e("AdminMessagesViewModel", "Error enviando mensaje difundido: ${e.message}")
                onFailure(e)
            }
    }

    /**
     * Crea mensajes individuales para cada usuario desde el broadcast
     */
    private fun createMessagesForBroadcast(
        adminEmail: String,
        adminName: String,
        content: String,
        userEmails: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        var successCount = 0
        var failureCount = 0

        for (userEmail in userEmails) {
            // Buscar chat de soporte existente
            db.collection("chats")
                .whereEqualTo("user1Email", adminEmail)
                .whereEqualTo("user2Email", userEmail)
                .whereEqualTo("chatType", "support")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        val chatId = snapshot.documents[0].id
                        // Chat existe, enviar mensaje
                        sendMessageToChat(chatId, adminEmail, adminName, content, userEmails.size, { successCount++; checkBroadcastComplete(successCount, failureCount, userEmails.size, onSuccess, onFailure) }, { failureCount++; checkBroadcastComplete(successCount, failureCount, userEmails.size, onSuccess, onFailure) })
                    } else {
                        Log.d("AdminMessagesViewModel", "Chat de soporte no encontrado para: $userEmail. Creando uno nuevo...")
                        // Chat no existe, crear uno
                        createSupportChatAndSendMessage(adminEmail, adminName, userEmail, content, userEmails.size, { successCount++; checkBroadcastComplete(successCount, failureCount, userEmails.size, onSuccess, onFailure) }, { failureCount++; checkBroadcastComplete(successCount, failureCount, userEmails.size, onSuccess, onFailure) })
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AdminMessagesViewModel", "Error buscando chat de soporte: ${e.message}")
                    failureCount++
                    checkBroadcastComplete(successCount, failureCount, userEmails.size, onSuccess, onFailure)
                }
        }
    }

    /**
     * Envía un mensaje a un chat específico
     */
    private fun sendMessageToChat(
        chatId: String,
        adminEmail: String,
        adminName: String,
        content: String,
        totalUsers: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val message = Message(
            chatId = chatId,
            senderEmail = adminEmail,
            senderName = adminName,
            content = content,
            timestamp = Timestamp.now()
        )
        db.collection("messages")
            .add(message)
            .addOnSuccessListener {
                updateChatLastMessage(chatId, content)
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("AdminMessagesViewModel", "Error enviando mensaje: ${e.message}")
                onFailure()
            }
    }

    /**
     * Crea un chat de soporte y envía el mensaje
     */
    private fun createSupportChatAndSendMessage(
        adminEmail: String,
        adminName: String,
        userEmail: String,
        content: String,
        totalUsers: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val chat = Chat(
            user1Email = adminEmail,
            user1Name = adminName,
            user1Photo = "",
            user2Email = userEmail,
            user2Name = userEmail,
            user2Photo = "",
            chatType = "support",
            lastMessage = content
        )

        db.collection("chats")
            .add(chat)
            .addOnSuccessListener { docRef ->
                Log.d("AdminMessagesViewModel", "Chat de soporte creado: ${docRef.id}")
                sendMessageToChat(docRef.id, adminEmail, adminName, content, totalUsers, onSuccess, onFailure)
            }
            .addOnFailureListener { e ->
                Log.e("AdminMessagesViewModel", "Error creando chat de soporte: ${e.message}")
                onFailure()
            }
    }

    /**
     * Verifica si el broadcast está completo
     */
    private fun checkBroadcastComplete(
        successCount: Int,
        failureCount: Int,
        totalUsers: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (successCount + failureCount == totalUsers) {
            if (failureCount == 0) {
                _broadcastSent.value = true
                onSuccess()
            } else if (successCount > 0) {
                _broadcastSent.value = true
                onSuccess()
                Log.w("AdminMessagesViewModel", "Broadcast parcial: se enviaron $successCount de $totalUsers mensajes")
            } else {
                onFailure(Exception("Error enviando mensajes a todos los usuarios"))
            }
        }
    }

    /**
     * Actualiza el último mensaje del chat
     */
    private fun updateChatLastMessage(chatId: String, lastMessage: String) {
        db.collection("chats")
            .document(chatId)
            .update(
                "lastMessage", lastMessage,
                "lastMessageTimestamp", Timestamp.now()
            )
            .addOnFailureListener { e ->
                Log.e("AdminMessagesViewModel", "Error actualizando chat: ${e.message}")
            }
    }
}

