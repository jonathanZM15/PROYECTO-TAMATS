package com.example.myapplication.cloud

import android.util.Log
import com.example.myapplication.model.UsuarioEntity
import com.example.myapplication.util.EncryptionUtil
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseService {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Guarda un usuario en Firestore. La contraseña DEBE estar cifrada antes de llamar este método.
     * @param usuario UsuarioEntity con passwordHash cifrada
     */
    fun guardarUsuario(usuario: UsuarioEntity) {
        // Validar que la contraseña esté cifrada (no debe ser texto plano de 8 caracteres)
        if (esContraseñaPlana(usuario.passwordHash)) {
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
    private fun esContraseñaPlana(passwordHash: String): Boolean {
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
}