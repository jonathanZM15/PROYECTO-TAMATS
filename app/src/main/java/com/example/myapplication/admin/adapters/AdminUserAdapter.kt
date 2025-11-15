package com.example.myapplication.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.myapplication.R
import com.example.myapplication.admin.models.AdminUser
import com.example.myapplication.admin.models.AdminUserStatus

/**
 * Adapter para la lista de usuarios en el panel de administración
 *
 * Funcionalidades:
 * - Muestra información básica del usuario
 * - Badge de estado (Activo/Bloqueado/Suspendido)
 * - Avatar con iniciales o foto
 * - Click para ver detalles
 * - Toggle rápido de estado
 * - Animaciones suaves con DiffUtil
 */
class AdminUserAdapter(
    private val onUserClick: (AdminUser) -> Unit,
    private val onToggleStatus: (AdminUser) -> Unit
) : ListAdapter<AdminUser, AdminUserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    /**
     * ViewHolder para cada item de usuario
     */
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val avatarImage: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val avatarInitials: TextView = itemView.findViewById(R.id.tvUserAvatarInitials)
        private val nameView: TextView = itemView.findViewById(R.id.tvUserName)
        private val emailView: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val statusBadge: TextView = itemView.findViewById(R.id.tvUserStatus)
        private val joinDateView: TextView = itemView.findViewById(R.id.tvJoinDate)
        private val postsCountView: TextView = itemView.findViewById(R.id.tvPostsCount)
        private val toggleButton: ImageView = itemView.findViewById(R.id.ivToggleStatus)
        private val moreButton: ImageView = itemView.findViewById(R.id.ivMoreOptions)

        fun bind(user: AdminUser) {
            // Información básica
            nameView.text = user.name.ifEmpty { "Sin nombre" }
            emailView.text = user.email.ifEmpty { "Sin email" }

            // Avatar con foto o iniciales
            setupAvatar(user)

            // Badge de estado
            setupStatusBadge(user)

            // Información adicional
            joinDateView.text = "Registro: ${user.joinDate.ifEmpty { "N/A" }}"
            postsCountView.text = "${user.posts} posts"

            // Botón de toggle rápido
            setupToggleButton(user)

            // Click listeners
            setupClickListeners(user)

            // Accesibilidad
            setupAccessibility(user)
        }

        private fun setupAvatar(user: AdminUser) {
            val photo = user.profileImageUrl?.trim() ?: ""

            if (photo.isNotEmpty()) {
                // Detectar si es URL, gs:// o data URI
                val lower = photo.lowercase()
                try {
                    when {
                        lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("gs://") || lower.startsWith("data:") -> {
                            // URL o data uri -> Glide puede manejar data: y http(s)
                            avatarInitials.visibility = View.GONE
                            avatarImage.visibility = View.VISIBLE

                            Glide.with(itemView.context)
                                .load(photo)
                                .circleCrop()
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(avatarImage)
                        }
                        else -> {
                            // Probablemente Base64 puro (sin prefijo). Intentar decodificar.
                            val decoded = try {
                                android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                            } catch (e: Exception) {
                                null
                            }

                            if (decoded != null) {
                                val bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                if (bmp != null) {
                                    avatarInitials.visibility = View.GONE
                                    avatarImage.visibility = View.VISIBLE
                                    avatarImage.setImageBitmap(bmp)
                                } else {
                                    // No es Base64 válido: fallback a iniciales
                                    showInitialsFallback(user)
                                }
                            } else {
                                showInitialsFallback(user)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showInitialsFallback(user)
                }
            } else {
                showInitialsFallback(user)
            }
        }

        private fun showInitialsFallback(user: AdminUser) {
            avatarImage.setImageResource(R.drawable.ic_person)
            avatarImage.visibility = View.VISIBLE
            avatarInitials.visibility = View.VISIBLE
            val initials = user.getInitials()
            avatarInitials.text = initials.ifEmpty { "?" }
        }

        private fun setupStatusBadge(user: AdminUser) {
            val status = user.getStatus()

            statusBadge.text = status.getDisplayText()

            val resourceName = status.getBackgroundResourceName()
            val resourceId = itemView.resources.getIdentifier(resourceName, "drawable", itemView.context.packageName)
            if (resourceId != 0) {
                statusBadge.setBackgroundResource(resourceId)
            }

            // Animación sutil para cambios de estado
            statusBadge.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction {
                    statusBadge.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }

        private fun setupToggleButton(user: AdminUser) {
            val iconRes = when {
                user.blocked -> R.drawable.ic_add
                user.isCurrentlySuspended() -> R.drawable.ic_star
                else -> R.drawable.ic_close
            }

            toggleButton.setImageResource(iconRes)

            // Color del icono según la acción
            val colorRes = when {
                user.blocked -> R.color.admin_action_unblock
                user.isCurrentlySuspended() -> R.color.admin_action_activate
                else -> R.color.admin_action_block
            }

            toggleButton.setColorFilter(itemView.context.getColor(colorRes))
        }

        private fun setupClickListeners(user: AdminUser) {
            // Click en el item completo para ver detalles
            itemView.setOnClickListener {
                onUserClick(user)

                // Animación de feedback
                itemView.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }

            // Click en toggle para acción rápida
            toggleButton.setOnClickListener {
                onToggleStatus(user)

                // Animación de rotación
                toggleButton.animate()
                    .rotationBy(180f)
                    .setDuration(300)
                    .start()
            }

            // Click en más opciones
            moreButton.setOnClickListener {
                onUserClick(user)
            }
        }

        private fun setupAccessibility(user: AdminUser) {
            val status = user.getStatus()

            itemView.contentDescription = "Usuario ${user.name}, ${status.getDisplayText()}"

            toggleButton.contentDescription = when {
                user.blocked -> "Desbloquear a ${user.name}"
                user.isCurrentlySuspended() -> "Reactivar a ${user.name}"
                else -> "Bloquear a ${user.name}"
            }

            moreButton.contentDescription = "Más opciones para ${user.name}"
        }
    }

    /**
     * DiffUtil.Callback para optimizar las actualizaciones de la lista
     */
    private class UserDiffCallback : DiffUtil.ItemCallback<AdminUser>() {

        override fun areItemsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: AdminUser, newItem: AdminUser): Any? {
            val changes = mutableListOf<String>()

            if (oldItem.name != newItem.name) changes.add("name")
            if (oldItem.email != newItem.email) changes.add("email")
            if (oldItem.blocked != newItem.blocked) changes.add("blocked")
            if (oldItem.suspended != newItem.suspended) changes.add("suspended")
            if (oldItem.posts != newItem.posts) changes.add("posts")

            return if (changes.isNotEmpty()) changes else null
        }
    }

    /**
     * Actualiza un item específico con animación
     */
    fun updateUser(updatedUser: AdminUser) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedUser.id }

        if (index != -1) {
            currentList[index] = updatedUser
            submitList(currentList)
        }
    }

    /**
     * Busca un usuario por ID
     */
    fun findUserById(userId: String): AdminUser? {
        return currentList.find { it.id == userId }
    }
}
