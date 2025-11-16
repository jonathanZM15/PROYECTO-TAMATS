package com.example.myapplication.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.admin.activities.AdminActivity
import com.example.myapplication.cloud.FirebaseService
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.model.UsuarioEntity
import com.example.myapplication.ui.register.RegisterActivity
import com.example.myapplication.ui.simulacion.EditProfileActivity
import com.example.myapplication.util.EncryptionUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    companion object {
        // Credenciales administrativas
        private const val ADMIN_EMAIL = "yendermejia0@gmail.com"
        private const val ADMIN_PASSWORD = "Xiomy.123"
    }

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: TextView

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val usuarioDao by lazy { db.usuarioDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Si ya existe sesión guardada, ir a la actividad correspondiente
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val savedEmail = prefs.getString("user_email", null)
        val isAdmin = prefs.getBoolean("is_admin", false)

        if (!savedEmail.isNullOrEmpty()) {
            // Redirigir a AdminActivity si es admin, sino a MainActivity
            val intent = if (isAdmin) {
                Intent(this, AdminActivity::class.java)
            } else {
                Intent(this, com.example.myapplication.MainActivity::class.java).apply {
                    putExtra("fragment", "explore")
                }
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        setupListeners()
    }

    private fun setupListeners() {
        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        btnLogin.setOnClickListener {
            handleLoginAttempt()
        }
    }

    private fun handleLoginAttempt() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            if (email.isEmpty()) etEmail.error = "El correo es obligatorio."
            if (password.isEmpty()) etPassword.error = "La contraseña es obligatoria."
            return
        }

        // Verificar si son credenciales de admin
        if (email == ADMIN_EMAIL && password == ADMIN_PASSWORD) {
            navigateToAdmin()
            return
        }

        lifecycleScope.launch {
            val localUser = withContext(Dispatchers.IO) {
                usuarioDao.getUserByEmail(email)
            }

            if (localUser != null) {
                // Verificar la contraseña cifrada usando EncryptionUtil
                if (EncryptionUtil.verifyPassword(password, localUser.passwordHash)) {
                    navigateToProfile(localUser)
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this@LoginActivity, "Buscando en la nube...", Toast.LENGTH_SHORT).show()

                FirebaseService.findUserByEmail(email) { firebaseUser ->
                    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        return@findUserByEmail
                    }

                    if (firebaseUser != null && EncryptionUtil.verifyPassword(password, firebaseUser.passwordHash)) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            usuarioDao.insertar(firebaseUser)
                            withContext(Dispatchers.Main) {
                                navigateToProfile(firebaseUser)
                            }
                        }
                    } else {
                        // Falla la autenticación de Firebase
                        // Usamos runOnUiThread para asegurar que se ejecuta en el hilo principal
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToAdmin() {
        Toast.makeText(this, "¡Bienvenido, Administrador!", Toast.LENGTH_LONG).show()

        // Intentar autenticar en Firebase con las credenciales de admin para poder leer Firestore
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Guardar que es admin en SharedPreferences
                    val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                    prefs.edit(commit = true) {
                        putString("user_email", ADMIN_EMAIL)
                        putBoolean("is_admin", true)
                    }

                    // Ir al panel de administración
                    val intent = Intent(this, AdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Si falla la autenticación en Firebase, mostrar mensaje y no permitir acceso
                    val err = task.exception?.message ?: "Error autenticando admin en Firebase"
                    android.util.Log.e("LoginActivity", "Auth admin failed: $err")
                    Toast.makeText(this, "No fue posible autenticar al administrador en Firebase: $err", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToProfile(user: UsuarioEntity) {
        Toast.makeText(this, "¡Bienvenido, ${user.name}!", Toast.LENGTH_LONG).show()

        // Verificar si el usuario ya tiene perfil en Firebase
        FirebaseService.getUserProfile(user.email) { profileData ->
            runOnUiThread {
                // Guardar el email en SharedPreferences siempre (identificador del usuario en la app)
                try {
                    val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                    prefs.edit(commit = true) {
                        putString("user_email", user.email)
                        if (profileData != null) {
                            val name = profileData["name"]?.toString() ?: ""
                            if (name.isNotEmpty()) putString("user_name", name) else remove("user_name")
                            // NO guardar user_photo en SharedPreferences para evitar TransactionTooLargeException
                            // La foto se cargará desde Firebase cuando sea necesaria
                            remove("user_photo")
                        } else {
                            remove("user_name")
                            remove("user_photo")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Siempre ir a MainActivity (que tiene la barra de navegación)
                // Si el usuario no tiene perfil completado, irá a EditProfileActivity desde aquí
                val intent = if (profileData != null) {
                    // Usuario YA tiene perfil → ir a MainActivity mostrando el fragment Profile
                    Intent(this, com.example.myapplication.MainActivity::class.java).apply {
                        putExtra("fragment", "profile")
                    }
                } else {
                    // Usuario NO tiene perfil → ir a EditProfileActivity para completarlo
                    Intent(this, EditProfileActivity::class.java).apply {
                        putExtra("userEmail", user.email)
                    }
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}