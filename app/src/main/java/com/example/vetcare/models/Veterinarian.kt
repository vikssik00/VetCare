package com.example.vetcare.models

import java.io.Serializable

data class Veterinarian(
    val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val specialization: String = "",
    val experience: Int = 0,
    val description: String = "",
    val email: String = "",
    val isActive: Boolean = true
) : Serializable {

    fun getFullName(): String {
        return listOf(lastName, firstName, middleName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    override fun toString(): String {
        return getFullName()
    }

    fun getShortInfo(): String {
        return "$specialization, опыт: $experience лет"
    }

    fun isExperienced(): Boolean {
        return experience >= 3
    }

    fun getStatusText(): String {
        return if (isActive) "Активен" else "Не активен"
    }
}