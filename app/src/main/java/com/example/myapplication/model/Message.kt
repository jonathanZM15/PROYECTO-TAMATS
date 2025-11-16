package com.example.myapplication.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text" // "text" o "image"
)

