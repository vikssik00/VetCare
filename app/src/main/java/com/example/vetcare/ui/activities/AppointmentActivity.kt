package com.example.vetcare.ui.activities

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.vetcare.models.Appointment
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.utils.NotificationUtils
import com.example.vetcare.R
import com.example.vetcare.models.Service
import com.example.vetcare.utils.VetWorkingHours
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.iterator

class AppointmentActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var tvPetName: TextView
    private lateinit var spinnerServices: Spinner
    private lateinit var cardDateSelection: CardView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvAvailableDates: TextView
    private lateinit var datesContainer: LinearLayout
    private lateinit var cardTimeSelection: CardView
    private lateinit var tvAvailableTimes: TextView
    private lateinit var timeSlotsContainer: LinearLayout
    private lateinit var btnConfirmAppointment: Button
    private lateinit var progressBarDates: ProgressBar
    private lateinit var progressBarTimes: ProgressBar
    private lateinit var tvAppointmentSummary: TextView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Данные
    private var petId: String = ""
    private var petName: String = ""
    private var selectedServiceId: String = ""
    private var selectedServiceName: String = ""
    private var selectedServicePrice: Double = 0.0
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedVetId: String = ""
    private var selectedVetName: String = ""

    // Списки
    private val servicesList = mutableListOf<Service>()
    private val availableDates = mutableListOf<String>()
    private val availableTimeSlots = mutableListOf<TimeSlot>()
    private val vetsSchedule = mutableMapOf<String, MutableList<TimeSlot>>()

    // Тег для логов
    private val TAG = "AppointmentActivity"

    // Форматы дат
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("ru"))
    private val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class TimeSlot(
        val vetId: String,
        val vetName: String,
        val date: String,
        val time: String,
        val isAvailable: Boolean = true
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        Log.d(TAG, "Начало AppointmentActivity")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        getIntentData()
        loadServices()
        setupClickListeners()
    }

    private fun initViews() {
        tvPetName = findViewById(R.id.tvPetName)
        spinnerServices = findViewById(R.id.spinnerServices)

        // Выбор даты
        cardDateSelection = findViewById(R.id.cardDateSelection)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvAvailableDates = findViewById(R.id.tvAvailableDates)
        datesContainer = findViewById(R.id.datesContainer)
        progressBarDates = findViewById(R.id.progressBarDates)

        // Выбор времени
        cardTimeSelection = findViewById(R.id.cardTimeSelection)
        tvAvailableTimes = findViewById(R.id.tvAvailableTimes)
        timeSlotsContainer = findViewById(R.id.timeSlotsContainer)
        progressBarTimes = findViewById(R.id.progressBarTimes)

        btnConfirmAppointment = findViewById(R.id.btnConfirmAppointment)
        tvAppointmentSummary = findViewById(R.id.tvAppointmentSummary)

        // Начальные состояния
        cardDateSelection.visibility = View.GONE
        cardTimeSelection.visibility = View.GONE
        btnConfirmAppointment.isEnabled = false
        tvAppointmentSummary.text = "Выберите услугу, дату и время"
    }

    private fun getIntentData() {
        petId = intent.getStringExtra("petId") ?: ""
        petName = intent.getStringExtra("petName") ?: "Питомец"
        tvPetName.text = "Запись для: $petName"
    }

    private fun loadServices() {
        db.collection(FirestoreCollections.SERVICES)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { result ->
                servicesList.clear()

                for (document in result) {
                    val service = Service(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        durationMinutes = (document.getLong("durationMinutes") ?: 30).toInt(),
                        price = document.getDouble("price") ?: 0.0,
                        category = document.getString("category") ?: "Общие",
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    servicesList.add(service)
                }

                setupServicesSpinner()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки услуг", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Ошибка загрузки услуг: ${e.message}")
            }
    }

    private fun setupServicesSpinner() {
        val serviceNames = mutableListOf("Выберите услугу")
        serviceNames.addAll(servicesList.map { it.name })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServices.adapter = adapter

        spinnerServices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedService = servicesList[position - 1]
                    selectedServiceId = selectedService.id
                    selectedServiceName = selectedService.name
                    selectedServicePrice = selectedService.price

                    // Показываем выбор даты
                    cardDateSelection.visibility = View.VISIBLE
                    loadAvailableDates()

                    // Скрываем выбор времени пока не выбрана дата
                    cardTimeSelection.visibility = View.GONE
                    clearTimeSlots()
                } else {
                    selectedServiceId = ""
                    selectedServiceName = ""
                    selectedServicePrice = 0.0
                    cardDateSelection.visibility = View.GONE
                    cardTimeSelection.visibility = View.GONE
                }

                updateAppointmentSummary()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedServiceId = ""
                selectedServiceName = ""
                selectedServicePrice = 0.0
            }
        }
    }

    private fun loadAvailableDates() {
        if (selectedServiceId.isEmpty()) return

        progressBarDates.visibility = View.VISIBLE
        datesContainer.visibility = View.GONE
        tvAvailableDates.text = "Загрузка доступных дат..."

        // Генерируем доступные даты на 14 дней вперед
        availableDates.clear()
        datesContainer.removeAllViews()

        val calendar = Calendar.getInstance()
        val today = calendar.time

        for (i in 0..13) { // 14 дней
            calendar.time = today
            calendar.add(Calendar.DAY_OF_MONTH, i)

            // Пропускаем выходные (Суббота и Воскресенье)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                continue
            }

            val date = storageDateFormat.format(calendar.time)
            availableDates.add(date)
        }

        displayAvailableDates()
    }

    private fun displayAvailableDates() {
        datesContainer.removeAllViews()

        if (availableDates.isEmpty()) {
            tvAvailableDates.text = "Нет доступных дат для записи"
            progressBarDates.visibility = View.GONE
            return
        }

        var currentRow: LinearLayout? = null

        for ((index, date) in availableDates.withIndex()) {
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dpToPx(8)
                    }
                    orientation = LinearLayout.HORIZONTAL
                }
                datesContainer.addView(currentRow)
            }

            val dateButton = createDateButton(date)
            currentRow?.addView(dateButton)

            if (index % 2 != 1 && index != availableDates.size - 1) {
                val space = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1)
                }
                currentRow?.addView(space)
            }
        }

        tvAvailableDates.text = "Выберите дату приема:"
        datesContainer.visibility = View.VISIBLE
        progressBarDates.visibility = View.GONE
    }

    private fun createDateButton(dateStr: String): Button {
        val displayDate = try {
            val date = storageDateFormat.parse(dateStr)
            SimpleDateFormat("dd MMM\nEEEE", Locale("ru")).format(date)
        } catch (e: Exception) {
            dateStr
        }

        return Button(this).apply {
            text = displayDate
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                minimumHeight = dpToPx(60)
            }

            setBackgroundResource(R.drawable.date_button_background)
            setTextColor(resources.getColor(android.R.color.black))
            textSize = 12f
            isAllCaps = false

            if (selectedDate == dateStr) {
                setBackgroundResource(R.drawable.date_button_selected_background)
            }

            setOnClickListener {
                selectedDate = dateStr
                tvSelectedDate.text = try {
                    val date = storageDateFormat.parse(dateStr)
                    displayDateFormat.format(date)
                } catch (e: Exception) {
                    dateStr
                }

                // Обновляем вид кнопок
                updateDateButtonsAppearance()

                // Загружаем доступное время для выбранной даты
                loadAvailableTimeSlots()

                // Показываем выбор времени
                cardTimeSelection.visibility = View.VISIBLE

                updateAppointmentSummary()
            }
        }
    }

    private fun generateTimeSlotsForSchedule(
        vetId: String,
        vetName: String,
        schedule: DocumentSnapshot,
        bookedSlots: Set<String>
    ): List<TimeSlot> {

        val slots = mutableListOf<TimeSlot>()

        val startTime = schedule.getString("startTime") ?: return slots
        val endTime = schedule.getString("endTime") ?: return slots

        val startParts = startTime.split(":")
        val endParts = endTime.split(":")

        var hour = startParts[0].toInt()
        var minute = startParts[1].toInt()

        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        while (hour < endHour || (hour == endHour && minute < endMinute)) {

            val time = String.format("%02d:%02d", hour, minute)
            val key = "${vetId}_$time"

            if (!bookedSlots.contains(key)) {
                slots.add(
                    TimeSlot(
                        vetId = vetId,
                        vetName = vetName,
                        date = selectedDate,
                        time = time,
                        isAvailable = true
                    )
                )
            }

            minute += 30
            if (minute >= 60) {
                minute = 0
                hour++
            }
        }

        return slots
    }


    private fun updateDateButtonsAppearance() {
        for (i in 0 until datesContainer.childCount) {
            val row = datesContainer.getChildAt(i) as? LinearLayout
            row?.let {
                for (j in 0 until it.childCount) {
                    val view = it.getChildAt(j)
                    if (view is Button) {
                        // Извлекаем дату из текста кнопки
                        val buttonDate = extractDateFromButtonText(view.text.toString())
                        view.setBackgroundResource(
                            if (buttonDate == selectedDate) R.drawable.date_button_selected_background
                            else R.drawable.date_button_background
                        )
                    }
                }
            }
        }
    }

    private fun extractDateFromButtonText(buttonText: String): String {
        return try {
            val lines = buttonText.split("\n")
            val dayMonth = lines[0] // "15 янв"
            val day = dayMonth.split(" ")[0].toInt()
            val monthStr = dayMonth.split(" ")[1]

            val monthsMap = mapOf(
                "янв" to 0, "фев" to 1, "мар" to 2, "апр" to 3,
                "май" to 4, "июн" to 5, "июл" to 6, "авг" to 7,
                "сен" to 8, "окт" to 9, "ноя" to 10, "дек" to 11
            )

            val month = monthsMap[monthStr] ?: 0
            val year = Calendar.getInstance().get(Calendar.YEAR)

            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            storageDateFormat.format(calendar.time)
        } catch (e: Exception) {
            ""
        }
    }

    private fun loadAvailableTimeSlots() {
        if (selectedDate.isEmpty() || selectedServiceId.isEmpty()) return

        progressBarTimes.visibility = View.VISIBLE
        timeSlotsContainer.visibility = View.GONE
        tvAvailableTimes.text = "Поиск свободных окон..."
        availableTimeSlots.clear()

        // 1. Получаем смены на выбранную дату
        db.collection("vet_schedule")
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { scheduleResult ->

                if (scheduleResult.isEmpty) {
                    showNoSlotsAvailable("На выбранную дату нет смен врачей")
                    return@addOnSuccessListener
                }

                // 2. Получаем уже существующие записи
                db.collection(FirestoreCollections.APPOINTMENTS)
                    .whereEqualTo("appointmentDate", selectedDate)
                    .whereEqualTo("status", AppointmentStatus.SCHEDULED)
                    .get()
                    .addOnSuccessListener { appointmentsResult ->

                        val bookedSlots = appointmentsResult.documents.mapNotNull {
                            "${it.getString("vetId")}_${it.getString("appointmentTime")}"
                        }.toSet()

                        // 3. Генерируем слоты по каждой смене
                        for (doc in scheduleResult.documents) {

                            val vetId = doc.getString("vetId") ?: continue
                            val vetName = doc.getString("vetName") ?: "Ветеринар"
                            val startTime = doc.getString("startTime") ?: continue
                            val endTime = doc.getString("endTime") ?: continue

                            val startParts = startTime.split(":")
                            val endParts = endTime.split(":")

                            val startHour = startParts[0].toInt()
                            val startMinute = startParts[1].toInt()
                            val endHour = endParts[0].toInt()
                            val endMinute = endParts[1].toInt()

                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, startHour)
                            calendar.set(Calendar.MINUTE, startMinute)

                            val endCalendar = Calendar.getInstance()
                            endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
                            endCalendar.set(Calendar.MINUTE, endMinute)

                            while (calendar.before(endCalendar)) {
                                val time = String.format(
                                    "%02d:%02d",
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE)
                                )

                                val slotKey = "${vetId}_$time"

                                if (!bookedSlots.contains(slotKey)) {
                                    availableTimeSlots.add(
                                        TimeSlot(
                                            vetId = vetId,
                                            vetName = vetName,
                                            date = selectedDate,
                                            time = time,
                                            isAvailable = true
                                        )
                                    )
                                }

                                calendar.add(Calendar.MINUTE, 30)
                            }
                        }

                        displayAvailableTimeSlots()
                    }
            }
            .addOnFailureListener {
                showNoSlotsAvailable("Ошибка загрузки расписания")
            }
    }

    private fun generateTimeSlotsForVet(
        vetId: String,
        vetName: String,
        vetSchedule: DocumentSnapshot?,
        bookedSlots: Set<String>
    ): MutableList<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()

        // Рабочие часы (по умолчанию или из расписания)
        val workStart = if (vetSchedule != null) {
            vetSchedule.getString("startTime")?.split(":")?.get(0)?.toInt()
                ?: VetWorkingHours.WORK_HOURS_START
        } else {
            VetWorkingHours.WORK_HOURS_START
        }

        val workEnd = if (vetSchedule != null) {
            vetSchedule.getString("endTime")?.split(":")?.get(0)?.toInt()
                ?: VetWorkingHours.WORK_HOURS_END
        } else {
            VetWorkingHours.WORK_HOURS_END
        }

        val breakStart = VetWorkingHours.BREAK_START
        val breakEnd = VetWorkingHours.BREAK_END

        // Генерация слотов по 30 минут
        for (hour in workStart until workEnd) {
            for (minute in listOf(0, 30)) {
                // Проверка на обеденный перерыв
                if (hour == breakStart && minute == 0) {
                    continue
                }

                val timeSlot = String.format("%02d:%02d", hour, minute)
                val slotKey = "${vetId}_$timeSlot"

                // Проверка, не занят ли слот
                if (!bookedSlots.contains(slotKey)) {
                    timeSlots.add(
                        TimeSlot(
                            vetId = vetId,
                            vetName = vetName,
                            date = selectedDate,
                            time = timeSlot,
                            isAvailable = true
                        )
                    )
                }
            }
        }

        return timeSlots
    }

    private fun displayAvailableTimeSlots() {
        timeSlotsContainer.removeAllViews()

        if (availableTimeSlots.isEmpty()) {
            showNoSlotsAvailable("Нет свободных окон на выбранную дату")
            return
        }

        // Группируем по ветеринарам
        val slotsByVet = availableTimeSlots.groupBy { it.vetName }

        for ((vetName, slots) in slotsByVet) {
            // Заголовок с именем ветеринара
            val vetHeader = TextView(this).apply {
                text = "👨‍⚕️ $vetName"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dpToPx(16), 0, dpToPx(8))
            }
            timeSlotsContainer.addView(vetHeader)

            // Контейнер для слотов этого ветеринара
            var slotsRow = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dpToPx(16))
            }

            // Добавляем слоты времени
            for ((index, slot) in slots.withIndex()) {
                if (index > 0 && index % 4 == 0) {
                    // Новый ряд после 4 слотов
                    timeSlotsContainer.addView(slotsRow)

                    val newRow = LinearLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 0, 0, dpToPx(16))
                    }
                    slotsRow = newRow
                }

                val timeButton = createTimeSlotButton(slot)
                slotsRow.addView(timeButton)

                // Добавляем промежуток между кнопками
                if (index % 4 != 3 && index != slots.size - 1) {
                    val space = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(4), 1)
                    }
                    slotsRow.addView(space)
                }
            }

            timeSlotsContainer.addView(slotsRow)
        }

        tvAvailableTimes.text = "Выберите удобное время:"
        timeSlotsContainer.visibility = View.VISIBLE
        progressBarTimes.visibility = View.GONE
    }

    private fun createTimeSlotButton(slot: TimeSlot): Button {
        return Button(this).apply {
            text = slot.time
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                minimumHeight = dpToPx(40)
            }

            setBackgroundResource(R.drawable.time_slot_background)
            setTextColor(resources.getColor(android.R.color.white))
            textSize = 14f

            if (selectedTime == slot.time && selectedVetId == slot.vetId) {
                setBackgroundResource(R.drawable.time_slot_selected_background)
            }

            setOnClickListener {
                selectedTime = slot.time
                selectedVetId = slot.vetId
                selectedVetName = slot.vetName

                updateTimeSlotsAppearance()
                updateAppointmentSummary()
                btnConfirmAppointment.isEnabled = true
            }
        }
    }

    private fun updateTimeSlotsAppearance() {
        for (i in 0 until timeSlotsContainer.childCount) {
            val child = timeSlotsContainer.getChildAt(i)
            if (child is LinearLayout) {
                for (j in 0 until child.childCount) {
                    val view = child.getChildAt(j)
                    if (view is Button) {
                        // Найти соответствующий слот
                        val slot = availableTimeSlots.find {
                            it.time == view.text.toString()
                        }

                        view.setBackgroundResource(
                            if (slot != null &&
                                selectedTime == slot.time &&
                                selectedVetId == slot.vetId) {
                                R.drawable.time_slot_selected_background
                            } else {
                                R.drawable.time_slot_background
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showNoSlotsAvailable(message: String) {
        tvAvailableTimes.text = message
        timeSlotsContainer.visibility = View.GONE
        progressBarTimes.visibility = View.GONE
        btnConfirmAppointment.isEnabled = false
    }

    private fun clearTimeSlots() {
        selectedTime = ""
        selectedVetId = ""
        selectedVetName = ""
        timeSlotsContainer.removeAllViews()
        btnConfirmAppointment.isEnabled = false
    }

    private fun updateAppointmentSummary() {
        val summary = StringBuilder()

        if (selectedServiceName.isNotEmpty()) {
            summary.append("Услуга: $selectedServiceName\n")
        }

        if (selectedDate.isNotEmpty()) {
            try {
                val date = storageDateFormat.parse(selectedDate)
                summary.append("Дата: ${displayDateFormat.format(date)}\n")
            } catch (e: Exception) {
                summary.append("Дата: $selectedDate\n")
            }
        }

        if (selectedTime.isNotEmpty() && selectedVetName.isNotEmpty()) {
            summary.append("Время: $selectedTime\n")
            summary.append("Ветеринар: $selectedVetName")
        }

        if (selectedServicePrice > 0) {
            summary.append("\nСтоимость: $selectedServicePrice руб.")
        }

        tvAppointmentSummary.text = if (summary.isNotEmpty()) {
            summary.toString()
        } else {
            "Выберите услугу, дату и время"
        }
    }

    private fun setupClickListeners() {
        btnConfirmAppointment.setOnClickListener {
            createAppointment()
        }
    }

    private fun createAppointment() {
        if (selectedDate.isEmpty() ||
            selectedTime.isEmpty() ||
            selectedVetId.isEmpty() ||
            selectedServiceId.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerId = auth.currentUser?.uid ?: ""
        val ownerName = auth.currentUser?.email?.split("@")?.get(0) ?: "Владелец"

        val appointment = Appointment(
            petId = petId,
            petName = petName,
            ownerId = ownerId,
            ownerName = ownerName,
            vetId = selectedVetId,
            vetName = selectedVetName,
            serviceId = selectedServiceId,
            serviceName = selectedServiceName,
            servicePrice = selectedServicePrice,
            appointmentDate = selectedDate,
            appointmentTime = selectedTime,
            notes = ""
        )

        // Двойная проверка доступности слота
        db.collection(FirestoreCollections.APPOINTMENTS)
            .whereEqualTo("vetId", selectedVetId)
            .whereEqualTo("appointmentDate", selectedDate)
            .whereEqualTo("appointmentTime", selectedTime)
            .whereEqualTo("status", AppointmentStatus.SCHEDULED)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    saveAppointmentToFirestore(appointment)
                } else {
                    Toast.makeText(
                        this,
                        "К сожалению, это время уже занято. Выберите другое время.",
                        Toast.LENGTH_LONG
                    ).show()
                    loadAvailableTimeSlots() // Обновить список
                }
            }
            .addOnFailureListener {
                saveAppointmentToFirestore(appointment)
            }
    }

    private fun saveAppointmentToFirestore(appointment: Appointment) {
        btnConfirmAppointment.isEnabled = false
        btnConfirmAppointment.text = "Создание записи..."

        db.collection(FirestoreCollections.APPOINTMENTS)
            .add(appointment)
            .addOnSuccessListener { documentReference ->
                val appointmentWithId = appointment.copy(id = documentReference.id)

                documentReference.set(appointmentWithId)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Запись успешно создана!", Toast.LENGTH_SHORT).show()

                        NotificationUtils.sendNotification(
                            userId = appointmentWithId.ownerId,
                            title = "📅 Запись создана",
                            message = "Вы записались на приём ${appointmentWithId.appointmentDate} в ${appointmentWithId.appointmentTime} к ${appointmentWithId.vetName}"
                        )

                        createReminderForAppointment(appointmentWithId)

                        // Возвращаемся назад через 2 секунды
                        Handler().postDelayed({
                            finish()
                        }, 2000)
                    }
                    .addOnFailureListener {
                        btnConfirmAppointment.isEnabled = true
                        btnConfirmAppointment.text = "✅ Подтвердить запись"
                        Toast.makeText(this, "Ошибка обновления записи", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                btnConfirmAppointment.isEnabled = true
                btnConfirmAppointment.text = "✅ Подтвердить запись"
                Toast.makeText(this, "Ошибка создания записи", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createReminderForAppointment(appointment: Appointment) {
        val reminder = hashMapOf(
            "userId" to appointment.ownerId,
            "appointmentId" to appointment.id,
            "title" to "Напоминание о приеме",
            "message" to "Завтра в ${appointment.appointmentTime} прием для ${appointment.petName} у ${appointment.vetName}",
            "reminderDate" to System.currentTimeMillis() + (23 * 60 * 60 * 1000),
            "isRead" to false,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection(FirestoreCollections.REMINDERS)
            .add(reminder)
            .addOnSuccessListener {
                Log.d(TAG, "Напоминание создано")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка создания напоминания: ${e.message}")
            }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        super.onBackPressed()
    }
}