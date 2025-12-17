package com.example.vetcare.models

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)