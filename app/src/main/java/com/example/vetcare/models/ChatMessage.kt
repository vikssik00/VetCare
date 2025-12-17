package com.example.vetcare.models

data class ChatMessage(
    val text: String? = null,
    val imageUri: String? = null,
    val isUser: Boolean = true
)