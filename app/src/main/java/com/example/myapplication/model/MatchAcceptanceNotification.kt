package com.example.myapplication.model

import com.google.firebase.Timestamp

data class MatchAcceptanceNotification(
    val id: String = "",
    val fromUserEmail: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String = "",
    val toUserEmail: String = "",
    val acceptedAt: Timestamp = Timestamp.now(),
    val read: Boolean = false
)

