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
            # Términos y Condiciones de Uso de TAMATS

            **Fecha de última actualización: 12 de noviembre de 2025**

            Bienvenido/a a TAMATS, una aplicación de citas diseñada para que puedas conocer gente nueva, interactuar y tener una grata experiencia. Al crear una cuenta y utilizar nuestros servicios, aceptas y te comprometes a cumplir los siguientes términos y condiciones. Si no estás de acuerdo con ellos, por favor, no utilices la aplicación.

            ## 1. Aceptación de los Términos

            Al crear una cuenta en TAMATS, confirmas que has leído, entendido y aceptado en su totalidad estos Términos y Condiciones, así como nuestra Política de Privacidad. Estos términos constituyen un acuerdo legal vinculante entre tú (el "Usuario") y el equipo de TAMATS (los "Administradores").

            ## 2. Uso de Datos y Privacidad

            - **Recopilación y Uso de Datos:** Para el correcto funcionamiento de la aplicación, es necesario que nos proporciones ciertos datos personales (como nombre, edad, fotos y otros detalles de tu perfil). Estos datos se almacenarán de forma segura en nuestra base de datos y se utilizarán exclusivamente para:
                - Crear y gestionar tu perfil de usuario.
                - Permitir que otros usuarios te encuentren y puedan interactuar contigo.
                - Facilitar el sistema de "match" para conectar perfiles compatibles.
                - Mejorar tu experiencia general dentro de la aplicación.

            - **Responsabilidad sobre la Información Compartida:** TAMATS no se hace responsable de la información personal o sensible (como números de teléfono, direcciones, datos financieros, etc.) que decidas compartir voluntariamente con otros usuarios a través del chat interno o cualquier otro medio. Te recomendamos ser prudente y no compartir datos privados con personas que no conoces bien.

            ## 3. Normas de la Comunidad y Conducta del Usuario

            Para mantener un ambiente seguro y respetuoso para todos, el Usuario se compromete a no realizar ninguna de las siguientes acciones:

            - **Acoso y Abuso:** Hostigar, intimidar, amenazar o faltar al respeto a otros usuarios.
            - **Contenido Inapropiado:** Publicar, compartir o enviar cualquier contenido que sea:
                - **De naturaleza sexual explícita:** pornografía, desnudos no solicitados o lenguaje obsceno.
                - **Violento:** imágenes o descripciones de violencia gráfica, gore o crueldad.
                - **Discriminatorio:** que promueva el odio, el racismo, la homofobia o cualquier tipo de discriminación contra un individuo o grupo.
                - **Ilegal:** que incite a actividades ilegales o peligrosas.
                - **Spam o Publicidad no deseada.**
            - **Suplantación de Identidad:** Crear perfiles falsos o hacerse pasar por otra persona.

            ## 4. Denuncias y Consecuencias

            - **Sistema de Denuncias:** Cualquier usuario puede denunciar un perfil o un mensaje que considere que infringe nuestras normas. El equipo de Administradores revisará cada denuncia.
            - **Bloqueo de Cuenta:** Si se comprueba que un usuario ha violado estos términos y condiciones, especialmente en casos de acoso o publicación de contenido indebido, su cuenta será **bloqueada o eliminada de forma permanente**, a discreción de los Administradores.

            ## 5. Comunicación entre Administradores y Usuarios

            - **Mensajes de los Administradores:** Los Administradores de TAMATS podrán encontrar mensajes directos a todos los usuarios o a un usuario en específico si la situación lo amerita (por ejemplo, para notificar sobre actualizaciones importantes, advertencias sobre conducta o responder a una consulta).
            - **Soporte al Usuario:** Si tienes algún problema, duda o necesitas reportar una situación, puedes comunicarte con nuestro equipo de soporte a través de los canales que se habilitarán para ello dentro de la aplicación.

            ## 6. Limitación de Responsabilidad

            TAMATS se proporciona "tal cual" y no garantizamos que la aplicación esté libre de errores o interrupciones. No nos responsabilizamos por las interacciones o encuentros que ocurran entre usuarios fuera de la plataforma. La decisión de conocer a alguien en persona es exclusivamente tuya y bajo tu propio riesgo.

            ## 7. Modificaciones de los Términos

            Nos reservamos el derecho de modificar estos Términos y Condiciones en cualquier momento. Cuando lo hagamos, te notificaremos a través de la aplicación o por otros medios. El uso continuado de TAMATS después de una modificación significará que aceptas los nuevos términos.
        """.trimIndent()
    }
}

