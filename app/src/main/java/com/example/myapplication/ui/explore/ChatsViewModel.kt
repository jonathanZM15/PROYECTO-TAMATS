package com.example.myapplication.ui.explore

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Chat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    private var chatsListener: ListenerRegistration? = null

    fun loadChats(currentUserEmail: String) {
        // Escuchar en tiempo real los chats donde el usuario es user1 o user2
        db.collection("chats")
            .whereEqualTo("user1Email", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatsViewModel", "Error cargando chats: ${e.message}", e)
                    _chats.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chatList = mutableListOf<Chat>()
                    for (doc in snapshot.documents) {
                        try {
                            val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                            if (chat != null) {
                                chatList.add(chat)
                            }
                        } catch (ex: Exception) {
                            Log.e("ChatsViewModel", "Error parseando chat: ${ex.message}", ex)
                        }
                    }

                    // También buscar chats donde el usuario es user2
                    db.collection("chats")
                        .whereEqualTo("user2Email", currentUserEmail)
                        .get()
                        .addOnSuccessListener { snapshot2 ->
                            for (doc in snapshot2.documents) {
                                try {
                                    val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                                    if (chat != null && !chatList.any { it.id == chat.id }) {
                                        chatList.add(chat)
                                    }
                                } catch (ex: Exception) {
                                    Log.e("ChatsViewModel", "Error parseando chat user2: ${ex.message}", ex)
                                }
                            }
                            // Separar chats de soporte (isPinned) de los demás
                            val supportChats = chatList.filter { it.isPinned && it.isSupportChat }.toMutableList()
                            val regularChats = chatList.filterNot { it.isPinned && it.isSupportChat }.toMutableList()

                            // Ordenar cada grupo por timestamp
                            supportChats.sortByDescending { it.lastMessageTimestamp.toDate() }
                            regularChats.sortByDescending { it.lastMessageTimestamp.toDate() }

                            // Combinar: soporte primero, luego los demás
                            val finalList = supportChats + regularChats
                            _chats.value = finalList
                            Log.d("ChatsViewModel", "Chats cargados: ${finalList.size}")
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("ChatsViewModel", "Error cargando chats como user2: ${e2.message}")
                            // Separar y ordenar aunque haya error
                            val supportChats = chatList.filter { it.isPinned && it.isSupportChat }.toMutableList()
                            val regularChats = chatList.filterNot { it.isPinned && it.isSupportChat }.toMutableList()

                            supportChats.sortByDescending { it.lastMessageTimestamp.toDate() }
                            regularChats.sortByDescending { it.lastMessageTimestamp.toDate() }

                            val finalList = supportChats + regularChats
                            _chats.value = finalList
                        }
                }
            }
    }

    fun createChat(
        user1Email: String,
        user1Name: String,
        user1Photo: String,
        user2Email: String,
        user2Name: String,
        user2Photo: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Verificar si el chat ya existe
        db.collection("chats")
            .whereEqualTo("user1Email", user1Email)
            .whereEqualTo("user2Email", user2Email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    // El chat ya existe
                    onSuccess(querySnapshot.documents[0].id)
                } else {
                    // Crear nuevo chat
                    val chat = Chat(
                        user1Email = user1Email,
                        user1Name = user1Name,
                        user1Photo = user1Photo,
                        user2Email = user2Email,
                        user2Name = user2Name,
                        user2Photo = user2Photo
                    )

                    db.collection("chats")
                        .add(chat)
                        .addOnSuccessListener { documentReference ->
                            Log.d("ChatsViewModel", "Chat creado exitosamente")
                            onSuccess(documentReference.id)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatsViewModel", "Error creando chat: ${e.message}")
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatsViewModel", "Error verificando chat existente: ${e.message}")
                onFailure(e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
    }
}

