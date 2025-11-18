package com.example.myapplication.model

import com.google.firebase.Timestamp

/**
 * Modelo para mensajes difundidos a todos los usuarios
 */
data class BroadcastMessage(
    val id: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val recipients: List<String> = emptyList() // Lista de emails de destinatarios
)

