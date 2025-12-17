package com.example.vetcare.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.adapters.ChatAdapter
import com.example.vetcare.models.ChatBotAnswers
import com.example.vetcare.models.ChatMessage
import com.example.vetcare.R

class ChatBotFragment : Fragment() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Добавляем фото пользователя
            messages.add(ChatMessage(imageUri = it.toString(), isUser = true))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerChat.scrollToPosition(messages.size - 1)

            // Авто-ответ бота
            messages.add(ChatMessage(text = "Спасибо! Я передал фото ветеринару 🐾", isUser = false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerChat.scrollToPosition(messages.size - 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat_bot, container, false)

        recyclerChat = view.findViewById(R.id.recyclerChat)
        chatAdapter = ChatAdapter(messages)
        recyclerChat.layoutManager = LinearLayoutManager(requireContext())
        recyclerChat.adapter = chatAdapter

        // Приветственное сообщение бота
        addBotMessage("Здравствуйте! Я виртуальный помощник клиники 🤖")

        setupButtons(view)
        setupAttachButton(view)

        return view
    }

    private fun setupButtons(view: View) {
        val buttons = listOf(
            view.findViewById<Button>(R.id.btnQuestion1),
            view.findViewById<Button>(R.id.btnQuestion2),
            view.findViewById<Button>(R.id.btnQuestion3),
            view.findViewById<Button>(R.id.btnQuestion4)
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                val question = button.text.toString()
                addUserMessage(question)

                val answer = ChatBotAnswers.getAnswer(question)
                addBotMessage(answer)
            }
        }
    }

    private fun setupAttachButton(view: View) {
        val btnAttach = view.findViewById<ImageButton>(R.id.btnAttach)
        btnAttach.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text = text, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerChat.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text = text, isUser = false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerChat.scrollToPosition(messages.size - 1)
    }
}