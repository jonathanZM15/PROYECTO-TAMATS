package com.example.myapplication.ui.register

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity // Asegúrate de importar LoginActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicialización de Vistas (Usando findViewById)
        etName = findViewById(R.id.etName)
        etEmailRegister = findViewById(R.id.etEmailRegister)
        etBirthDate = findViewById(R.id.etBirthDate)
        spinnerGender = findViewById(R.id.spinnerGender)
        etPasswordRegister = findViewById(R.id.etPasswordRegister)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)

        // Configuración inicial de la UI
        setupNameField()
        setupGenderSpinner()
        setupBirthDatePicker()

        // Listener principal para el registro
        btnRegister.setOnClickListener {
            // Llama al método de validación. Si es true, inicia el proceso de finalización.
            if (validateForm()) {
                handleSuccessfulRegistration()
            }
        }
    }

    /**
     * Maneja la lógica después de que el formulario es válido.
     */
    private fun handleSuccessfulRegistration() {
        // [1] Muestra el mensaje de éxito
        Toast.makeText(this, "¡Registro Exitoso! Redirigiendo a Login.", Toast.LENGTH_LONG).show()

        // [2] Crea el Intent para ir a LoginActivity
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)

        // [3] Cierra RegisterActivity para que el usuario no pueda volver con el botón "Atrás"
        finish()
    }


    /**
     * Configura el EditText del nombre para limitar a 50 caracteres y solo aceptar letras y espacios.
     */
    private fun setupNameField() {
        // Validación de longitud máxima (ya en XML, pero reforzado aquí)
        etName.filters = arrayOf(InputFilter.LengthFilter(50))
    }

    /**
     * Configura el Spinner para las opciones de género.
     */
    private fun setupGenderSpinner() {
        // Los ítems se toman del array definido en res/values/arrays.xml
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_dropdown_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    /**
     * Configura el DatePickerDialog con la restricción de edad (mayor de 18 años).
     */
    private fun setupBirthDatePicker() {
        etBirthDate.setOnClickListener {
            // Obtiene la fecha actual
            val c = Calendar.getInstance()

            // Restringe el máximo de la fecha a 18 años menos que hoy (mayoría de edad)
            val maxDate = Calendar.getInstance()
            maxDate.add(Calendar.YEAR, -18)

            // Determina el año inicial a mostrar en el selector (el año de la restricción)
            val year = maxDate.get(Calendar.YEAR)
            val month = maxDate.get(Calendar.MONTH)
            val day = maxDate.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formato DD/MM/AAAA (Month es 0-indexed, se suma 1)
                    val formattedDate = String.format(
                        "%02d/%02d/%d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    etBirthDate.setText(formattedDate)
                },
                year, month, day // Muestra el selector en la fecha máxima permitida
            )

            // Establece la fecha máxima seleccionable
            dpd.datePicker.maxDate = maxDate.timeInMillis
            dpd.show()
        }
    }

    /**
     * Realiza todas las validaciones del formulario.
     * @return true si todas las validaciones pasan, false en caso contrario.
     */
    private fun validateForm(): Boolean {
        var isValid = true
        // Restablecer errores antes de validar
        etName.error = null
        etEmailRegister.error = null
        etBirthDate.error = null
        etPasswordRegister.error = null
        etConfirmPassword.error = null

        // 1. Nombre
        val name = etName.text.toString().trim()
        val nameRegex = Regex("^[a-zA-Z\\s]{1,50}$") // Letras y espacios, 1-50 caracteres
        if (name.isEmpty()) {
            etName.error = "El nombre es obligatorio."
            isValid = false
        } else if (!name.matches(nameRegex)) {
            etName.error = "Solo se permiten letras y espacios, máximo 50 caracteres."
            isValid = false
        }

        // 2. Correo Electrónico
        val email = etEmailRegister.text.toString().trim()
        val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]+")
        if (email.isEmpty()) {
            etEmailRegister.error = "El correo es obligatorio."
            isValid = false
        } else if (!email.matches(emailRegex)) {
            etEmailRegister.error = "Formato de correo inválido (ej: usuario@dominio.com)."
            isValid = false
        }

        // 3. Fecha de Nacimiento
        val birthDate = etBirthDate.text.toString()
        if (birthDate.isEmpty()) {
            etBirthDate.error = "Debes seleccionar tu fecha de nacimiento."
            isValid = false
        }

        // 4. Género
        if (spinnerGender.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecciona tu género.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // 5. Contraseñas
        val password = etPasswordRegister.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (password.length != 8) {
            etPasswordRegister.error = "La contraseña debe ser de exactamente 8 caracteres."
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirma la contraseña."
            isValid = false
        } else if (password != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden."
            isValid = false
        }


        // 6. Términos y Condiciones
        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }
}
