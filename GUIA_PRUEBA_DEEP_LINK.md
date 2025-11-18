# 🔧 SOLUCIÓN FINAL: Deep Link para Recuperación de Contraseña

## ✅ **CAMBIOS REALIZADOS:**

### 1️⃣ **LoginActivity.kt - Línea ~339**
Se cambió el formato del enlace a un **Intent URL** que funciona desde correos electrónicos:

```kotlin
// FORMATO INTENT URL (funciona desde Gmail, Outlook, etc.)
val resetLink = "intent://reset?token=$resetToken&email=$encodedEmail#Intent;scheme=tamats;package=com.example.myapplication;end"
```

**¿Por qué este formato?**
- ✅ Es reconocido por Android desde clientes de correo
- ✅ Especifica el package de la app
- ✅ No requiere navegador intermedio
- ✅ Funciona en Gmail, Outlook, Yahoo Mail

---

### 2️⃣ **EmailService.kt - Plantilla simplificada**
Se eliminó la opción de copiar enlace y se dejó **SOLO el botón principal**:

```html
<a href="$resetLink" class="button">📱 Abrir TAMATS</a>
```

**Mejoras:**
- 📱 Diseño limpio y profesional
- 🎯 Botón grande y visible
- 📝 Instrucciones claras paso a paso
- ⚠️ Advertencias de seguridad
- ⏰ Recordatorio de expiración

---

### 3️⃣ **ResetPasswordActivity.kt - Logs de depuración**
Se agregaron logs para diagnosticar problemas:

```kotlin
android.util.Log.d("ResetPassword", "Activity iniciada")
android.util.Log.d("ResetPassword", "Intent data: ${intent.data}")
android.util.Log.d("ResetPassword", "Email: $email")
android.util.Log.d("ResetPassword", "Token: $token")
```

---

## 🧪 **CÓMO PROBAR:**

### **Paso 1: Instalar la app en el móvil**
```bash
# Compilar e instalar
gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **Paso 2: Solicitar recuperación**
1. Abre TAMATS en tu móvil
2. Toca "Olvidé mi contraseña"
3. Ingresa un correo registrado
4. Presiona "Enviar"

### **Paso 3: Revisar correo**
1. **Abre la app de Gmail** en tu móvil (NO desde PC, NO desde web)
2. Busca el correo de TAMATS
3. Verás un botón morado grande "📱 Abrir TAMATS"

### **Paso 4: Tocar el botón**
1. Toca el botón morado
2. Android detectará el Intent URL
3. La app TAMATS se abrirá automáticamente
4. Verás la pantalla de "Cambiar Contraseña"

### **Paso 5: Ver logs (si hay problemas)**
```bash
# Ver logs en tiempo real
adb logcat -s ResetPassword:D

# Deberías ver:
# ResetPassword: Activity iniciada
# ResetPassword: Intent data: tamats://reset?token=xxx&email=xxx
# ResetPassword: Email: usuario@ejemplo.com
# ResetPassword: Token: abc-123-def
# ResetPassword: ✅ Token válido, mostrando UI
```

---

## ⚠️ **IMPORTANTE PARA QUE FUNCIONE:**

### ✅ **Requisitos obligatorios:**

1. **App instalada**: TAMATS debe estar instalada en el móvil
2. **Correo desde móvil**: Abre el correo desde el teléfono (no PC)
3. **App nativa**: Usa la app de Gmail/Outlook (no Gmail web)
4. **Token vigente**: El enlace expira en 1 hora
5. **Internet activo**: Para enviar y recibir el correo

---

## 🐛 **DIAGNÓSTICO DE PROBLEMAS:**

### ❌ **"El botón no hace nada"**

**Verifica:**
1. ¿Abriste el correo desde el móvil? → Debe ser en el móvil
2. ¿Usas la app de Gmail? → No funciona bien desde Gmail web
3. ¿Está instalada TAMATS? → Instálala primero

**Solución:**
```bash
# Ver qué pasa cuando tocas el botón
adb logcat | grep -i "intent\|tamats\|reset"
```

---

### ❌ **"La app no se abre"**

**Causa probable:** El intent-filter no está bien configurado

**Verificar AndroidManifest.xml:**
```xml
<activity
    android:name=".ui.password.ResetPasswordActivity"
    android:exported="true">
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

**Verificación manual:**
```bash
# Probar el deep link directamente
adb shell am start -W -a android.intent.action.VIEW \
  -d "tamats://reset?token=test123&email=test@test.com" \
  com.example.myapplication
```

Si esto abre la app, el problema está en el formato del enlace del correo.

---

### ❌ **"Token inválido o expirado"**

**Causas:**
1. Pasó más de 1 hora → Solicita nuevo correo
2. Ya usaste ese enlace → Solo funciona una vez
3. El email no coincide → El token está asociado a otro email

**Ver token guardado:**
```bash
# Ver SharedPreferences
adb shell run-as com.example.myapplication \
  cat /data/data/com.example.myapplication/shared_prefs/password_reset.xml
```

---

### ❌ **"El correo no llega"**

**Soluciones:**
1. Revisa **Spam/Correo no deseado**
2. Espera **30-60 segundos** (SMTP puede tardar)
3. Verifica tu **conexión a internet**
4. Confirma que el correo esté **registrado en TAMATS**

**Ver logs de envío:**
```bash
adb logcat -s EmailService:D PasswordReset:D
```

Deberías ver:
```
EmailService: ✅ Correo enviado exitosamente a: usuario@ejemplo.com
PasswordReset: ✅ Email sent to: usuario@ejemplo.com, Token: abc-123
```

---

## 🔍 **PRUEBA ALTERNATIVA (sin correo):**

Si quieres probar sin esperar el correo:

### **Opción 1: ADB Direct**
```bash
# Generar un token de prueba manualmente
# Ejecutar en tu código o agreagar temporalmente:
val testToken = "TEST-TOKEN-123"
val prefs = getSharedPreferences("password_reset", MODE_PRIVATE)
prefs.edit().apply {
    putString("token_$testToken", "tu_email@ejemplo.com")
    putLong("timestamp_$testToken", System.currentTimeMillis())
    apply()
}

# Luego abrir con ADB:
adb shell am start -W -a android.intent.action.VIEW \
  -d "tamats://reset?token=TEST-TOKEN-123&email=tu_email@ejemplo.com" \
  com.example.myapplication
```

### **Opción 2: Desde Chrome móvil**
1. Copia el enlace del correo
2. Abre Chrome en el móvil
3. Pega: `tamats://reset?token=xxx&email=xxx`
4. Presiona Enter
5. Android pregunta "¿Abrir con TAMATS?"
6. Confirma

---

## 📊 **FLUJO COMPLETO ESPERADO:**

```
1. Usuario: "Olvidé mi contraseña"
        ↓
2. Ingresa correo registrado
        ↓
3. App genera token UUID único
        ↓
4. Guarda token en SharedPreferences
        ↓
5. Envía correo con Intent URL
        ↓
6. Usuario abre Gmail app en móvil
        ↓
7. Toca botón morado
        ↓
8. Android parsea intent URL
        ↓
9. Detecta scheme "tamats" y host "reset"
        ↓
10. Busca app con intent-filter matching
        ↓
11. Encuentra com.example.myapplication
        ↓
12. Lanza ResetPasswordActivity
        ↓
13. Activity recibe Intent con data
        ↓
14. Parsea token y email de intent.data
        ↓
15. Valida token en SharedPreferences
        ↓
16. Token válido? → Muestra UI
        ↓
17. Usuario ingresa nueva contraseña
        ↓
18. Valida requisitos (8+ chars, mayúscula, número)
        ↓
19. Cifra con BCrypt
        ↓
20. Actualiza en Room DB
        ↓
21. Actualiza en Firebase
        ↓
22. Invalida token (un solo uso)
        ↓
23. Muestra diálogo de éxito
        ↓
24. Redirige a LoginActivity
        ↓
25. ✅ Usuario inicia sesión con nueva contraseña
```

---

## 📝 **CHECKLIST FINAL:**

Antes de probar, verifica:

- [ ] App compilada e instalada en el móvil
- [ ] AndroidManifest.xml tiene el intent-filter correcto
- [ ] LoginActivity genera Intent URL (no simple deep link)
- [ ] EmailService envía correo con botón
- [ ] ResetPasswordActivity tiene logs de depuración
- [ ] Tienes acceso al correo en el móvil
- [ ] Internet activo en el móvil
- [ ] Logcat corriendo para ver logs

---

## 🎯 **FORMATO DEL ENLACE:**

### ✅ **CORRECTO (Intent URL):**
```
intent://reset?token=abc123&email=user%40test.com#Intent;scheme=tamats;package=com.example.myapplication;end
```

### ❌ **INCORRECTO (Simple deep link):**
```
tamats://reset?token=abc123&email=user@test.com
```

**¿Por qué?**
El Intent URL incluye:
- `intent://` → Protocolo reconocido por Android
- `#Intent;` → Delimitador de parámetros
- `scheme=tamats` → Scheme personalizado
- `package=com.example.myapplication` → Package exacto
- `;end` → Cierre del Intent

Esto hace que Android sepa **exactamente qué app abrir**.

---

## ✅ **RESULTADO ESPERADO:**

Cuando todo funcione correctamente:

1. ✅ Recibes correo en menos de 1 minuto
2. ✅ Botón morado es visible y clicable
3. ✅ Al tocar el botón, la app se abre sola
4. ✅ Ves la pantalla de "Cambiar Contraseña"
5. ✅ El email se muestra correctamente
6. ✅ Puedes ingresar nueva contraseña
7. ✅ Se actualiza exitosamente
8. ✅ Redirige al login
9. ✅ Puedes iniciar sesión con la nueva contraseña

---

## 📞 **SI NADA FUNCIONA:**

1. **Copia los logs completos:**
```bash
adb logcat > logs.txt
```

2. **Busca errores:**
```bash
grep -i "error\|exception\|failed" logs.txt
```

3. **Verifica el Intent:**
```bash
grep -i "intent\|tamats\|reset" logs.txt
```

4. **Comparte los logs** para diagnóstico

---

**Última actualización:** 2025-11-17  
**Estado:** ✅ Listo para probar  
**Formato enlace:** Intent URL con package explícito

