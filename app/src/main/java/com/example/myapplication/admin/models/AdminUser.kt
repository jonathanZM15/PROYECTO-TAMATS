package com.example.myapplication.admin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Modelo de datos para usuarios en el panel de administración
 *
 * @property id ID único del usuario
 * @property name Nombre completo del usuario
 * @property email Email del usuario
 * @property blocked Estado de bloqueo (true = bloqueado)
 * @property suspended Estado de suspensión temporal
 * @property suspensionEnd Timestamp cuando termina la suspensión
 * @property joinDate Fecha de registro formateada
 * @property lastLogin Fecha del último acceso formateada
 * @property posts Número de publicaciones del usuario
 * @property profileImageUrl URL de la imagen de perfil
 */
@Parcelize
data class AdminUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val blocked: Boolean = false,
    val suspended: Boolean = false,
    val suspensionEnd: Long? = null,
    val joinDate: String = "",
    val lastLogin: String = "",
    val posts: Int = 0,
    val profileImageUrl: String = ""
) : Parcelable {

    /**
     * Calcula el estado actual del usuario
     * Prioridad: Bloqueado > Suspendido > Activo
     */
    fun getStatus(): AdminUserStatus {
        return when {
            blocked -> AdminUserStatus.BLOCKED
            suspended && suspensionEnd != null && System.currentTimeMillis() < suspensionEnd -> {
                val daysLeft = ((suspensionEnd - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt() + 1
                AdminUserStatus.SUSPENDED(daysLeft)
            }
            else -> AdminUserStatus.ACTIVE
        }
    }

    /**
     * Genera las iniciales del nombre para el avatar
     * Toma las primeras letras de cada palabra (máximo 2)
     */
    fun getInitials(): String {
        return name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
    }

    /**
     * Verifica si el usuario está actualmente suspendido
     */
    fun isCurrentlySuspended(): Boolean {
        return suspended && suspensionEnd != null && System.currentTimeMillis() < suspensionEnd
    }

    /**
     * Calcula los días restantes de suspensión
     */
    fun getDaysLeftSuspension(): Int {
        return if (isCurrentlySuspended()) {
            ((suspensionEnd!! - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt() + 1
        } else 0
    }

    /**
     * Obtiene el color del avatar según el estado
     */
    fun getAvatarColor(): String {
        return when (getStatus()) {
            is AdminUserStatus.ACTIVE -> "#4CAF50"
            is AdminUserStatus.BLOCKED -> "#F44336"
            is AdminUserStatus.SUSPENDED -> "#FF9800"
        }
    }
}

/**
 * Estados posibles de un usuario en el sistema
 */
sealed class AdminUserStatus {
    object ACTIVE : AdminUserStatus()
    object BLOCKED : AdminUserStatus()
    data class SUSPENDED(val daysLeft: Int) : AdminUserStatus()

    /**
     * Texto para mostrar en la UI
     */
    fun getDisplayText(): String = when (this) {
        is ACTIVE -> "Activo"
        is BLOCKED -> "Bloqueado"
        is SUSPENDED -> "Suspendido (${daysLeft}d)"
    }

    /**
     * Color del badge según el estado
     */
    fun getColorResource(): Int = when (this) {
        is ACTIVE -> android.R.color.holo_green_dark
        is BLOCKED -> android.R.color.holo_red_dark
        is SUSPENDED -> android.R.color.holo_orange_dark
    }

    /**
     * Drawable del background del badge
     * Retorna el nombre del recurso (será resuelto en la UI)
     */
    fun getBackgroundResourceName(): String = when (this) {
        is ACTIVE -> "badge_active"
        is BLOCKED -> "badge_blocked"
        is SUSPENDED -> "badge_suspended"
    }
}
