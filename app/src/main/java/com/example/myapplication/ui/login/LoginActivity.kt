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
    private lateinit var tvForgotPassword: TextView

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
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        setupListeners()
    }

    private fun setupListeners() {
        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        btnLogin.setOnClickListener {
            handleLoginAttempt()
        }
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
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
                    // Antes de navegar, chequear estado en Firestore (por si fue bloqueado/suspendido/eliminado)
                    checkUserStateAndProceed(localUser.email, localUser)
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
                                // Chequear estado en Firestore antes de permitir entrada
                                checkUserStateAndProceed(firebaseUser.email, firebaseUser)
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

    /**
     * Verifica en Firebase el estado del usuario (blocked/suspended/existence) y navega o muestra Toast con motivo
     */
    private fun checkUserStateAndProceed(email: String, userEntity: UsuarioEntity) {
        FirebaseService.getUserProfile(email) { profileData ->
            runOnUiThread {
                // Si profileData es null: usuario eliminado o no tiene perfil en Firestore
                if (profileData == null) {
                    Toast.makeText(this, "No fue posible iniciar sesión: cuenta no encontrada (eliminada).", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                // Revisar campos 'blocked' y 'suspended'
                val blocked = profileData["blocked"] as? Boolean ?: false
                val suspended = profileData["suspended"] as? Boolean ?: false
                val suspensionEnd = when (val v = profileData["suspensionEnd"]) {
                    is com.google.firebase.Timestamp -> v.toDate().time
                    is Number -> v.toLong()
                    else -> null
                }

                if (blocked) {
                    Toast.makeText(this, "Tu cuenta ha sido bloqueada. Contacta al soporte para más información.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (suspended) {
                    // Si suspensionEnd existe y es futuro, mostrar días restantes
                    if (suspensionEnd != null && System.currentTimeMillis() < suspensionEnd) {
                        val daysLeft = ((suspensionEnd - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt() + 1
                        Toast.makeText(this, "Tu cuenta está suspendida. Días restantes: $daysLeft", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    } else {
                        // Si suspensionEnd no existe o ya pasó, permitir continuar
                    }
                }

                // Si llegó hasta aquí, el usuario está activo → proceder con el flujo normal
                try {
                    val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                    prefs.edit(commit = true) {
                        putString("user_email", userEntity.email)
                        // name/photo se guardan posteriormente si existen
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Navegar a MainActivity o EditProfile según profileData
                val intent = Intent(this, com.example.myapplication.MainActivity::class.java).apply {
                    putExtra("fragment", "profile")
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
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

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etRecoveryEmail = dialogView.findViewById<EditText>(R.id.etRecoveryEmail)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnSendRecovery).setOnClickListener {
            val email = etRecoveryEmail.text.toString().trim()

            if (email.isEmpty()) {
                etRecoveryEmail.error = "Ingresa tu correo electrónico"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etRecoveryEmail.error = "Ingresa un correo válido"
                return@setOnClickListener
            }

            sendPasswordResetEmail(email, dialog)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancelRecovery).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendPasswordResetEmail(email: String, dialog: androidx.appcompat.app.AlertDialog) {
        // Mostrar loading
        val btnSend = dialog.findViewById<MaterialButton>(R.id.btnSendRecovery)
        btnSend?.isEnabled = false
        btnSend?.text = "Verificando..."

        // PRIMERO: Verificar que el correo exista en la base de datos
        lifecycleScope.launch(Dispatchers.IO) {
            val userExists = usuarioDao.getUserByEmail(email) != null

            withContext(Dispatchers.Main) {
                if (!userExists) {
                    // Correo NO existe en la BD
                    btnSend?.isEnabled = true
                    btnSend?.text = "Enviar"
                    Toast.makeText(
                        this@LoginActivity,
                        "❌ No existe una cuenta registrada con este correo electrónico",
                        Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.w("PasswordReset", "⚠️ Intento de recuperación para correo no registrado: $email")
                    return@withContext
                }

                // Correo SÍ existe, continuar con el envío
                btnSend?.text = "Enviando..."

                // Generar token único de recuperación
                val resetToken = java.util.UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                // Crear link de recuperación
                val resetLink = "tamats://reset?token=$resetToken&email=$email"

                // Guardar token en SharedPreferences (expira en 1 hora)
                val prefs = getSharedPreferences("password_reset", MODE_PRIVATE)
                prefs.edit().apply {
                    putString("token_$resetToken", email)
                    putLong("timestamp_$resetToken", timestamp)
                    apply()
                }

                // Enviar correo usando SMTP (EmailService)
                lifecycleScope.launch(Dispatchers.IO) {
                    val emailSent = com.example.myapplication.util.EmailService.sendPasswordResetEmail(
                        toEmail = email,
                        resetLink = resetLink
                    )

                    withContext(Dispatchers.Main) {
                        btnSend?.isEnabled = true
                        btnSend?.text = "Enviar"

                        if (emailSent) {
                            Toast.makeText(
                                this@LoginActivity,
                                "✅ ¡Correo enviado a $email!\nRevisa tu bandeja (puede tardar 30 seg)",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                            android.util.Log.d("PasswordReset", "✅ Email sent to: $email, Token: $resetToken")
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "❌ Error al enviar. Verifica tu conexión a internet",
                                Toast.LENGTH_LONG
                            ).show()
                            android.util.Log.e("PasswordReset", "❌ Failed to send email to: $email")
                        }
                    }
                }
            }
        }
    }
}
