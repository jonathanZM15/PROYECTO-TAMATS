package com.example.myapplication.admin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.admin.models.AdminUser
import com.example.myapplication.admin.models.AdminUserStatus
import com.example.myapplication.cloud.FirebaseService
import kotlinx.coroutines.launch
import android.util.Log
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para el panel de administración
 * Simula operaciones de base de datos con datos de prueba
 */
class AdminViewModel : ViewModel() {

    companion object {
        private const val TAG = "AdminViewModel"
    }

    // LiveData para la UI
    private val _users = MutableLiveData<List<AdminUser>>()

    private val _filteredUsers = MutableLiveData<List<AdminUser>>()
    val filteredUsers: LiveData<List<AdminUser>> = _filteredUsers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _userStats = MutableLiveData<Map<String, Int>>()
    val userStats: LiveData<Map<String, Int>> = _userStats

    // Datos simulados
    private var allUsers = mutableListOf<AdminUser>()
    private var currentSearchQuery = ""

    init {
        Log.d(TAG, "AdminViewModel inicializado")
        // La carga de datos se hace en loadUsers() cuando se inicia el fragment
    }

    /**
     * Carga la lista de usuarios desde Firebase Firestore
     */
    fun loadUsers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando usuarios desde Firebase...")
                _loading.value = true
                _error.value = null

                // Llamar a Firebase para obtener usuarios
                FirebaseService.loadAllUsersForAdmin { usersData ->
                    allUsers = convertFirebaseUsersToAdminUsers(usersData).toMutableList()

                    _users.value = allUsers.toList()

                    // Aplicar filtro actual si existe
                    if (currentSearchQuery.isNotEmpty()) {
                        filterUsers(currentSearchQuery)
                    } else {
                        _filteredUsers.value = allUsers.toList()
                    }

                    updateStats()
                    _loading.value = false
                    Log.d(TAG, "Usuarios cargados desde Firebase: ${allUsers.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuarios: ${e.message}")
                _error.value = "Error al cargar usuarios: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Convierte datos de Firebase a AdminUser
     */
    private fun convertFirebaseUsersToAdminUsers(firebaseUsers: List<Map<String, Any>>): List<AdminUser> {
        return firebaseUsers.mapNotNull { userData ->
            try {
                val id = userData["firebaseDocId"]?.toString() ?: return@mapNotNull null
                val name = userData["name"]?.toString() ?: ""
                val email = userData["email"]?.toString() ?: ""
                val blocked = userData["blocked"] as? Boolean ?: false
                val suspended = userData["suspended"] as? Boolean ?: false
                val suspensionEnd = (userData["suspensionEnd"] as? Number)?.toLong()
                val photo = userData["photo"]?.toString() ?: ""

                // Convertir timestamp de Firestore a fecha formateada
                val joinDate = if (userData.containsKey("joinDate")) {
                    formatTimestampToString(userData["joinDate"])
                } else {
                    "N/A"
                }

                val lastLogin = if (userData.containsKey("lastLogin")) {
                    formatTimestampToString(userData["lastLogin"])
                } else {
                    "N/A"
                }

                val posts = (userData["posts"] as? Number)?.toInt() ?: 0

                AdminUser(
                    id = id,
                    name = name,
                    email = email,
                    blocked = blocked,
                    suspended = suspended,
                    suspensionEnd = suspensionEnd,
                    joinDate = joinDate,
                    lastLogin = lastLogin,
                    posts = posts,
                    profileImageUrl = photo
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error convirtiendo usuario de Firebase: ${e.message}")
                null
            }
        }
    }

    /**
     * Convierte Timestamp de Firestore a String formateado
     */
    private fun formatTimestampToString(timestamp: Any?): String {
        return try {
            val millis = when (timestamp) {
                is com.google.firebase.Timestamp -> timestamp.toDate().time
                is Number -> timestamp.toLong()
                else -> return "N/A"
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateFormat.format(Date(millis))
        } catch (e: Exception) {
            Log.w(TAG, "Error formateando timestamp: ${e.message}")
            "N/A"
        }
    }

    /**
     * Busca usuarios por nombre o email
     */
    fun searchUsers(query: String) {
        Log.d(TAG, "Buscando usuarios con: '$query'")
        currentSearchQuery = query
        filterUsers(query)
    }

    /**
     * Filtra la lista de usuarios
     */
    private fun filterUsers(query: String) {
        val filtered = if (query.isEmpty()) {
            allUsers.toList()
        } else {
            allUsers.filter { user ->
                user.name.contains(query, ignoreCase = true) || user.email.contains(query, ignoreCase = true)
            }
        }
        _filteredUsers.value = filtered
        Log.d(TAG, "Usuarios filtrados: ${filtered.size}")
    }

    /**
     * Alterna el estado de bloqueo de un usuario
     */
    fun toggleUserStatus(user: AdminUser) {
        if (user.blocked) {
            unblockUser(user.id)
        } else {
            blockUser(user.id)
        }
    }

    /**
     * Bloquea un usuario (sincroniza con Firebase)
     */
    fun blockUser(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Bloqueando usuario: $userId")
                _loading.value = true

                // Actualizar en Firebase
                FirebaseService.blockUser(userId) { success, message ->
                    if (success) {
                        // Actualizar en la lista local
                        val userIndex = allUsers.indexOfFirst { it.id == userId }
                        if (userIndex != -1) {
                            val updatedUser = allUsers[userIndex].copy(
                                blocked = true,
                                suspended = false,
                                suspensionEnd = null
                            )
                            allUsers[userIndex] = updatedUser

                            _users.value = allUsers.toList()
                            filterUsers(currentSearchQuery)
                            updateStats()
                            _message.value = message
                            Log.d(TAG, "Usuario $userId bloqueado")
                        }
                    } else {
                        _error.value = message
                        Log.e(TAG, "Error bloqueando usuario: $message")
                    }
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al bloquear usuario: ${e.message}")
                _error.value = "Error al bloquear usuario: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Desbloquea un usuario (sincroniza con Firebase)
     */
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Desbloqueando usuario: $userId")
                _loading.value = true

                // Actualizar en Firebase
                FirebaseService.unblockUser(userId) { success, message ->
                    if (success) {
                        val userIndex = allUsers.indexOfFirst { it.id == userId }
                        if (userIndex != -1) {
                            val updatedUser = allUsers[userIndex].copy(blocked = false)
                            allUsers[userIndex] = updatedUser

                            _users.value = allUsers.toList()
                            filterUsers(currentSearchQuery)
                            updateStats()
                            _message.value = message
                            Log.d(TAG, "Usuario $userId desbloqueado")
                        }
                    } else {
                        _error.value = message
                        Log.e(TAG, "Error desbloqueando usuario: $message")
                    }
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al desbloquear usuario: ${e.message}")
                _error.value = "Error al desbloquear usuario: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Suspende un usuario temporalmente (sincroniza con Firebase)
     */
    fun suspendUser(userId: String, days: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Suspendiendo usuario $userId por $days días")
                _loading.value = true

                // Actualizar en Firebase
                FirebaseService.suspendUser(userId, days) { success, message ->
                    if (success) {
                        val userIndex = allUsers.indexOfFirst { it.id == userId }
                        if (userIndex != -1) {
                            val suspensionEnd = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                            val updatedUser = allUsers[userIndex].copy(
                                suspended = true,
                                suspensionEnd = suspensionEnd,
                                blocked = false
                            )
                            allUsers[userIndex] = updatedUser

                            _users.value = allUsers.toList()
                            filterUsers(currentSearchQuery)
                            updateStats()
                            _message.value = message
                            Log.d(TAG, "Usuario $userId suspendido por $days días")
                        }
                    } else {
                        _error.value = message
                        Log.e(TAG, "Error suspendiendo usuario: $message")
                    }
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al suspender usuario: ${e.message}")
                _error.value = "Error al suspender usuario: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Remueve la suspensión de un usuario (sincroniza con Firebase)
     */
    fun removeSuspension(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Removiendo suspensión del usuario: $userId")
                _loading.value = true

                // Actualizar en Firebase
                FirebaseService.removeSuspension(userId) { success, message ->
                    if (success) {
                        val userIndex = allUsers.indexOfFirst { it.id == userId }
                        if (userIndex != -1) {
                            val updatedUser = allUsers[userIndex].copy(
                                suspended = false,
                                suspensionEnd = null
                            )
                            allUsers[userIndex] = updatedUser

                            _users.value = allUsers.toList()
                            filterUsers(currentSearchQuery)
                            updateStats()
                            _message.value = message
                            Log.d(TAG, "Suspensión removida del usuario $userId")
                        }
                    } else {
                        _error.value = message
                        Log.e(TAG, "Error removiendo suspensión: $message")
                    }
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al remover suspensión: ${e.message}")
                _error.value = "Error al remover suspensión: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Elimina un usuario permanentemente (sincroniza con Firebase)
     */
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Eliminando usuario permanentemente: $userId")
                _loading.value = true

                // Actualizar en Firebase
                FirebaseService.deleteUser(userId) { success, message ->
                    if (success) {
                        val userIndex = allUsers.indexOfFirst { it.id == userId }
                        if (userIndex != -1) {
                            val deletedUser = allUsers[userIndex]
                            allUsers.removeAt(userIndex)

                            _users.value = allUsers.toList()
                            filterUsers(currentSearchQuery)
                            updateStats()
                            _message.value = "Usuario ${deletedUser.name} eliminado permanentemente"
                            Log.d(TAG, "Usuario $userId eliminado permanentemente")
                        }
                    } else {
                        _error.value = message
                        Log.e(TAG, "Error eliminando usuario: $message")
                    }
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar usuario: ${e.message}")
                _error.value = "Error al eliminar usuario: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Actualiza las estadísticas de usuarios
     */
    private fun updateStats() {
        val stats = mutableMapOf<String, Int>()
        stats["total"] = allUsers.size
        stats["active"] = allUsers.count { it.getStatus() is AdminUserStatus.ACTIVE }
        stats["blocked"] = allUsers.count { it.getStatus() is AdminUserStatus.BLOCKED }
        stats["suspended"] = allUsers.count { it.getStatus() is AdminUserStatus.SUSPENDED }

        _userStats.value = stats
        Log.d(TAG, "Estadísticas actualizadas: $stats")
    }

    /**
     * Busca un usuario por ID
     */
    fun getUserById(userId: String): AdminUser? {
        return allUsers.find { it.id == userId }
    }

    /**
     * Limpia mensajes de error y éxito
     */
    fun clearMessages() {
        _error.value = null
        _message.value = null
    }

    /**
     * Recarga la lista de usuarios
     */
    @Suppress("unused")
    fun refreshUsers() {
        Log.d(TAG, "Recargando usuarios...")
        loadUsers()
    }

    /**
     * Agrega un nuevo usuario (para testing)
     */
    fun addSampleUser() {
        val newUser = AdminUser(
            id = "new_${System.currentTimeMillis()}",
            name = "Usuario Nuevo",
            email = "nuevo@email.com",
            blocked = false,
            suspended = false,
            suspensionEnd = null,
            joinDate = "Hoy",
            lastLogin = "Ahora",
            posts = 0,
            profileImageUrl = ""
        )
        allUsers.add(newUser)
        _users.value = allUsers.toList()
        filterUsers(currentSearchQuery)
        updateStats()
        _message.value = "Usuario de prueba agregado"
    }
}
