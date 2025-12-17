package com.example.vetcare.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.ChatMessage
import com.example.vetcare.R
import com.squareup.picasso.Picasso

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER_TEXT = 1
        private const val VIEW_TYPE_BOT_TEXT = 2
        private const val VIEW_TYPE_USER_IMAGE = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isUser && message.imageUri != null -> VIEW_TYPE_USER_IMAGE
            message.isUser -> VIEW_TYPE_USER_TEXT
            else -> VIEW_TYPE_BOT_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER_TEXT -> {
                val view = inflater.inflate(R.layout.item_chat_message_user, parent, false)
                UserTextViewHolder(view)
            }
            VIEW_TYPE_BOT_TEXT -> {
                val view = inflater.inflate(R.layout.item_chat_message_bot, parent, false)
                BotTextViewHolder(view)
            }
            VIEW_TYPE_USER_IMAGE -> {
                val view = inflater.inflate(R.layout.item_chat_message_image, parent, false)
                UserImageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserTextViewHolder -> holder.bind(message)
            is BotTextViewHolder -> holder.bind(message)
            is UserImageViewHolder -> holder.bind(message)
        }
    }

    // ViewHolder для текстового сообщения пользователя
    class UserTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
        }
    }

    // ViewHolder для текстового сообщения бота
    class BotTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
        }
    }

    // ViewHolder для изображения пользователя
    class UserImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        fun bind(message: ChatMessage) {
            message.imageUri?.let {
                Picasso.get().load(Uri.parse(it)).into(ivImage)
            }
        }
    }
}