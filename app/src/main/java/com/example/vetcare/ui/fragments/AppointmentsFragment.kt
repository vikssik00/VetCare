package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.R
import com.example.vetcare.adapters.AppointmentsAdapter
import com.example.vetcare.models.Appointment
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.utils.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AppointmentsFragment : Fragment() {

    private lateinit var recyclerAppointments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val appointmentsList = mutableListOf<Appointment>()
    private lateinit var appointmentsAdapter: AppointmentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerAppointments = view.findViewById(R.id.recyclerAppointments)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        setupRecyclerView()
        loadAppointments()

        return view
    }

    private fun setupRecyclerView() {
        appointmentsAdapter = AppointmentsAdapter(appointmentsList) { appointment ->
            // Нажатие на запись
            showAppointmentDetails(appointment)
        }
        recyclerAppointments.layoutManager = LinearLayoutManager(requireContext())
        recyclerAppointments.adapter = appointmentsAdapter
    }

    private fun loadAppointments() {
        progressBar.visibility = View.VISIBLE
        val ownerId = auth.currentUser?.uid ?: return

        db.collection(FirestoreCollections.APPOINTMENTS)
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { result ->
                appointmentsList.clear()
                for (document in result) {
                    val appointment = document.toObject(Appointment::class.java).copy(id = document.id)
                    appointmentsList.add(appointment)
                }

                // Обновляем статусы: если дата/время прошло и статус "Запланирован", делаем "Завершен"
                val now = Calendar.getInstance()
                appointmentsList.forEach { appt ->
                    val apptTime = appt.getDateTimeAsCalendar()
                    // Проверяем, что apptTime не null
                    if (apptTime != null && appt.status == AppointmentStatus.SCHEDULED && apptTime.before(now)) {
                        appt.status = AppointmentStatus.COMPLETED
                        // Обновляем Firestore
                        db.collection(FirestoreCollections.APPOINTMENTS)
                            .document(appt.id)
                            .update("status", AppointmentStatus.COMPLETED)
                    }
                }


                // Сортировка по дате и времени
                appointmentsList.sortWith(
                    compareByDescending<Appointment> { it.appointmentDate }
                        .thenByDescending { it.appointmentTime }
                )

                appointmentsAdapter.notifyDataSetChanged()

                if (appointmentsList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "У вас пока нет записей"
                } else {
                    tvEmpty.visibility = View.GONE
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки записей", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun showAppointmentDetails(appointment: Appointment) {
        Toast.makeText(
            requireContext(),
            "${appointment.serviceName} у ${appointment.vetName}\n${appointment.getFullDateTime()}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}