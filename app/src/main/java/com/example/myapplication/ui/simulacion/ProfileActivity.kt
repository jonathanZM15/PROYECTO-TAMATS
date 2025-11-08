package com.example.myapplication.ui.simulacion

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService

// Constantes para las claves de Intent (útiles para LoginActivity)
const val EXTRA_USER_NAME = "com.example.myapplication.ui.profile.EXTRA_USER_NAME"
const val EXTRA_USER_EMAIL = "com.example.myapplication.ui.profile.EXTRA_USER_EMAIL"
const val EXTRA_USER_BIRTH_DATE = "com.example.myapplication.ui.profile.EXTRA_USER_BIRTH_DATE"
const val EXTRA_USER_GENDER = "com.example.myapplication.ui.profile.EXTRA_USER_GENDER"

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile)

            // --- Mostrar datos del Intent ---
            displayUserDataFromIntent()

            // --- Configurar el botón para mostrar datos de Firebase ---
            setupShowDataButton()
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error en onCreate: ${e.message}", e)
            e.printStackTrace()
            finish()
        }
    }

    /**
     * Obtiene los datos pasados desde LoginActivity y los muestra en la UI.
     */
    private fun displayUserDataFromIntent() {
        try {
            val tvName = findViewById<TextView?>(R.id.tvProfileName)
            val tvEmail = findViewById<TextView?>(R.id.tvProfileEmail)
            val tvBirthDate = findViewById<TextView?>(R.id.tvProfileBirthDate)
            val tvGender = findViewById<TextView?>(R.id.tvProfileGender)
            val tvWelcomeTitle = findViewById<TextView?>(R.id.tvWelcomeTitle)

            val name = intent.getStringExtra(EXTRA_USER_NAME) ?: "N/A"
            val email = intent.getStringExtra(EXTRA_USER_EMAIL) ?: "N/A"
            val birthDate = intent.getStringExtra(EXTRA_USER_BIRTH_DATE) ?: "N/A"
            val gender = intent.getStringExtra(EXTRA_USER_GENDER) ?: "N/A"

            tvWelcomeTitle?.text = "Bienvenido, $name"
            tvName?.text = name
            tvEmail?.text = email
            tvBirthDate?.text = birthDate
            tvGender?.text = gender
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error en displayUserDataFromIntent: ${e.message}", e)
        }
    }

    /**
     * Configura el listener del botón para obtener y mostrar los datos de Firebase.
     */
    private fun setupShowDataButton() {
        try {
            val btnShowData = findViewById<Button?>(R.id.btnShowFirebaseData)
            btnShowData?.setOnClickListener {
                // Llama al método para obtener usuarios de Firebase
                FirebaseService.obtenerUsuarios { userList ->
                    // El resultado de Firebase se recibe aquí
                    val formattedData = StringBuilder()
                    if (userList.isEmpty()) {
                        formattedData.append("No hay usuarios en Firebase.")
                    } else {
                        formattedData.append("Usuarios en Firebase:\n\n")
                        userList.forEach { usuario ->
                            formattedData.append(
                                "Nombre: ${usuario.name}\n" +
                                        "Email: ${usuario.email}\n" +
                                        "Fecha Nac: ${usuario.birthDate}\n" +
                                        "Género: ${usuario.gender}\n\n"
                            )
                        }
                    }

                    // Mostramos los datos en un diálogo de alerta
                    // Es importante ejecutar esto en el hilo principal
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Datos de Firestore")
                            .setMessage(formattedData.toString())
                            .setPositiveButton("Aceptar", null)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error en setupShowDataButton: ${e.message}", e)
        }
    }
}