package com.example.myapplication.ui.register

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class TermsAndConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_and_conditions)

        val btnReturn = findViewById<Button>(R.id.btnReturnFromTerms)
        btnReturn.setOnClickListener {
            finish()
        }

        // Cargar el contenido de los términos y condiciones
        val tvTermsContent = findViewById<TextView>(R.id.tvTermsContent)
        tvTermsContent.text = getTermsAndConditionsText()
    }

    private fun getTermsAndConditionsText(): String {
        return """
            📱 TÉRMINOS Y CONDICIONES
            
            Última actualización: 16 de noviembre de 2025
            
            
            👋 ¡Hola!
            
            Bienvenido/a a TAMATS, tu app para conocer gente increíble. Al usar nuestra plataforma, aceptas estos términos. Si no estás de acuerdo, por favor no uses la app.
            
            
            
            ✅ 1. ACEPTACIÓN
            
            Para usar TAMATS debes:
            
            🔸 Tener al menos 18 años
            🔸 Aceptar estos términos
            🔸 Usar la app responsablemente
            
            
            
            🔒 2. TUS DATOS Y PRIVACIDAD
            
            📊 Recopilamos:
            • Nombre, edad y fotos
            • Preferencias y ubicación
            • Mensajes e interacciones
            
            🎯 Los usamos para:
            • Crear tu perfil
            • Conectarte con personas
            • Mejorar tu experiencia
            • Mantener la seguridad
            
            ⚠️ IMPORTANTE
            No compartas información sensible:
            ❌ Dirección de casa
            ❌ Datos bancarios
            ❌ Contraseñas
            
            
            
            👥 3. NORMAS DE LA COMUNIDAD
            
            ✅ SÍ PUEDES:
            • Ser auténtico y respetuoso
            • Usar fotos reales tuyas
            • Reportar comportamientos
            • Conocer gente increíble
            
            ❌ PROHIBIDO:
            • Acosar o intimidar
            • Contenido sexual no solicitado
            • Imágenes violentas
            • Discriminar
            • Perfiles falsos
            • Spam o publicidad
            • Contenido ilegal
            • Solicitar dinero
            
            
            
            ⚖️ 4. CONSECUENCIAS
            
            Si violas las normas:
            
            1️⃣ Primera vez
               → Advertencia
            
            2️⃣ Reincidencia
               → Suspensión (7-30 días)
            
            3️⃣ Casos graves
               → ❌ Bloqueo permanente
            
            💡 Cualquier usuario puede denunciar. Revisamos cada caso.
            
            
            
            🛡️ 5. TU SEGURIDAD
            
            Consejos importantes:
            
            ✓ Revisa perfiles antes de interactuar
            ✓ Confía en tu instinto
            ✓ Reporta comportamientos raros
            ✓ No compartas info personal pronto
            
            Si decides conocer a alguien:
            → Lugar público
            → Avisa a un amigo/a
            → Celular cargado
            → Mantente sobrio/a
            
            
            
            📋 6. RESPONSABILIDAD
            
            TAMATS NO se responsabiliza de:
            
            • Encuentros fuera de la app
            • Info compartida voluntariamente
            • Acciones de otros usuarios
            • Relaciones que surjan
            
            ⚠️ Tú decides con quién hablar y qué compartir. La seguridad es TU responsabilidad.
            
            
            
            🔄 7. CAMBIOS
            
            Podemos actualizar estos términos. Te avisaremos mediante:
            
            • Notificación en la app
            • Email registrado
            
            Seguir usando la app = aceptas cambios.
            
            
            
            
            ✨ RECUERDA
            
            Al usar TAMATS confirmas que:
            • Has leído estos términos
            • Los entiendes y aceptas
            • Te comprometes a seguirlos
            
            
            💬 ¿Dudas?
            
            Contáctanos en el soporte de la app.
            Estamos aquí para ayudarte.
            
            
            
            
            Hecho con 💜 para conectar personas
        """.trimIndent()
    }
}

