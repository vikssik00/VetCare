package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.Notification
import com.example.vetcare.adapters.NotificationAdapter
import com.example.vetcare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val list = mutableListOf<Notification>()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        recycler = view.findViewById(R.id.recyclerNotifications)
        progressBar = view.findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        loadNotifications()

        return view
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                list.clear()

                for (doc in result) {
                    list.add(
                        Notification(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    )
                }

                list.sortByDescending { it.createdAt }
                recycler.adapter = NotificationAdapter(list)


                recycler.adapter = NotificationAdapter(list)
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
            }
    }
}