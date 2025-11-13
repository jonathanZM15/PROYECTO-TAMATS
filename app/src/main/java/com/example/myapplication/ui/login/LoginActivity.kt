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
import com.example.myapplication.cloud.FirebaseService
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.model.UsuarioEntity
import com.example.myapplication.ui.register.RegisterActivity
import com.example.myapplication.ui.simulacion.EditProfileActivity
import com.example.myapplication.ui.simulacion.ViewProfileActivity
import com.example.myapplication.util.EncryptionUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: TextView

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val usuarioDao by lazy { db.usuarioDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
                            val photo = profileData["photo"]?.toString() ?: ""
                            if (name.isNotEmpty()) putString("user_name", name) else remove("user_name")
                            if (photo.isNotEmpty()) putString("user_photo", photo) else remove("user_photo")
                        } else {
                            remove("user_name")
                            remove("user_photo")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val intent = if (profileData != null) {
                    // Usuario YA tiene perfil → ir a ViewProfileActivity
                    Intent(this, ViewProfileActivity::class.java).apply {
                        putExtra("userEmail", user.email)
                    }
                } else {
                    // Usuario NO tiene perfil → ir a EditProfileActivity
                    Intent(this, EditProfileActivity::class.java).apply {
                        putExtra("userEmail", user.email)
                    }
                }
                startActivity(intent)
                finish()
            }
        }
    }
}