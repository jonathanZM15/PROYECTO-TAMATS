package com.example.myapplication.cloud

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.myapplication.model.UsuarioEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FirebaseService {
    // Evitamos mantener una referencia estática a FirebaseFirestore (posible leak de contexto)
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // Helper: obtiene la URL gs:// del bucket de Storage, con fallback
    @Suppress("unused")
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

        val data = hashMapOf<String, Any>(
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

    @Suppress("unused")
    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection("usuarios").get().addOnSuccessListener { result ->
            val lista = result.map { doc ->
                UsuarioEntity(
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
                        id = 0,
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
     * Sube una imagen (Uri) a Base64 y devuelve la cadena Base64 mediante el callback.
     * NOTA: ahora NO usa Firebase Storage.
     * @param uri Uri de la imagen
     * @param context Context necesario para resolver el Uri
     */
    @Suppress("unused")
    fun uploadImage(uri: Uri, context: Context, callback: (String?) -> Unit) {
        try {
            Log.d("FirebaseService", "Convirtiendo imagen a Base64 uri=$uri")
            val base64 = try {
                uriToBase64(context, uri)
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error convirtiendo Uri a Base64: ${e.message}", e)
                null
            }

            if (base64 != null) {
                Log.d("FirebaseService", "Conversión a Base64 exitosa, longitud=${base64.length}")
                callback(base64)
            } else {
                Log.e("FirebaseService", "Conversión a Base64 devolvió null")
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Excepción en uploadImage (Base64): ${e.message}", e)
            callback(null)
        }
    }

    /**
     * SUSPEND: Convierte la imagen referida por Uri a Base64 y devuelve la cadena (o null si falla).
     */
    @Suppress("unused")
    suspend fun uploadImageSuspend(uri: Uri, context: Context): String? = suspendCancellableCoroutine { cont ->
        try {
            Log.d("FirebaseService", "uploadImageSuspend: convirtiendo uri=$uri a Base64")
            val base64 = try {
                uriToBase64(context, uri)
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error en uriToBase64: ${e.message}", e)
                null
            }
            if (cont.isActive) cont.resume(base64)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Excepción en uploadImageSuspend (Base64): ${e.message}", e)
            if (cont.isActive) cont.resume(null)
        }
    }

    /**
     * Convierte el contenido del Uri a una cadena Base64 (optimizando lectura en streaming).
     * Si el tamaño excede `maxBytes`, intenta decodificar a Bitmap y comprimir/redimensionar
     * reduciendo calidad y muestreo hasta que la salida quepa en `maxBytes`.
     */
    private fun uriToBase64(context: Context, uri: Uri, maxBytes: Int = 900 * 1024): String? {
        var input: InputStream? = null
        try {
            // Intento rápido: leer en streaming hasta maxBytes. Si el archivo binario cabe, codificar.
            input = context.contentResolver.openInputStream(uri) ?: return null
            val buffer = ByteArray(8192)
            val baos = ByteArrayOutputStream()
            var totalRead = 0
            var read: Int
            while (true) {
                read = input.read(buffer)
                if (read <= 0) break
                totalRead += read
                // Si ya supera un umbral grande (p.ej. 5 MB) abortamos para evitar OOM
                if (totalRead > 5 * 1024 * 1024) {
                    Log.w("FirebaseService", "Archivo muy grande (>5MB), intentaremos compresión por Bitmap")
                    break
                }
                baos.write(buffer, 0, read)
            }
            val rawBytes = baos.toByteArray()
            // Si lo leímos completamente y su Base64 cabe en maxBytes, retornarlo
            val directB64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            if (directB64.toByteArray(Charsets.UTF_8).size <= maxBytes) {
                return directB64
            }
            // Si no cabe, intentaremos procesar con Bitmap (redimensionar/comprimir)
        } catch (e: Exception) {
            Log.w("FirebaseService", "Lectura directa falló o excedió buffer: ${e.message}")
            // caeremos al intento con Bitmap
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }

        // Intento de compresión/redimensionado con Bitmap
        var bitmapInput: InputStream? = null
        return try {
            // Primero obtener dimensiones sin cargar el bitmap completo
            val optionsBounds = android.graphics.BitmapFactory.Options()
            optionsBounds.inJustDecodeBounds = true
            bitmapInput = context.contentResolver.openInputStream(uri) ?: return null
            android.graphics.BitmapFactory.decodeStream(bitmapInput, null, optionsBounds)
            bitmapInput.close()

            var sampleSize = 1
            val desiredMaxDim = 1280 // tamaño objetivo razonable para ancho/alto
            val width = optionsBounds.outWidth
            val height = optionsBounds.outHeight
            if (width > desiredMaxDim || height > desiredMaxDim) {
                val scale = Math.max(width / desiredMaxDim, height / desiredMaxDim)
                // calcular la potencia de 2 más cercana
                var s = 1
                while (s < scale) s *= 2
                sampleSize = s
            }

            // Decodificar con el sampleSize calculado
            val optionsDecode = android.graphics.BitmapFactory.Options()
            optionsDecode.inSampleSize = sampleSize
            bitmapInput = context.contentResolver.openInputStream(uri) ?: return null
            val bmp = android.graphics.BitmapFactory.decodeStream(bitmapInput, null, optionsDecode)
            bitmapInput.close()

            if (bmp == null) return null

            // Ahora comprimir iterativamente reduciendo calidad hasta caber en maxBytes
            val baos2 = ByteArrayOutputStream()
            var quality = 85
            var b64: String
            while (quality >= 30) {
                baos2.reset()
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos2)
                val compressed = baos2.toByteArray()
                b64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                if (b64.toByteArray(Charsets.UTF_8).size <= maxBytes) {
                    // Liberar bitmap
                    try { bmp.recycle() } catch (_: Exception) {}
                    return b64
                }
                quality -= 10
            }

            // Si no lo conseguimos con la calidad, intentar reducir aún más dimensiones
            var currentWidth = bmp.width
            var currentHeight = bmp.height
            var reducedBmp: android.graphics.Bitmap? = bmp
            while (true) {
                // reducir dimensiones a 75%
                val newW = (currentWidth * 0.75).toInt().coerceAtLeast(100)
                val newH = (currentHeight * 0.75).toInt().coerceAtLeast(100)
                if (newW >= currentWidth || newH >= currentHeight) break
                val scaled = android.graphics.Bitmap.createScaledBitmap(reducedBmp!!, newW, newH, true)
                if (reducedBmp !== bmp) {
                    try { reducedBmp.recycle() } catch (_: Exception) {}
                }
                reducedBmp = scaled
                currentWidth = reducedBmp.width
                currentHeight = reducedBmp.height

                baos2.reset()
                reducedBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos2)
                val compressed = baos2.toByteArray()
                val b64try = Base64.encodeToString(compressed, Base64.NO_WRAP)
                if (b64try.toByteArray(Charsets.UTF_8).size <= maxBytes) {
                    try { reducedBmp.recycle() } catch (_: Exception) {}
                    if (bmp !== reducedBmp) try { bmp.recycle() } catch (_: Exception) {}
                    return b64try
                }

                // Si ya muy pequeño, salir
                if (currentWidth <= 200 || currentHeight <= 200) break
            }

            // Liberar bitmaps
            try { reducedBmp?.recycle() } catch (_: Exception) {}
            if (bmp !== reducedBmp) try { bmp.recycle() } catch (_: Exception) {}

            // Si no se pudo comprimir suficientemente, devolver null
            Log.e("FirebaseService", "No se pudo reducir imagen lo suficiente para Firestore (limite ${maxBytes} bytes)")
            null
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error procesando Bitmap para Base64: ${e.message}", e)
            try { bitmapInput?.close() } catch (_: Exception) {}
            null
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

    /**
     * Obtiene el email del usuario actualmente autenticado
     */
    fun getCurrentUserEmail(): String {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            currentUser?.email ?: ""
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error obteniendo email del usuario: ${e.message}")
            ""
        }
    }

    /**
     * Cierra la sesión del usuario actual y limpia datos locales
     */
    fun logout(context: Context? = null) {
        try {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()

            // Limpiar SharedPreferences si se proporciona contexto
            context?.let {
                val prefs = it.getSharedPreferences("user_data", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            }

            Log.d("FirebaseService", "Sesión cerrada exitosamente y datos locales limpios")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al cerrar sesión: ${e.message}", e)
        }
    }
}
