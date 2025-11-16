package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.model.MatchAcceptanceNotification
import com.google.firebase.firestore.FirebaseFirestore

class MatchAcceptanceNotificationAdapter(
    private val onNotificationRemoved: () -> Unit
) : RecyclerView.Adapter<MatchAcceptanceNotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<MatchAcceptanceNotification>()
    private val db = FirebaseFirestore.getInstance()

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivAcceptanceUserPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvAcceptanceUserName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAcceptanceMessage)
        private val btnClose: ImageButton = itemView.findViewById(R.id.btnAcceptanceClose)

        fun bind(notification: MatchAcceptanceNotification) {
            tvUserName.text = notification.fromUserName
            tvMessage.text = "¡Aceptó tu match!"

            // Cargar foto con Glide
            if (!notification.fromUserPhoto.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(notification.fromUserPhoto, Base64.DEFAULT)
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
                    Log.w("MatchAcceptanceNotificationAdapter", "Error cargando foto: ${e.message}")
                    ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Botón cerrar
            btnClose.setOnClickListener {
                deleteNotification(notification)
            }
        }

        private fun deleteNotification(notification: MatchAcceptanceNotification) {
            db.collection("matchAcceptanceNotifications").document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("MatchAcceptanceNotificationAdapter", "Notificación de aceptación eliminada")
                    val position = items.indexOf(notification)
                    if (position != -1) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        onNotificationRemoved()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAcceptanceNotificationAdapter", "Error eliminando notificación: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_acceptance_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setNotifications(newNotifications: List<MatchAcceptanceNotification>) {
        items.clear()
        items.addAll(newNotifications)
        notifyDataSetChanged()
    }

    fun addNotification(notification: MatchAcceptanceNotification) {
        items.add(0, notification)
        notifyItemInserted(0)
    }
}

