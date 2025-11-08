package com.example.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar Firebase cuando la aplicación se inicia
        FirebaseApp.initializeApp(this)
    }
}
