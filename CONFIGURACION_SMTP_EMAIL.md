# 📧 CONFIGURACIÓN DE ENVÍO DE CORREOS CON SMTP (Gmail)

## 🎯 **¿POR QUÉ SMTP EN LUGAR DE FIREBASE?**

Firebase Auth tiene limitaciones:
- ❌ No permite personalizar completamente los correos
- ❌ Los links abren en navegador web (no siempre en la app)
- ❌ Plantillas muy básicas
- ❌ A veces los correos tardan o no llegan

**SMTP con Gmail:**
- ✅ Control total sobre el contenido
- ✅ Plantillas HTML personalizadas
- ✅ Correos instantáneos
- ✅ GRATIS (500 correos/día)
- ✅ Muy confiable

---

## 📝 **PASO 1: CREAR/USAR CUENTA DE GMAIL**

Puedes usar:
1. Tu Gmail personal
2. Crear uno nuevo específico para la app (RECOMENDADO)

**Ejemplo:** `tamatsapp2025@gmail.com`

---

## 🔐 **PASO 2: GENERAR CONTRASEÑA DE APLICACIÓN**

⚠️ **IMPORTANTE:** NO uses tu contraseña normal de Gmail

### **Instrucciones:**

1. **Ir a tu cuenta de Google:**
   - https://myaccount.google.com/

2. **Activar Verificación en 2 pasos** (si no la tienes):
   - Seguridad → Verificación en 2 pasos → Activar
   - Sigue los pasos (teléfono, etc.)

3. **Generar Contraseña de Aplicación:**
   - Seguridad → Verificación en 2 pasos
   - Scroll abajo hasta "Contraseñas de aplicaciones"
   - Click en "Contraseñas de aplicaciones"
   - Selecciona:
     - **App:** Correo
     - **Dispositivo:** Otro (nombre personalizado) → "TAMATS Android"
   - Click en "Generar"
   - **COPIA LA CONTRASEÑA DE 16 CARACTERES** (ej: `abcd efgh ijkl mnop`)

---

## ⚙️ **PASO 3: CONFIGURAR EN LA APP**

Abre el archivo:
```
app/src/main/java/com/example/myapplication/util/EmailService.kt
```

### **Cambiar estas líneas (líneas 27-28):**

```kotlin
// ANTES:
private const val EMAIL_FROM = "tu-correo@gmail.com" 
private const val EMAIL_PASSWORD = "tu-password-de-app"

// DESPUÉS:
private const val EMAIL_FROM = "tamatsapp2025@gmail.com"  // ← Tu Gmail
private const val EMAIL_PASSWORD = "abcd efgh ijkl mnop"   // ← Contraseña de app
```

⚠️ **Quita los espacios de la contraseña:**
```kotlin
private const val EMAIL_PASSWORD = "abcdefghijklmnop"  // Sin espacios
```

---

## 📦 **PASO 4: AGREGAR DEPENDENCIAS**

Abre `app/build.gradle.kts` y agrega al final del bloque `dependencies`:

```kotlin
// JavaMail para envío de correos SMTP
implementation("com.sun.mail:android-mail:1.6.7")
implementation("com.sun.mail:android-activation:1.6.7")
```

Luego click en **"Sync Now"** (arriba a la derecha en Android Studio)

---

## 🌐 **PASO 5: AGREGAR PERMISO DE INTERNET**

En `AndroidManifest.xml`, agrega (si no está):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 🚀 **PASO 6: USAR EL SERVICIO**

### **A) Enviar correo de BIENVENIDA al registrarse:**

En `RegisterActivity.kt`, después de crear el usuario:

```kotlin
import com.example.myapplication.util.EmailService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Después de guardar el usuario en Firebase/Room
CoroutineScope(Dispatchers.IO).launch {
    val emailSent = EmailService.sendWelcomeEmail(
        toEmail = userEmail,
        userName = userName
    )
    
    withContext(Dispatchers.Main) {
        if (emailSent) {
            Log.d("Register", "✅ Correo de bienvenida enviado")
        }
    }
}
```

### **B) Enviar correo de RECUPERACIÓN:**

En `LoginActivity.kt`, al solicitar reset:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val resetLink = "https://tu-app.com/reset?token=abc123"
    val emailSent = EmailService.sendPasswordResetEmail(
        toEmail = userEmail,
        resetLink = resetLink
    )
    
    withContext(Dispatchers.Main) {
        if (emailSent) {
            Toast.makeText(this@LoginActivity, 
                "📧 Correo enviado! Revisa tu bandeja", 
                Toast.LENGTH_LONG).show()
        }
    }
}
```

---

## 📊 **LÍMITES Y CONSIDERACIONES:**

### **Gmail SMTP Gratis:**
- ✅ **500 correos por día**
- ✅ Suficiente para app pequeña/mediana
- ✅ Completamente gratis

### **Si necesitas más:**
- SendGrid: 100/día gratis, luego pago
- Mailgun: 100/día gratis
- AWS SES: $0.10 por 1000 correos

---

## 🎨 **PLANTILLAS INCLUIDAS:**

### **1. Correo de Bienvenida:**
- ✅ Diseño moderno con gradiente morado
- ✅ Lista de beneficios
- ✅ Mensaje personalizado con nombre
- ✅ Logo de TAMATS

### **2. Correo de Recuperación:**
- ✅ Botón grande para reset
- ✅ Advertencia de seguridad
- ✅ Link alternativo (por si el botón no funciona)
- ✅ Expiración en 1 hora

---

## ✅ **CHECKLIST DE CONFIGURACIÓN:**

- [ ] Cuenta de Gmail creada/seleccionada
- [ ] Verificación en 2 pasos activada
- [ ] Contraseña de aplicación generada
- [ ] Email y contraseña configurados en `EmailService.kt`
- [ ] Dependencias JavaMail agregadas en `build.gradle.kts`
- [ ] Sync del proyecto realizado
- [ ] Permiso INTERNET en manifest
- [ ] Código implementado en Register/Login

---

## 🧪 **CÓMO PROBAR:**

1. Compila la app
2. Regístrate con un correo REAL
3. Revisa tu bandeja de entrada
4. Debería llegar el correo en **menos de 5 segundos**
5. Revisa también la carpeta de SPAM (primera vez puede ir ahí)

---

## 🐛 **SOLUCIÓN DE PROBLEMAS:**

### **"Authentication failed"**
- ❌ Contraseña incorrecta
- ✅ Usa la contraseña de APLICACIÓN, NO la normal
- ✅ Sin espacios en la contraseña

### **"Connection timed out"**
- ❌ Sin internet
- ❌ Firewall bloqueando puerto 587
- ✅ Probar en red diferente

### **"Email not sent"**
- ❌ Configuración incorrecta
- ✅ Revisar logs con `adb logcat | grep EmailService`

---

## 📞 **DATOS QUE NECESITO:**

Para completar la configuración, necesito que me des:

1. **Email de Gmail** que vas a usar (ej: `tamatsapp2025@gmail.com`)
2. **Contraseña de aplicación** generada (16 caracteres)

O si prefieres, puedes editarlo tú mismo en `EmailService.kt` líneas 27-28.

---

¡Con esto tendrás un sistema profesional de correos! 💜✨

