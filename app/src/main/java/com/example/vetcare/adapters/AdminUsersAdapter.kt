package com.example.vetcare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.R
import com.example.vetcare.utils.UserRoles

class AdminUsersAdapter(
    private val users: List<Map<String, Any>>,
    private val onActionClick: (Map<String, Any>, String) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        val btnView: Button = view.findViewById(R.id.btnViewUser)
        val btnEdit: Button = view.findViewById(R.id.btnEditUser)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteUser)

        fun bind(user: Map<String, Any>) {
            val firstName = user["firstName"] as? String ?: ""
            val lastName = user["lastName"] as? String ?: ""
            val middleName = user["middleName"] as? String ?: ""
            val email = user["email"] as? String ?: ""
            val role = user["role"] as? String ?: "Владелец питомца"

            // Форматируем ФИО
            val fullName = buildString {
                append(lastName)
                if (firstName.isNotEmpty()) append(" $firstName")
                if (middleName.isNotEmpty()) append(" $middleName")
            }.trim()

            tvName.text = fullName
            tvEmail.text = email
            tvRole.text = role

            // Цвет роли
            val roleColor = when (role) {
                UserRoles.ADMIN -> android.R.color.holo_red_light
                UserRoles.VETERINARIAN -> android.R.color.holo_blue_light
                else -> android.R.color.holo_green_light
            }
            tvRole.setTextColor(ContextCompat.getColor(itemView.context, roleColor))

            // Кнопки действий
            btnView.setOnClickListener { onActionClick(user, "view") }
            btnEdit.setOnClickListener { onActionClick(user, "edit") }
            btnDelete.setOnClickListener { onActionClick(user, "delete") }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}