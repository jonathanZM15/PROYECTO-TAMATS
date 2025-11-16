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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.model.Match
import com.example.myapplication.model.RejectionNotification
import com.example.myapplication.util.ChatIdGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class MatchAdapter(
    private val onProfileClick: (String) -> Unit,
    private val onMatchRemoved: () -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    private val items = mutableListOf<Match>()
    private val db = FirebaseFirestore.getInstance()

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMatchUserPhoto: ImageView = itemView.findViewById(R.id.ivMatchUserPhoto)
        private val tvMatchUserName: TextView = itemView.findViewById(R.id.tvMatchUserName)
        private val tvMatchMessage: TextView = itemView.findViewById(R.id.tvMatchMessage)
        private val btnMatchAccept: ImageButton = itemView.findViewById(R.id.btnMatchAccept)
        private val btnMatchReject: ImageButton = itemView.findViewById(R.id.btnMatchReject)

        fun bind(match: Match) {
            tvMatchUserName.text = match.fromUserName
            tvMatchMessage.text = "¡Te ha hecho match!"

            // Cargar foto con Glide
            if (!match.fromUserPhoto.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(match.fromUserPhoto, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivMatchUserPhoto)
                    }
                } catch (e: Exception) {
                    Log.w("MatchAdapter", "Error cargando foto: ${e.message}")
                    ivMatchUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivMatchUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Click en la foto o nombre para ir al perfil
            ivMatchUserPhoto.setOnClickListener {
                onProfileClick(match.fromUserEmail)
            }

            tvMatchUserName.setOnClickListener {
                onProfileClick(match.fromUserEmail)
            }

            // Botón aceptar (corazón)
            btnMatchAccept.setOnClickListener {
                acceptMatch(match)
            }

            // Botón rechazar (equis)
            btnMatchReject.setOnClickListener {
                rejectMatch(match)
            }
        }

        private fun acceptMatch(match: Match) {
            // Obtener datos del usuario actual (receptor)
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""
            val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

            // Cargar la foto del usuario actual desde Firebase
            com.example.myapplication.cloud.FirebaseService.getUserProfile(currentUserEmail) { profileData ->
                val currentUserPhoto = if (profileData != null) {
                    profileData["photo"]?.toString() ?: ""
                } else {
                    ""
                }

                // Paso 1: Crear registro de match aceptado mutuamente
                createAcceptedMatch(
                    match.fromUserEmail,
                    match.fromUserName,
                    match.fromUserPhoto,
                    currentUserEmail,
                    currentUserName,
                    currentUserPhoto,
                    match.id
                )
            }
        }

        private fun createAcceptedMatch(
            user1Email: String,
            user1Name: String,
            user1Photo: String,
            user2Email: String,
            user2Name: String,
            user2Photo: String,
            originalMatchId: String
        ) {
            val acceptedMatch = hashMapOf(
                "user1Email" to user1Email,
                "user1Name" to user1Name,
                "user1Photo" to user1Photo,
                "user2Email" to user2Email,
                "user2Name" to user2Name,
                "user2Photo" to user2Photo,
                "acceptedAt" to Timestamp.now(),
                "mutualAcceptance" to true
            )

            db.collection("acceptedMatches")
                .add(acceptedMatch)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Match aceptado mutuamente guardado")

                    // Paso 2: Crear UN SOLO chat compartido
                    createSharedChat(user1Email, user1Name, user1Photo, user2Email, user2Name, user2Photo)

                    // Paso 3: Crear notificación para el usuario emisor (user1)
                    createMatchAcceptanceNotification(
                        toUserEmail = user1Email,
                        fromUserEmail = user2Email,
                        fromUserName = user2Name,
                        fromUserPhoto = user2Photo
                    )

                    // Paso 4: Eliminar la notificación original del receptor
                    deleteOriginalMatch(originalMatchId)
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error creando match aceptado: ${e.message}")
                }
        }

        private fun createSharedChat(
            user1Email: String,
            user1Name: String,
            user1Photo: String,
            user2Email: String,
            user2Name: String,
            user2Photo: String

        ) {
            // Crear un ID único y consistente para el chat usando hash seguro
            // Esto asegura que ambos usuarios ven el MISMO chat
            val chatId = ChatIdGenerator.generateChatId(user1Email, user2Email)

            val chat = hashMapOf(
                "chatId" to chatId,
                "user1Email" to user1Email,
                "user1Name" to user1Name,
                "user1Photo" to user1Photo,
                "user2Email" to user2Email,
                "user2Name" to user2Name,
                "user2Photo" to user2Photo,
                "lastMessage" to "",
                "lastMessageTimestamp" to Timestamp.now(),
                "createdAt" to Timestamp.now()
            )

            // Usar el chatId generado como ID del documento en Firestore
            db.collection("chats").document(chatId)
                .set(chat)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Chat compartido creado exitosamente con ID: $chatId")
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error creando chat compartido: ${e.message}")
                }
        }

        private fun createMatchAcceptanceNotification(
            toUserEmail: String,
            fromUserEmail: String,
            fromUserName: String,
            fromUserPhoto: String
        ) {
            val notification = hashMapOf(
                "fromUserEmail" to fromUserEmail,
                "fromUserName" to fromUserName,
                "fromUserPhoto" to fromUserPhoto,
                "toUserEmail" to toUserEmail,
                "acceptedAt" to Timestamp.now(),
                "read" to false
            )

            db.collection("matchAcceptanceNotifications")
                .add(notification)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Notificación de aceptación creada para: $toUserEmail")
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error creando notificación: ${e.message}")
                }
        }

        private fun deleteOriginalMatch(matchId: String) {
            db.collection("matches").document(matchId)
                .delete()
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Notificación original eliminada")
                    val position = items.indexOfFirst { it.id == matchId }
                    if (position != -1) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        onMatchRemoved()
                    }
                    Toast.makeText(itemView.context, "¡Match aceptado! Puedes escribirle", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error eliminando notificación original: ${e.message}")
                }
        }

        private fun createChat(
            user1Email: String,
            user1Name: String,
            user1Photo: String,
            user2Email: String,
            user2Name: String,
            user2Photo: String
        ) {
            val chat = hashMapOf(
                "user1Email" to user1Email,
                "user1Name" to user1Name,
                "user1Photo" to user1Photo,
                "user2Email" to user2Email,
                "user2Name" to user2Name,
                "user2Photo" to user2Photo,
                "lastMessage" to "",
                "lastMessageTimestamp" to Timestamp.now(),
                "createdAt" to Timestamp.now()
            )

            db.collection("chats")
                .add(chat)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Chat creado exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error creando chat: ${e.message}")
                }
        }

        private fun rejectMatch(match: Match) {
            // Obtener datos del usuario actual para la notificación de rechazo
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""
            val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

            // Marcar el match como rechazado
            val updates = hashMapOf<String, Any>(
                "rejected" to true,
                "rejectedAt" to Timestamp.now()
            )

            db.collection("matches").document(match.id)
                .update(updates)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Match rechazado exitosamente")

                    // Crear notificación de rechazo para el usuario que hizo el match
                    createRejectionNotification(
                        currentUserEmail,
                        currentUserName,
                        match.fromUserEmail,
                        match
                    )

                    val position = items.indexOf(match)
                    if (position != -1) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        onMatchRemoved()
                    }
                    Toast.makeText(itemView.context, "Match rechazado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error rechazando match: ${e.message}")
                    Toast.makeText(itemView.context, "Error al rechazar el match", Toast.LENGTH_SHORT).show()
                }
        }


        private fun createRejectionNotification(
            currentUserEmail: String,
            currentUserName: String,
            toUserEmail: String,
            match: Match
        ) {
            // Obtener la foto del usuario actual
            db.collection("userProfiles")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { userProfileDocs ->
                    var currentUserPhoto = ""

                    if (userProfileDocs.documents.isNotEmpty()) {
                        currentUserPhoto = userProfileDocs.documents[0].data?.get("photo")?.toString() ?: ""
                    }

                    if (currentUserPhoto.isEmpty()) {
                        db.collection("usuarios")
                            .whereEqualTo("email", currentUserEmail)
                            .get()
                            .addOnSuccessListener { usuariosDocs ->
                                if (usuariosDocs.documents.isNotEmpty()) {
                                    currentUserPhoto = usuariosDocs.documents[0].data?.get("photo")?.toString() ?: ""
                                }
                                saveRejectionNotification(currentUserEmail, currentUserName, currentUserPhoto, toUserEmail)
                            }
                            .addOnFailureListener { e ->
                                Log.e("MatchAdapter", "Error obteniendo foto: ${e.message}")
                                saveRejectionNotification(currentUserEmail, currentUserName, "", toUserEmail)
                            }
                    } else {
                        saveRejectionNotification(currentUserEmail, currentUserName, currentUserPhoto, toUserEmail)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error en userProfiles: ${e.message}")
                    db.collection("usuarios")
                        .whereEqualTo("email", currentUserEmail)
                        .get()
                        .addOnSuccessListener { usuariosDocs ->
                            var currentUserPhoto = ""
                            if (usuariosDocs.documents.isNotEmpty()) {
                                currentUserPhoto = usuariosDocs.documents[0].data?.get("photo")?.toString() ?: ""
                            }
                            saveRejectionNotification(currentUserEmail, currentUserName, currentUserPhoto, toUserEmail)
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("MatchAdapter", "Error en fallback: ${e2.message}")
                            saveRejectionNotification(currentUserEmail, currentUserName, "", toUserEmail)
                        }
                }
        }

        private fun saveRejectionNotification(
            fromUserEmail: String,
            fromUserName: String,
            fromUserPhoto: String,
            toUserEmail: String
        ) {
            val rejectionData = hashMapOf(
                "fromUserEmail" to fromUserEmail,
                "fromUserName" to fromUserName,
                "fromUserPhoto" to fromUserPhoto,
                "toUserEmail" to toUserEmail,
                "timestamp" to Timestamp.now(),
                "read" to false
            )

            db.collection("rejectionNotifications")
                .add(rejectionData)
                .addOnSuccessListener {
                    Log.d("MatchAdapter", "Notificación de rechazo creada exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e("MatchAdapter", "Error creando notificación de rechazo: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_notification, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setMatches(newMatches: List<Match>) {
        items.clear()
        items.addAll(newMatches)
        notifyDataSetChanged()
    }

    fun addMatch(match: Match) {
        items.add(0, match)
        notifyItemInserted(0)
    }

    fun removeMatch(matchId: String) {
        val index = items.indexOfFirst { it.id == matchId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}

