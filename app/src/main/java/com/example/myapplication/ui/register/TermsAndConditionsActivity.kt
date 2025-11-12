package com.example.myapplication.ui.register

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import java.io.BufferedReader
import java.io.InputStreamReader

class TermsAndConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_and_conditions)

        val tvTermsContent = findViewById<TextView>(R.id.tvTermsContent)
        val btnReturn = findViewById<Button>(R.id.btnReturn)

        // Cargar el contenido de los términos y condiciones
        val termsText = loadTermsFromFile()
        tvTermsContent.text = termsText

        // Configurar el botón de regreso
        btnReturn.setOnClickListener {
            finish()
        }
    }

    private fun loadTermsFromFile(): String {
        return try {
            val inputStream = assets.open("TERMINOS_Y_CONDICIONES.md")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val text = bufferedReader.use { it.readText() }
            // Remover los markers de markdown de código si existen
            text.replace("```markdown", "").replace("```", "")
                .replace("// filepath: c:\\Users\\CompuStore\\Desktop\\proyecto_moviles\\TERMINOS_Y_CONDICIONES.md", "")
                .trim()
        } catch (e: Exception) {
            "No se pudieron cargar los términos y condiciones. Por favor, intenta más tarde."
        }
    }
}

