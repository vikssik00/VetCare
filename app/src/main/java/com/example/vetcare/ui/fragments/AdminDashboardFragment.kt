package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.vetcare.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvUsersCount: TextView
    private lateinit var tvPetsCount: TextView
    private lateinit var tvAppointmentsToday: TextView
    private lateinit var tvVeterinariansCount: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        loadDashboardStats()

        return view
    }

    private fun initViews(view: View) {
        tvUsersCount = view.findViewById(R.id.tvUsersCount)
        tvPetsCount = view.findViewById(R.id.tvPetsCount)
        tvAppointmentsToday = view.findViewById(R.id.tvAppointmentsToday)
        tvVeterinariansCount = view.findViewById(R.id.tvVeterinariansCount)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun loadDashboardStats() {
        progressBar.visibility = View.VISIBLE

        // Формат даты должен совпадать с Firestore
        val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        // Альтернативные форматы на случай разных записей
        val todayAlt1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAlt2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        try {
            // Параллельные запросы
            val usersTask = db.collection("users").get()
            val petsTask = db.collection("pets").get()

            // Запросы на сегодня - пробуем разные форматы
            val appointmentsTask1 = db.collection("appointments")
                .whereEqualTo("appointment_date", today)
                .get()

            val appointmentsTask2 = db.collection("appointments")
                .whereEqualTo("appointment_date", todayAlt1)
                .get()

            val appointmentsTask3 = db.collection("appointments")
                .whereEqualTo("appointment_date", todayAlt2)
                .get()

            // Ветеринары - используем русское название
            val vetsTask = db.collection("users")
                .whereEqualTo("role", "Ветеринар")
                .get()

            Tasks.whenAllSuccess<Any>(
                usersTask,
                petsTask,
                appointmentsTask1,
                appointmentsTask2,
                appointmentsTask3,
                vetsTask
            ).addOnSuccessListener { results ->
                try {
                    // Обработка результатов
                    val usersCount = (results[0] as QuerySnapshot).size()
                    val petsCount = (results[1] as QuerySnapshot).size()

                    // Суммируем записи из разных форматов дат
                    val appointments1 = (results[2] as QuerySnapshot).size()
                    val appointments2 = (results[3] as QuerySnapshot).size()
                    val appointments3 = (results[4] as QuerySnapshot).size()
                    val totalAppointmentsToday = appointments1 + appointments2 + appointments3

                    val vetsCount = (results[5] as QuerySnapshot).size()

                    // Обновляем UI на главном потоке
                    requireActivity().runOnUiThread {
                        tvUsersCount.text = usersCount.toString()
                        tvPetsCount.text = petsCount.toString()
                        tvAppointmentsToday.text = totalAppointmentsToday.toString()
                        tvVeterinariansCount.text = vetsCount.toString()
                        progressBar.visibility = View.GONE

                        // Проверка если все нули
                        if (usersCount == 0 && petsCount == 0 && totalAppointmentsToday == 0 && vetsCount == 0) {
                            Toast.makeText(
                                requireContext(),
                                "Данные для статистики отсутствуют",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Ошибка обработки данных: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    e.printStackTrace()
                }
            }.addOnFailureListener { e ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Ошибка загрузки: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
            }

        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    // Добавьте константы ролей прямо в фрагмент для надежности
    companion object {
        const val ROLE_OWNER = "Владелец"
        const val ROLE_VETERINARIAN = "Ветеринар"
        const val ROLE_ADMIN = "Администратор"
    }
}