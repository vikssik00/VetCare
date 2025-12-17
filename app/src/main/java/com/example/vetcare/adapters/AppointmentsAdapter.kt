package com.example.vetcare.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.Appointment
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.R
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentsAdapter(
    private val appointments: List<Appointment>,
    private val onItemClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvService: TextView = view.findViewById(R.id.tvService)
        val tvVetName: TextView = view.findViewById(R.id.tvVetName)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnCancel: TextView = view.findViewById(R.id.btnCancel)

        fun bind(appointment: Appointment) {
            tvService.text = appointment.serviceName
            tvVetName.text = "Ветеринар: ${appointment.vetName}"
            tvDateTime.text = appointment.getFullDateTime()
            tvStatus.text = appointment.status

            // Цвет статуса
            val statusColor = when (appointment.status) {
                AppointmentStatus.SCHEDULED -> android.R.color.holo_green_light
                AppointmentStatus.COMPLETED -> android.R.color.holo_blue_light
                AppointmentStatus.CANCELLED -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

            // Кнопка "Отмена" видна только для будущих записей и если не отменена
            btnCancel.visibility = if (appointment.isUpcoming() && appointment.status == AppointmentStatus.SCHEDULED) {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnCancel.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Отмена записи")
                    .setMessage("Вы действительно хотите отменить запись на ${appointment.getFullDateTime()} у ${appointment.vetName}?")
                    .setPositiveButton("Отменить") { _, _ ->
                        cancelAppointment(appointment)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }

            itemView.setOnClickListener {
                onItemClick(appointment)
            }
        }

        private fun cancelAppointment(appointment: Appointment) {
            val updateData = mapOf("status" to AppointmentStatus.CANCELLED, "updatedAt" to System.currentTimeMillis())
            db.collection(FirestoreCollections.APPOINTMENTS)
                .document(appointment.id)
                .update(updateData)
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Запись отменена", Toast.LENGTH_SHORT).show()
                    // Обновляем локально, чтобы UI сразу поменялся
                    appointment.status = AppointmentStatus.CANCELLED
                    notifyItemChanged(adapterPosition)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size
}