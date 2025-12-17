package com.example.vetcare.models

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VetSchedule(
    val id: String = "",
    val vetId: String = "",
    val vetName: String = "",
    val date: String = "", // "2024-01-15"
    val dayOfWeek: String = "", // "Понедельник"
    val startTime: String = "", // "09:00"
    val endTime: String = "", // "18:00"
    val isAvailable: Boolean = true,
    val maxAppointments: Int = 10
) : Serializable {
    fun getTimeRange(): String {
        return "$startTime - $endTime"
    }

    fun isToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return date == today
    }

    fun isFuture(): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val scheduleDate = dateFormat.parse(date)
            val today = Date()
            scheduleDate.after(today) || isToday()
        } catch (e: Exception) {
            false
        }
    }
}