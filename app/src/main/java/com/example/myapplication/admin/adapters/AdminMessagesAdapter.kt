package com.example.myapplication.admin.adapters

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
import com.example.myapplication.model.Message
import java.text.SimpleDateFormat
import java.util.*

class AdminMessagesAdapter(
    private val currentUserEmail: String
) : RecyclerView.Adapter<AdminMessagesAdapter.MessageViewHolder>() {

    private val items = mutableListOf<Message>()

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ivUserPhoto: ImageView? = itemView.findViewById(R.id.ivUserPhoto)

        fun bind(message: Message) {
            tvMessage.text = message.content
            tvSenderName.text = message.senderName

            // Formato de timestamp
            val calendar = Calendar.getInstance()
            calendar.time = message.timestamp.toDate()
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTimestamp.text = formatter.format(calendar.time)

            // Cargar foto si existe
            ivUserPhoto?.let {
                // Obtener foto del usuario desde Firebase si es necesario
                Log.d("AdminMessagesAdapter", "Mostrando mensaje de: ${message.senderName}")
            }

            // Diferente alineación según si es enviado o recibido
            val isOutgoing = message.senderEmail == currentUserEmail
            val backgroundColor = if (isOutgoing) {
                itemView.context.getColor(R.color.message_sent_bg)
            } else {
                itemView.context.getColor(R.color.message_received_bg)
            }
            itemView.setBackgroundColor(backgroundColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setMessages(newMessages: List<Message>) {
        items.clear()
        items.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }
}

