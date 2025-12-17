// AuthPrefs.kt
package com.example.vetcare.utils

import android.content.Context
import android.content.SharedPreferences

class AuthPrefs(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER_ME = "remember_me"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginData(email: String, password: String, rememberMe: Boolean) {
        prefs.edit().apply {
            putString(KEY_EMAIL, email)
            if (rememberMe) {
                putString(KEY_PASSWORD, password)
            } else {
                remove(KEY_PASSWORD)
            }
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)
    fun shouldRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun clearLoginData() {
        prefs.edit().clear().apply()
    }
}