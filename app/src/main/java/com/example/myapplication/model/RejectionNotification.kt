package com.example.myapplication.model

import com.google.firebase.Timestamp

data class RejectionNotification(
    val id: String = "",
    val fromUserEmail: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String = "",
    val toUserEmail: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
)

