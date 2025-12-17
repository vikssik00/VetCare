package com.example.vetcare.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.vetcare.ui.fragments.AdminDashboardFragment
import com.example.vetcare.ui.fragments.AdminScheduleFragment
import com.example.vetcare.ui.fragments.AdminUsersFragment
import com.example.vetcare.ui.fragments.AppointmentsFragment
import com.example.vetcare.ui.fragments.HomeFragment
import com.example.vetcare.ui.fragments.PetsFragment
import com.example.vetcare.ui.fragments.ProfileFragment
import com.example.vetcare.R
import com.example.vetcare.ui.fragments.VeterinarianHomeFragment
import com.example.vetcare.ui.fragments.VeterinarianScheduleFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    // UI элементы
    private lateinit var bottomNavigation: BottomNavigationView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Текущая роль пользователя
    private var userRole: String = "Владелец"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Проверка авторизации
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        initViews()
        setupNavigation()
        loadUserRole()
    }

    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupNavigation() {
        // Временно показываем стандартную навигацию
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)

        // Показываем заглушку пока загружается роль
        showFragment(HomeFragment())

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            // Навигация будет обновлена после loadUserRole()
            false
        }
    }

    /**
     * Загружает роль пользователя из Firestore
     */
    private fun loadUserRole() {
        val userId = auth.currentUser?.uid ?: return

        // Toast.makeText(this, "Загружаю роль для user: $userId", Toast.LENGTH_SHORT).show()

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Отладочный вывод ВСЕХ полей документа
                    println("DEBUG: Все поля документа:")
                    document.data?.forEach { (key, value) ->
                        println("DEBUG: $key = $value")
                    }

                    // Получаем роль из документа
                    val role = document.getString("role") ?: "Владелец"
                    val firstName = document.getString("firstName") ?:
                    document.getString("name") ?:
                    "Пользователь"

                    // Toast.makeText(this, "Роль из Firestore: $role, Имя: $firstName", Toast.LENGTH_LONG).show()
                    println("DEBUG: Роль пользователя: $role")

                    userRole = role
                    updateNavigationForRole(role)
                    showWelcomeMessage(firstName)

                } else {
                    Toast.makeText(this, "Документ пользователя не найден", Toast.LENGTH_SHORT).show()
                    createNewUserRecord(userId)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки: ${exception.message}", Toast.LENGTH_LONG).show()
                setupPetOwnerNavigation()
            }
    }

    /**
     * Создает запись нового пользователя в Firestore
     */
    private fun createNewUserRecord(userId: String) {
        val userEmail = auth.currentUser?.email ?: ""
        val userData = hashMapOf<String, Any>(
            "firstName" to userEmail.substringBefore("@"),
            "name" to userEmail.substringBefore("@"),
            "email" to userEmail,
            "role" to "Владелец",
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                setupPetOwnerNavigation()
                Toast.makeText(this, "Профиль создан", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                setupPetOwnerNavigation()
            }
    }

    /**
     * Обновляет навигацию в зависимости от роли пользователя
     * ВАЖНО: Используем те же значения, что и в Firestore!
     */
    private fun updateNavigationForRole(role: String) {
        when (role) {
            "Ветеринар" -> setupVeterinarianNavigation()
            "Администратор" -> setupAdminNavigation()
            else -> setupPetOwnerNavigation() // "Владелец" или любая другая роль
        }
    }

    /**
     * Навигация для владельца питомца
     */
    private fun setupPetOwnerNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment())
                    true
                }
                R.id.nav_pets -> {
                    showFragment(PetsFragment())
                    true
                }
                R.id.nav_appointments -> {
                    showFragment(AppointmentsFragment())
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            showFragment(HomeFragment())
        }

        bottomNavigation.selectedItemId = R.id.nav_home
    }

    /**
     * Навигация для ветеринара
     */
    private fun setupVeterinarianNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_nav_veterinarian)

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_vet_patients -> {
                    showFragment(VeterinarianHomeFragment())
                    true
                }
                R.id.nav_vet_schedule -> {
                    showFragment(VeterinarianScheduleFragment())
                    true
                }
                R.id.nav_vet_profile -> {
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        showFragment(VeterinarianHomeFragment())
    }

    /**
     * Навигация для администратора
     */
    private fun setupAdminNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_nav_admin)

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    showFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_users -> {
                    showFragment(AdminUsersFragment())
                    true
                }
                R.id.nav_schedule -> {
                    showFragment(AdminScheduleFragment())
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        showFragment(AdminDashboardFragment())
    }

    /**
     * Показывает фрагмент в контейнере
     */
    private fun showFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        } catch (e: Exception) {
            // Логируем ошибку, но не крашим приложение
            Toast.makeText(this, "Ошибка загрузки экрана", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * Показывает приветственное сообщение
     */
    private fun showWelcomeMessage(userName: String) {
        // Toast.makeText(this, "Добро пожаловать, $userName!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Выход из аккаунта
     */
    fun logout() {
        auth.signOut()
        goToLogin()
    }

    /**
     * Переход на экран входа
     */
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Обработка кнопки "Назад"
     */
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Навигация по табам (для вызова из фрагментов)
     */
    fun navigateToTab(tabId: Int) {
        bottomNavigation.selectedItemId = tabId
    }


}