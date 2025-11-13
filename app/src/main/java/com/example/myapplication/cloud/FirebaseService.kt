package com.example.myapplication.cloud

import android.net.Uri
import android.util.Log
import com.example.myapplication.model.UsuarioEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FirebaseService {
    // Evitamos mantener una referencia estática a FirebaseFirestore (posible leak de contexto)
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // Helper: obtiene la URL gs:// del bucket de Storage, con fallback
    private fun getStorageBucketUrl(): String {
        return try {
            val cfg = FirebaseApp.getInstance().options.storageBucket
            if (!cfg.isNullOrEmpty()) {
                var bucket = cfg
                // Si el valor del bucket está en el formato 'project-id.firebasestorage.app', convertirlo a appspot
                if (bucket.endsWith("firebasestorage.app")) {
                    bucket = bucket.replace("firebasestorage.app", "appspot.com")
                    Log.w("FirebaseService", "storageBucket en config parece no estándar; usando fallback a '$bucket'")
                }
                if (!bucket.startsWith("gs://")) bucket = "gs://$bucket"
                bucket
            } else {
                // Fallback explícito (reemplaza con tu bucket real si lo conoces)
                val fallback = "gs://tamats-aea71.appspot.com"
                Log.w("FirebaseService", "storageBucket no configurado en FirebaseApp.options; usando fallback: $fallback")
                fallback
            }
        } catch (e: Exception) {
            val fallback = "gs://tamats-aea71.appspot.com"
            Log.e("FirebaseService", "Error leyendo storageBucket: ${e.message}. Usando fallback: $fallback", e)
            fallback
        }
    }

    /**
     * Guarda un usuario en Firestore. La contraseña DEBE estar cifrada antes de llamar este método.
     * @param usuario UsuarioEntity con passwordHash cifrada
     */
    fun guardarUsuario(usuario: UsuarioEntity) {
        // Validar que la contraseña esté cifrada (no debe ser texto plano de 8 caracteres)
        if (esContrasenaPlana(usuario.passwordHash)) {
            Log.e("FirebaseService", "ERROR: Se intentó guardar una contraseña sin cifrar. Use EncryptionUtil.encryptPassword() primero.")
            return
        }

        val data = hashMapOf(
            "name" to usuario.name,
            "email" to usuario.email,
            "birthDate" to usuario.birthDate,
            "gender" to usuario.gender,
            "passwordHash" to usuario.passwordHash  // Ya debe estar cifrada
        )

        db.collection("usuarios").add(data).addOnSuccessListener {
            Log.d("FirebaseService", "Usuario guardado correctamente en Firestore (contraseña cifrada)")
        }.addOnFailureListener { e ->
            Log.e("FirebaseService", "Error al guardar el usuario en Firestore", e)
        }
    }

    /**
     * Detecta si una contraseña está en texto plano (heurística simple)
     */
    private fun esContrasenaPlana(passwordHash: String): Boolean {
        // Si la contraseña tiene exactamente 8 caracteres alfanuméricos sin caracteres especiales,
        // probablemente sea texto plano (ya que las cifradas en Base64 son más largas)
        return passwordHash.length == 8 && passwordHash.matches(Regex("[a-zA-Z0-9]*"))
    }

    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection("usuarios").get().addOnSuccessListener { result ->
            val lista = result.map {
                doc -> UsuarioEntity(
                id = 0, // El ID de Firestore no se usa en Room, se genera uno nuevo
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                birthDate = doc.getString("birthDate") ?: "",
                gender = doc.getString("gender") ?: "",
                passwordHash = doc.getString("passwordHash") ?: ""
            )
            }
            callback(lista)
        }.addOnFailureListener { e ->
            Log.e("FirebaseService", "Error al obtener los usuarios de Firestore", e)
            callback(emptyList())
        }
    }

    /**
     * Busca un usuario por email en Firestore. Devuelve el UsuarioEntity o null.
     */
    fun findUserByEmail(email: String, callback: (UsuarioEntity?) -> Unit) {
        db.collection("usuarios")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No se encontró ningún usuario con ese email
                    callback(null)
                } else {
                    // Se encontró un usuario, lo convertimos a UsuarioEntity
                    val doc = documents.first()
                    val user = UsuarioEntity(
                        id = 0, // El ID no es relevante aquí, Room lo autogenerará si se inserta
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        birthDate = doc.getString("birthDate") ?: "",
                        gender = doc.getString("gender") ?: "",
                        passwordHash = doc.getString("passwordHash") ?: ""
                    )
                    callback(user)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error al buscar usuario por email en Firestore", e)
                callback(null)
            }
    }

    /**
     * Sube una imagen (Uri) a Firebase Storage dentro de la carpeta "posts/"
     * y devuelve la URL pública mediante el callback.
     *
     * Nota: Esta función usa listeners habituales de Firebase. Si prefieres
     * un enfoque `suspend`/coroutines, podemos adaptarla fácilmente.
     *
     * Uso desde una Activity:
     * FirebaseService.uploadImage(uri) { url ->
     *   if (url != null) { /* usar url */ } else { /* manejar error */ }
     * }
     */
    @Suppress("unused")
    fun uploadImage(uri: Uri, callback: (String?) -> Unit) {
        try {
            // Obtener bucket correctamente formateado y crear una instancia de FirebaseStorage apuntando a ese bucket
            val bucketUrl = getStorageBucketUrl()
            val storage = try {
                FirebaseStorage.getInstance(bucketUrl)
            } catch (e: Exception) {
                Log.w("FirebaseService", "No se pudo obtener FirebaseStorage con $bucketUrl: ${e.message}; usando instancia por defecto", e)
                FirebaseStorage.getInstance()
            }

            // Log de config de FirebaseApp para depuración
            try {
                val app = FirebaseApp.getInstance()
                Log.d("FirebaseService", "FirebaseApp projectId=${app.options.projectId} storageBucket=${app.options.storageBucket} | usando storage bucketUrl=$bucketUrl")
            } catch (_: Exception) { /* ignore */ }

            val rootRef = storage.reference

            val filename = "${System.currentTimeMillis()}_${(uri.lastPathSegment ?: "img")}".replace("/", "_")
            val fileRef = rootRef.child("posts/$filename")

            Log.d("FirebaseService", "Subiendo a Storage bucketUrl=$bucketUrl path=posts/$filename uri=$uri")

            try {
                val uploadTask = fileRef.putFile(uri)
                uploadTask.addOnSuccessListener { _ ->
                    fileRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            callback(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseService", "Error obteniendo downloadUrl", e)
                            callback(null)
                        }
                }.addOnFailureListener { e ->
                    Log.e("FirebaseService", "Error subiendo imagen a Storage", e)
                    // Si es StorageException, registrar detalles HTTP
                    if (e is com.google.firebase.storage.StorageException) {
                        Log.e("FirebaseService", "StorageException: code=${e.errorCode} message=${e.message}")
                    }
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Excepción al iniciar uploadTask: ${e.message}", e)
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Excepción en uploadImage: ${e.message}", e)
            callback(null)
        }
    }

    /**
     * SUSPEND: Sube una imagen y devuelve la URL (o null si falla).
     * Uso desde coroutines:
     * val url = FirebaseService.uploadImageSuspend(uri)
     */
    suspend fun uploadImageSuspend(uri: Uri): String? = suspendCancellableCoroutine { cont ->
        try {
            // Intentar usar el bucket configurado explícitamente
            val bucketUrl = getStorageBucketUrl()
            // Intentaremos usar la instancia por defecto primero (es la más probable que esté correctamente configurada)
            val defaultStorage = try {
                FirebaseStorage.getInstance()
            } catch (e: Exception) {
                Log.w("FirebaseService", "No se pudo obtener FirebaseStorage.default: ${e.message}")
                null
            }

            val explicitStorage = try {
                FirebaseStorage.getInstance(bucketUrl)
            } catch (e: Exception) {
                Log.w("FirebaseService", "No se pudo obtener FirebaseStorage con bucket $bucketUrl: ${e.message}")
                null
            }

            // Elegir orden de intento: default -> explicit -> cualquiera disponible
            val storageCandidates = listOfNotNull(defaultStorage, explicitStorage)
            if (storageCandidates.isEmpty()) {
                Log.e("FirebaseService", "No hay instancias de FirebaseStorage disponibles")
                if (cont.isActive) cont.resume(null)
                return@suspendCancellableCoroutine
            }

            // Nombre de archivo seguro
            val filename = "${System.currentTimeMillis()}_${(uri.lastPathSegment ?: "img")}".replace("/", "_")

            // Función auxiliar para intentar subir con un storage dado
            fun attemptUpload(storage: FirebaseStorage, onComplete: (String?) -> Unit) {
                try {
                    val rootRef = storage.reference
                    val fileRef = rootRef.child("posts/$filename")
                    Log.d("FirebaseService", "Intentando subir a Storage (bucket=${storage.app.options.storageBucket}) path=posts/$filename uri=$uri")
                    val uploadTask = fileRef.putFile(uri)
                    cont.invokeOnCancellation { uploadTask.cancel() }

                    uploadTask.addOnSuccessListener { _ ->
                        fileRef.downloadUrl
                            .addOnSuccessListener { downloadUri ->
                                onComplete(downloadUri.toString())
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseService", "Error obteniendo downloadUrl tras éxito de upload: ${e.message}", e)
                                onComplete(null)
                            }
                    }.addOnFailureListener { e ->
                        Log.e("FirebaseService", "Error subiendo imagen a Storage (bucket=${storage.app.options.storageBucket}): ${e.message}", e)
                        onComplete(null)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Excepción iniciando uploadTask: ${e.message}", e)
                    onComplete(null)
                }
            }

            // Intentar cada candidato en secuencia hasta que uno funcione
            var finished = false
            fun tryNext(index: Int) {
                if (!cont.isActive) return
                if (index >= storageCandidates.size) {
                    // Ningún candidato funcionó: log detallado y terminar con null
                    val buckets = storageCandidates.mapNotNull { it.app.options.storageBucket }
                    Log.e("FirebaseService", "Todos los intentos de upload fallaron. Storage candidates buckets=$buckets filename=$filename uri=$uri")
                    if (!finished && cont.isActive) cont.resume(null)
                    return
                }
                val storage = storageCandidates[index]
                attemptUpload(storage) { result ->
                    if (!cont.isActive) return@attemptUpload
                    if (result != null) {
                        finished = true
                        cont.resume(result)
                    } else {
                        // intentar con el siguiente candidato
                        Log.w("FirebaseService", "Intento de upload falló para storage index=$index, probando siguiente")
                        tryNext(index + 1)
                    }
                }
            }

            tryNext(0)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Excepción en uploadImageSuspend: ${e.message}", e)
            if (cont.isActive) cont.resume(null)
        }
    }

    // Helper para sanitizar un id de documento (evitar '/' y cadenas vacías)
    private fun sanitizeDocId(id: String?): String? {
        if (id.isNullOrBlank()) return null
        // Reemplazar barras para evitar fragmentación en segmentos
        return id.replace("/", "_")
    }

    /**
     * Guarda o actualiza el perfil del usuario en Firestore
     */
    fun saveUserProfile(email: String, profileData: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("usuarios")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Si no existe, crear una nueva entrada en "userProfiles"
                    val docId = sanitizeDocId(email)
                    if (docId != null) {
                        db.collection("userProfiles").document(docId)
                            .set(profileData)
                            .addOnSuccessListener {
                                Log.d("FirebaseService", "Perfil de usuario guardado exitosamente (documentId=$docId)")
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseService", "Error al guardar perfil de usuario", e)
                                callback(false)
                            }
                    } else {
                        // Email inválido/ausente: crear documento con id automático
                        db.collection("userProfiles").add(profileData)
                            .addOnSuccessListener {
                                Log.d("FirebaseService", "Perfil de usuario guardado exitosamente (auto-id)")
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseService", "Error al guardar perfil de usuario (auto-id)", e)
                                callback(false)
                            }
                    }
                } else {
                    // Si existe, actualizar el documento
                    val doc = documents.first()
                    val existingId = doc.id
                    val existingCollection = doc.reference.parent.id // debería ser 'usuarios'
                    // Actualizar en la colección correcta (la query vino de 'usuarios')
                    db.collection(existingCollection).document(existingId)
                        .update(profileData)
                        .addOnSuccessListener {
                            Log.d("FirebaseService", "Perfil de usuario actualizado exitosamente en $existingCollection/$existingId")
                            callback(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseService", "Error al actualizar perfil de usuario", e)
                            callback(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error al buscar usuario", e)
                callback(false)
            }
    }

    /**
     * Obtiene el perfil del usuario desde Firestore
     */
    fun getUserProfile(email: String, callback: (Map<String, Any>?) -> Unit) {
        Log.d("FirebaseService", "Buscando perfil para email: $email")

        // Primero intentar buscar en userProfiles
        val docId = sanitizeDocId(email)
        if (docId != null) {
            db.collection("userProfiles").document(docId)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("FirebaseService", "userProfiles.document($docId).exists() = ${document.exists()}")

                    if (document.exists()) {
                        val data = document.data
                        Log.d("FirebaseService", "✓ Perfil encontrado en userProfiles: $data")
                        callback(data)
                    } else {
                        Log.d("FirebaseService", "⚠ No encontrado en userProfiles, buscando en usuarios...")

                        // Si no existe en userProfiles, buscar en usuarios
                        db.collection("usuarios")
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val userData = documents.first().data
                                    Log.d("FirebaseService", "✓ Datos encontrados en usuarios: $userData")
                                    callback(userData)
                                } else {
                                    Log.d("FirebaseService", "❌ No se encontró perfil en ningún lugar")
                                    callback(null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseService", "Error buscando en usuarios: ${e.message}", e)
                                callback(null)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "Error buscando en userProfiles: ${e.message}", e)
                    callback(null)
                }
        } else {
            // Si no hay email validable, buscar directamente en collection "usuarios"
            db.collection("usuarios")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userData = documents.first().data
                        Log.d("FirebaseService", "✓ Datos encontrados en usuarios (sin userProfiles): $userData")
                        callback(userData)
                    } else {
                        Log.d("FirebaseService", "❌ No se encontró perfil en ningún lugar")
                        callback(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseService", "Error buscando en usuarios: ${e.message}", e)
                    callback(null)
                }
        }
    }
}