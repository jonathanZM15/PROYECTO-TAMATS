package com.example.myapplication.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. @Entity: Declara esta data class como una tabla en la DB
@Entity(tableName = "user_table")
data class User(
    // @PrimaryKey: Define la clave principal. autoGenerate = true le asigna un ID único automáticamente.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    // Usaremos el email como un campo único (idealmente)
    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "birth_date")
    val birthDate: String,

    @ColumnInfo(name = "gender")
    val gender: String,

    // NOTA DE SEGURIDAD: La contraseña real debe ser cifrada (hashed) antes de guardar.
    @ColumnInfo(name = "password_hash")
    val passwordHash: String
)