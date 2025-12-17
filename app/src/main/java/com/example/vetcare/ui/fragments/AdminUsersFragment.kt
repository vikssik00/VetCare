package com.example.vetcare.ui.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.vetcare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminUsersFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var tableContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvAdminTitle: TextView
    private lateinit var tvAllUsers: TextView
    private lateinit var btnAddUser: ImageButton
    private lateinit var spinnerFilter: Spinner

    private val usersList = mutableListOf<User>()
    private val filteredUsersList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_users, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews(view)
        setupSearch()
        setupFilter()
        setupAddButton()
        loadUsers()

        return view
    }

    private fun initViews(view: View) {
        tableContainer = view.findViewById(R.id.tableContainer)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        etSearch = view.findViewById(R.id.etSearch)
        tvAdminTitle = view.findViewById(R.id.tvAdminTitle)
        tvAllUsers = view.findViewById(R.id.tvAllUsers)
        btnAddUser = view.findViewById(R.id.btnAddUser)
        spinnerFilter = view.findViewById(R.id.spinnerFilter)

        tvAdminTitle.text = "Администрирование"
        tvAllUsers.text = "Управление пользователями"
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterUsers()
            }
        })
    }

    private fun setupFilter() {
        val filters = arrayOf("Все", "Владельцы", "Ветеринары", "Администраторы")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterUsers()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAddButton() {
        btnAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null)
        val etFirstName = dialogView.findViewById<EditText>(R.id.etEditFirstName)
        val etLastName = dialogView.findViewById<EditText>(R.id.etEditLastName)
        val etMiddleName = dialogView.findViewById<EditText>(R.id.etEditMiddleName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmail)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerEditRole)

        // Очищаем поля для нового пользователя
        etFirstName.text.clear()
        etLastName.text.clear()
        etMiddleName.text.clear()
        etEmail.text.clear()

        // Настройка спиннера ролей (теперь все 3 роли)
        val roles = arrayOf("Владелец питомца", "Ветеринар", "Администратор")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
        spinnerRole.setSelection(0) // По умолчанию Владелец

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить пользователя")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val firstName = etFirstName.text.toString().trim()
                val lastName = etLastName.text.toString().trim()
                val middleName = etMiddleName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val role = when (spinnerRole.selectedItem.toString()) {
                    "Ветеринар" -> "Ветеринар"
                    "Администратор" -> "Администратор"
                    else -> "Владелец питомца"
                }

                if (validateUserData(firstName, lastName, middleName, email)) {
                    addUserToFirestore(firstName, lastName, middleName, email, role)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun validateUserData(firstName: String, lastName: String, middleName: String, email: String): Boolean {
        if (firstName.isEmpty() || lastName.isEmpty() || middleName.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните имя, фамилию и отчество", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun addUserToFirestore(
        firstName: String,
        lastName: String,
        middleName: String,
        email: String,
        role: String
    ) {
        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "middleName" to middleName,
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Toast.makeText(
                        requireContext(),
                        "Пользователь с таким email уже существует",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .add(userData)
                    .addOnSuccessListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Пользователь добавлен")
                            .setMessage(
                                "Пользователь добавлен.\n\n" +
                                        "Он должен самостоятельно зарегистрироваться " +
                                        "с этим email через экран регистрации."
                            )
                            .setPositiveButton("OK", null)
                            .show()

                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Ошибка добавления: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }



    private fun showSuccessDialog(firstName: String, lastName: String, middleName: String, email: String, password: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Пользователь добавлен")
            .setMessage(
                "Пользователь успешно создан!\n\n" +
                        "Данные для входа:\n" +
                        "Email: $email\n" +
                        "Пароль: $password\n\n" +
                        "Сообщите эти данные пользователю."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                usersList.clear()
                for (document in result) {
                    val user = User(
                        id = document.id,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        middleName = document.getString("middleName") ?: "",
                        email = document.getString("email") ?: "",
                        role = document.getString("role") ?: "Владелец питомца",
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                    usersList.add(user)
                }

                // Сортируем по фамилии
                usersList.sortBy { it.lastName }

                filterUsers()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки пользователей", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun filterUsers() {
        val searchQuery = etSearch.text.toString().trim().lowercase()
        val filterType = spinnerFilter.selectedItem.toString()

        filteredUsersList.clear()

        usersList.forEach { user ->
            val matchesSearch = searchQuery.isEmpty() ||
                    user.firstName.lowercase().contains(searchQuery) ||
                    user.lastName.lowercase().contains(searchQuery) ||
                    user.middleName.lowercase().contains(searchQuery) ||
                    user.email.lowercase().contains(searchQuery)

            val matchesFilter = when (filterType) {
                "Все" -> true
                "Владельцы" -> user.role == "Владелец питомца"
                "Ветеринары" -> user.role == "Ветеринар"
                "Администраторы" -> user.role == "Администратор"
                else -> true
            }

            if (matchesSearch && matchesFilter) {
                filteredUsersList.add(user)
            }
        }

        updateTable()
    }

    private fun updateTable() {
        tableContainer.removeAllViews()

        if (filteredUsersList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (etSearch.text.isNotEmpty() || spinnerFilter.selectedItemPosition > 0) {
                "Пользователи не найдены"
            } else {
                "Нет пользователей в системе"
            }
            return
        } else {
            tvEmpty.visibility = View.GONE
        }

        addTableHeader()

        filteredUsersList.forEachIndexed { index, user ->
            val tableRow = createTableRow(user, index)
            tableContainer.addView(tableRow)
        }
    }

    private fun addTableHeader() {
        val headerRow = LinearLayout(requireContext())
        headerRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.setBackgroundResource(R.drawable.card_background)
        headerRow.setPadding(12, 12, 12, 12)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        marginParams.bottomMargin = 8
        headerRow.layoutParams = marginParams

        val headers = listOf("ФИО", "Email", "Роль", "Действия")
        val weights = listOf(2f, 2f, 1f, 1f)

        headers.forEachIndexed { index, headerText ->
            val tvHeader = TextView(requireContext())
            tvHeader.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[index])
            tvHeader.text = headerText
            tvHeader.textSize = 14f
            tvHeader.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
            tvHeader.setTypeface(null, Typeface.BOLD)
            headerRow.addView(tvHeader)
        }

        tableContainer.addView(headerRow)
    }

    private fun createTableRow(user: User, index: Int): LinearLayout {
        val tableRow = LinearLayout(requireContext())
        tableRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tableRow.orientation = LinearLayout.HORIZONTAL

        // Чередующийся фон для строк
        val backgroundColor = if (index % 2 == 0) {
            R.color.white
        } else {
            R.color.light_gray
        }
        tableRow.setBackgroundResource(backgroundColor)
        tableRow.setPadding(12, 12, 12, 12)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        marginParams.bottomMargin = 4
        tableRow.layoutParams = marginParams

        // ФИО (теперь включая отчество)
        val tvName = TextView(requireContext())
        tvName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        tvName.textSize = 14f
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
        val fullName = "${user.lastName} ${user.firstName} ${user.middleName}".trim()
        tvName.text = fullName

        // Email
        val tvEmail = TextView(requireContext())
        tvEmail.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        tvEmail.textSize = 14f
        tvEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
        tvEmail.text = user.email
        tvEmail.maxLines = 2
        tvEmail.ellipsize = TextUtils.TruncateAt.END

        // Роль
        val tvRole = TextView(requireContext())
        tvRole.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvRole.textSize = 14f
        tvRole.text = user.role

        val roleColor = when (user.role) {
            "Администратор" -> R.color.error_red
            "Ветеринар" -> R.color.primary
            else -> R.color.success_green
        }
        tvRole.setTextColor(ContextCompat.getColor(requireContext(), roleColor))
        tvRole.setTypeface(null, Typeface.BOLD)

        // Кнопки действий
        val actionsLayout = LinearLayout(requireContext())
        actionsLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        actionsLayout.orientation = LinearLayout.HORIZONTAL
        actionsLayout.gravity = Gravity.CENTER

        // Проверяем, можно ли редактировать/удалять пользователя
        val currentUserId = auth.currentUser?.uid
        val canEdit = user.id != currentUserId // Нельзя редактировать себя
        val canDelete = user.id != currentUserId && user.role != "Администратор"

        // Кнопка просмотра
        val btnView = ImageButton(requireContext())
        val btnParams = LinearLayout.LayoutParams(36, 36)
        btnParams.marginEnd = 8
        btnView.layoutParams = btnParams
        btnView.setImageResource(R.drawable.ic_eye)
        btnView.background = null
        btnView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        btnView.setOnClickListener { viewUser(user) }

        // Кнопка редактирования
        val btnEdit = ImageButton(requireContext())
        btnEdit.layoutParams = btnParams
        btnEdit.setImageResource(R.drawable.ic_edit)
        btnEdit.background = null
        btnEdit.scaleType = ImageView.ScaleType.CENTER_INSIDE
        btnEdit.setOnClickListener { editUser(user) }
        btnEdit.isEnabled = canEdit
        btnEdit.alpha = if (canEdit) 1.0f else 0.3f

        // Кнопка удаления
        val btnDelete = ImageButton(requireContext())
        btnDelete.layoutParams = btnParams
        btnDelete.setImageResource(R.drawable.ic_delete)
        btnDelete.background = null
        btnDelete.scaleType = ImageView.ScaleType.CENTER_INSIDE
        btnDelete.setOnClickListener { deleteUser(user) }
        btnDelete.isEnabled = canDelete
        btnDelete.alpha = if (canDelete) 1.0f else 0.3f

        actionsLayout.addView(btnView)
        actionsLayout.addView(btnEdit)
        actionsLayout.addView(btnDelete)

        tableRow.addView(tvName)
        tableRow.addView(tvEmail)
        tableRow.addView(tvRole)
        tableRow.addView(actionsLayout)

        return tableRow
    }

    private fun viewUser(user: User) {
        // Показываем детальную информацию о пользователе
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val createdDate = dateFormat.format(Date(user.createdAt))

        AlertDialog.Builder(requireContext())
            .setTitle("Информация о пользователе")
            .setMessage(
                "Фамилия: ${user.lastName}\n" +
                        "Имя: ${user.firstName}\n" +
                        "Отчество: ${user.middleName}\n" +
                        "Email: ${user.email}\n" +
                        "Роль: ${user.role}\n" +
                        "Зарегистрирован: $createdDate"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun editUser(user: User) {
        // Проверяем, можно ли редактировать
        if (user.id == auth.currentUser?.uid) {
            Toast.makeText(requireContext(), "Нельзя редактировать свой профиль", Toast.LENGTH_SHORT).show()
            return
        }

        showEditUserDialog(user)
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null)
        val etFirstName = dialogView.findViewById<EditText>(R.id.etEditFirstName)
        val etLastName = dialogView.findViewById<EditText>(R.id.etEditLastName)
        val etMiddleName = dialogView.findViewById<EditText>(R.id.etEditMiddleName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmail)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerEditRole)

        // Заполняем текущие данные
        etFirstName.setText(user.firstName)
        etLastName.setText(user.lastName)
        etMiddleName.setText(user.middleName)
        etEmail.setText(user.email)

        // Настройка спиннера ролей
        val roles = arrayOf("Владелец питомца", "Ветеринар", "Администратор")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Устанавливаем текущую роль
        val position = when (user.role) {
            "Ветеринар" -> 1
            "Администратор" -> 2
            else -> 0
        }
        spinnerRole.setSelection(position)

        // Если редактируем себя - блокируем смену роли
        if (user.id == auth.currentUser?.uid) {
            spinnerRole.isEnabled = false
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать пользователя")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val firstName = etFirstName.text.toString().trim()
                val lastName = etLastName.text.toString().trim()
                val middleName = etMiddleName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val role = spinnerRole.selectedItem.toString()

                if (validateUserData(firstName, lastName, middleName, email)) {
                    val updatedData = hashMapOf<String, Any>(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "middleName" to middleName,
                        "email" to email,
                        "role" to role,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    updateUserInFirestore(user.id, updatedData)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun deleteUser(user: User) {
        // Проверяем, можно ли удалять
        if (user.id == auth.currentUser?.uid) {
            Toast.makeText(requireContext(), "Нельзя удалить свой аккаунт", Toast.LENGTH_SHORT).show()
            return
        }

        if (user.role == "Администратор") {
            Toast.makeText(requireContext(), "Нельзя удалить администратора", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Удаление пользователя")
            .setMessage("Вы уверены, что хотите удалить пользователя ${user.lastName} ${user.firstName} ${user.middleName}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteUserFromFirestore(user.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUserInFirestore(userId: String, updatedData: Map<String, Any>) {
        db.collection("users").document(userId)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Пользователь успешно обновлен", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserFromFirestore(userId: String) {
        db.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Пользователь успешно удален", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

// Модель пользователя с добавленным полем middleName
data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "", // Добавлено отчество
    val email: String = "",
    val role: String = "Владелец питомца",
    val createdAt: Long = 0L
)