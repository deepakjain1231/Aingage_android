package com.example.aingage.model

data class MessageItem(
    val message: String,
    val messageType: String,   // TEXT, FILE, SYS-COPY
    val filename: String,
    val addedOn: String,
    val fromParticipantId: Int,
    val toParticipantId: Int
)
