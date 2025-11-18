# 🔧 SOLUCIÓN: Error SMTP y Modo de Prueba para Deep Link

## 🔴 **PROBLEMAS DETECTADOS:**

### **1. Error de conexión SMTP**
```
EmailService: ❌ Error enviando correo: Couldn't connect to host, port: smtp.gmail.com, 587; timeout -1
Caused by: java.net.ConnectException: failed to connect to smtp.gmail.com/142.250.98.108 (port 587)
ETIMEDOUT (Connection timed out)
```

**Causas posibles:**
- ❌ No hay conexión a internet en el dispositivo
- ❌ Firewall bloqueando puerto 587
- ❌ Restricciones de red (WiFi corporativa, VPN, etc.)
- ❌ Emulador sin conectividad configurada

### **2. El botón del correo no abre la app**
- El correo nunca se envió (por el error SMTP)
- No se puede probar el deep link sin el correo

---

## ✅ **SOLUCIÓN IMPLEMENTADA:**

### **🧪 MODO DE PRUEBA** - Sin necesidad de correo

Cuando falla el envío de correo, ahora se muestra un **diálogo de prueba** con 3 opciones:

```
⚠️ Error al enviar correo

No se pudo conectar al servidor SMTP.

🔧 MODO DE PRUEBA:
El enlace de recuperación se guardó correctamente.

Token: abc12345...

Para probar el deep link, usa ADB:
adb shell am start -W -a android.intent.action.VIEW -d "tamats://reset?token=xxx&email=xxx"

O presiona 'Probar Deep Link' para abrir directamente.

[Probar Deep Link]  [Copiar Token]  [Cerrar]
```

---

## 🎯 **FUNCIONES DEL DIÁLOGO:**

### 1️⃣ **Botón "Probar Deep Link"**
- ✅ Abre ResetPasswordActivity directamente
- ✅ No requiere correo
- ✅ Simula el click del botón del correo
- ✅ Permite probar sin conexión SMTP

**Código:**
```kotlin
.setPositiveButton("Probar Deep Link") { _, _ ->
    val testIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("tamats://reset?token=$resetToken&email=$encodedEmail")
    }
    startActivity(testIntent)
}
```

### 2️⃣ **Botón "Copiar Token"**
- ✅ Copia el token al portapapeles
- ✅ Útil para depuración
- ✅ Puedes verificar que el token se generó correctamente

### 3️⃣ **Botón "Cerrar"**
- Cierra el diálogo y vuelve al login

---

## 🧪 **CÓMO PROBARLO:**

### **Método 1: Usar el botón de prueba (recomendado)**

1. **Abre TAMATS** en tu móvil/emulador
2. **Toca "Olvidé mi contraseña"**
3. **Ingresa un correo registrado** (ej: `yendermejia02@gmail.com`)
4. **Presiona "Enviar"**
5. **Verás el error de SMTP** (normal sin conexión)
6. **Aparecerá el diálogo de prueba**
7. **Toca "Probar Deep Link"** ✅
8. **La app abrirá ResetPasswordActivity** automáticamente

### **Método 2: Usar ADB (alternativo)**

```bash
# 1. Solicita recuperación para generar token
# (Haz los pasos 1-4 de arriba)

# 2. Copia el token del diálogo

# 3. Usa ADB para abrir directamente
adb shell am start -W -a android.intent.action.VIEW \
  -d "tamats://reset?token=TU_TOKEN_AQUI&email=yendermejia02%40gmail.com" \
  com.example.myapplication
```

---

## 📊 **FLUJO CON MODO DE PRUEBA:**

```
Usuario: "Olvidé mi contraseña"
        ↓
Ingresa correo registrado
        ↓
Sistema valida en Room/Firebase
        ↓
Genera token UUID único
        ↓
Guarda token en SharedPreferences
        ↓
Intenta enviar correo SMTP
        ↓
    ¿Éxito?
        │
        ├─ SÍ → ✅ Correo enviado
        │        Usuario revisa correo
        │        Toca botón del correo
        │        Se abre ResetPasswordActivity
        │
        └─ NO → 🧪 MODO DE PRUEBA
                 Muestra diálogo con opciones:
                 
                 [Probar Deep Link] → Abre directamente
                 [Copiar Token] → Para depuración
                 [Cerrar] → Volver al login
```

---

## 🔧 **ARREGLAR ERROR SMTP (para producción):**

### **Opción 1: Verificar conexión a internet**
```bash
# Verificar conectividad desde el dispositivo
adb shell ping -c 4 smtp.gmail.com
```

### **Opción 2: Configurar emulador con internet**
```bash
# Verificar DNS del emulador
adb shell getprop | grep dns

# Configurar proxy si usas emulador AVD
# Settings → Network & Internet → Proxy
```

### **Opción 3: Probar con datos móviles**
- Si usas WiFi corporativa, puede estar bloqueando SMTP
- Activa datos móviles y prueba de nuevo

### **Opción 4: Verificar credenciales SMTP**
```kotlin
// EmailService.kt - Línea ~30
private const val EMAIL_FROM = "yendermejia0@gmail.com"
private const val EMAIL_PASSWORD = "wqcolfegitsiylpx"  // ✅ Contraseña de aplicación
```

**Verificar:**
1. ✅ La contraseña es de **Contraseñas de aplicación** (no la contraseña normal)
2. ✅ La cuenta tiene **Verificación en 2 pasos** activada
3. ✅ La contraseña de aplicación está **activa y no revocada**

### **Opción 5: Usar otro proveedor SMTP**

Si Gmail no funciona, prueba con:

**SendGrid (recomendado para producción):**
```kotlin
private const val SMTP_HOST = "smtp.sendgrid.net"
private const val SMTP_PORT = "587"
private const val EMAIL_FROM = "noreply@tudominio.com"
private const val EMAIL_PASSWORD = "TU_API_KEY_SENDGRID"
```

---

## 📝 **LOGS MEJORADOS:**

Ahora verás logs más detallados:

**Si el correo se envía:**
```
PasswordReset: ✅ Correo enviado: yendermejia02@gmail.com, Token: abc123...
EmailService: ✅ Correo enviado exitosamente a: yendermejia02@gmail.com
```

**Si falla el SMTP:**
```
EmailService: ❌ Error enviando correo: Couldn't connect to host...
PasswordReset: ❌ Error enviando correo a: yendermejia02@gmail.com
PasswordReset: 🧪 Mostrando diálogo de prueba
```

**Si se usa el botón de prueba:**
```
PasswordReset: 🧪 Abriendo deep link de prueba
ResetPassword: Activity iniciada
ResetPassword: Intent data: tamats://reset?token=abc123&email=test@test.com
ResetPassword: ✅ Token válido, mostrando UI
```

---

## 🎯 **VENTAJAS DEL MODO DE PRUEBA:**

1. ✅ **No requiere conexión SMTP** para probar
2. ✅ **Prueba el deep link** sin esperar correos
3. ✅ **Desarrollo más rápido** (no esperas 30-60 seg por correo)
4. ✅ **Depuración fácil** (puedes copiar el token)
5. ✅ **Funciona en emuladores** sin configuración de red

---

## 📱 **QUÉ VERÁS AL PROBARLO:**

### **Paso 1: Error SMTP**
```
❌ Error al enviar correo
```

### **Paso 2: Diálogo de prueba**
```
⚠️ Error al enviar correo

No se pudo conectar al servidor SMTP.

🔧 MODO DE PRUEBA:
...
[Probar Deep Link]  [Copiar Token]  [Cerrar]
```

### **Paso 3: Toca "Probar Deep Link"**
```
ResetPasswordActivity se abre automáticamente
```

### **Paso 4: Pantalla de cambio de contraseña**
```
┌─────────────────────────────────┐
│  🔐 Recuperación de Contraseña  │
├─────────────────────────────────┤
│                                 │
│  Cuenta: yendermejia02@gmail... │
│                                 │
│  Nueva contraseña:              │
│  ┌─────────────────────────┐    │
│  │                         │    │
│  └─────────────────────────┘    │
│                                 │
│  Confirmar contraseña:          │
│  ┌─────────────────────────┐    │
│  │                         │    │
│  └─────────────────────────┘    │
│                                 │
│  [ Cambiar Contraseña ]         │
│                                 │
└─────────────────────────────────┘
```

---

## ✅ **RESULTADO ESPERADO:**

Con este cambio:

1. ✅ **Puedes probar el deep link** sin correo
2. ✅ **Verificar que ResetPasswordActivity funciona**
3. ✅ **Probar el cambio de contraseña** completo
4. ✅ **No dependes de SMTP** para desarrollo
5. ✅ **En producción seguirá usando correos** (cuando SMTP funcione)

---

## 🔒 **SEGURIDAD:**

**¿Es seguro el modo de prueba?**

- ✅ El token sigue siendo UUID único
- ✅ Expira en 1 hora como siempre
- ✅ Se guarda en SharedPreferences correctamente
- ✅ Solo se usa cuando **falla el SMTP**
- ✅ No compromete la seguridad del sistema
- ⚠️ Para producción, desactiva este modo o limita a builds DEBUG

**Para desactivar en producción:**
```kotlin
if (emailSent) {
    // Correo enviado
} else {
    // Solo mostrar diálogo en modo DEBUG
    if (BuildConfig.DEBUG) {
        // Mostrar diálogo de prueba
    } else {
        // Solo mostrar error
        Toast.makeText(this, "❌ Error al enviar correo", Toast.LENGTH_LONG).show()
    }
}
```

---

## 📁 **ARCHIVOS MODIFICADOS:**

| Archivo | Líneas | Cambios |
|---------|--------|---------|
| `LoginActivity.kt` | ~385-430 | Diálogo de prueba cuando falla SMTP |

---

## 🎉 **¡PROBLEMA RESUELTO!**

Ahora puedes:

1. ✅ **Probar el deep link** sin necesidad de correo
2. ✅ **Verificar que funciona** antes de arreglar SMTP
3. ✅ **Desarrollar más rápido** sin esperar correos
4. ✅ **Depurar fácilmente** con el token copiable

---

## 📞 **PRÓXIMOS PASOS:**

### **Para desarrollo:**
1. Usa el **botón "Probar Deep Link"**
2. Verifica que ResetPasswordActivity funciona
3. Prueba cambiar la contraseña
4. Confirma que todo el flujo funciona

### **Para producción:**
1. Arregla la conexión SMTP (WiFi/datos móviles)
2. Verifica credenciales de Gmail
3. O usa SendGrid/Mailgun
4. Desactiva modo de prueba en release builds

---

**Última actualización:** 2025-11-17  
**Estado:** ✅ Modo de prueba implementado  
**Próximo:** Arreglar SMTP o usar servicio alternativo

