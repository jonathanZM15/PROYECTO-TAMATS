package com.example.myapplication.ui.explore

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Chat
import com.example.myapplication.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp

class MessagesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _chat = MutableLiveData<Chat>()
    val chat: LiveData<Chat> = _chat

    private var messagesListener: ListenerRegistration? = null

    fun loadMessages(chatId: String) {
        // Escuchar en tiempo real los mensajes del chat
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessagesViewModel", "Error cargando mensajes: ${e.message}", e)
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
                            Log.e("MessagesViewModel", "Error parseando mensaje: ${ex.message}", ex)
                        }
                    }
                    _messages.value = messageList
                    Log.d("MessagesViewModel", "Mensajes cargados: ${messageList.size}")
                }
            }
    }

    fun loadChat(chatId: String) {
        db.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessagesViewModel", "Error cargando chat: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val chat = snapshot.toObject(Chat::class.java)?.copy(id = snapshot.id)
                        if (chat != null) {
                            _chat.value = chat
                            Log.d("MessagesViewModel", "Chat cargado: ${chat.id}")
                        }
                    } catch (ex: Exception) {
                        Log.e("MessagesViewModel", "Error parseando chat: ${ex.message}", ex)
                    }
                }
            }
    }

    fun sendMessage(
        chatId: String,
        senderEmail: String,
        senderName: String,
        content: String,
        imageUrl: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = Message(
            chatId = chatId,
            senderEmail = senderEmail,
            senderName = senderName,
            content = content,
            imageUrl = imageUrl,
            type = if (imageUrl != null) "image" else "text"
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Actualizar el último mensaje en el chat
                val updateData = hashMapOf<String, Any>(
                    "lastMessage" to content,
                    "lastMessageTimestamp" to Timestamp.now()
                )
                db.collection("chats").document(chatId)
                    .update(updateData)
                    .addOnSuccessListener {
                        Log.d("MessagesViewModel", "Mensaje enviado exitosamente")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("MessagesViewModel", "Error actualizando chat: ${e.message}")
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MessagesViewModel", "Error enviando mensaje: ${e.message}")
                onFailure(e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}

