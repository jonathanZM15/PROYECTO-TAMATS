package com.example.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.UsuarioEntity

@Dao
interface UsuarioDao {

    // 1. Insertar un nuevo usuario
    // suspend: Indica que la función debe ejecutarse en un hilo secundario (Coroutines)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(user: UsuarioEntity)

    // 2. Actualizar un usuario existente (para cambiar contraseña, etc.)
    @Update
    suspend fun actualizar(user: UsuarioEntity)

    // 3. Obtener un usuario por email
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UsuarioEntity?

    // 4. Verificar si un email ya está registrado
    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE email = :email)")
    suspend fun isEmailRegistered(email: String): Boolean
}