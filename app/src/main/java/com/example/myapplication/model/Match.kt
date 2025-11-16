package com.example.myapplication.model

import com.google.firebase.Timestamp

data class Match(
    val id: String = "",
    val fromUserEmail: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String = "",
    val toUserEmail: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val rejected: Boolean = false,
    val rejectedAt: Timestamp? = null
)

