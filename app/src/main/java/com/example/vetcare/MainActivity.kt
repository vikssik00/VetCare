package com.example.vetcare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Автоматический переход на LoginActivity через 2 секунды
        android.os.Handler().postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // закрываем MainActivity чтобы нельзя было вернуться назад
        }, 2000) // 2000 миллисекунд = 2 секунды
    }
}