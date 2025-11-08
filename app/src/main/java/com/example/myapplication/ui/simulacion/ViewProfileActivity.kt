package com.example.myapplication.ui.simulacion

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.google.android.material.button.MaterialButton

class ViewProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePhotoView: ImageView
    private lateinit var tvViewName: TextView
    private lateinit var tvViewAge: TextView
    private lateinit var tvViewCity: TextView
    private lateinit var tvViewDescription: TextView
    private lateinit var interestsViewContainer: LinearLayout
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_view_profile)

            userEmail = intent.getStringExtra("userEmail") ?: ""

            initializeViews()
            setupListeners()
            loadProfileData()
        } catch (e: Exception) {
            android.util.Log.e("ViewProfileActivity", "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al cargar el perfil: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        ivProfilePhotoView = findViewById(R.id.ivProfilePhotoView)
        tvViewName = findViewById(R.id.tvViewName)
        tvViewAge = findViewById(R.id.tvViewAge)
        tvViewCity = findViewById(R.id.tvViewCity)
        tvViewDescription = findViewById(R.id.tvViewDescription)
        interestsViewContainer = findViewById(R.id.interestsViewContainer)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("userEmail", userEmail)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar tu sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    // Limpiar datos guardados
                    val preferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
                    preferences.edit().clear().apply()

                    val intent = Intent(this, com.example.myapplication.ui.login.LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Configurar barra de navegación
        val navExplorar = findViewById<LinearLayout>(R.id.navExplorar)
        val navMatches = findViewById<LinearLayout>(R.id.navMatches)
        val navChats = findViewById<LinearLayout>(R.id.navChats)
        val navPerfil = findViewById<LinearLayout>(R.id.navPerfil)

        navExplorar.setOnClickListener {
            // Ir a ExploreActivity si existe
            try {
                val intent = Intent(this, Class.forName("com.example.myapplication.ui.explore.ExploreActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Explorar no disponible", Toast.LENGTH_SHORT).show()
            }
        }

        navMatches.setOnClickListener {
            Toast.makeText(this, "Matches - Próximamente", Toast.LENGTH_SHORT).show()
        }

        navChats.setOnClickListener {
            Toast.makeText(this, "Chats - Próximamente", Toast.LENGTH_SHORT).show()
        }

        navPerfil.setOnClickListener {
            // Ya estamos en el perfil, no hacer nada
            Toast.makeText(this, "Ya estás en tu perfil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileData() {
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Email no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseService.getUserProfile(userEmail) { profileData ->
            runOnUiThread {
                if (profileData != null) {
                    try {
                        // Cargar nombre
                        val name = profileData["name"]?.toString() ?: "N/A"
                        tvViewName.text = name

                        // Cargar edad
                        val age = profileData["age"]?.toString() ?: "N/A"
                        tvViewAge.text = age

                        // Cargar ciudad
                        val city = profileData["city"]?.toString() ?: "N/A"
                        tvViewCity.text = city

                        // Cargar descripción
                        val description = profileData["description"]?.toString()
                        if (!description.isNullOrEmpty()) {
                            tvViewDescription.text = description
                        } else {
                            tvViewDescription.text = "Sin descripción"
                        }

                        // Cargar foto
                        val photoBase64 = profileData["photo"]?.toString()
                        if (!photoBase64.isNullOrEmpty()) {
                            try {
                                val decodedString = Base64.decode(photoBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                if (bitmap != null) {
                                    ivProfilePhotoView.setImageBitmap(bitmap)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ViewProfileActivity", "Error decodificando foto: ${e.message}")
                            }
                        }

                        // Cargar intereses
                        @Suppress("UNCHECKED_CAST")
                        val interests = profileData["interests"] as? List<String> ?: emptyList()
                        displayInterests(interests)
                    } catch (e: Exception) {
                        android.util.Log.e("ViewProfileActivity", "Error cargando datos: ${e.message}", e)
                        Toast.makeText(this, "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No hay perfil completado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayInterests(interests: List<String>) {
        interestsViewContainer.removeAllViews()

        if (interests.isEmpty()) {
            val tvNoInterests = TextView(this)
            tvNoInterests.text = "Sin intereses seleccionados"
            tvNoInterests.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            interestsViewContainer.addView(tvNoInterests)
            return
        }

        // Mostrar intereses en un layout de 3 columnas
        var currentRow: LinearLayout? = null

        interests.forEachIndexed { index, interest ->
            if (index % 3 == 0) {
                currentRow = LinearLayout(this)
                currentRow!!.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                currentRow!!.weightSum = 3f
                interestsViewContainer.addView(currentRow)
            }

            val tvInterest = TextView(this)
            tvInterest.text = "✓ $interest"
            tvInterest.setTextColor(resources.getColor(android.R.color.black, null))
            tvInterest.textSize = 14f

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.weight = 1f
            params.marginEnd = 8
            tvInterest.layoutParams = params

            currentRow?.addView(tvInterest)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando volvemos de editar
        loadProfileData()
    }
}

