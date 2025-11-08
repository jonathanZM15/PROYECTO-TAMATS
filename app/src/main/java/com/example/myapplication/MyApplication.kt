package com.example.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import java.lang.Thread

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Capturar excepciones no capturadas para depuración
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("CRASH", "Excepción no capturada en ${thread.name}", exception)
            exception.printStackTrace()
            // Relanzar la excepción para que se ejecute el handler por defecto
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, exception)
        }

        try {
            // Inicializar Firebase cuando la aplicación se inicia
            FirebaseApp.initializeApp(this)
            android.util.Log.d("MyApplication", "Firebase inicializado correctamente")
        } catch (e: Exception) {
            android.util.Log.e("MyApplication", "Error inicializando Firebase: ${e.message}", e)
            e.printStackTrace()
        }
    }
}

