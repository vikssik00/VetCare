package com.example.vetcare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.Appointment
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.R
import java.text.SimpleDateFormat
import java.util.Locale

class VetScheduleAdapter(
    private val onItemClick: (Appointment) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val appointments = mutableListOf<Appointment>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APPOINTMENT = 1
    }

    fun submitList(newList: List<Appointment>) {
        appointments.clear()
        appointments.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (appointments[position].isHeader) TYPE_HEADER else TYPE_APPOINTMENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_schedule_header, parent, false))
        } else {
            AppointmentViewHolder(inflater.inflate(R.layout.item_schedule, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = appointments[position]

        if (holder is HeaderViewHolder) {
            holder.bind(item)
        }

        if (holder is AppointmentViewHolder) {
            holder.bind(item)
            holder.itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount(): Int = appointments.size

    // ---------- HEADER ----------
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        fun bind(item: Appointment) {
            tvDate.text = item.petName // уже отформатированная дата
        }
    }

    // ---------- APPOINTMENT ----------
    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvPetName: TextView = view.findViewById(R.id.tvPetName)
        private val tvService: TextView = view.findViewById(R.id.tvService)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)

        fun bind(item: Appointment) {
            tvDate.text = formatDate(item.appointmentDate)
            tvTime.text = item.appointmentTime
            tvPetName.text = item.petName
            tvService.text = item.serviceName

            val (text, color) = statusInfo(item.status)
            tvStatus.text = text
            tvStatus.setTextColor(color)
        }

        private fun formatDate(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val output = SimpleDateFormat("dd MMMM", Locale("ru"))
                output.format(input.parse(date)!!)
            } catch (e: Exception) {
                date
            }
        }

        private fun statusInfo(status: String): Pair<String, Int> {
            val ctx = itemView.context
            return when (status) {
                AppointmentStatus.COMPLETED ->
                    "✅ Завершен" to ContextCompat.getColor(ctx, R.color.success_green)

                AppointmentStatus.CANCELLED ->
                    "❌ Отменен" to ContextCompat.getColor(ctx, R.color.error_red)

                AppointmentStatus.SCHEDULED ->
                    "⏳ Запланирован" to ContextCompat.getColor(ctx, R.color.warning_orange)

                else ->
                    status to ContextCompat.getColor(ctx, R.color.text_color)
            }
        }
    }
}