package com.example.vetcare.models

import java.io.Serializable
import java.util.Locale

data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val durationMinutes: Int = 30,
    val price: Double = 0.0,
    val category: String = "Общие",
    val isActive: Boolean = true
) : Serializable {

    fun getFormattedPrice(): String {
        return String.Companion.format(Locale.getDefault(), "%.2f ₽", price)
    }

    fun getFormattedDuration(): String {
        return "$durationMinutes мин"
    }
}