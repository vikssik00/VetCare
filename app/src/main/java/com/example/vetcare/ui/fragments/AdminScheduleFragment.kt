package com.example.vetcare.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.vetcare.R
import com.example.vetcare.models.VetSchedule
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminScheduleFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var btnAddSchedule: MaterialButton
    private lateinit var btnViewSchedule: MaterialButton
    private lateinit var btnManageVets: MaterialButton
    private lateinit var scheduleListContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTitle: TextView

    private val veterinariansList = mutableListOf<User>()
    private val schedulesList = mutableListOf<VetSchedule>()
    private var currentView: String = "schedule"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_schedule, container, false)
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupButtons()
        loadVeterinarians()
        loadSchedules()

        return view
    }

    private fun initViews(view: View) {
        btnAddSchedule = view.findViewById(R.id.btnAddSchedule)
        btnViewSchedule = view.findViewById(R.id.btnViewSchedule)
        btnManageVets = view.findViewById(R.id.btnManageVets)
        scheduleListContainer = view.findViewById(R.id.scheduleListContainer)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvTitle = view.findViewById(R.id.tvTitle)

        tvTitle.text = "Управление расписанием"
    }

    private fun setupButtons() {
        btnAddSchedule.setOnClickListener {
            showAddScheduleDialog()
        }

        btnViewSchedule.setOnClickListener {
            switchToScheduleView()
        }

        btnManageVets.setOnClickListener {
            switchToVetsView()
        }

        switchToScheduleView()
    }

    private fun switchToScheduleView() {
        currentView = "schedule"
        tvTitle.text = "Расписание клиники"

        btnViewSchedule.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
        btnViewSchedule.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        btnManageVets.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
        btnManageVets.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))

        loadSchedules()
    }

    private fun switchToVetsView() {
        currentView = "vets"
        tvTitle.text = "Ветеринары клиники"

        btnManageVets.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
        btnManageVets.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        btnViewSchedule.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
        btnViewSchedule.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))

        showVeterinariansList()
    }

    private fun loadVeterinarians() {
        db.collection("users")
            .whereEqualTo("role", "Ветеринар")
            .get()
            .addOnSuccessListener { result ->
                veterinariansList.clear()
                for (document in result) {
                    val vet = User(
                        id = document.id,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        middleName = document.getString("middleName") ?: "", // Добавлено отчество
                        email = document.getString("email") ?: "",
                        role = document.getString("role") ?: "Ветеринар"
                    )
                    veterinariansList.add(vet)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки ветеринаров", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSchedules() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        scheduleListContainer.removeAllViews()

        db.collection("vet_schedule")
            .orderBy("date")
            .get()
            .addOnSuccessListener { result ->
                schedulesList.clear()
                for (document in result) {
                    val schedule = VetSchedule(
                        id = document.id,
                        vetId = document.getString("vetId") ?: document.getString("vet_id") ?: "",
                        vetName = document.getString("vetName") ?: document.getString("vet_name")
                        ?: "",
                        date = document.getString("date") ?: "",
                        dayOfWeek = document.getString("dayOfWeek")
                            ?: document.getString("day_of_week") ?: "",
                        startTime = document.getString("startTime")
                            ?: document.getString("start_time") ?: "",
                        endTime = document.getString("endTime") ?: document.getString("end_time")
                        ?: "",
                        isAvailable = document.getBoolean("isAvailable")
                            ?: document.getBoolean("is_available") ?: true,
                        maxAppointments = (document.getLong("maxAppointments")
                            ?: document.getLong("max_appointments") ?: 10).toInt()
                    )
                    schedulesList.add(schedule)
                }

                if (currentView == "schedule") {
                    showSchedulesList()
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки расписания", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Ошибка загрузки данных"
            }
    }

    private fun showSchedulesList() {
        scheduleListContainer.removeAllViews()

        if (schedulesList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Расписание не настроено\nНажмите \"Добавить смену врача\""
            return
        }

        tvEmpty.visibility = View.GONE

        // Группируем по дате
        val schedulesByDate = schedulesList.groupBy { it.date }

        // Сортируем даты
        val sortedDates = schedulesByDate.keys.sorted()

        sortedDates.forEach { date ->
            addDateHeader(date)

            schedulesByDate[date]?.forEach { schedule ->
                val scheduleCard = createScheduleCard(schedule)
                scheduleListContainer.addView(scheduleCard)
            }
        }
    }

    private fun addDateHeader(date: String) {
        val dateHeader = TextView(requireContext())
        dateHeader.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val formattedDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("ru"))
            val dateObj = inputFormat.parse(date)
            outputFormat.format(dateObj)
        } catch (e: Exception) {
            date
        }

        dateHeader.text = formattedDate
        dateHeader.textSize = 16f
        dateHeader.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        dateHeader.setTypeface(null, Typeface.BOLD)
        dateHeader.setPadding(0, 16, 0, 8)

        scheduleListContainer.addView(dateHeader)
    }

    private fun createScheduleCard(schedule: VetSchedule): LinearLayout {
        val card = LinearLayout(requireContext())
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.card_background)
        card.setPadding(16, 16, 16, 16)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        marginParams.bottomMargin = 12
        card.layoutParams = marginParams

        // Первая строка: Врач
        val vetRow = LinearLayout(requireContext())
        vetRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        vetRow.orientation = LinearLayout.HORIZONTAL

        val tvVetName = TextView(requireContext())
        tvVetName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvVetName.text = schedule.vetName
        tvVetName.textSize = 16f
        tvVetName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
        tvVetName.setTypeface(null, Typeface.BOLD)

        // Пометка "сегодня" если смена сегодня
        if (schedule.isToday()) {
            val tvToday = TextView(requireContext())
            tvToday.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tvToday.text = "Сегодня"
            tvToday.textSize = 12f
            tvToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            tvToday.setPadding(8, 4, 8, 4)
            tvToday.background = ContextCompat.getDrawable(requireContext(), R.drawable.card_background)
            vetRow.addView(tvToday)
        }

        vetRow.addView(tvVetName)

        // Вторая строка: Время и статус
        val timeRow = LinearLayout(requireContext())
        timeRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.setPadding(0, 8, 0, 8)

        val tvTime = TextView(requireContext())
        tvTime.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvTime.text = schedule.getTimeRange()
        tvTime.textSize = 14f
        tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))

        val tvStatus = TextView(requireContext())
        tvStatus.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tvStatus.text = if (schedule.isAvailable) "✓ Доступен" else "✗ Не доступен"
        tvStatus.textSize = 12f
        tvStatus.setTextColor(
            ContextCompat.getColor(requireContext(),
            if (schedule.isAvailable) R.color.success_green else R.color.error_red
        ))
        tvStatus.setPadding(12, 4, 12, 4)
        tvStatus.background = ContextCompat.getDrawable(requireContext(), R.drawable.card_background)

        timeRow.addView(tvTime)
        timeRow.addView(tvStatus)

        // Третья строка: Кнопки действий
        val actionsRow = LinearLayout(requireContext())
        actionsRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        actionsRow.orientation = LinearLayout.HORIZONTAL
        actionsRow.gravity = Gravity.END

        val btnEdit = Button(requireContext())
        btnEdit.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnEdit.text = "Изменить"
        btnEdit.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
        btnEdit.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        btnEdit.setOnClickListener { editSchedule(schedule) }

        val btnDelete = Button(requireContext())
        val deleteParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        deleteParams.marginStart = 8
        btnDelete.layoutParams = deleteParams
        btnDelete.text = "Удалить"
        btnDelete.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error_red))
        btnDelete.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        btnDelete.setOnClickListener { deleteSchedule(schedule) }

        actionsRow.addView(btnEdit)
        actionsRow.addView(btnDelete)

        card.addView(vetRow)
        card.addView(timeRow)
        card.addView(actionsRow)

        return card
    }

    private fun showVeterinariansList() {
        scheduleListContainer.removeAllViews()

        if (veterinariansList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Ветеринары не найдены"
            return
        }

        tvEmpty.visibility = View.GONE

        veterinariansList.forEach { vet ->
            val vetCard = createVeterinarianCard(vet)
            scheduleListContainer.addView(vetCard)
        }
    }

    private fun createVeterinarianCard(vet: User): LinearLayout {
        val card = LinearLayout(requireContext())
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.card_background)
        card.setPadding(16, 16, 16, 16)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        marginParams.bottomMargin = 12
        card.layoutParams = marginParams

        // Полное ФИО с отчеством
        val tvName = TextView(requireContext())
        tvName.text = "${vet.lastName} ${vet.firstName} ${vet.middleName}" // Добавлено отчество
        tvName.textSize = 16f
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
        tvName.setTypeface(null, Typeface.BOLD)

        val tvEmail = TextView(requireContext())
        tvEmail.text = vet.email
        tvEmail.textSize = 14f
        tvEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        val tvRole = TextView(requireContext())
        tvRole.text = "Ветеринар"
        tvRole.textSize = 12f
        tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        tvRole.setTypeface(null, Typeface.BOLD)

        card.addView(tvName)
        card.addView(tvEmail)
        card.addView(tvRole)

        return card
    }

    private fun showAddScheduleDialog() {
        if (veterinariansList.isEmpty()) {
            Toast.makeText(requireContext(), "Нет доступных ветеринаров", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null)
        val spinnerVeterinarian = dialogView.findViewById<Spinner>(R.id.spinnerVeterinarian)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etStartTime = dialogView.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<EditText>(R.id.etEndTime)
        val etMaxAppointments = dialogView.findViewById<EditText>(R.id.etMaxAppointments)
        val cbIsAvailable = dialogView.findViewById<CheckBox>(R.id.cbIsAvailable)

        // Настройка спиннера ветеринаров с ФИО (включая отчество)
        val vetNames = veterinariansList.map {
            "${it.lastName} ${it.firstName} ${it.middleName}".trim()
        }
        val vetAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vetNames)
        vetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVeterinarian.adapter = vetAdapter

        // Значения по умолчанию
        etMaxAppointments.setText("10")
        cbIsAvailable.isChecked = true

        // Выбор даты
        etDate.setOnClickListener {
            showDatePickerDialog(etDate)
        }

        // Выбор времени начала
        etStartTime.setOnClickListener {
            showTimePickerDialog(etStartTime, "Выберите время начала")
        }

        // Выбор времени окончания
        etEndTime.setOnClickListener {
            showTimePickerDialog(etEndTime, "Выберите время окончания")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить смену врача")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val selectedIndex = spinnerVeterinarian.selectedItemPosition
                if (selectedIndex >= 0) {
                    val selectedVet = veterinariansList[selectedIndex]
                    val date = etDate.text.toString()
                    val startTime = etStartTime.text.toString()
                    val endTime = etEndTime.text.toString()
                    val maxAppointments = etMaxAppointments.text.toString().toIntOrNull() ?: 10
                    val isAvailable = cbIsAvailable.isChecked

                    if (validateScheduleData(date, startTime, endTime)) {
                        addScheduleToFirestore(selectedVet, date, startTime, endTime, maxAppointments, isAvailable)
                    }
                } else {
                    Toast.makeText(requireContext(), "Выберите ветеринара", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun showDatePickerDialog(etDate: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etDate.setText(dateFormat.format(selectedDate.time))
            }, year, month, day)

        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePickerDialog(etTime: EditText, title: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val time = String.Companion.format(
                Locale.getDefault(),
                "%02d:%02d",
                selectedHour,
                selectedMinute
            )
            etTime.setText(time)
        }, hour, minute, true)

        timePicker.setTitle(title)
        timePicker.show()
    }

    private fun validateScheduleData(date: String, startTime: String, endTime: String): Boolean {
        if (date.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите дату", Toast.LENGTH_SHORT).show()
            return false
        }
        if (startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите время начала и окончания", Toast.LENGTH_SHORT).show()
            return false
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val start = timeFormat.parse(startTime)
            val end = timeFormat.parse(endTime)
            if (end.before(start) || end.equals(start)) {
                Toast.makeText(requireContext(), "Время окончания должно быть позже времени начала", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Некорректный формат времени", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun addScheduleToFirestore(
        vet: User,
        date: String,
        startTime: String,
        endTime: String,
        maxAppointments: Int,
        isAvailable: Boolean
    ) {
        val dayOfWeek = getDayOfWeekFromDate(date)
        // Полное ФИО с отчеством
        val vetFullName = "${vet.lastName} ${vet.firstName} ${vet.middleName}".trim()

        val scheduleData = hashMapOf(
            "vetId" to vet.id,
            "vetName" to vetFullName,
            "date" to date,
            "dayOfWeek" to dayOfWeek,
            "startTime" to startTime,
            "endTime" to endTime,
            "maxAppointments" to maxAppointments,
            "isAvailable" to isAvailable,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("vet_schedule")
            .add(scheduleData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Смена врача добавлена", Toast.LENGTH_SHORT).show()
                loadSchedules()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка добавления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editSchedule(schedule: VetSchedule) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null)
        val spinnerVeterinarian = dialogView.findViewById<Spinner>(R.id.spinnerVeterinarian)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etStartTime = dialogView.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<EditText>(R.id.etEndTime)
        val etMaxAppointments = dialogView.findViewById<EditText>(R.id.etMaxAppointments)
        val cbIsAvailable = dialogView.findViewById<CheckBox>(R.id.cbIsAvailable)

        // Заполняем текущими данными
        etDate.setText(schedule.date)
        etStartTime.setText(schedule.startTime)
        etEndTime.setText(schedule.endTime)
        etMaxAppointments.setText(schedule.maxAppointments.toString())
        cbIsAvailable.isChecked = schedule.isAvailable

        // Находим текущего ветеринара в списке
        val vetNames = veterinariansList.map {
            "${it.lastName} ${it.firstName} ${it.middleName}".trim()
        }
        val currentVetIndex = veterinariansList.indexOfFirst {
            "${it.lastName} ${it.firstName} ${it.middleName}".trim() == schedule.vetName
        }

        val vetAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vetNames)
        vetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVeterinarian.adapter = vetAdapter

        if (currentVetIndex >= 0) {
            spinnerVeterinarian.setSelection(currentVetIndex)
        }

        // Выбор даты
        etDate.setOnClickListener {
            showDatePickerDialog(etDate)
        }

        // Выбор времени
        etStartTime.setOnClickListener {
            showTimePickerDialog(etStartTime, "Выберите время начала")
        }

        etEndTime.setOnClickListener {
            showTimePickerDialog(etEndTime, "Выберите время окончания")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Изменить смену врача")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val selectedIndex = spinnerVeterinarian.selectedItemPosition
                if (selectedIndex >= 0) {
                    val selectedVet = veterinariansList[selectedIndex]
                    val date = etDate.text.toString()
                    val startTime = etStartTime.text.toString()
                    val endTime = etEndTime.text.toString()
                    val maxAppointments = etMaxAppointments.text.toString().toIntOrNull() ?: 10
                    val isAvailable = cbIsAvailable.isChecked

                    if (validateScheduleData(date, startTime, endTime)) {
                        updateScheduleInFirestore(schedule.id, selectedVet, date, startTime, endTime, maxAppointments, isAvailable)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun updateScheduleInFirestore(
        scheduleId: String,
        vet: User,
        date: String,
        startTime: String,
        endTime: String,
        maxAppointments: Int,
        isAvailable: Boolean
    ) {
        val dayOfWeek = getDayOfWeekFromDate(date)
        // Полное ФИО с отчеством
        val vetFullName = "${vet.lastName} ${vet.firstName} ${vet.middleName}".trim()

        val scheduleData = hashMapOf<String, Any>(
            "vetId" to vet.id,
            "vetName" to vetFullName,
            "date" to date,
            "dayOfWeek" to dayOfWeek,
            "startTime" to startTime,
            "endTime" to endTime,
            "maxAppointments" to maxAppointments,
            "isAvailable" to isAvailable,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("vet_schedule").document(scheduleId)
            .update(scheduleData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Смена врача обновлена", Toast.LENGTH_SHORT).show()
                loadSchedules()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteSchedule(schedule: VetSchedule) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление смены")
            .setMessage("Удалить смену врача ${schedule.vetName} от ${formatDateForDisplay(schedule.date)}?")
            .setPositiveButton("Удалить") { _, _ ->
                db.collection("vet_schedule").document(schedule.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Смена удалена", Toast.LENGTH_SHORT).show()
                        loadSchedules()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getDayOfWeekFromDate(dateString: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ru"))
            dayFormat.format(date)
        } catch (e: Exception) {
            "Неизвестно"
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
}