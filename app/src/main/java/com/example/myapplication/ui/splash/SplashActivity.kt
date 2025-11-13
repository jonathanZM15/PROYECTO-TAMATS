package com.example.myapplication.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity // RUTA AL LOGIN
import com.example.myapplication.ui.explore.ExploreActivity

class SplashActivity : AppCompatActivity() {

    // Define el tiempo que el Splash estará visible (2000 ms = 2 segundos)
    private val SPLASH_TIME_OUT: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta el código con tu archivo XML de diseño
        setContentView(R.layout.activity_splash)

        // Usa un Handler para crear un temporizador
        Handler(Looper.getMainLooper()).postDelayed({
            // Comprobar si hay un usuario ya guardado en SharedPreferences
            val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
            val savedEmail = prefs.getString("user_email", null)

            val intent = if (!savedEmail.isNullOrEmpty()) {
                // Usuario previamente logueado → ir directo a ExploreActivity
                Intent(this, ExploreActivity::class.java)
            } else {
                // No hay sesión → ir a LoginActivity
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)

            // Finaliza la SplashActivity para que no se pueda volver atrás
            finish()

        }, SPLASH_TIME_OUT)
    }
}