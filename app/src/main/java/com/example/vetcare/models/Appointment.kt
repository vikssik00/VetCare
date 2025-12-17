package com.example.vetcare.models

import com.example.vetcare.utils.AppointmentStatus
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Appointment(
    val id: String = "",
    val petId: String = "",
    val petName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val vetId: String = "",
    val vetName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val servicePrice: Double = 0.0,
    val appointmentDate: String = "",    // "2024-01-15"
    val appointmentTime: String = "",    // "14:30"
    val duration: Int = 30,
    var status: String = AppointmentStatus.SCHEDULED,
    val consultationType: String = "offline", // offline/online
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isHeader: Boolean = false
) : Serializable {

    fun getDateTime(): String {
        return "$appointmentDate $appointmentTime"
    }

    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(appointmentDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            appointmentDate
        }
    }

    fun getFullDateTime(): String {
        return "${getFormattedDate()} в $appointmentTime"
    }

    fun getEndTime(): String {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = timeFormat.parse(appointmentTime)
            val calendar = Calendar.getInstance()
            calendar.time = startTime
            calendar.add(Calendar.MINUTE, duration)
            timeFormat.format(calendar.time)
        } catch (e: Exception) {
            ""
        }
    }

    fun getTimeSlot(): String {
        return "$appointmentTime - ${getEndTime()}"
    }

    fun isUpcoming(): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDateTime = dateFormat.parse("$appointmentDate $appointmentTime")
            appointmentDateTime.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun isToday(): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val appointmentDateObj = dateFormat.parse(appointmentDate)
            val today = Date()

            val calendarAppointment = Calendar.getInstance().apply { time = appointmentDateObj }
            val calendarToday = Calendar.getInstance().apply { time = today }

            calendarAppointment.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                    calendarAppointment.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }

    fun getDateTimeAsCalendar(): Calendar? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = dateFormat.parse("$appointmentDate $appointmentTime")
            Calendar.getInstance().apply { time = date!! }
        } catch (e: Exception) {
            null
        }
    }
}