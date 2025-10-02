package com.example.myapplication.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.R // Si usas R.layout.activity_login
import com.example.myapplication.ui.register.RegisterActivity
import com.google.android.material.button.MaterialButton


class LoginActivity : AppCompatActivity() {

    // Declara las referencias como propiedades de la clase para acceder a ellas en otros métodos
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegisterLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [1] CICLO DE VIDA: onCreate() - Inicialización de UI y Listeners
        setContentView(R.layout.activity_login)

        // 1. Obtener referencias de los elementos de la UI usando findViewById
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        // 2. Configurar los Listeners una sola vez
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // [2] CICLO DE VIDA: onResume() - Restablecer el estado cada vez que la actividad vuelve a ser visible
        // Limpiamos los campos de email y contraseña. Esto es útil si el usuario vuelve desde RegisterActivity.
        etEmail.text.clear()
        etPassword.text.clear()
    }

    private fun setupListeners() {

        // Lógica de Navegación (tvRegisterLink)
        tvRegisterLink.setOnClickListener {
            // Crea un Intent explícito para ir a RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Lógica del Botón de Inicio de Sesión (btnLogin)
        btnLogin.setOnClickListener {
            handleLoginAttempt()
        }
    }

    /**
     * Valida los campos de email y contraseña y, si son válidos, simula el login.
     */
    private fun handleLoginAttempt() {

        // Obtener los textos y eliminar espacios al inicio y final
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Restablecer errores anteriores
        etEmail.error = null
        etPassword.error = null

        var isValid = true

        // A. Validación de Email vacío
        if (email.isEmpty()) {
            etEmail.error = "El correo electrónico es obligatorio."
            isValid = false
        }

        // B. Validación de Contraseña vacía
        if (password.isEmpty()) {
            etPassword.error = "La contraseña es obligatoria."
            isValid = false
        }

        // C. Resultado de la Validación
        if (isValid) {
            // **CAMPOS VÁLIDOS:** Muestra el Toast de Bienvenida
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_LONG).show()

            // NOTA: Aquí iría la lógica real de autenticación y navegación a la actividad principal.

        } else {
            // **CAMPOS VACÍOS:** Muestra un Toast genérico de error
            Toast.makeText(this, "Por favor, completa los campos obligatorios.", Toast.LENGTH_SHORT).show()
        }
    }
}