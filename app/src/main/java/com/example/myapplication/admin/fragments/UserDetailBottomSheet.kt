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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.text.SimpleDateFormat
import java.util.*

class UserDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_USER = "arg_user"

        fun newInstance(user: AdminUser): UserDetailBottomSheet {
            return UserDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER, user)
                }
            }
        }
    }

    private lateinit var user: AdminUser
    private var actionListener: OnActionListener? = null

    private lateinit var avatarView: TextView
    private lateinit var avatarImageView: ImageView
    private lateinit var nameView: TextView
    private lateinit var emailView: TextView
    private lateinit var statusBadge: TextView

    private lateinit var joinDateView: TextView
    private lateinit var lastLoginView: TextView
    private lateinit var suspensionInfoView: TextView

    private lateinit var blockButton: MaterialButton
    private lateinit var suspendButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    private lateinit var suspensionContainer: LinearLayout

    interface OnActionListener {
        fun onBlock(user: AdminUser)
        fun onUnblock(user: AdminUser)
        fun onSuspend(user: AdminUser, days: Int)
        fun onRemoveSuspension(user: AdminUser)
        fun onDelete(user: AdminUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = arguments?.getParcelable(ARG_USER) as? AdminUser
            ?: throw IllegalArgumentException(getString(R.string.admin_error_user_not_found))
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
        avatarImageView = view.findViewById(R.id.ivUserDetailPhoto)
        nameView = view.findViewById(R.id.tvUserDetailName)
        emailView = view.findViewById(R.id.tvUserDetailEmail)
        statusBadge = view.findViewById(R.id.tvUserDetailStatus)

        joinDateView = view.findViewById(R.id.tvUserDetailJoinDate)
        lastLoginView = view.findViewById(R.id.tvUserDetailLastLogin)
        suspensionInfoView = view.findViewById(R.id.tvSuspensionInfo)

        blockButton = view.findViewById(R.id.btnBlockUser)
        suspendButton = view.findViewById(R.id.btnSuspendUser)
        deleteButton = view.findViewById(R.id.btnDeleteUser)

        suspensionContainer = view.findViewById(R.id.suspensionContainer)
    }

    private fun setupUserInfo() {
        // Si hay URL/imagen, cargar con Glide; sino mostrar iniciales
        val photo = user.profileImageUrl?.trim() ?: ""
        android.util.Log.d("UserDetailBottomSheet", "setupUserInfo - photo value: '$photo' for user: ${user.email}")
        if (photo.isNotEmpty()) {
            try {
                avatarView.visibility = View.GONE
                avatarImageView.visibility = View.VISIBLE

                val lower = photo.lowercase()
                when {
                    lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:") -> {
                        Glide.with(this)
                            .load(photo)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(avatarImageView)
                    }
                    lower.startsWith("gs://") -> {
                        // gs:// references: resolver a downloadUrl con Firebase Storage
                        try {
                            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                            val ref = storage.getReferenceFromUrl(photo)
                            ref.downloadUrl
                                .addOnSuccessListener { uri ->
                                    Glide.with(this)
                                        .load(uri)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .into(avatarImageView)
                                }
                                .addOnFailureListener {
                                    // fallback a iniciales
                                    avatarView.visibility = View.VISIBLE
                                    avatarImageView.visibility = View.GONE
                                    avatarView.text = user.getInitials()
                                }
                        } catch (e: Exception) {
                            avatarView.visibility = View.VISIBLE
                            avatarImageView.visibility = View.GONE
                            avatarView.text = user.getInitials()
                        }
                    }
                    photo.contains("/") -> {
                        // Es probable que sea una ruta relativa en Firebase Storage, intentar obtener downloadUrl
                        try {
                            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                            val ref = storage.reference.child(photo)
                            ref.downloadUrl
                                .addOnSuccessListener { uri ->
                                    Glide.with(this)
                                        .load(uri)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .into(avatarImageView)
                                }
                                .addOnFailureListener {
                                    // Si falla, seguir con intento Base64
                                    tryLoadBase64OrFallback(photo)
                                }
                        } catch (e: Exception) {
                            tryLoadBase64OrFallback(photo)
                        }
                    }
                    else -> {
                        // Intentar decodificar Base64 puro (sin prefijo)
                        tryLoadBase64OrFallback(photo)
                    }
                }
            } catch (e: Exception) {
                avatarView.visibility = View.VISIBLE
                avatarImageView.visibility = View.GONE
                avatarView.text = user.getInitials()
            }
        } else {
            avatarView.visibility = View.VISIBLE
            avatarImageView.visibility = View.GONE
            avatarView.text = user.getInitials()
        }

        nameView.text = user.name.ifEmpty { getString(R.string.admin_user_info_title) }
        emailView.text = user.email.ifEmpty { getString(R.string.sin_email) }

        val joinDateText = user.joinDate.ifEmpty { getString(R.string.n_a) }
        val lastLoginText = user.lastLogin.ifEmpty { getString(R.string.n_a) }

        joinDateView.text = "${getString(R.string.label_registration)} $joinDateText"
        lastLoginView.text = "${getString(R.string.label_last_access)} $lastLoginText"

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
                ⏰ ${getString(R.string.admin_suspension_active)}
                ${getString(R.string.admin_days_left)}: $daysLeft
                ${getString(R.string.admin_ends)}: $endDate
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
    }

    private fun setupBlockButton() {
        if (user.blocked) {
            blockButton.text = getString(R.string.admin_unblock_user)
        } else {
            blockButton.text = getString(R.string.admin_block_user)
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
            suspendButton.text = getString(R.string.admin_remove_suspension)

            suspendButton.setOnClickListener {
                showRemoveSuspensionConfirmation()
            }
        } else {
            suspendButton.text = getString(R.string.admin_suspend_user)

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
            .setTitle(getString(R.string.admin_confirm_block_title))
            .setMessage(getString(R.string.admin_confirm_block_message, user.name))
            .setPositiveButton(getString(R.string.admin_block_user)) { _, _ ->
                actionListener?.onBlock(user)
                dismiss()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showUnblockConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_confirm_unblock_title))
            .setMessage(getString(R.string.admin_confirm_unblock_message, user.name))
            .setPositiveButton(getString(R.string.admin_unblock_user)) { _, _ ->
                actionListener?.onUnblock(user)
                dismiss()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showSuspensionOptions() {
        val options = arrayOf(
            getString(R.string.admin_suspension_1_day),
            getString(R.string.admin_suspension_3_days),
            getString(R.string.admin_suspension_7_days),
            getString(R.string.admin_suspension_30_days),
            getString(R.string.admin_suspension_custom)
        )
        val days = arrayOf(1, 3, 7, 30, -1)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_suspension_options_title))
            .setItems(options) { _, which ->
                if (days[which] == -1) {
                    showCustomSuspensionDialog()
                } else {
                    showSuspensionConfirmation(days[which])
                }
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showCustomSuspensionDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.admin_suspension_custom_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_suspension_custom_title))
            .setMessage(getString(R.string.admin_suspension_custom_message))
            .setView(input)
            .setPositiveButton(getString(R.string.admin_suspend_user)) { _, _ ->
                val daysText = input.text.toString()
                val days = daysText.toIntOrNull()

                when {
                    days == null -> {
                        Toast.makeText(requireContext(), getString(R.string.admin_error_invalid_days), Toast.LENGTH_SHORT).show()
                    }
                    days < 1 || days > 365 -> {
                        Toast.makeText(requireContext(), getString(R.string.admin_error_days_range), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        showSuspensionConfirmation(days)
                    }
                }
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showSuspensionConfirmation(days: Int) {
        val dayText = if (days == 1) getString(R.string.admin_suspension_day) else getString(R.string.admin_suspension_days)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_suspension_confirm_title))
            .setMessage(getString(R.string.admin_suspension_confirm_message, user.name, days, dayText))
            .setPositiveButton(getString(R.string.admin_confirm)) { _, _ ->
                actionListener?.onSuspend(user, days)
                dismiss()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showRemoveSuspensionConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_remove_suspension))
            .setMessage(getString(R.string.admin_suspension_removed_success))
            .setPositiveButton(getString(R.string.admin_confirm)) { _, _ ->
                actionListener?.onRemoveSuspension(user)
                dismiss()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_confirm_delete_title))
            .setMessage(getString(R.string.admin_confirm_delete_message, user.name))
            .setPositiveButton(getString(R.string.admin_delete_user)) { _, _ ->
                actionListener?.onDelete(user)
                dismiss()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    private fun tryLoadBase64OrFallback(photo: String) {
        val decoded = try {
            android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }

        if (decoded != null) {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            if (bmp != null) {
                Glide.with(this)
                    .load(bmp)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(avatarImageView)
                return
            }
        }

        // fallback final: iniciales
        avatarView.visibility = View.VISIBLE
        avatarImageView.visibility = View.GONE
        avatarView.text = user.getInitials()
    }

    fun setOnActionListener(listener: OnActionListener) {
        this.actionListener = listener
    }
}
