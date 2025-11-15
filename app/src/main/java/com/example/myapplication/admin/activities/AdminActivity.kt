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
            title = "Panel de Administración"
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
        val action = if (user.blocked) "desbloquear" else "bloquear"

        AlertDialog.Builder(this)
            .setTitle("Confirmar acción")
            .setMessage("¿Estás seguro de que quieres $action a ${user.name}?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.toggleUserStatus(user)
            }
            .setNegativeButton("Cancelar", null)
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
            .setTitle("⚠️ Eliminar Usuario")
            .setMessage(
                "Esta acción eliminará permanentemente la cuenta de ${user.name} " +
                "y todos sus datos asociados.\n\n" +
                "⚠️ Esta acción NO se puede deshacer."
            )
            .setPositiveButton("Eliminar Definitivamente") { _, _ ->
                // Segunda confirmación
                showFinalDeleteConfirmation(user)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Segunda confirmación para eliminación
     */
    private fun showFinalDeleteConfirmation(user: AdminUser) {
        val input = EditText(this).apply {
            hint = "Escribe 'ELIMINAR' para confirmar"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmación Final")
            .setMessage("Escribe 'ELIMINAR' para confirmar la eliminación de ${user.name}")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                if (input.text.toString() == "ELIMINAR") {
                    viewModel.deleteUser(user.id)
                } else {
                    Toast.makeText(this, "Confirmación incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
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
            .setTitle("Error")
            .setMessage(error)
            .setPositiveButton("OK", null)
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
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar la sesión de administrador?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
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
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra dialog con estadísticas
     */
    private fun showStatsDialog() {
        val stats = viewModel.userStats.value
        if (stats != null) {
            val message = """
                Total de usuarios: ${stats["total"] ?: 0}
                Usuarios activos: ${stats["active"] ?: 0}
                Usuarios bloqueados: ${stats["blocked"] ?: 0}
                Usuarios suspendidos: ${stats["suspended"] ?: 0}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("📊 Estadísticas de Usuarios")
                .setMessage(message)
                .setPositiveButton("OK", null)
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
