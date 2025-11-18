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

        android.util.Log.d("ResetPassword", "Activity iniciada")
        android.util.Log.d("ResetPassword", "Intent data: ${intent.data}")
        android.util.Log.d("ResetPassword", "Intent action: ${intent.action}")
        android.util.Log.d("ResetPassword", "Intent extras: ${intent.extras}")

        // Extraer email y oobCode del Intent
        // Firebase envía los parámetros en la URL
        val intentData = intent.data

        // Intentar obtener email desde varios lugares
        var extractedEmail = intentData?.getQueryParameter("email")
            ?: intent.getStringExtra("email")
            ?: ""

        // Firebase envía el oobCode como parámetro de query
        var extractedToken = intentData?.getQueryParameter("oobCode")
            ?: intent.getStringExtra("oobCode")
            ?: ""

        android.util.Log.d("ResetPassword", "Email extraído: '$extractedEmail'")
        android.util.Log.d("ResetPassword", "OobCode extraído: '$extractedToken'")

        // Si no tenemos email, intentar extraerlo de continueUrl
        if (extractedEmail.isEmpty()) {
            val continueUrl = intentData?.getQueryParameter("continueUrl")
            android.util.Log.d("ResetPassword", "ContinueUrl: $continueUrl")
            if (!continueUrl.isNullOrEmpty()) {
                val uri = android.net.Uri.parse(continueUrl)
                extractedEmail = uri.getQueryParameter("email") ?: ""
                android.util.Log.d("ResetPassword", "Email extraído de continueUrl: '$extractedEmail'")
            }
        }

        // Validar que tengamos los datos necesarios
        if (extractedEmail.isEmpty() || extractedToken.isEmpty()) {
            android.util.Log.e("ResetPassword", "❌ Datos faltantes - Email: '$extractedEmail', OobCode: '$extractedToken'")
            Toast.makeText(this, "❌ Link inválido o expirado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Asignar a las propiedades de la clase
        email = extractedEmail
        token = extractedToken

        android.util.Log.d("ResetPassword", "✅ Datos válidos, mostrando UI")
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
     * Maneja el cambio de contraseña con validaciones estrictas
     */
    private fun handlePasswordChange() {
        // Obtener textos de forma explícita
        val newPasswordObj = etNewPassword.text
        val confirmPasswordObj = etConfirmNewPassword.text

        val newPassword = newPasswordObj?.toString() ?: ""
        val confirmPassword = confirmPasswordObj?.toString() ?: ""

        android.util.Log.d("ResetPassword", "=== VALIDACIÓN INICIADA ===")
        android.util.Log.d("ResetPassword", "Nueva contraseña: '$newPassword'")
        android.util.Log.d("ResetPassword", "Longitud: ${newPassword.length}")
        android.util.Log.d("ResetPassword", "Confirmar contraseña: '$confirmPassword'")

        // Limpiar errores previos
        etNewPassword.error = null
        etConfirmNewPassword.error = null

        // === VALIDACIÓN 1: Vacía ===
        if (newPassword.trim().isEmpty()) {
            etNewPassword.error = "La contraseña no puede estar vacía"
            Toast.makeText(this, "❌ La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
            return
        }

        // === VALIDACIÓN 2: Longitud ===
        if (newPassword.length < 8) {
            val msg = "La contraseña debe tener AL MENOS 8 caracteres (tienes ${newPassword.length})"
            etNewPassword.error = msg
            Toast.makeText(this, "❌ $msg", Toast.LENGTH_SHORT).show()
            return
        }

        // === VALIDACIÓN 3: Mayúscula ===
        var tieneMAYUSCULA = false
        for (char in newPassword) {
            if (char.isUpperCase() && char.isLetter()) {
                tieneMAYUSCULA = true
                break
            }
        }

        if (!tieneMAYUSCULA) {
            etNewPassword.error = "DEBE tener AL MENOS UNA letra MAYÚSCULA"
            Toast.makeText(this, "❌ DEBE tener AL MENOS UNA letra MAYÚSCULA", Toast.LENGTH_SHORT).show()
            return
        }

        // === VALIDACIÓN 4: Número ===
        var tieneNUMERO = false
        for (char in newPassword) {
            if (char.isDigit()) {
                tieneNUMERO = true
                break
            }
        }

        if (!tieneNUMERO) {
            etNewPassword.error = "DEBE tener AL MENOS UN número"
            Toast.makeText(this, "❌ DEBE tener AL MENOS UN número", Toast.LENGTH_SHORT).show()
            return
        }

        // === VALIDACIÓN 5: Confirmación vacía ===
        if (confirmPassword.trim().isEmpty()) {
            etConfirmNewPassword.error = "Confirma tu contraseña"
            Toast.makeText(this, "❌ Confirma tu contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // === VALIDACIÓN 6: Coinciden ===
        if (newPassword != confirmPassword) {
            etConfirmNewPassword.error = "Las contraseñas NO coinciden"
            Toast.makeText(this, "❌ Las contraseñas NO coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("ResetPassword", "✅ TODAS LAS VALIDACIONES PASARON")

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

                // También actualizar en Firebase
                com.example.myapplication.cloud.FirebaseService.actualizarContrasena(email!!, encryptedPassword)

                // Invalidar el token usado
                invalidateToken(token!!)

                withContext(Dispatchers.Main) {
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

