package com.example.myapplication.ui.password

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.ui.login.LoginActivity
import com.example.myapplication.util.EncryptionUtil
import com.example.myapplication.util.PasswordValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var tvEmailDisplay: TextView
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmNewPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var tvCancelReset: TextView
    
    // Indicadores visuales de requisitos
    private lateinit var tvReqLength: TextView
    private lateinit var tvReqUppercase: TextView
    private lateinit var tvReqNumber: TextView

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val usuarioDao by lazy { db.usuarioDao() }

    private var email: String? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Obtener datos del Intent (desde Deep Link)
        email = intent.data?.getQueryParameter("email") ?: intent.getStringExtra("email")
        token = intent.data?.getQueryParameter("token") ?: intent.getStringExtra("token")

        // Validar que tengamos los datos necesarios
        if (email.isNullOrEmpty() || token.isNullOrEmpty()) {
            Toast.makeText(this, "❌ Link inválido o expirado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Validar que el token sea válido y no haya expirado
        if (!isTokenValid(token!!)) {
            Toast.makeText(
                this,
                "❌ Este enlace ha expirado.\nSolicita uno nuevo desde 'Olvidé mi contraseña'",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()
        displayEmail()
    }

    private fun initializeViews() {
        tvEmailDisplay = findViewById(R.id.tvEmailDisplay)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        tvCancelReset = findViewById(R.id.tvCancelReset)
        
        tvReqLength = findViewById(R.id.tvReqLength)
        tvReqUppercase = findViewById(R.id.tvReqUppercase)
        tvReqNumber = findViewById(R.id.tvReqNumber)
    }

    private fun setupListeners() {
        // Validación en tiempo real mientras escribe
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                updatePasswordRequirements(password)
            }
        })

        btnChangePassword.setOnClickListener {
            handlePasswordChange()
        }

        tvCancelReset.setOnClickListener {
            finish()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun displayEmail() {
        tvEmailDisplay.text = email
    }

    /**
     * Actualiza visualmente los requisitos de contraseña
     */
    private fun updatePasswordRequirements(password: String) {
        val validation = PasswordValidator.validate(password)
        
        val colorValid = ContextCompat.getColor(this, R.color.green_success)
        val colorInvalid = Color.parseColor("#757575")
        val iconValid = R.drawable.ic_check_circle
        val iconInvalid = R.drawable.ic_circle_outline

        // Longitud
        tvReqLength.setTextColor(if (validation.meetsLength) colorValid else colorInvalid)
        tvReqLength.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (validation.meetsLength) iconValid else iconInvalid, 0, 0, 0
        )

        // Mayúscula
        tvReqUppercase.setTextColor(if (validation.meetsUppercase) colorValid else colorInvalid)
        tvReqUppercase.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (validation.meetsUppercase) iconValid else iconInvalid, 0, 0, 0
        )

        // Número
        tvReqNumber.setTextColor(if (validation.meetsNumber) colorValid else colorInvalid)
        tvReqNumber.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (validation.meetsNumber) iconValid else iconInvalid, 0, 0, 0
        )
    }

    /**
     * Maneja el cambio de contraseña
     */
    private fun handlePasswordChange() {
        val newPassword = etNewPassword.text?.toString() ?: ""
        val confirmPassword = etConfirmNewPassword.text?.toString() ?: ""

        // Limpiar errores previos
        etNewPassword.error = null
        etConfirmNewPassword.error = null

        // Validar contraseña
        val validation = PasswordValidator.validate(newPassword)
        if (!validation.isValid) {
            etNewPassword.error = PasswordValidator.getErrorMessage(validation)
            Toast.makeText(this, "❌ La contraseña no cumple los requisitos", Toast.LENGTH_LONG).show()
            return
        }

        // Verificar que coincidan
        if (!PasswordValidator.passwordsMatch(newPassword, confirmPassword)) {
            etConfirmNewPassword.error = "Las contraseñas no coinciden"
            Toast.makeText(this, "❌ Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        btnChangePassword.isEnabled = false
        btnChangePassword.text = "Actualizando..."

        // Actualizar contraseña en la base de datos
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = usuarioDao.getUserByEmail(email!!)
                
                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "❌ Usuario no encontrado",
                            Toast.LENGTH_LONG
                        ).show()
                        btnChangePassword.isEnabled = true
                        btnChangePassword.text = "Cambiar Contraseña"
                    }
                    return@launch
                }

                // Cifrar nueva contraseña
                val encryptedPassword = EncryptionUtil.encryptPassword(newPassword)
                
                // Actualizar contraseña en Room
                val updatedUser = user.copy(passwordHash = encryptedPassword)
                usuarioDao.actualizar(updatedUser)
                
                // También actualizar en Firebase (opcional pero recomendado)
                com.example.myapplication.cloud.FirebaseService.actualizarContrasena(email!!, encryptedPassword)
                
                // Invalidar el token usado
                invalidateToken(token!!)

                withContext(Dispatchers.Main) {
                    // Mostrar diálogo de éxito
                    showSuccessDialog()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "❌ Error al actualizar contraseña: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnChangePassword.isEnabled = true
                    btnChangePassword.text = "Cambiar Contraseña"
                    android.util.Log.e("ResetPassword", "Error: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Verifica que el token sea válido y no haya expirado
     */
    private fun isTokenValid(token: String): Boolean {
        val prefs = getSharedPreferences("password_reset", MODE_PRIVATE)
        val savedEmail = prefs.getString("token_$token", null)
        val timestamp = prefs.getLong("timestamp_$token", 0)

        // Verificar que el token existe y coincide con el email
        if (savedEmail != email) {
            android.util.Log.w("ResetPassword", "Token no coincide con email")
            return false
        }

        // Verificar que no haya expirado (1 hora = 3600000 ms)
        val oneHour = 3600000L
        val isExpired = (System.currentTimeMillis() - timestamp) > oneHour

        if (isExpired) {
            android.util.Log.w("ResetPassword", "Token expirado")
        }

        return !isExpired
    }

    /**
     * Invalida el token después de usarlo
     */
    private fun invalidateToken(token: String) {
        val prefs = getSharedPreferences("password_reset", MODE_PRIVATE)
        prefs.edit().apply {
            remove("token_$token")
            remove("timestamp_$token")
            apply()
        }
        android.util.Log.d("ResetPassword", "Token invalidado: $token")
    }

    /**
     * Muestra diálogo de éxito y redirige al login
     */
    private fun showSuccessDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("✅ Contraseña Actualizada")
        builder.setMessage("Tu contraseña ha sido actualizada exitosamente.\n\n¡Ya puedes iniciar sesión con tu nueva contraseña!")
        builder.setPositiveButton("Ir al Login") { dialog, _ ->
            dialog.dismiss()
            navigateToLogin()
        }
        builder.setCancelable(false)
        builder.show()
    }

    /**
     * Redirige al login limpiando el stack de actividades
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevenir volver atrás, solo permitir ir al login
        navigateToLogin()
    }
}

