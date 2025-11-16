package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.model.Chat

class ChatsAdapter(
    private val currentUserEmail: String,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    private val items = mutableListOf<Chat>()

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivChatUserPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvChatUserName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvChatLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvChatTimestamp)

        fun bind(chat: Chat) {
            // Determinar quién es el "otro usuario" según quién esté logeado
            val otherUserName: String
            val otherUserPhoto: String

            if (currentUserEmail == chat.user1Email) {
                // El usuario logeado es user1, mostrar info de user2
                otherUserName = chat.user2Name
                otherUserPhoto = chat.user2Photo
            } else {
                // El usuario logeado es user2, mostrar info de user1
                otherUserName = chat.user1Name
                otherUserPhoto = chat.user1Photo
            }

            tvUserName.text = otherUserName
            tvLastMessage.text = chat.lastMessage.ifEmpty { "Comienza la conversación..." }

            // Formato de timestamp
            val calendar = java.util.Calendar.getInstance()
            calendar.time = chat.lastMessageTimestamp.toDate()
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            tvTimestamp.text = formatter.format(calendar.time)

            // Cargar foto con Glide
            if (!otherUserPhoto.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(otherUserPhoto, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivUserPhoto)
                    }
                } catch (e: Exception) {
                    Log.w("ChatsAdapter", "Error cargando foto: ${e.message}")
                    ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Click en el chat - pasar info del otro usuario
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setChats(newChats: List<Chat>) {
        items.clear()
        items.addAll(newChats)
        notifyDataSetChanged()
    }

    fun addChat(chat: Chat) {
        items.add(0, chat)
        notifyItemInserted(0)
    }
}

