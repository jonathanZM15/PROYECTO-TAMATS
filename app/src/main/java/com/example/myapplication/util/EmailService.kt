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
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { 
                        display: inline-block; 
                        background: #9C27B0; 
                        color: white !important; 
                        padding: 16px 40px; 
                        text-decoration: none; 
                        border-radius: 25px; 
                        margin: 20px 0;
                        font-size: 16px;
                        font-weight: bold;
                        box-shadow: 0 4px 6px rgba(156, 39, 176, 0.3);
                    }
                    .button:hover { background: #7B1FA2; }
                    .footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; border-radius: 4px; }
                    .steps { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .step { margin: 10px 0; padding-left: 25px; position: relative; }
                    .step:before { content: "✓"; position: absolute; left: 0; color: #9C27B0; font-weight: bold; }
                    .link-box { 
                        background: white; 
                        border: 2px dashed #9C27B0; 
                        padding: 15px; 
                        margin: 20px 0; 
                        border-radius: 8px;
                        word-break: break-all;
                        font-family: monospace;
                        font-size: 12px;
                        color: #9C27B0;
                    }
                    .info-box {
                        background: #e3f2fd;
                        border-left: 4px solid #2196F3;
                        padding: 15px;
                        margin: 15px 0;
                        border-radius: 4px;
                    }
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
                        
                        <div class="steps">
                            <h3 style="margin-top: 0; color: #9C27B0;">📱 Opción 1 - Click directo (recomendado):</h3>
                            <div class="step">Abre este correo desde tu dispositivo móvil con TAMATS instalada</div>
                            <div class="step">Toca el botón morado de abajo</div>
                            <div class="step">La app se abrirá automáticamente</div>
                        </div>
                        
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="$resetLink" class="button" style="color: white;">📱 Abrir TAMATS y Cambiar Contraseña</a>
                        </p>
                        
                        <div class="info-box">
                            <h3 style="margin-top: 0; color: #2196F3;">🔧 Opción 2 - Si el botón no funciona:</h3>
                            <p style="margin: 5px 0;">1. Copia el siguiente enlace (mantén presionado y selecciona "Copiar"):</p>
                            <div class="link-box">$resetLink</div>
                            <p style="margin: 5px 0;">2. Abre la app TAMATS manualmente</p>
                            <p style="margin: 5px 0;">3. Ve a "Olvidé mi contraseña"</p>
                            <p style="margin: 5px 0;">4. Pega el enlace cuando se te solicite (próximamente)</p>
                        </div>
                        
                        <p style="font-size: 12px; color: #666; text-align: center; margin-top: 30px;">
                            <strong>⏰ Este enlace expirará en 1 hora</strong> por motivos de seguridad.
                        </p>
                    </div>
                    <div class="footer">
                        <p>💜 Gracias por ser parte de TAMATS</p>
                        <p>© 2025 TAMATS. Todos los derechos reservados.</p>
                        <p style="font-size: 10px; margin-top: 10px;">Este es un correo automático, por favor no respondas.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}

