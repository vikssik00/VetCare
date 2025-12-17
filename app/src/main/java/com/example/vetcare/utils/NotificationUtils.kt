package com.example.vetcare.utils

import com.google.firebase.firestore.FirebaseFirestore

object NotificationUtils {

    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(
        userId: String,
        title: String,
        message: String
    ) {
        val data = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "isRead" to false,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("notifications")
            .add(data)
    }
}