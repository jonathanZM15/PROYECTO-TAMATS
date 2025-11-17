package com.example.myapplication.cloud

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.myapplication.model.UsuarioEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FirebaseService {
    // Instancia de Firestore
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // --- Estado para panel admin en realtime ---
    private var adminUsersListener: ListenerRegistration? = null
    private var adminProfilesListener: ListenerRegistration? = null
    private val adminUsersMap = mutableMapOf<String, MutableMap<String, Any>>()
    private val adminProfilesMap = mutableMapOf<String, Map<String, Any>>()
    private var adminCallback: ((List<Map<String, Any>>) -> Unit)? = null

    // Fusiona users + profiles y notifica al callback
    private fun mergeAndNotifyAdmin() {
        try {
            val merged = adminUsersMap.values.map { u ->
                val mergedMap = u.toMutableMap()
                val email = u["email"]?.toString() ?: ""
                val docIdFromEmail = sanitizeDocId(email)
                val profileKey = docIdFromEmail ?: (u["firebaseDocId"]?.toString())
                val profile = profileKey?.let { adminProfilesMap[it] }

                if (profile != null && profile.isNotEmpty()) {
                    profile["name"]?.let { if (it.toString().isNotEmpty()) mergedMap["name"] = it }
                    profile["photo"]?.let { if (it.toString().isNotEmpty()) mergedMap["photo"] = it }
                    profile["city"]?.let { if (it.toString().isNotEmpty()) mergedMap["city"] = it }
                    profile["description"]?.let { if (it.toString().isNotEmpty()) mergedMap["description"] = it }
                }

                // Campos mínimos
                if (!mergedMap.containsKey("name")) mergedMap["name"] = ""
                if (!mergedMap.containsKey("email")) mergedMap["email"] = ""
                if (!mergedMap.containsKey("photo")) mergedMap["photo"] = ""
                if (!mergedMap.containsKey("blocked")) mergedMap["blocked"] = false
                if (!mergedMap.containsKey("suspended")) mergedMap["suspended"] = false
                if (!mergedMap.containsKey("posts")) mergedMap["posts"] = 0
                if (!mergedMap.containsKey("joinDate")) mergedMap["joinDate"] = ""
                if (!mergedMap.containsKey("lastLogin")) mergedMap["lastLogin"] = ""

                mergedMap.toMap()
            }

            Log.d("FirebaseService", "Notificando merged users: ${merged.size}")
            adminCallback?.invoke(merged)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al mergear usuarios y profiles: ${e.message}", e)
            adminCallback?.invoke(emptyList())
        }
    }

    /**
     * ADMIN: Suscribe en tiempo real a 'usuarios' y 'userProfiles' y llama callback con la lista fusionada.
     * Si ya hay listeners activos, sólo reemplaza el callback y notifica con el estado actual.
     */
    fun loadAllUsersForAdmin(callback: (List<Map<String, Any>>) -> Unit) {
        adminCallback = callback

        // Si ya están los listeners, notificar inmediatamente
        if (adminUsersListener != null || adminProfilesListener != null) {
            mergeAndNotifyAdmin()
            return
        }

        adminUsersListener = db.collection("usuarios")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseService", "Error al escuchar 'usuarios': ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (docChange in snapshot.documentChanges) {
                        val doc = docChange.document
                        val docId = doc.id
                        val data = doc.data

                        when (docChange.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                if (data != null) {
                                    adminUsersMap[docId] = data.toMutableMap()
                                    adminUsersMap[docId]?.put("firebaseDocId", docId)
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                adminUsersMap.remove(docId)
                            }
                        }
                    }
                    mergeAndNotifyAdmin()
                }
            }

        adminProfilesListener = db.collection("userProfiles")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseService", "Error al escuchar 'userProfiles': ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (docChange in snapshot.documentChanges) {
                        val doc = docChange.document
                        val docId = doc.id
                        val data = doc.data

                        when (docChange.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                if (data != null) adminProfilesMap[docId] = data
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                adminProfilesMap.remove(docId)
                            }
                        }
                    }
                    mergeAndNotifyAdmin()
                }
            }
    }

    /**
     * Detiene listeners del admin y limpia el estado.
     */
    fun stopAdminListeners() {
        try { adminUsersListener?.remove() } catch (_: Exception) {}
        adminUsersListener = null
        try { adminProfilesListener?.remove() } catch (_: Exception) {}
        adminProfilesListener = null
        adminUsersMap.clear()
        adminProfilesMap.clear()
        adminCallback = null
    }

    // ------------------ Resto de utilidades del servicio ------------------

    private fun getStorageBucketUrl(): String {
        return try {
            val cfg = FirebaseApp.getInstance().options.storageBucket
            if (!cfg.isNullOrEmpty()) {
                var bucket = cfg
                if (bucket.endsWith("firebasestorage.app")) bucket = bucket.replace("firebasestorage.app", "appspot.com")
                if (!bucket.startsWith("gs://")) bucket = "gs://$bucket"
                bucket
            } else {
                "gs://tamats-aea71.appspot.com"
            }
        } catch (e: Exception) {
            Log.w("FirebaseService", "Error leyendo storageBucket: ${e.message}")
            "gs://tamats-aea71.appspot.com"
        }
    }

    fun guardarUsuario(usuario: UsuarioEntity) {
        if (esContrasenaPlana(usuario.passwordHash)) {
            Log.e("FirebaseService", "ERROR: Contraseña sin cifrar. Use EncryptionUtil.encryptPassword().")
            return
        }

        val data = mapOf(
            "name" to usuario.name,
            "email" to usuario.email,
            "birthDate" to usuario.birthDate,
            "gender" to usuario.gender,
            "passwordHash" to usuario.passwordHash,
            "blocked" to false,
            "suspended" to false,
            "suspensionEnd" to null,
            "posts" to 0,
            "photo" to "",
            "joinDate" to Timestamp.now(),
            "lastLogin" to Timestamp.now()
        )

        val docId = usuario.email.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        db.collection("usuarios").document(docId).set(data)
            .addOnSuccessListener { Log.d("FirebaseService", "Usuario guardado: $docId") }
            .addOnFailureListener { e -> Log.e("FirebaseService", "Error guardando usuario: ${e.message}", e) }
    }

    private fun esContrasenaPlana(passwordHash: String): Boolean {
        return passwordHash.length == 8 && passwordHash.matches(Regex("[a-zA-Z0-9]*"))
    }

    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection("usuarios").get()
            .addOnSuccessListener { result ->
                val lista = result.map { doc ->
                    UsuarioEntity(
                        id = 0,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        birthDate = doc.getString("birthDate") ?: "",
                        gender = doc.getString("gender") ?: "",
                        passwordHash = doc.getString("passwordHash") ?: ""
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error al obtener usuarios: ${e.message}", e)
                callback(emptyList())
            }
    }

    fun findUserByEmail(email: String, callback: (UsuarioEntity?) -> Unit) {
        db.collection("usuarios").whereEqualTo("email", email).limit(1).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.first()
                    val user = UsuarioEntity(
                        id = 0,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        birthDate = doc.getString("birthDate") ?: "",
                        gender = doc.getString("gender") ?: "",
                        passwordHash = doc.getString("passwordHash") ?: ""
                    )
                    callback(user)
                } else {
                    val docId = email.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    db.collection("usuarios").document(docId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val user = UsuarioEntity(
                                    id = 0,
                                    name = doc.getString("name") ?: "",
                                    email = doc.getString("email") ?: "",
                                    birthDate = doc.getString("birthDate") ?: "",
                                    gender = doc.getString("gender") ?: "",
                                    passwordHash = doc.getString("passwordHash") ?: ""
                                )
                                callback(user)
                            } else callback(null)
                        }
                        .addOnFailureListener { e -> Log.e("FirebaseService", "Error buscando por ID: ${e.message}", e); callback(null) }
                }
            }
            .addOnFailureListener { e -> Log.e("FirebaseService", "Error buscando por email: ${e.message}", e); callback(null) }
    }

    @Suppress("unused")
    fun uploadImage(uri: Uri, context: Context, callback: (String?) -> Unit) {
        try {
            val base64 = uriToBase64(context, uri)
            callback(base64)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error uploadImage: ${e.message}", e)
            callback(null)
        }
    }

    @Suppress("unused")
    suspend fun uploadImageSuspend(uri: Uri, context: Context): String? = suspendCancellableCoroutine { cont ->
        try {
            val b64 = uriToBase64(context, uri)
            if (cont.isActive) cont.resume(b64)
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private fun uriToBase64(context: Context, uri: Uri, maxBytes: Int = 900 * 1024): String? {
        var input: InputStream? = null
        try {
            input = context.contentResolver.openInputStream(uri) ?: return null
            val buffer = ByteArray(8192)
            val baos = ByteArrayOutputStream()
            var read: Int
            while (true) {
                read = input.read(buffer)
                if (read <= 0) break
                baos.write(buffer, 0, read)
            }
            val rawBytes = baos.toByteArray()
            val directB64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            if (directB64.toByteArray(Charsets.UTF_8).size <= maxBytes) return directB64
        } catch (e: Exception) {
            Log.w("FirebaseService", "Lectura directa falló: ${e.message}")
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }

        // Intentar con Bitmap y compresión (omito detalles para mantener legible)
        try {
            var bitmapInput: InputStream? = null
            val optionsBounds = android.graphics.BitmapFactory.Options()
            optionsBounds.inJustDecodeBounds = true
            bitmapInput = context.contentResolver.openInputStream(uri) ?: return null
            android.graphics.BitmapFactory.decodeStream(bitmapInput, null, optionsBounds)
            bitmapInput.close()

            var sampleSize = 1
            val desiredMaxDim = 1280
            val width = optionsBounds.outWidth
            val height = optionsBounds.outHeight
            if (width > desiredMaxDim || height > desiredMaxDim) {
                val scale = Math.max(width / desiredMaxDim, height / desiredMaxDim)
                var s = 1
                while (s < scale) s *= 2
                sampleSize = s
            }

            val optionsDecode = android.graphics.BitmapFactory.Options()
            optionsDecode.inSampleSize = sampleSize
            bitmapInput = context.contentResolver.openInputStream(uri) ?: return null
            val bmp = android.graphics.BitmapFactory.decodeStream(bitmapInput, null, optionsDecode)
            bitmapInput.close()
            if (bmp == null) return null

            val baos2 = ByteArrayOutputStream()
            var quality = 85
            while (quality >= 30) {
                baos2.reset()
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos2)
                val compressed = baos2.toByteArray()
                val b64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                if (b64.toByteArray(Charsets.UTF_8).size <= maxBytes) return b64
                quality -= 10
            }

            // Si no entra, reducir dimensiones iterativamente
            var currentWidth = bmp.width
            var currentHeight = bmp.height
            var reducedBmp: android.graphics.Bitmap? = bmp
            while (true) {
                val newW = (currentWidth * 0.75).toInt().coerceAtLeast(100)
                val newH = (currentHeight * 0.75).toInt().coerceAtLeast(100)
                if (newW >= currentWidth || newH >= currentHeight) break
                val scaled = android.graphics.Bitmap.createScaledBitmap(reducedBmp!!, newW, newH, true)
                if (reducedBmp !== bmp) try { reducedBmp.recycle() } catch (_: Exception) {}
                reducedBmp = scaled
                currentWidth = reducedBmp.width
                currentHeight = reducedBmp.height

                baos2.reset()
                reducedBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos2)
                val compressed = baos2.toByteArray()
                val b64try = Base64.encodeToString(compressed, Base64.NO_WRAP)
                if (b64try.toByteArray(Charsets.UTF_8).size <= maxBytes) return b64try
                if (currentWidth <= 200 || currentHeight <= 200) break
            }

            return null
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error procesando bitmap: ${e.message}", e)
            return null
        }
    }

    private fun sanitizeDocId(id: String?): String? {
        if (id.isNullOrBlank()) return null
        return id.replace("/", "_")
    }

    fun saveUserProfile(email: String, profileData: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("usuarios").whereEqualTo("email", email).limit(1).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val docId = sanitizeDocId(email)
                    if (docId != null) {
                        db.collection("userProfiles").document(docId).set(profileData)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { e -> Log.e("FirebaseService","Error saveUserProfile: ${e.message}", e); callback(false) }
                    } else {
                        db.collection("userProfiles").add(profileData)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { e -> Log.e("FirebaseService","Error saveUserProfile auto-id: ${e.message}", e); callback(false) }
                    }
                } else {
                    val doc = documents.first()
                    val existingId = doc.id
                    val existingCollection = doc.reference.parent.id
                    val updates = profileData.mapValues { it.value as Any }
                    db.collection(existingCollection).document(existingId)
                        .update(updates)
                        .addOnSuccessListener { callback(true) }
                        .addOnFailureListener { e -> Log.e("FirebaseService","Error update profile: ${e.message}", e); callback(false) }
                }
            }
            .addOnFailureListener { e -> Log.e("FirebaseService","Error finding user for saveUserProfile: ${e.message}", e); callback(false) }
    }

    fun getUserProfile(email: String, callback: (Map<String, Any>?) -> Unit) {
        val docId = sanitizeDocId(email)
        if (docId != null) {
            db.collection("userProfiles").document(docId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) callback(document.data) else {
                        db.collection("usuarios").whereEqualTo("email", email).limit(1).get()
                            .addOnSuccessListener { docs -> if (!docs.isEmpty) callback(docs.first().data) else callback(null) }
                            .addOnFailureListener { e -> Log.e("FirebaseService","Error getUserProfile usuarios: ${e.message}", e); callback(null) }
                    }
                }
                .addOnFailureListener { e -> Log.e("FirebaseService","Error getUserProfile userProfiles: ${e.message}", e); callback(null) }
        } else {
            db.collection("usuarios").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener { docs -> if (!docs.isEmpty) callback(docs.first().data) else callback(null) }
                .addOnFailureListener { e -> Log.e("FirebaseService","Error getUserProfile usuarios fallback: ${e.message}", e); callback(null) }
        }
    }

    fun getCurrentUserEmail(): String {
        return try {
            val auth = FirebaseAuth.getInstance()
            auth.currentUser?.email ?: ""
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error obteniendo email: ${e.message}", e)
            ""
        }
    }

    fun logout(context: Context? = null) {
        try {
            FirebaseAuth.getInstance().signOut()
            context?.let {
                val prefs = it.getSharedPreferences("user_data", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error logout: ${e.message}", e)
        }
    }

    /**
     * ADMIN: Bloquea un usuario (marca blocked = true, suspended = false)
     */
    fun blockUser(userId: String, callback: (Boolean, String) -> Unit) {
        val updates = mapOf<String, Any>(
            "blocked" to true,
            "suspended" to false,
            // eliminar el campo suspensionEnd si existía
            "suspensionEnd" to FieldValue.delete()
        )
        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario $userId bloqueado en Firestore")
                callback(true, "Usuario bloqueado exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error bloqueando usuario $userId: ${e.message}", e)
                callback(false, "Error al bloquear usuario: ${e.message}")
            }
    }

    /**
     * ADMIN: Desbloquea un usuario (marca blocked = false)
     */
    fun unblockUser(userId: String, callback: (Boolean, String) -> Unit) {
        val updates = mapOf<String, Any>("blocked" to false)
        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario $userId desbloqueado en Firestore")
                callback(true, "Usuario desbloqueado exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error desbloqueando usuario $userId: ${e.message}", e)
                callback(false, "Error al desbloquear usuario: ${e.message}")
            }
    }

    /**
     * ADMIN: Suspende un usuario por X días (marca suspended = true y suspensionEnd en millis)
     */
    fun suspendUser(userId: String, days: Int, callback: (Boolean, String) -> Unit) {
        val suspensionEnd = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)
        val updates = mapOf<String, Any>(
            "suspended" to true,
            "suspensionEnd" to suspensionEnd,
            "blocked" to false
        )
        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario $userId suspendido por $days días")
                callback(true, "Usuario suspendido por $days días")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error suspendiendo usuario $userId: ${e.message}", e)
                callback(false, "Error al suspender usuario: ${e.message}")
            }
    }

    /**
     * ADMIN: Remueve la suspensión de un usuario
     */
    fun removeSuspension(userId: String, callback: (Boolean, String) -> Unit) {
        val updates = mapOf<String, Any>(
            "suspended" to false,
            // eliminar el campo suspensionEnd en lugar de asignar null
            "suspensionEnd" to FieldValue.delete()
        )
        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Suspensión removida para usuario $userId")
                callback(true, "Suspensión removida exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error removiendo suspensión $userId: ${e.message}", e)
                callback(false, "Error al remover suspensión: ${e.message}")
            }
    }

    /**
     * ADMIN: Elimina un usuario de la colección 'usuarios'
     */
    fun deleteUser(userId: String, callback: (Boolean, String) -> Unit) {
        db.collection("usuarios").document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario $userId eliminado de Firestore")
                callback(true, "Usuario eliminado exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error eliminando usuario $userId: ${e.message}", e)
                callback(false, "Error al eliminar usuario: ${e.message}")
            }
    }
    
    /**
     * Actualiza la contraseña de un usuario en Firebase
     */
    fun actualizarContrasena(email: String, newPasswordHash: String, callback: ((Boolean, String) -> Unit)? = null) {
        val docId = email.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        
        db.collection("usuarios").document(docId)
            .update("passwordHash", newPasswordHash)
            .addOnSuccessListener {
                Log.d("FirebaseService", "✅ Contraseña actualizada para: $email")
                callback?.invoke(true, "Contraseña actualizada exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "❌ Error actualizando contraseña: ${e.message}", e)
                callback?.invoke(false, "Error al actualizar contraseña: ${e.message}")
            }
    }
}
