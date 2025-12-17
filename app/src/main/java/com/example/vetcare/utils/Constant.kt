package com.example.vetcare.utils

object UserRoles {
    const val ADMIN = "Администратор"
    const val VETERINARIAN = "Ветеринар"
    const val PET_OWNER = "Владелец питомца"
}

object FirestoreCollections {
    const val USERS = "users"
    const val PETS = "pets"
    const val SERVICES = "services"
    const val VET_SCHEDULE = "vet_schedule"
    const val APPOINTMENTS = "appointments"
    const val TELEMEDICINE_MESSAGES = "telemedicine_messages"
    const val MEDICAL_RECORDS = "medical_records"
    const val REMINDERS = "reminders"
}

object AppointmentStatus {
    const val SCHEDULED = "Запланирован"
    const val COMPLETED = "Завершен"
    const val CANCELLED = "Отменен"
}

object TimeSlots {
    val DEFAULT_TIME_SLOTS = listOf(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
    )
}

object VetWorkingHours {
    // Рабочие дни (1 = понедельник, 7 = воскресенье)
    val WORK_DAYS = listOf(1, 2, 3, 4, 5) // Пн-Пт

    // Рабочее время (9:00 - 18:00)
    val WORK_HOURS_START = 9
    val WORK_HOURS_END = 18

    // Длительность приема (минуты)
    val APPOINTMENT_DURATION = 30

    // Перерыв (например, с 13:00 до 14:00)
    val BREAK_START = 13
    val BREAK_END = 14
}