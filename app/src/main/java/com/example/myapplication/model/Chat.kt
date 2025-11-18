package com.example.myapplication.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val chatId: String = "",
    val user1Email: String = "",
    val user1Name: String = "",
    val user1Photo: String = "",
    val user2Email: String = "",
    val user2Name: String = "",
    val user2Photo: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val chatType: String = "regular" // "support" o "regular"
) {
    // Propiedades calculadas
    val isSupportChat: Boolean
        get() = chatType == "support"

    val isPinned: Boolean
        get() = chatType == "support"
}

