package com.example.vetcare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.Pet
import com.example.vetcare.R

class PetListAdapter(
    private val pets: MutableList<Pet>,
    private val onClick: (Pet) -> Unit,
    private val onEdit: (Pet) -> Unit,
    private val onDelete: (Pet) -> Unit
) : RecyclerView.Adapter<PetListAdapter.PetViewHolder>() {

    var hideButtons: Boolean = false

    inner class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPetName)
        val tvSpecies: TextView = view.findViewById(R.id.tvPetSpecies)
        val tvBirthDate: TextView = view.findViewById(R.id.tvPetBirthDate)
        val tvMedicalHistory: TextView = view.findViewById(R.id.tvPetMedicalHistory)
        val btnEdit: Button = view.findViewById(R.id.btnEditPet)
        val btnDelete: Button = view.findViewById(R.id.btnDeletePet)

        fun bind(pet: Pet) {
            tvName.text = pet.name
            tvSpecies.text = "Вид: ${pet.species}"
            tvBirthDate.text = "Дата рождения: ${pet.birthDate}"
            tvMedicalHistory.text = "Мед. история: ${pet.medicalHistory.takeIf { it.isNotEmpty() } ?: "нет"}"

            // Обработчики кликов
            itemView.setOnClickListener { onClick(pet) }
            btnEdit.setOnClickListener { onEdit(pet) }
            btnDelete.setOnClickListener { onDelete(pet) }

            // Управление видимостью кнопок
            if (hideButtons) {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
            } else {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount(): Int = pets.size

    fun updatePets(newPets: List<Pet>) {
        pets.clear()
        pets.addAll(newPets)
        notifyDataSetChanged()
    }
}