package com.example.myapplication.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio para envío de correos usando SMTP (Gmail)
 *
 * CONFIGURACIÓN NECESARIA:
 * 1. Crear una cuenta de Gmail
 * 2. Habilitar "Contraseñas de aplicaciones" en Google Account
 * 3. Usar esa contraseña aquí (NO la contraseña normal)
 */
object EmailService {

    // ⚙️ CONFIGURACIÓN - Email configurado
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val EMAIL_FROM = "yendermejia0@gmail.com"
    private const val EMAIL_PASSWORD = "wqcolfegitsiylpx"
    private const val EMAIL_FROM_NAME = "TAMATS App"

    /**
     * Enviar correo de bienvenida al registrarse
     */
    suspend fun sendWelcomeEmail(toEmail: String, userName: String): Boolean {
        return sendEmail(
            to = toEmail,
            subject = "💜 ¡Bienvenido a TAMATS, $userName!",
            body = createWelcomeEmailBody(userName)
        )
    }

    /**
     * Enviar correo de recuperación de contraseña
     */
    suspend fun sendPasswordResetEmail(toEmail: String, resetLink: String): Boolean {
        return sendEmail(
            to = toEmail,
            subject = "🔐 Recupera tu cuenta de TAMATS",
            body = createPasswordResetEmailBody(resetLink)
        )
    }

    /**
     * Función genérica para enviar correos
     */
    private suspend fun sendEmail(to: String, subject: String, body: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Configurar propiedades SMTP
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
            }

            // Crear sesión con autenticación
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD)
                }
            })

            // Crear mensaje
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EMAIL_FROM, EMAIL_FROM_NAME))
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8", "html")
            }

            // Enviar
            Transport.send(message)
            Log.d("EmailService", "✅ Correo enviado exitosamente a: $to")
            true
        } catch (e: MessagingException) {
            Log.e("EmailService", "❌ Error enviando correo: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("EmailService", "❌ Error inesperado: ${e.message}", e)
            false
        }
    }

    /**
     * Plantilla HTML para correo de bienvenida
     */
    private fun createWelcomeEmailBody(userName: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #9C27B0; color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💜 TAMATS</h1>
                        <h2>¡Bienvenido!</h2>
                    </div>
                    <div class="content">
                        <h2>¡Hola, $userName! 👋</h2>
                        <p>¡Gracias por unirte a <strong>TAMATS</strong>!</p>
                        <p>Estamos emocionados de tenerte con nosotros. Ahora formas parte de una comunidad increíble donde podrás:</p>
                        <ul>
                            <li>✨ Conocer personas increíbles</li>
                            <li>💬 Chatear y hacer amigos</li>
                            <li>💜 Compartir tus intereses</li>
                            <li>📸 Crear historias y publicaciones</li>
                        </ul>
                        <p>Tu cuenta ha sido creada exitosamente. ¡Ya puedes empezar a explorar!</p>
                        <p style="text-align: center;">
                            <strong>¡Que disfrutes tu experiencia en TAMATS! 🚀</strong>
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2025 TAMATS. Todos los derechos reservados.</p>
                        <p>Este es un correo automático, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Plantilla HTML para correo de recuperación de contraseña
     */
    private fun createPasswordResetEmailBody(resetLink: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #9C27B0; color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 TAMATS</h1>
                        <h2>Recuperación de Contraseña</h2>
                    </div>
                    <div class="content">
                        <h2>¡Hola!</h2>
                        <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>TAMATS</strong>.</p>
                        <div class="warning">
                            <strong>⚠️ Importante:</strong> Si NO solicitaste este cambio, ignora este correo y tu contraseña permanecerá segura.
                        </div>
                        <p>Para crear una nueva contraseña, haz clic en el siguiente botón:</p>
                        <p style="text-align: center;">
                            <a href="$resetLink" class="button">Restablecer Contraseña</a>
                        </p>
                        <p><small>O copia y pega este enlace en tu navegador:</small><br>
                        <small style="color: #9C27B0; word-break: break-all;">$resetLink</small></p>
                        <p><strong>Este enlace expirará en 1 hora</strong> por motivos de seguridad.</p>
                    </div>
                    <div class="footer">
                        <p>💜 Gracias por ser parte de TAMATS</p>
                        <p>© 2025 TAMATS. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}

