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
import com.example.myapplication.model.Chat
import java.text.SimpleDateFormat
import java.util.*

class AdminChatsAdapter(
    private val onChatClick: (Chat) -> Unit,
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Chat>()
    private var isLoadingMore = false

    companion object {
        private const val VIEW_TYPE_CHAT = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    inner class AdminChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivChatUserPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvChatUserName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvChatLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvChatTimestamp)

        fun bind(chat: Chat) {
            tvUserName.text = chat.user2Name
            tvLastMessage.text = chat.lastMessage.ifEmpty { "Comienza la conversación..." }

            // Formato de timestamp
            val calendar = Calendar.getInstance()
            calendar.time = chat.lastMessageTimestamp.toDate()
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTimestamp.text = formatter.format(calendar.time)

            // Cargar foto con Glide
            if (!chat.user2Photo.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(chat.user2Photo, Base64.DEFAULT)
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
                    Log.w("AdminChatsAdapter", "Error cargando foto: ${e.message}")
                    ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Click en el chat
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && isLoadingMore) VIEW_TYPE_LOADING else VIEW_TYPE_CHAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat, parent, false)
            AdminChatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdminChatViewHolder) {
            holder.bind(items[position])

            // Cargar más cuando está cerca del final
            if (position == items.size - 2 && !isLoadingMore) {
                isLoadingMore = true
                onLoadMore()
            }
        }
    }

    override fun getItemCount() = items.size + (if (isLoadingMore) 1 else 0)

    fun setChats(newChats: List<Chat>) {
        items.clear()
        items.addAll(newChats)
        isLoadingMore = false
        notifyDataSetChanged()
    }

    fun addChats(newChats: List<Chat>) {
        val startPosition = items.size
        items.addAll(newChats)
        isLoadingMore = false
        notifyItemRangeInserted(startPosition, newChats.size)
    }

    fun setLoading(isLoading: Boolean) {
        isLoadingMore = isLoading
        if (isLoading) {
            notifyItemInserted(items.size)
        } else {
            notifyItemRemoved(items.size)
        }
    }

    fun getChats(): List<Chat> = items
}

