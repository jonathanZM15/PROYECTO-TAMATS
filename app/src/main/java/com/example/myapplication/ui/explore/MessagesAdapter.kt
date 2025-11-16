package com.example.myapplication.ui.explore

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
import android.graphics.BitmapFactory
import android.util.Base64

class MessagesAdapter(
    private val currentUserEmail: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_RECEIVED = 4
    }

    private val items = mutableListOf<Message>()

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageSent)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestampSent)

        fun bind(message: Message) {
            tvMessage.text = message.content
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            tvTimestamp.text = formatter.format(message.timestamp.toDate())
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestampReceived)

        fun bind(message: Message) {
            tvMessage.text = message.content
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            tvTimestamp.text = formatter.format(message.timestamp.toDate())
        }
    }

    inner class SentImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImageSent)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.ivImageTimestampSent)

        fun bind(message: Message) {
            if (!message.imageUrl.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(message.imageUrl, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivImage)
                    }
                } catch (e: Exception) {
                    ivImage.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            tvTimestamp.text = formatter.format(message.timestamp.toDate())
        }
    }

    inner class ReceivedImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImageReceived)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.ivImageTimestampReceived)

        fun bind(message: Message) {
            if (!message.imageUrl.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(message.imageUrl, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivImage)
                    }
                } catch (e: Exception) {
                    ivImage.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            tvTimestamp.text = formatter.format(message.timestamp.toDate())
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = items[position]
        return when {
            message.senderEmail == currentUserEmail && message.type == "text" -> VIEW_TYPE_MESSAGE_SENT
            message.senderEmail != currentUserEmail && message.type == "text" -> VIEW_TYPE_MESSAGE_RECEIVED
            message.senderEmail == currentUserEmail && message.type == "image" -> VIEW_TYPE_IMAGE_SENT
            else -> VIEW_TYPE_IMAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_IMAGE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_sent, parent, false)
                SentImageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_received, parent, false)
                ReceivedImageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(items[position])
            is ReceivedMessageViewHolder -> holder.bind(items[position])
            is SentImageViewHolder -> holder.bind(items[position])
            is ReceivedImageViewHolder -> holder.bind(items[position])
        }
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

