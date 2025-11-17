package com.example.myapplication.ui.register

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.model.UsuarioEntity
import com.example.myapplication.ui.login.LoginActivity
import com.example.myapplication.util.EncryptionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class RegisterActivity : AppCompatActivity() {

    // Declaración de variables de la UI
    private lateinit var etName: EditText
    private lateinit var etEmailRegister: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etPasswordRegister: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var tvViewTerms: TextView

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val usuarioDao by lazy { db.usuarioDao() }
    private val firebaseService by lazy { FirebaseService }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicialización de Vistas
        etName = findViewById(R.id.etName)
        etEmailRegister = findViewById(R.id.etEmailRegister)
        etBirthDate = findViewById(R.id.etBirthDate)
        spinnerGender = findViewById(R.id.spinnerGender)
        etPasswordRegister = findViewById(R.id.etPasswordRegister)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        tvViewTerms = findViewById(R.id.tvViewTerms)

        setupListeners()
    }

    private fun setupListeners() {
        etName.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (source.toString().matches(Regex("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*"))) source else ""
        })

        etBirthDate.setOnClickListener { showDatePickerDialog() }

        btnRegister.setOnClickListener { handleRegistration() }

        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        tvViewTerms.setOnClickListener {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = "${selectedDay.toString().padStart(2, '0')}/${(selectedMonth + 1).toString().padStart(2, '0')}/${selectedYear}"
                etBirthDate.setText(date)
            },
            year,
            month,
            day
        )

        // --- RESTRICCIÓN DE EDAD ---
        // 1. Crear una instancia de Calendar para la fecha máxima
        val maxDate = Calendar.getInstance()
        // 2. Restar 18 años a la fecha actual
        maxDate.add(Calendar.YEAR, -18)
        // 3. Establecer esa fecha como la máxima seleccionable
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun handleRegistration() {
        // (El resto del código de validación y registro permanece igual)
        if (validateFields()) {
            val name = etName.text.toString().trim()
            val email = etEmailRegister.text.toString().trim()
            val birthDate = etBirthDate.text.toString()
            val gender = spinnerGender.selectedItem.toString()
            val password = etPasswordRegister.text.toString()

            // Cifrar la contraseña antes de guardarla
            val encryptedPassword = EncryptionUtil.encryptPassword(password)

            val newUser = UsuarioEntity(
                name = name,
                email = email,
                birthDate = birthDate,
                gender = gender,
                passwordHash = encryptedPassword
            )

            scope.launch {
                // Verificar en base de datos local (Room)
                val isRegisteredLocally = usuarioDao.isEmailRegistered(email)

                if (isRegisteredLocally) {
                    withContext(Dispatchers.Main) {
                        etEmailRegister.error = "Este correo ya está registrado"
                        Toast.makeText(
                            this@RegisterActivity,
                            "❌ Ya existe una cuenta con este correo electrónico.\n¿Olvidaste tu contraseña?",
                            Toast.LENGTH_LONG
                        ).show()
                        android.util.Log.w("RegisterActivity", "⚠️ Intento de registro con correo duplicado (local): $email")
                    }
                    return@launch
                }

                // Verificar también en Firebase (por si el usuario se registró en otro dispositivo)
                FirebaseService.findUserByEmail(email) { firebaseUser ->
                    if (firebaseUser != null) {
                        runOnUiThread {
                            etEmailRegister.error = "Este correo ya está registrado"
                            Toast.makeText(
                                this@RegisterActivity,
                                "❌ Ya existe una cuenta con este correo en la nube.\nIntenta iniciar sesión.",
                                Toast.LENGTH_LONG
                            ).show()
                            android.util.Log.w("RegisterActivity", "⚠️ Intento de registro con correo duplicado (Firebase): $email")
                        }
                        return@findUserByEmail
                    }

                    // Correo NO está duplicado, proceder con el registro
                    runOnUiThread {
                        scope.launch {
                            try {
                                // Guardar en Room (local)
                                usuarioDao.insertar(newUser)

                                // Guardar en Firebase (nube)
                                firebaseService.guardarUsuario(newUser)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "✅ ¡Registro Exitoso! Completa tu perfil.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    android.util.Log.d("RegisterActivity", "✅ Usuario registrado exitosamente: $email")

                                    // Guardar sesión automáticamente
                                    val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                                    prefs.edit().apply {
                                        putString("user_email", email)
                                        putString("user_name", name)
                                        apply()
                                    }

                                    // Redirigir a EditProfileActivity para completar perfil
                                    val intent = Intent(this@RegisterActivity, com.example.myapplication.ui.simulacion.EditProfileActivity::class.java)
                                    intent.putExtra("userEmail", email)
                                    startActivity(intent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "❌ Error al guardar: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    android.util.Log.e("RegisterActivity", "❌ Error al registrar usuario: ${e.message}", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        etName.error = null
        etEmailRegister.error = null
        etBirthDate.error = null
        etPasswordRegister.error = null
        etConfirmPassword.error = null
        var isValid = true

        if (etName.text.toString().trim().isEmpty()) {
            etName.error = "El nombre es obligatorio."
            isValid = false
        }

        val email = etEmailRegister.text.toString().trim()
        if (email.isEmpty()) {
            etEmailRegister.error = "El correo electrónico es obligatorio."
            isValid = false
        } else if (!email.matches(emailRegex)) {
            etEmailRegister.error = "Formato de correo inválido (ej: usuario@dominio.com)."
            isValid = false
        }

        if (etBirthDate.text.toString().isEmpty()) {
            etBirthDate.error = "Debes seleccionar tu fecha de nacimiento."
            isValid = false
        }

        if (spinnerGender.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecciona tu género.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val password = etPasswordRegister.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validar contraseña con PasswordValidator
        val passwordValidation = com.example.myapplication.util.PasswordValidator.validate(password)
        if (!passwordValidation.isValid) {
            etPasswordRegister.error = com.example.myapplication.util.PasswordValidator.getErrorMessage(passwordValidation)
            isValid = false
        }

        // Verificar que las contraseñas coincidan
        if (!com.example.myapplication.util.PasswordValidator.passwordsMatch(password, confirmPassword)) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }
}