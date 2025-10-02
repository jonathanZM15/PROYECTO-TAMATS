package com.example.myapplication.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity // RUTA AL LOGIN

class SplashActivity : AppCompatActivity() {

    // Define el tiempo que el Splash estará visible (2000 ms = 2 segundos)
    private val SPLASH_TIME_OUT: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta el código con tu archivo XML de diseño
        setContentView(R.layout.activity_splash)

        // Usa un Handler para crear un temporizador
        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Crea el Intent para iniciar la LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 2. Finaliza la SplashActivity para que no se pueda volver atrás
            finish()

        }, SPLASH_TIME_OUT)
    }
}