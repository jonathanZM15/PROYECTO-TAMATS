package com.example.myapplication.admin.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Chat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminChatsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _adminChats = MutableLiveData<List<Chat>>()
    val adminChats: LiveData<List<Chat>> = _adminChats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _searchResults = MutableLiveData<List<Chat>>()
    val searchResults: LiveData<List<Chat>> = _searchResults

    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLastPageReached = false
    private val pageSize = 10L
    private var allChatsCache = mutableListOf<Chat>()

    /**
     * Carga los primeros 10 chats de soporte
     */
    fun loadSupportChats(adminEmail: String) {
        // Primero crear chats para usuarios que no los tengan
        createMissingSupportChats(adminEmail) {
            // Después cargar los chats
            _isLoading.value = true
            lastDocument = null
            isLastPageReached = false
            allChatsCache.clear()

            // Usar consulta sin índice compuesto: filtrar en memoria
            db.collection("chats")
                .whereEqualTo("user1Email", adminEmail)
                .get()
                .addOnSuccessListener { snapshot ->
                    val allChats = mutableListOf<Chat>()

                    for (doc in snapshot.documents) {
                        try {
                            val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                            if (chat != null && chat.chatType == "support") {
                                allChats.add(chat)
                            }
                        } catch (ex: Exception) {
                            Log.e("AdminChatsViewModel", "Error parseando chat: ${ex.message}", ex)
                        }
                    }

                    // Ordenar por timestamp en memoria
                    allChats.sortByDescending { it.lastMessageTimestamp.toDate() }

                    // Aplicar paginación
                    val chatList = if (allChats.size > pageSize) {
                        allChats.subList(0, pageSize.toInt())
                    } else {
                        allChats
                    }

                    isLastPageReached = allChats.size <= pageSize
                    if (allChats.size > pageSize) {
                        lastDocument = null // Usar índice manual para paginación
                    }

                    allChatsCache.addAll(allChats)
                    _adminChats.value = chatList
                    _isLoading.value = false
                    Log.d("AdminChatsViewModel", "Chats de soporte cargados: ${chatList.size} de ${allChats.size}")
                }
                .addOnFailureListener { e ->
                    Log.e("AdminChatsViewModel", "Error cargando chats de soporte: ${e.message}")
                    _adminChats.value = emptyList()
                    _isLoading.value = false
                }
        }
    }

    /**
     * Crea chats de soporte para usuarios que no los tengan
     */
    private fun createMissingSupportChats(adminEmail: String, onComplete: () -> Unit) {
        // Obtener todos los usuarios
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { usersSnapshot ->
                val usersList = mutableListOf<String>()
                for (doc in usersSnapshot.documents) {
                    val email = doc.getString("email") ?: ""
                    val name = doc.getString("name") ?: "Usuario"
                    val photo = doc.getString("photo") ?: ""
                    if (email.isNotEmpty() && email != adminEmail) {
                        usersList.add(email)
                        // Verificar si ya existe chat de soporte
                        checkAndCreateSupportChat(adminEmail, email, name, photo)
                    }
                }
                Log.d("AdminChatsViewModel", "Procesando ${usersList.size} usuarios")
                // Esperar un poco para que se creen los chats
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onComplete()
                }, 1000)
            }
            .addOnFailureListener { e ->
                Log.e("AdminChatsViewModel", "Error obteniendo usuarios: ${e.message}")
                onComplete()
            }
    }

    /**
     * Verifica si existe chat de soporte y lo crea si no existe
     */
    private fun checkAndCreateSupportChat(adminEmail: String, userEmail: String, userName: String, userPhoto: String) {
        db.collection("chats")
            .whereEqualTo("user1Email", adminEmail)
            .whereEqualTo("user2Email", userEmail)
            .whereEqualTo("chatType", "support")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isEmpty()) {
                    // No existe, crear nuevo chat de soporte
                    val chat = Chat(
                        user1Email = adminEmail,
                        user1Name = "Soporte",
                        user1Photo = "",
                        user2Email = userEmail,
                        user2Name = userName,
                        user2Photo = userPhoto,
                        chatType = "support",
                        lastMessage = "¡Bienvenido! Este es tu chat de soporte."
                    )

                    db.collection("chats")
                        .add(chat)
                        .addOnSuccessListener {
                            Log.d("AdminChatsViewModel", "Chat de soporte creado para: $userEmail")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdminChatsViewModel", "Error creando chat de soporte: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminChatsViewModel", "Error verificando chat: ${e.message}")
            }
    }

    /**
     * Carga más chats al hacer scroll (lazy loading)
     */
    fun loadMoreChats(adminEmail: String) {
        if (isLastPageReached || _isLoading.value == true) return

        _isLoading.value = true

        // Obtener el índice del siguiente lote desde el caché
        val currentCount = _adminChats.value?.size ?: 0
        val startIndex = currentCount
        val endIndex = (startIndex + pageSize).toInt()

        if (startIndex >= allChatsCache.size) {
            isLastPageReached = true
            _isLoading.value = false
            return
        }

        val moreChats = if (endIndex > allChatsCache.size) {
            allChatsCache.subList(startIndex, allChatsCache.size)
        } else {
            allChatsCache.subList(startIndex, endIndex)
        }

        isLastPageReached = endIndex >= allChatsCache.size

        // Agregar los nuevos chats a la lista actual
        val currentList = _adminChats.value?.toMutableList() ?: mutableListOf()
        currentList.addAll(moreChats)
        _adminChats.value = currentList
        _isLoading.value = false

        Log.d("AdminChatsViewModel", "Más chats cargados: ${moreChats.size}")
    }

    /**
     * Busca chats por nombre de usuario
     */
    fun searchChats(adminEmail: String, query: String) {
        if (query.isEmpty()) {
            _searchResults.value = allChatsCache
            return
        }

        val filtered = allChatsCache.filter { chat ->
            chat.user2Name.contains(query, ignoreCase = true) ||
            chat.user2Email.contains(query, ignoreCase = true)
        }
        _searchResults.value = filtered
    }

    /**
     * Obtiene un chat específico entre admin y usuario
     */
    fun getSupportChat(adminEmail: String, userEmail: String, onComplete: (Chat?) -> Unit) {
        db.collection("chats")
            .whereEqualTo("user1Email", adminEmail)
            .whereEqualTo("user2Email", userEmail)
            .whereEqualTo("chatType", "support")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val chat = snapshot.documents[0].toObject(Chat::class.java)?.copy(id = snapshot.documents[0].id)
                    onComplete(chat)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminChatsViewModel", "Error obteniendo chat: ${e.message}")
                onComplete(null)
            }
    }

    /**
     * Crea un chat de soporte entre admin y usuario
     */
    fun createSupportChat(
        adminEmail: String,
        adminName: String,
        adminPhoto: String,
        userEmail: String,
        userName: String,
        userPhoto: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chat = Chat(
            user1Email = adminEmail,
            user1Name = adminName,
            user1Photo = adminPhoto,
            user2Email = userEmail,
            user2Name = userName,
            user2Photo = userPhoto,
            chatType = "support"
        )

        db.collection("chats")
            .add(chat)
            .addOnSuccessListener { documentReference ->
                Log.d("AdminChatsViewModel", "Chat de soporte creado: ${documentReference.id}")
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.e("AdminChatsViewModel", "Error creando chat de soporte: ${e.message}")
                onFailure(e)
            }
    }

    /**
     * Actualiza el último mensaje del chat
     */
    fun updateLastMessage(chatId: String, lastMessage: String) {
        db.collection("chats")
            .document(chatId)
            .update(
                "lastMessage", lastMessage,
                "lastMessageTimestamp", com.google.firebase.Timestamp.now()
            )
            .addOnFailureListener { e ->
                Log.e("AdminChatsViewModel", "Error actualizando último mensaje: ${e.message}")
            }
    }
}

