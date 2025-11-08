package com.example.myapplication.cloud

import android.util.Log
import com.example.myapplication.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseService {
    //instanciar la base de daos de firebase y las guardamos en la variable db
    private val db = FirebaseFirestore.getInstance()

    fun guardarUsuario(usuario: UsuarioEntity) {
        val data = hashMapOf(
            "name" to usuario.name,
            "email" to usuario.email,
            "birthDate" to usuario.birthDate,
            "gender" to usuario.gender,
            "passwordHash" to usuario.passwordHash
        )

        db.collection("usuarios").add(data).addOnSuccessListener {
            //cuando es informativo se coloca Log.d
            Log.d("FirebaseService","Usuario guardado correctamente en Firestore")
        }
            .addOnFailureListener {
                //se usa Log.e para errores
                Log.e("FirebaseService", "Error al guardar el usuario en Firestore", it)
            }

    }

    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit){
        db.collection("usuarios").get().addOnSuccessListener {
                result -> val lista = result.map {
                doc -> UsuarioEntity(
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
            .addOnFailureListener {e ->
                Log.e("FirebaseService", "Error al obtener los usuarios de Firestore", e)
                //devolver una lista vacia
                callback(emptyList())
            }
    }
}