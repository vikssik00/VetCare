package com.example.vetcare.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.ui.fragments.PatientDetailFragment
import com.example.vetcare.models.Pet
import com.example.vetcare.adapters.PetListAdapter
import com.example.vetcare.R
import com.example.vetcare.ui.activities.AppointmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PetsFragment : Fragment() {

    private lateinit var recyclerPets: RecyclerView
    private val petList = mutableListOf<Pet>()
    private lateinit var petAdapter: PetListAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pets, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerPets = view.findViewById(R.id.recyclerPets)

        // Инициализируем адаптер с ВСЕМИ обработчиками
        petAdapter = PetListAdapter(
            pets = petList,
            onClick = { pet ->
                // Открываем детали питомца (PatientDetailFragment)
                openPetDetails(pet)
            },
            onEdit = { pet ->
                showAddOrEditPetDialog(pet)
            },
            onDelete = { pet ->
                deletePet(pet)
            }
        )

        recyclerPets.layoutManager = LinearLayoutManager(requireContext())
        recyclerPets.adapter = petAdapter

        // Загружаем питомцев
        loadPets()

        return view
    }

    private fun loadPets() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                petList.clear()
                for (doc in result) {
                    val pet = Pet(
                        petId = doc.id,
                        name = doc.getString("name") ?: "Без имени",
                        species = doc.getString("species") ?: "Не указано",
                        breed = doc.getString("breed") ?: "",
                        birthDate = doc.getString("birthDate") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        medicalHistory = doc.getString("medicalHistory") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        lastCheckup = doc.getString("lastCheckup") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                    petList.add(pet)
                }
                petAdapter.notifyDataSetChanged()

                // Если питомцев нет - показываем диалог добавления
                if (petList.isEmpty()) {
                    showNoPetsDialog()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки питомцев", Toast.LENGTH_SHORT).show()
            }
    }

    // Диалог "нет питомцев"
    private fun showNoPetsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Нет питомцев")
            .setMessage("У вас пока нет питомцев. Хотите добавить первого?")
            .setPositiveButton("Добавить питомца") { _, _ ->
                showAddOrEditPetDialog()
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    // Запись на прием для питомца
    private fun bookAppointmentForPet(pet: Pet) {
        // Открываем Activity записи на прием
        val intent = Intent(requireContext(), AppointmentActivity::class.java)
        intent.putExtra("petId", pet.petId)
        intent.putExtra("petName", pet.name)
        startActivity(intent)
    }

    // Открыть детали питомца
    private fun openPetDetails(pet: Pet) {
        // Проверяем, есть ли PatientDetailFragment
        try {
            val fragment = PatientDetailFragment.Companion.newInstance(pet.petId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("pets")
                .commit()
        } catch (e: Exception) {
            // Если PatientDetailFragment не найден, показываем Toast
            Toast.makeText(
                requireContext(),
                "${pet.name}\nВид: ${pet.species}\nПорода: ${pet.breed}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showAddOrEditPetDialog(pet: Pet? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_pet, null)

        val etName = dialogView.findViewById<EditText>(R.id.etPetName)
        val etSpecies = dialogView.findViewById<EditText>(R.id.etPetSpecies)
        val etBreed = dialogView.findViewById<EditText>(R.id.etPetBreed)
        val etBirthDate = dialogView.findViewById<EditText>(R.id.etPetBirthDate)
        val etMedicalHistory = dialogView.findViewById<EditText>(R.id.etPetMedicalHistory)

        if (pet != null) {
            etName.setText(pet.name)
            etSpecies.setText(pet.species)
            etBreed.setText(pet.breed)
            etBirthDate.setText(pet.birthDate)
            etMedicalHistory.setText(pet.medicalHistory)
        }

        val dialogTitle = if (pet == null) "Добавить питомца" else "Редактировать питомца"

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val species = etSpecies.text.toString().trim()
                val breed = etBreed.text.toString().trim()
                val birthDate = etBirthDate.text.toString().trim()
                val medicalHistory = etMedicalHistory.text.toString().trim()

                if (name.isEmpty() || species.isEmpty()) {
                    Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newPet = Pet(
                    petId = pet?.petId ?: "", // Будет создан в Firestore
                    name = name,
                    species = species,
                    breed = breed,
                    birthDate = birthDate,
                    ownerId = auth.currentUser?.uid ?: "",
                    medicalHistory = medicalHistory,
                    createdAt = pet?.createdAt ?: System.currentTimeMillis(),
                    isActive = true
                )

                savePetToFirestore(newPet, pet != null)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun savePetToFirestore(pet: Pet, isEdit: Boolean) {
        if (isEdit && pet.petId.isNotEmpty()) {
            // Обновление существующего питомца
            db.collection("pets").document(pet.petId)
                .set(pet)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Питомец обновлен", Toast.LENGTH_SHORT).show()
                    loadPets()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Создание нового питомца
            db.collection("pets")
                .add(pet)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Питомец добавлен", Toast.LENGTH_SHORT).show()
                    loadPets()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка добавления", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deletePet(pet: Pet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление питомца")
            .setMessage("Вы уверены, что хотите удалить ${pet.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                db.collection("pets").document(pet.petId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Питомец удалён", Toast.LENGTH_SHORT).show()
                        loadPets()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        loadPets()
    }
}