package com.example.vetcare.models

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

data class Pet(
    var petId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: String = "",      // Собака, Кошка и т.д.
    val breed: String = "",        // Порода
    val birthDate: String = "",    // Формат: "dd.MM.yyyy" из Firestore
    val photoUrl: String = "",     // Ссылка на фото (можно добавить позже)
    val medicalHistory: String = "",
    val lastCheckup: String = "",  // Дата последнего осмотра "dd.MM.yyyy"
    val createdAt: Long = 0,
    val isActive: Boolean = true,   // Активен/архивирован
    val appointmentTime: String = ""
) {
    @RequiresApi(Build.VERSION_CODES.O)
    @Exclude
    fun getAge(): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val birthDate = LocalDate.parse(birthDate, formatter)
            val today = LocalDate.now()
            val period = Period.between(birthDate, today)

            when {
                period.years > 0 -> "${period.years} лет"
                period.months > 0 -> "${period.months} мес."
                else -> "${period.days} дн."
            }
        } catch (e: Exception) {
            "Неизвестно"
        }
    }

    @Exclude
    fun getDisplayInfo(): String {
        return "$name • $breed • ${getAge()}"
    }

    @Exclude
    fun getShortInfo(): String {
        return "$name ($species)"
    }

    override fun toString(): String = name
}