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
import com.example.myapplication.model.RejectionNotification
import com.example.myapplication.util.ChatIdGenerator
import com.google.firebase.firestore.FirebaseFirestore

class RejectionAdapter(
    private val onRejectionRemoved: () -> Unit,
    private val onChatCreated: (String, String, String) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<RejectionAdapter.RejectionViewHolder>() {

    private val items = mutableListOf<RejectionNotification>()
    private val db = FirebaseFirestore.getInstance()

    inner class RejectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivRejectionUserPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvRejectionUserName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvRejectionMessage)
        private val btnClose: ImageButton = itemView.findViewById(R.id.btnRejectionClose)
        private val btnHeart: ImageButton? = itemView.findViewById(R.id.btnRejectionHeart)

        fun bind(rejection: RejectionNotification) {
            tvUserName.text = rejection.fromUserName
            tvMessage.text = "Ha rechazado tu match"

            // Cargar foto con Glide
            if (rejection.fromUserPhoto.isNotEmpty()) {
                try {
                    val decoded = Base64.decode(rejection.fromUserPhoto, Base64.DEFAULT)
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
                    Log.w("RejectionAdapter", "Error cargando foto: ${e.message}")
                    ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Botón cerrar
            btnClose.setOnClickListener {
                deleteRejection(rejection)
            }

            // Botón corazón (si existe) - para crear chat
            btnHeart?.setOnClickListener {
                acceptAndCreateChat(rejection)
            }
        }


        private fun acceptAndCreateChat(rejection: RejectionNotification) {
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""
            val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

            // Crear un ID único y consistente para el chat usando hash seguro
            val chatId = ChatIdGenerator.generateChatId(currentUserEmail, rejection.fromUserEmail)

            // Cargar la foto del usuario actual desde Firebase en lugar de SharedPreferences
            com.example.myapplication.cloud.FirebaseService.getUserProfile(currentUserEmail) { profileData ->
                val currentUserPhoto = if (profileData != null) {
                    profileData["photo"]?.toString() ?: ""
                } else {
                    ""
                }

                // Crear chat compartido
                val chat = hashMapOf(
                    "chatId" to chatId,
                    "user1Email" to currentUserEmail,
                    "user1Name" to currentUserName,
                    "user1Photo" to currentUserPhoto,
                    "user2Email" to rejection.fromUserEmail,
                    "user2Name" to rejection.fromUserName,
                    "user2Photo" to rejection.fromUserPhoto,
                    "lastMessage" to "",
                    "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("chats").document(chatId)
                    .set(chat)
                    .addOnSuccessListener {
                        // Eliminar la notificación de rechazo
                        deleteRejection(rejection)
                        Log.d("RejectionAdapter", "Chat compartido creado y notificación eliminada con ID: $chatId")
                        onChatCreated(rejection.fromUserEmail, rejection.fromUserName, rejection.fromUserPhoto)
                    }
                    .addOnFailureListener { e ->
                        Log.e("RejectionAdapter", "Error creando chat compartido: ${e.message}")
                    }
            }
        }

        private fun deleteRejection(rejection: RejectionNotification) {
            db.collection("rejectionNotifications").document(rejection.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("RejectionAdapter", "Notificación de rechazo eliminada")
                    val position = items.indexOf(rejection)
                    if (position != -1) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        onRejectionRemoved()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RejectionAdapter", "Error eliminando notificación: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RejectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rejection_notification, parent, false)
        return RejectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RejectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setRejections(newRejections: List<RejectionNotification>) {
        items.clear()
        items.addAll(newRejections)
        notifyDataSetChanged()
    }
}

