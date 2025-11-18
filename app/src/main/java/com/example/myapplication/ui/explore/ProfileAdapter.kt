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
import com.example.myapplication.util.AgeCalculator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

data class ProfileItem(
    val documentSnapshot: DocumentSnapshot,
    val name: String,
    val email: String,
    val city: String,
    val description: String,
    val photoBase64: String?,
    val age: Int = 0
)

class ProfileAdapter(
    private val onProfileClick: (String) -> Unit,
    private val viewModel: ExploreViewModel? = null,
    private val onFavoriteChanged: (() -> Unit)? = null
) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    private val items = mutableListOf<ProfileItem>()
    private val db = FirebaseFirestore.getInstance()

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserCity: TextView = itemView.findViewById(R.id.tvUserCity)
        private val tvUserDescription: TextView = itemView.findViewById(R.id.tvUserDescription)
        private val tvUserAge: TextView = itemView.findViewById(R.id.tvUserAge)
        private val ivUserProfilePhoto: ImageView = itemView.findViewById(R.id.ivUserProfilePhoto)
        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        private val btnReject: ImageButton = itemView.findViewById(R.id.btnReject)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)

        fun bind(profile: ProfileItem) {
            tvUserName.text = profile.name
            tvUserCity.text = profile.city
            tvUserDescription.text = profile.description
            tvUserAge.text = profile.age.toString()

            // Click listener para el nombre
            tvUserName.setOnClickListener {
                if (profile.email.isNotEmpty()) {
                    onProfileClick(profile.email)
                }
            }

            // Click listener para el botón de like (corazón)
            btnLike.setOnClickListener {
                createMatch(profile)
            }

            // Click listener para el botón de reject (equis)
            btnReject.setOnClickListener {
                rejectProfile(profile, adapterPosition)
            }

            // Click listener para el botón de favorito (estrella)
            btnFavorite.setOnClickListener {
                toggleFavorite(profile, btnFavorite)
            }

            // Actualizar estado visual del botón de favorito
            updateFavoriteButtonState(profile, btnFavorite)

            // Cargar foto con Glide - con caché y placeholder
            if (!profile.photoBase64.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(profile.photoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivUserProfilePhoto)
                    }
                } catch (e: Exception) {
                    Log.w("ProfileAdapter", "Error cargando foto: ${e.message}")
                    ivUserProfilePhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserProfilePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        private fun createMatch(profile: ProfileItem) {
            // Obtener el email del usuario actual desde SharedPreferences
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""
            val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

            if (currentUserEmail.isEmpty()) {
                Toast.makeText(itemView.context, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
                return
            }

            // Verificar si ya existe un match activo (no rechazado) entre estos dos usuarios
            db.collection("matches")
                .whereEqualTo("fromUserEmail", currentUserEmail)
                .whereEqualTo("toUserEmail", profile.email)
                .get()
                .addOnSuccessListener { matchDocs ->
                    // Buscar si hay un match activo (no rechazado)
                    var activeMatchFound = false
                    var rejectedMatchFound = false

                    for (doc in matchDocs.documents) {
                        val rejected = doc.data?.get("rejected") as? Boolean ?: false
                        if (!rejected) {
                            activeMatchFound = true
                        } else {
                            rejectedMatchFound = true
                        }
                    }

                    when {
                        activeMatchFound -> {
                            // Ya existe un match activo
                            Toast.makeText(
                                itemView.context,
                                "Ya has enviado un match a ${profile.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        rejectedMatchFound -> {
                            // El match fue rechazado, permitir reenviar
                            obtenerYGuardarMatch(currentUserEmail, currentUserName, profile)
                        }
                        else -> {
                            // No existe match, crear uno nuevo
                            obtenerYGuardarMatch(currentUserEmail, currentUserName, profile)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error verificando match: ${e.message}")
                    Toast.makeText(itemView.context, "Error al verificar match", Toast.LENGTH_SHORT).show()
                }
        }

        private fun obtenerYGuardarMatch(
            currentUserEmail: String,
            currentUserName: String,
            profile: ProfileItem
        ) {
            // Obtener la foto del usuario actual desde Firebase
            db.collection("userProfiles")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { userProfileDocs ->
                    var currentUserPhoto = ""

                    if (userProfileDocs.documents.isNotEmpty()) {
                        currentUserPhoto = userProfileDocs.documents[0].data?.get("photo")?.toString() ?: ""
                    }

                    // Si no encontró en userProfiles, buscar en usuarios
                    if (currentUserPhoto.isEmpty()) {
                        db.collection("usuarios")
                            .whereEqualTo("email", currentUserEmail)
                            .get()
                            .addOnSuccessListener { usuariosDocs ->
                                if (usuariosDocs.documents.isNotEmpty()) {
                                    currentUserPhoto = usuariosDocs.documents[0].data?.get("photo")?.toString() ?: ""
                                }
                                saveMatch(currentUserEmail, currentUserName, currentUserPhoto, profile)
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileAdapter", "Error obteniendo foto de usuarios: ${e.message}")
                                saveMatch(currentUserEmail, currentUserName, "", profile)
                            }
                    } else {
                        saveMatch(currentUserEmail, currentUserName, currentUserPhoto, profile)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error obteniendo foto de userProfiles: ${e.message}")
                    // Continuar sin foto
                    db.collection("usuarios")
                        .whereEqualTo("email", currentUserEmail)
                        .get()
                        .addOnSuccessListener { usuariosDocs ->
                            var currentUserPhoto = ""
                            if (usuariosDocs.documents.isNotEmpty()) {
                                currentUserPhoto = usuariosDocs.documents[0].data?.get("photo")?.toString() ?: ""
                            }
                            saveMatch(currentUserEmail, currentUserName, currentUserPhoto, profile)
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("ProfileAdapter", "Error en fallback: ${e2.message}")
                            saveMatch(currentUserEmail, currentUserName, "", profile)
                        }
                }
        }

        private fun saveMatch(currentUserEmail: String, currentUserName: String, currentUserPhoto: String, profile: ProfileItem) {
            // Crear el documento de match con la foto del usuario actual
            val matchData = hashMapOf(
                "fromUserEmail" to currentUserEmail,
                "fromUserName" to currentUserName,
                "fromUserPhoto" to currentUserPhoto,
                "toUserEmail" to profile.email,
                "timestamp" to Timestamp.now(),
                "read" to false
            )

            // Guardar en Firebase
            db.collection("matches")
                .add(matchData)
                .addOnSuccessListener { documentReference ->
                    Log.d("ProfileAdapter", "Match creado exitosamente: ${documentReference.id}")
                    Toast.makeText(itemView.context, "¡Match con ${profile.name}!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error creando match: ${e.message}")
                    Toast.makeText(itemView.context, "Error al crear el match", Toast.LENGTH_SHORT).show()
                }
        }

        private fun rejectProfile(profile: ProfileItem, position: Int) {
            // Obtener el email del usuario actual desde SharedPreferences
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""

            if (currentUserEmail.isEmpty()) {
                Toast.makeText(itemView.context, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("ProfileAdapter", "Rechazando perfil: ${profile.email} para usuario: $currentUserEmail")

            // Crear documento de rechazo en Firebase
            val rejectionData = hashMapOf(
                "fromUserEmail" to currentUserEmail,
                "toUserEmail" to profile.email,
                "timestamp" to Timestamp.now(),
                "reason" to "user_rejected" // Motivo del rechazo
            )

            // Guardar el rechazo en la colección "rejections"
            db.collection("rejections")
                .add(rejectionData)
                .addOnSuccessListener { documentReference ->
                    Log.d("ProfileAdapter", "Perfil rechazado y guardado en BD: ${documentReference.id}")

                    // Eliminar el perfil de la lista visible
                    if (position < items.size) {
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        Log.d("ProfileAdapter", "Perfil eliminado de la lista. Total restante: ${items.size}")
                    }

                    Toast.makeText(itemView.context, "Perfil descartado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error rechazando perfil: ${e.message}")
                    Toast.makeText(itemView.context, "Error al descartar perfil", Toast.LENGTH_SHORT).show()
                }
        }

        private fun updateFavoriteButtonState(profile: ProfileItem, btnFavorite: ImageButton) {
            val isFavorite = viewModel?.favoriteEmails?.contains(profile.email) ?: false

            if (isFavorite) {
                // Mostrar en rojo si es favorito
                btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
                Log.d("ProfileAdapter", "Botón favorito mostrado en rojo para: ${profile.email}")
            } else {
                // Mostrar en gris si no es favorito
                btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                Log.d("ProfileAdapter", "Botón favorito mostrado en gris para: ${profile.email}")
            }
        }

        private fun toggleFavorite(profile: ProfileItem, btnFavorite: ImageButton) {
            // Obtener el email del usuario actual desde SharedPreferences
            val prefs = itemView.context.getSharedPreferences("user_data", 0)
            val currentUserEmail = prefs.getString("user_email", "") ?: ""

            if (currentUserEmail.isEmpty()) {
                Toast.makeText(itemView.context, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("ProfileAdapter", "Toggling favorito para: ${profile.email} del usuario: $currentUserEmail")

            // Verificar si ya es favorito
            db.collection("favorites")
                .whereEqualTo("fromUserEmail", currentUserEmail)
                .whereEqualTo("toUserEmail", profile.email)
                .get()
                .addOnSuccessListener { favoriteDocs ->
                    if (favoriteDocs.documents.isNotEmpty()) {
                        // Ya existe como favorito, eliminarlo
                        val favoriteId = favoriteDocs.documents[0].id
                        val positionEliminado = favoriteDocs.documents[0].data?.get("position") as? Number ?: 0

                        // CAMBIAR COLOR INMEDIATAMENTE
                        btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                        Log.d("ProfileAdapter", "Color cambiado a gris INMEDIATAMENTE")

                        db.collection("favorites")
                            .document(favoriteId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("ProfileAdapter", "Favorito eliminado: ${profile.email} en posición $positionEliminado")

                                // Actualizar el ViewModel
                                val updatedFavorites = viewModel?.favoriteEmails?.toMutableSet() ?: mutableSetOf()
                                updatedFavorites.remove(profile.email)
                                viewModel?.favoriteEmails = updatedFavorites

                                // Reorganizar los favoritos restantes (mover los que estaban después hacia arriba)
                                reorganizarFavoritosAlEliminar(currentUserEmail, positionEliminado.toInt())

                                // Notificar al Fragment para reorganizar la lista
                                onFavoriteChanged?.invoke()

                                Toast.makeText(itemView.context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileAdapter", "Error eliminando favorito: ${e.message}")
                                // Revertir color si hay error
                                btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
                                Toast.makeText(itemView.context, "Error al eliminar favorito", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Contar cuántos favoritos tiene el usuario actual
                        db.collection("favorites")
                            .whereEqualTo("fromUserEmail", currentUserEmail)
                            .get()
                            .addOnSuccessListener { allFavorites ->
                                if (allFavorites.size() >= 3) {
                                    // Máximo de 3 favoritos alcanzado
                                    Toast.makeText(itemView.context, "Máximo de 3 favoritos alcanzado", Toast.LENGTH_SHORT).show()
                                    Log.d("ProfileAdapter", "Límite de favoritos alcanzado")
                                } else {
                                    // CAMBIAR COLOR INMEDIATAMENTE
                                    btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
                                    Log.d("ProfileAdapter", "Color cambiado a rojo INMEDIATAMENTE")

                                    // Agregar nuevo favorito en la primera posición disponible
                                    val nuevaPosicion = allFavorites.size()
                                    val favoriteData = hashMapOf(
                                        "fromUserEmail" to currentUserEmail,
                                        "toUserEmail" to profile.email,
                                        "timestamp" to Timestamp.now(),
                                        "position" to nuevaPosicion
                                    )

                                    db.collection("favorites")
                                        .add(favoriteData)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d("ProfileAdapter", "Favorito agregado en posición $nuevaPosicion: ${documentReference.id}")

                                            // Actualizar el ViewModel
                                            val updatedFavorites = viewModel?.favoriteEmails?.toMutableSet() ?: mutableSetOf()
                                            updatedFavorites.add(profile.email)
                                            viewModel?.favoriteEmails = updatedFavorites

                                            // Notificar al Fragment para reorganizar la lista
                                            onFavoriteChanged?.invoke()

                                            Toast.makeText(itemView.context, "Agregado a favoritos (posición ${nuevaPosicion + 1})", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ProfileAdapter", "Error agregando favorito: ${e.message}")
                                            // Revertir color si hay error
                                            btnFavorite.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                                            Toast.makeText(itemView.context, "Error al agregar favorito", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileAdapter", "Error contando favoritos: ${e.message}")
                                Toast.makeText(itemView.context, "Error al verificar favoritos", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error verificando favorito: ${e.message}")
                    Toast.makeText(itemView.context, "Error al verificar favorito", Toast.LENGTH_SHORT).show()
                }
        }

        private fun reorganizarFavoritosAlEliminar(currentUserEmail: String, posicionEliminada: Int) {
            // Obtener todos los favoritos del usuario ordenados por posición
            db.collection("favorites")
                .whereEqualTo("fromUserEmail", currentUserEmail)
                .orderBy("position")
                .get()
                .addOnSuccessListener { favoriteDocs ->
                    Log.d("ProfileAdapter", "Reorganizando favoritos. Total: ${favoriteDocs.size()}")

                    // Para cada favorito que esté después del eliminado, decrementar su posición
                    for (doc in favoriteDocs.documents) {
                        val currentPosition = (doc.data?.get("position") as? Number)?.toInt() ?: 0

                        if (currentPosition > posicionEliminada) {
                            // Este favorito debe moverse una posición hacia arriba
                            val newPosition = currentPosition - 1
                            db.collection("favorites")
                                .document(doc.id)
                                .update("position", newPosition)
                                .addOnSuccessListener {
                                    Log.d("ProfileAdapter", "Posición actualizada: $currentPosition -> $newPosition")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ProfileAdapter", "Error actualizando posición: ${e.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileAdapter", "Error reorganizando favoritos: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_profile_card, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun addItems(newItems: List<ProfileItem>) {
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
        Log.d("ProfileAdapter", "Agregados ${newItems.size} items. Total: ${items.size}")
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun setItems(newItems: List<ProfileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

