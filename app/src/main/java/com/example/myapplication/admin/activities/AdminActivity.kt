package com.example.myapplication.admin.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.admin.adapters.AdminUserAdapter
import com.example.myapplication.admin.fragments.UserDetailBottomSheet
import com.example.myapplication.admin.models.AdminUser
import com.example.myapplication.admin.viewmodels.AdminViewModel
import com.example.myapplication.cloud.FirebaseService
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity principal del panel de administración
 *
 * Funcionalidades:
 * - Lista de usuarios con búsqueda en tiempo real
 * - Acciones rápidas (bloquear/desbloquear)
 * - Detalles completos de usuario en BottomSheet
 * - Menú con opciones adicionales
 */
class AdminActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AdminActivity"

        // Extras para Intent
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_ADMIN_ACTION = "extra_admin_action"
    }

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText

    // Components
    private lateinit var userAdapter: AdminUserAdapter
    private lateinit var viewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Verificar sesión/permiso de admin antes de inflar layout
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val isAdminPref = prefs.getBoolean("is_admin", false)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentEmail = currentUser?.email ?: ""

        // Si no está marcado como admin en prefs ni el usuario actual de Firebase coincide con el email admin, redirigir
        if (!isAdminPref && !currentEmail.equals("yendermejia0@gmail.com", ignoreCase = true)) {
            // Forzar ir al login
            val intent = Intent(this, com.example.myapplication.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_admin_panel)

        setupToolbar()
        initViews()
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        // Cargar datos iniciales
        viewModel.loadUsers()

        // Procesar intent si viene con datos específicos
        handleIntent(intent)
    }

    /**
     * Configura la toolbar con título y botón de retroceso
     */
    private fun setupToolbar() {
        supportActionBar?.apply {
            title = getString(R.string.admin_panel_title)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    /**
     * Inicializa las vistas y el ViewModel
     */
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewAdminUsers)
        searchInput = findViewById(R.id.searchInputAdmin)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuAdmin)
        val header = findViewById<android.view.View>(R.id.headerContainer)

        // Ajustar automáticamente padding top según Insets (notch/status bar)
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val sysInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            // Añadir un padding extra de 6dp para separarlo un poco más
            val extra = (6 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = sysInsets.top + extra)
            insets
        }

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        // Configurar botón de menú dropdown
        btnMenu.setOnClickListener { view ->
            showMenuDropdown(view)
        }
    }

    /**
     * Muestra el menú dropdown con opciones
     */
    private fun showMenuDropdown(view: android.view.View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.admin_dropdown_menu, popupMenu.menu)

        // Animar la flecha: girar 180 grados al abrir
        try {
            view.animate().rotationBy(180f).setDuration(200).start()
        } catch (_: Exception) {}

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout_dropdown -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        // Restaurar rotación cuando se cierra el menú
        popupMenu.setOnDismissListener {
            try {
                view.animate().rotation(0f).setDuration(200).start()
            } catch (_: Exception) {}
        }

        popupMenu.show()
    }

    /**
     * Configura el RecyclerView con su adapter
     */
    private fun setupRecyclerView() {
        userAdapter = AdminUserAdapter(
            onUserClick = { user ->
                showUserDetails(user)
            },
            onToggleStatus = { user ->
                handleQuickToggle(user)
            }
        )

        recyclerView.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@AdminActivity)
            setHasFixedSize(true)

            // Agregar separadores entre items
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    this@AdminActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    /**
     * Configura la búsqueda en tiempo real
     */
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchUsers(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Limpiar búsqueda al hacer clic en el icono
        searchInput.setOnClickListener {
            if (searchInput.text.isNotEmpty()) {
                searchInput.setText("")
            }
        }
    }

    /**
     * Observa los LiveData del ViewModel
     */
    private fun observeViewModel() {
        // Lista de usuarios (usar filteredUsers para búsqueda)
        viewModel.filteredUsers.observe(this) { users ->
            userAdapter.submitList(users)
            updateEmptyState(users.isEmpty())

            // Forzar re-layout del RecyclerView para que los items se muestren inmediatamente
            recyclerView.post {
                try {
                    userAdapter.notifyDataSetChanged()
                    recyclerView.invalidate()
                    recyclerView.requestLayout()
                } catch (e: Exception) {
                    // No detener la app si ocurre un error menor al re-layout
                    e.printStackTrace()
                }
            }
        }

        // Estado de carga
        viewModel.loading.observe(this) { isLoading ->
            // Mostrar/ocultar indicador de carga
            // Puedes agregar un ProgressBar en el layout
        }

        // Errores
        viewModel.error.observe(this) { error ->
            error?.let {
                showErrorDialog(it)
                viewModel.clearMessages()
            }
        }

        // Mensajes de éxito
        viewModel.message.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // Estadísticas (opcional, para mostrar en el menú)
        viewModel.userStats.observe(this) { stats ->
            // Actualizar estadísticas en el menú o header
            invalidateOptionsMenu()
        }
    }

    /**
     * Maneja el toggle rápido de estado de usuario
     */
    private fun handleQuickToggle(user: AdminUser) {
        val action = if (user.blocked) getString(R.string.admin_unblock_user) else getString(R.string.admin_block_user)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_action_title))
            .setMessage(getString(R.string.confirm_action_message, action.lowercase(), user.name))
            .setPositiveButton(getString(R.string.admin_yes)) { _, _ ->
                viewModel.toggleUserStatus(user)
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    /**
     * Muestra los detalles completos del usuario en un BottomSheet
     */
    private fun showUserDetails(user: AdminUser) {
        val bottomSheet = UserDetailBottomSheet.newInstance(user)

        // Configurar listener para acciones del BottomSheet
        bottomSheet.setOnActionListener(object : UserDetailBottomSheet.OnActionListener {
            override fun onBlock(user: AdminUser) {
                viewModel.blockUser(user.id)
                // Recargar después de bloquear
                reloadUsersAfterAction(bottomSheet, user.id)
            }

            override fun onUnblock(user: AdminUser) {
                viewModel.unblockUser(user.id)
                // Recargar después de desbloquear
                reloadUsersAfterAction(bottomSheet, user.id)
            }

            override fun onSuspend(user: AdminUser, days: Int) {
                viewModel.suspendUser(user.id, days)
                // Recargar después de suspender
                reloadUsersAfterAction(bottomSheet, user.id)
            }

            override fun onRemoveSuspension(user: AdminUser) {
                viewModel.removeSuspension(user.id)
                // Recargar después de remover suspensión
                reloadUsersAfterAction(bottomSheet, user.id)
            }

            override fun onDelete(user: AdminUser) {
                showDeleteConfirmation(user)
            }
         })

        bottomSheet.show(supportFragmentManager, "UserDetail")
    }

    /**
     * Recarga los usuarios después de una acción y actualiza el BottomSheet
     */
    private fun reloadUsersAfterAction(bottomSheet: UserDetailBottomSheet, userId: String) {
        // Esperar un poco para que Firebase procese el cambio
        Handler(Looper.getMainLooper()).postDelayed({
            // Recargar la lista completa
            viewModel.loadUsers()

            // Obtener el usuario actualizado
            val updatedUser = viewModel.getUserById(userId)
            if (updatedUser != null) {
                // Actualizar el BottomSheet con los nuevos datos
                bottomSheet.updateUser(updatedUser)
            }
        }, 500)
    }

    /**
     * Muestra confirmación de eliminación con doble verificación
     */
    private fun showDeleteConfirmation(user: AdminUser) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.admin_confirm_delete_title))
            .setMessage(getString(R.string.admin_confirm_delete_message, user.name))
            .setPositiveButton(getString(R.string.confirm_delete_definitive)) { _, _ ->
                // Segunda confirmación
                showFinalDeleteConfirmation(user)
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    /**
     * Segunda confirmación para eliminación
     */
    private fun showFinalDeleteConfirmation(user: AdminUser) {
        val input = EditText(this).apply {
            hint = getString(R.string.delete_confirm_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_action_title))
            .setMessage(getString(R.string.admin_confirm_delete_message, user.name))
            .setView(input)
            .setPositiveButton(getString(R.string.admin_confirm)) { _, _ ->
                if (input.text.toString() == getString(R.string.delete_confirm_keyword)) {
                    viewModel.deleteUser(user.id)
                } else {
                    Toast.makeText(this, getString(R.string.admin_error_confirmation_incorrect), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    /**
     * Actualiza el estado vacío de la lista
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        // Implementar vista de estado vacío
        // Puedes agregar un TextView o layout para mostrar cuando no hay usuarios
    }

    /**
     * Muestra dialog de error
     */
    private fun showErrorDialog(error: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_ok))
            .setMessage(error)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .show()
    }

    /**
     * Maneja intents con datos específicos
     */
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val userId = it.getStringExtra(EXTRA_USER_ID)
            val action = it.getStringExtra(EXTRA_ADMIN_ACTION)

            if (userId != null) {
                // Buscar y mostrar usuario específico
                // Implementar lógica según sea necesario
            }
        }
    }

    /**
     * Crea el menú de opciones
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    /**
     * Maneja las opciones del menú
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                viewModel.loadUsers()
                true
            }
            R.id.action_stats -> {
                showStatsDialog()
                true
            }
            R.id.action_add_sample -> {
                viewModel.addSampleUser()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Realiza logout del administrador
     */
    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_logout_title))
            .setMessage(getString(R.string.confirm_logout_message))
            .setPositiveButton(getString(R.string.yes_logout)) { _, _ ->
                // Cerrar sesión en Firebase y limpiar SharedPreferences
                try {
                    FirebaseService.logout(this)
                } catch (_: Exception) {}

                val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                prefs.edit().clear().apply()

                // Ir a LoginActivity
                val intent = Intent(this, com.example.myapplication.ui.login.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.admin_cancel), null)
            .show()
    }

    /**
     * Muestra dialog con estadísticas
     */
    private fun showStatsDialog() {
        val stats = viewModel.userStats.value
        if (stats != null) {
            val message = getString(
                R.string.stats_message,
                stats["total"] ?: 0,
                stats["active"] ?: 0,
                stats["blocked"] ?: 0,
                stats["suspended"] ?: 0
            )

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.stats_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_ok), null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos si es necesario
        try {
            FirebaseService.stopAdminListeners()
        } catch (_: Exception) {}
    }
}
