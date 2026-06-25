package com.example.aingage.model

data class ContactItem(
    val name: String,
    val mobile: String,
    val unreadCount: Int,
    val participantId: Int,
    val rawJson: com.google.gson.JsonObject
)
