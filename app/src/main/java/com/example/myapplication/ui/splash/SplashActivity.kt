package com.example.myapplication.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity
import com.example.myapplication.admin.activities.AdminActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    // Define el tiempo que el Splash estará visible (2000 ms = 2 segundos)
    private val SPLASH_TIME_OUT: Long = 2000
    // Nota: debe coincidir con la constante en LoginActivity
    private val ADMIN_EMAIL = "yendermejia0@gmail.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("SplashActivity", "onCreate iniciado")

        // Conecta el código con tu archivo XML de diseño
        setContentView(R.layout.activity_splash)

        android.util.Log.d("SplashActivity", "Layout cargado correctamente")

        // Usa un Handler para crear un temporizador
        Handler(Looper.getMainLooper()).postDelayed({
            android.util.Log.d("SplashActivity", "Handler ejecutándose después de SPLASH_TIME_OUT")

            // Comprobar si hay un usuario ya guardado en SharedPreferences
            val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
            val savedEmail = prefs.getString("user_email", null)
            var isAdmin = prefs.getBoolean("is_admin", false)

            android.util.Log.d("SplashActivity", "savedEmail: $savedEmail, isAdmin: $isAdmin")

            // Si no hay email guardado pero Firebase tiene un usuario autenticado,
            // inferimos la sesión desde FirebaseAuth (esto asegura que el admin
            // permanezca logeado hasta hacer logout).
            if (savedEmail.isNullOrEmpty()) {
                val authUser = FirebaseAuth.getInstance().currentUser
                if (authUser != null) {
                    val authEmail = authUser.email
                    android.util.Log.d("SplashActivity", "Firebase authEmail: $authEmail")
                    if (!authEmail.isNullOrEmpty()) {
                        // Si coincide con el email admin, marcar como admin
                        isAdmin = authEmail.equals(ADMIN_EMAIL, ignoreCase = true)
                    }
                }
            }

            val intent = if (!savedEmail.isNullOrEmpty()) {
                // Si la sesión corresponde a administrador, abrir AdminActivity
                if (isAdmin) {
                    android.util.Log.d("SplashActivity", "Navegando a AdminActivity")
                    Intent(this, AdminActivity::class.java)
                } else {
                    android.util.Log.d("SplashActivity", "Navegando a MainActivity")
                    Intent(this, MainActivity::class.java)
                }
            } else {
                // No hay sesión → ir a LoginActivity
                android.util.Log.d("SplashActivity", "Navegando a LoginActivity")
                Intent(this, LoginActivity::class.java)
            }

            try {
                startActivity(intent)
                android.util.Log.d("SplashActivity", "startActivity ejecutado correctamente")
                // Finaliza la SplashActivity para que no se pueda volver atrás
                finish()
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Error al iniciar actividad: ${e.message}", e)
                e.printStackTrace()
            }

        }, SPLASH_TIME_OUT)
    }
}