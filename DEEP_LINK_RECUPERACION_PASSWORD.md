# 🔗 Sistema de Deep Links - Recuperación de Contraseña

## ✅ **CONFIGURACIÓN COMPLETADA**

### 📱 **Cómo funciona:**

1. Usuario solicita "Olvidé mi contraseña" en LoginActivity
2. Se genera un token único UUID
3. Se envía correo con enlace: `tamats://reset?token=xxx&email=xxx`
4. Usuario abre el correo en su móvil
5. Al tocar el enlace, Android abre la app TAMATS
6. Se abre ResetPasswordActivity con los parámetros
7. Usuario cambia su contraseña
8. Automáticamente regresa al LoginActivity

---

## 🔧 **COMPONENTES CONFIGURADOS:**

### 1️⃣ **AndroidManifest.xml** - Deep Link configurado

```xml
<activity
    android:name=".ui.password.ResetPasswordActivity"
    android:exported="true"
    android:launchMode="singleTop">
    <!-- Deep Link personalizado: tamats://reset -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="tamats"
            android:host="reset" />
    </intent-filter>
</activity>
```

✅ **Ya configurado** - La app puede recibir enlaces `tamats://reset`

---

### 2️⃣ **LoginActivity.kt** - Generación del enlace

```kotlin
// Genera token único
val resetToken = UUID.randomUUID().toString()

// Crea deep link
val deepLink = "tamats://reset?token=$resetToken&email=$email"

// Guarda token en SharedPreferences (expira en 1 hora)
val prefs = getSharedPreferences("password_reset", MODE_PRIVATE)
prefs.edit().apply {
    putString("token_$resetToken", email)
    putLong("timestamp_$resetToken", timestamp)
    apply()
}

// Envía correo con el enlace
EmailService.sendPasswordResetEmail(email, deepLink)
```

✅ **Ya implementado**

---

### 3️⃣ **EmailService.kt** - Correo con instrucciones

```kotlin
suspend fun sendPasswordResetEmail(toEmail: String, resetLink: String): Boolean
```

El correo incluye:
- 📱 Botón para abrir la app directamente
- 🔗 Enlace copiable si el botón no funciona
- ⏰ Aviso de expiración (1 hora)
- ⚠️ Advertencia de seguridad

✅ **Ya implementado**

---

### 4️⃣ **ResetPasswordActivity.kt** - Manejo del deep link

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Obtiene parámetros del deep link
    email = intent.data?.getQueryParameter("email")
    token = intent.data?.getQueryParameter("token")
    
    // Valida token (existe y no expiró)
    if (!isTokenValid(token!!)) {
        Toast.makeText(this, "❌ Link expirado", Toast.LENGTH_LONG).show()
        finish()
        return
    }
    
    // Muestra UI para cambiar contraseña
}
```

✅ **Ya implementado**

---

## 🧪 **CÓMO PROBARLO:**

### **Método 1: Usando ADB (recomendado para desarrollo)**

```bash
# Abre la app con el deep link desde terminal
adb shell am start -W -a android.intent.action.VIEW \
  -d "tamats://reset?token=test-token-123&email=usuario@ejemplo.com" \
  com.example.myapplication
```

### **Método 2: Desde correo real**

1. Solicita "Olvidé mi contraseña" desde la app
2. Revisa tu correo (Gmail, Outlook, etc.)
3. Toca el botón "Abrir TAMATS y Cambiar Contraseña"
4. La app debería abrirse automáticamente

### **Método 3: Desde navegador del móvil**

1. Copia el enlace del correo: `tamats://reset?token=xxx&email=xxx`
2. Pégalo en el navegador Chrome del móvil
3. Presiona Enter
4. Android preguntará "Abrir con TAMATS" → Confirmar

---

## ⚠️ **PROBLEMAS COMUNES Y SOLUCIONES:**

### ❌ **Problema 1: "El enlace no abre la app"**

**Causa:** Algunos clientes de correo (Gmail web, Outlook desktop) bloquean enlaces con esquemas personalizados por seguridad.

**Solución:**
- ✅ Usar la app de Gmail en el móvil (no la versión web)
- ✅ Copiar el enlace manualmente y pegarlo en Chrome móvil
- ✅ (Alternativa avanzada) Usar Firebase Dynamic Links

---

### ❌ **Problema 2: "Link expirado"**

**Causa:** El token tiene validez de 1 hora.

**Solución:**
- ✅ Solicitar un nuevo correo de recuperación
- ✅ Los tokens viejos se invalidan automáticamente

---

### ❌ **Problema 3: "Usuario no encontrado"**

**Causa:** El correo no está registrado en la BD.

**Solución:**
- ✅ El sistema ya valida que el correo exista ANTES de enviar el email
- ✅ Solo usuarios registrados reciben correos de recuperación

---

## 🔐 **SEGURIDAD IMPLEMENTADA:**

| Feature | Estado | Descripción |
|---------|--------|-------------|
| ✅ Token único UUID | Implementado | Cada enlace es único |
| ✅ Expiración 1 hora | Implementado | Los tokens viejos no funcionan |
| ✅ Un solo uso | Implementado | El token se invalida después de usarlo |
| ✅ Validación de correo | Implementado | Solo usuarios registrados |
| ✅ Contraseña cifrada | Implementado | BCrypt en BD |

---

## 📊 **FLUJO COMPLETO:**

```
Usuario olvida contraseña
        ↓
Ingresa email en LoginActivity
        ↓
Sistema verifica que email exista
        ↓
Genera token UUID único
        ↓
Guarda token en SharedPreferences
        ↓
Envía correo con deep link
        ↓
Usuario abre correo en móvil
        ↓
Toca enlace "tamats://reset?..."
        ↓
Android abre ResetPasswordActivity
        ↓
Valida token (existe + no expiró)
        ↓
Usuario ingresa nueva contraseña
        ↓
Actualiza en Room + Firebase
        ↓
Invalida token usado
        ↓
Muestra éxito → Redirige a Login
        ↓
Usuario inicia sesión con nueva contraseña
```

---

## 🚀 **PRÓXIMAS MEJORAS OPCIONALES:**

1. **Firebase Dynamic Links** - Enlaces universales que funcionan en web también
2. **Notificación Push** - Alternativa al correo
3. **SMS con código** - Para recuperación sin correo
4. **Autenticación 2FA** - Doble factor de seguridad

---

## 📝 **ARCHIVOS MODIFICADOS:**

| Archivo | Cambios | Estado |
|---------|---------|--------|
| `AndroidManifest.xml` | Deep link configurado | ✅ |
| `LoginActivity.kt` | Genera y envía enlace | ✅ |
| `EmailService.kt` | Plantilla mejorada | ✅ |
| `ResetPasswordActivity.kt` | Maneja deep link | ✅ |

---

## ✅ **SISTEMA LISTO PARA USAR**

El sistema de recuperación de contraseña con deep links está **completamente funcional**. 

**Instrucciones para el usuario:**
1. Abrir la app en el móvil
2. Tocar "Olvidé mi contraseña"
3. Ingresar correo registrado
4. Revisar correo en el móvil
5. Tocar el botón del correo
6. La app se abre sola
7. Cambiar contraseña
8. ¡Listo! 🎉

