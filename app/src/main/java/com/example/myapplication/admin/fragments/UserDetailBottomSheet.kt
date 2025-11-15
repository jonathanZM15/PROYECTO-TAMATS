package com.example.myapplication.admin.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.example.myapplication.R
import com.example.myapplication.admin.models.AdminUser
import com.example.myapplication.admin.models.AdminUserStatus
import java.text.SimpleDateFormat
import java.util.*

class UserDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_USER = "arg_user"

        fun newInstance(user: AdminUser): UserDetailBottomSheet {
            return UserDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_USER, user)
                }
            }
        }
    }

    private lateinit var user: AdminUser
    private var actionListener: OnActionListener? = null

    private lateinit var avatarView: TextView
    private lateinit var nameView: TextView
    private lateinit var emailView: TextView
    private lateinit var statusBadge: TextView

    private lateinit var joinDateView: TextView
    private lateinit var lastLoginView: TextView
    private lateinit var postsCountView: TextView
    private lateinit var suspensionInfoView: TextView

    private lateinit var blockButton: MaterialButton
    private lateinit var suspendButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    // viewPostsButton removed: 'Ver publicaciones' feature removed

    private lateinit var suspensionContainer: LinearLayout

    interface OnActionListener {
        fun onBlock(user: AdminUser)
        fun onUnblock(user: AdminUser)
        fun onSuspend(user: AdminUser, days: Int)
        fun onRemoveSuspension(user: AdminUser)
        fun onDelete(user: AdminUser)
        // fun onViewPosts(user: AdminUser) // removed onViewPosts from interface
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = arguments?.getSerializable(ARG_USER) as? AdminUser
            ?: throw IllegalArgumentException("User is required")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupUserInfo()
        setupActions()
        updateUIForUserStatus()

        // Listener para actualizar cuando se cierra el BottomSheet
        dialog?.setOnDismissListener {
            // Al cerrar, recargar los datos
        }
    }

    /**
     * Actualiza el usuario con nuevos datos
     */
    fun updateUser(newUser: AdminUser) {
        user = newUser
        if (view != null) {
            setupUserInfo()
            setupActions()
            updateUIForUserStatus()
        }
    }

    private fun initViews(view: View) {
        avatarView = view.findViewById(R.id.tvUserDetailAvatar)
        nameView = view.findViewById(R.id.tvUserDetailName)
        emailView = view.findViewById(R.id.tvUserDetailEmail)
        statusBadge = view.findViewById(R.id.tvUserDetailStatus)

        joinDateView = view.findViewById(R.id.tvUserDetailJoinDate)
        lastLoginView = view.findViewById(R.id.tvUserDetailLastLogin)
        postsCountView = view.findViewById(R.id.tvUserDetailPosts)
        suspensionInfoView = view.findViewById(R.id.tvSuspensionInfo)

        blockButton = view.findViewById(R.id.btnBlockUser)
        suspendButton = view.findViewById(R.id.btnSuspendUser)
        deleteButton = view.findViewById(R.id.btnDeleteUser)
        // btnViewPosts may have been removed from the layout; look it up dynamically by name
        // val viewPostsResId = resources.getIdentifier("btnViewPosts", "id", requireContext().packageName)
        // viewPostsButton = if (viewPostsResId != 0) view.findViewById(viewPostsResId) as? MaterialButton else null

        suspensionContainer = view.findViewById(R.id.suspensionContainer)
    }

    private fun setupUserInfo() {
        avatarView.text = user.getInitials()

        nameView.text = user.name.ifEmpty { "Sin nombre" }
        emailView.text = user.email.ifEmpty { "Sin email" }

        joinDateView.text = "Registro: ${user.joinDate.ifEmpty { "N/A" }}"
        lastLoginView.text = "Último acceso: ${user.lastLogin.ifEmpty { "N/A" }}"

        val postsText = when (user.posts) {
            0 -> "Sin publicaciones"
            1 -> "1 publicación"
            else -> "${user.posts} publicaciones"
        }
        postsCountView.text = postsText

        setupStatusBadge()
        setupSuspensionInfo()
    }

    private fun setupStatusBadge() {
        val status = user.getStatus()

        statusBadge.text = status.getDisplayText()

        val resourceName = status.getBackgroundResourceName()
        val resourceId = resources.getIdentifier(resourceName, "drawable", requireContext().packageName)
        if (resourceId != 0) {
            statusBadge.setBackgroundResource(resourceId)
        }
    }

    private fun setupSuspensionInfo() {
        if (user.isCurrentlySuspended()) {
            suspensionContainer.visibility = View.VISIBLE

            val daysLeft = user.getDaysLeftSuspension()
            val endDate = user.suspensionEnd?.let { timestamp ->
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(timestamp))
            } ?: "N/A"

            val suspensionText = """
                ⏰ Suspensión activa
                Días restantes: $daysLeft
                Termina: $endDate
            """.trimIndent()

            suspensionInfoView.text = suspensionText
        } else {
            suspensionContainer.visibility = View.GONE
        }
    }

    private fun setupActions() {
        setupBlockButton()
        setupSuspendButton()
        setupDeleteButton()
        // Setup view posts only if the button exists in the layout
        // if (viewPostsButton != null) setupViewPostsButton()
    }

    private fun setupBlockButton() {
        if (user.blocked) {
            blockButton.text = "Desbloquear Usuario"
        } else {
            blockButton.text = "Bloquear Usuario"
        }

        blockButton.setOnClickListener {
            if (user.blocked) {
                showUnblockConfirmation()
            } else {
                showBlockConfirmation()
            }
        }
    }

    private fun setupSuspendButton() {
        if (user.isCurrentlySuspended()) {
            suspendButton.text = "Remover Suspensión"

            suspendButton.setOnClickListener {
                showRemoveSuspensionConfirmation()
            }
        } else {
            suspendButton.text = "Suspender Usuario"

            suspendButton.setOnClickListener {
                showSuspensionOptions()
            }
        }
    }

    private fun setupDeleteButton() {
        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateUIForUserStatus() {
        when (user.getStatus()) {
            is AdminUserStatus.BLOCKED -> {
                suspendButton.isEnabled = false
                suspendButton.alpha = 0.5f
            }
            is AdminUserStatus.SUSPENDED -> {
                blockButton.isEnabled = false
                blockButton.alpha = 0.5f
            }
            is AdminUserStatus.ACTIVE -> {
                blockButton.isEnabled = true
                suspendButton.isEnabled = true
                blockButton.alpha = 1.0f
                suspendButton.alpha = 1.0f
            }
        }
    }

    private fun showBlockConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Bloquear Usuario")
            .setMessage("¿Estás seguro de que quieres bloquear a ${user.name}?\n\nEl usuario no podrá acceder a la aplicación.")
            .setPositiveButton("Bloquear") { _, _ ->
                actionListener?.onBlock(user)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showUnblockConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Desbloquear Usuario")
            .setMessage("¿Estás seguro de que quieres desbloquear a ${user.name}?")
            .setPositiveButton("Desbloquear") { _, _ ->
                actionListener?.onUnblock(user)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSuspensionOptions() {
        val options = arrayOf("1 día", "3 días", "7 días", "30 días", "Personalizado")
        val days = arrayOf(1, 3, 7, 30, -1)

        AlertDialog.Builder(requireContext())
            .setTitle("Suspender Usuario")
            .setItems(options) { _, which ->
                if (days[which] == -1) {
                    showCustomSuspensionDialog()
                } else {
                    showSuspensionConfirmation(days[which])
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCustomSuspensionDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Número de días (1-365)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Suspensión Personalizada")
            .setMessage("Ingresa el número de días:")
            .setView(input)
            .setPositiveButton("Suspender") { _, _ ->
                val daysText = input.text.toString()
                val days = daysText.toIntOrNull()

                when {
                    days == null -> {
                        Toast.makeText(requireContext(), "Número inválido", Toast.LENGTH_SHORT).show()
                    }
                    days < 1 || days > 365 -> {
                        Toast.makeText(requireContext(), "Debe ser entre 1 y 365 días", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        showSuspensionConfirmation(days)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSuspensionConfirmation(days: Int) {
        val dayText = if (days == 1) "día" else "días"

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Suspensión")
            .setMessage("¿Suspender a ${user.name} por $days $dayText?")
            .setPositiveButton("Suspender") { _, _ ->
                actionListener?.onSuspend(user, days)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRemoveSuspensionConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remover Suspensión")
            .setMessage("¿Estás seguro de que quieres remover la suspensión de ${user.name}?")
            .setPositiveButton("Remover") { _, _ ->
                actionListener?.onRemoveSuspension(user)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Eliminar Usuario")
            .setMessage(
                "Esta acción eliminará permanentemente la cuenta de ${user.name} " +
                "y todos sus datos asociados.\n\n" +
                "⚠️ Esta acción NO se puede deshacer."
            )
            .setPositiveButton("Eliminar") { _, _ ->
                actionListener?.onDelete(user)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun setOnActionListener(listener: OnActionListener) {
        this.actionListener = listener
    }
}
