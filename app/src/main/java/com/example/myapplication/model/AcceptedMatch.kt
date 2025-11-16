package com.example.myapplication.model

import com.google.firebase.Timestamp

data class AcceptedMatch(
    val id: String = "",
    val user1Email: String = "",
    val user1Name: String = "",
    val user1Photo: String = "",
    val user2Email: String = "",
    val user2Name: String = "",
    val user2Photo: String = "",
    val acceptedAt: Timestamp = Timestamp.now(),
    val mutualAcceptance: Boolean = false
)

