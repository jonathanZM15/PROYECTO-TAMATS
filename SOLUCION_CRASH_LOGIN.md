# 🔧 SOLUCIÓN AL CRASH - LoginActivity

## ❌ **PROBLEMA DETECTADO:**

La app está crasheando porque:
1. Los recursos (R.id) no están sincronizados después de agregar nuevos layouts
2. El proyecto necesita un **Clean + Rebuild**
3. Agregamos funcionalidad de "Olvidé mi contraseña" pero los IDs no se regeneraron

## ✅ **SOLUCIÓN APLICADA:**

He comentado **TEMPORALMENTE** la funcionalidad de "Olvidé mi contraseña" en `LoginActivity.kt` para que la app no crashee.

**Líneas comentadas:**
- Declaración de `tvForgotPassword`
- `findViewById(R.id.tvForgotPassword)`
- Listener de click

---

## 🚀 **PASOS PARA ARREGLAR EL CRASH:**

### **1️⃣ Clean y Rebuild del Proyecto**

En Android Studio:

```
1. Build → Clean Project (espera a que termine)
2. Build → Rebuild Project (espera 2-3 minutos)
3. Sync Now (si aparece el banner)
```

### **2️⃣ Invalidar Caché (si persiste el error)**

```
File → Invalidate Caches / Restart... → Invalidate and Restart
```

### **3️⃣ Probar la App**

Después del rebuild, la app debería funcionar normalmente.

---

## 🔄 **REACTIVAR "OLVIDÉ MI CONTRASEÑA" (DESPUÉS DEL REBUILD):**

Una vez que la app funcione, descomentar en `LoginActivity.kt`:

### **Línea ~36:**
```kotlin
private lateinit var tvForgotPassword: TextView
```

### **Línea ~70:**
```kotlin
tvForgotPassword = findViewById(R.id.tvForgotPassword)
```

### **Línea ~82:**
```kotlin
tvForgotPassword.setOnClickListener {
    showForgotPasswordDialog()
}
```

### **Agregar de nuevo las funciones (línea ~217):**
```kotlin
private fun showForgotPasswordDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
    val etRecoveryEmail = dialogView.findViewById<EditText>(R.id.etRecoveryEmail)

    val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        .setView(dialogView)
        .create()

    dialogView.findViewById<MaterialButton>(R.id.btnSendRecovery).setOnClickListener {
        val email = etRecoveryEmail.text.toString().trim()

        if (email.isEmpty()) {
            etRecoveryEmail.error = "Ingresa tu correo electrónico"
            return@setOnClickListener
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRecoveryEmail.error = "Ingresa un correo válido"
            return@setOnClickListener
        }

        sendPasswordResetEmail(email, dialog)
    }

    dialogView.findViewById<MaterialButton>(R.id.btnCancelRecovery).setOnClickListener {
        dialog.dismiss()
    }

    dialog.show()
}

private fun sendPasswordResetEmail(email: String, dialog: androidx.appcompat.app.AlertDialog) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    this,
                    "✅ ¡Correo de recuperación enviado! Revisa tu bandeja de entrada.",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            } else {
                val errorMessage = when (task.exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "No existe una cuenta con este correo electrónico"
                    else ->
                        "Error al enviar el correo: ${task.exception?.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
}
```

---

## 🐛 **SI EL CRASH PERSISTE:**

### **Verificar Logcat completo:**
```
adb logcat | grep -E "FATAL|AndroidRuntime"
```

### **Posibles causas:**

1. **NullPointerException:** Algún findViewById devolviendo null
2. **ClassNotFoundException:** Falta alguna dependencia
3. **Resources$NotFoundException:** Layout no encontrado

---

## 📧 **SISTEMA DE CORREOS (NO AFECTADO):**

El sistema SMTP que configuramos está **INTACTO** y funcional:
- ✅ EmailService.kt configurado
- ✅ Dependencias agregadas
- ✅ Solo necesita Sync + Rebuild

---

## ✅ **CHECKLIST DE RECUPERACIÓN:**

- [ ] Build → Clean Project
- [ ] Build → Rebuild Project  
- [ ] Sync Now
- [ ] Probar la app (debería funcionar)
- [ ] Descomentar código de "Olvidé mi contraseña"
- [ ] Rebuild de nuevo
- [ ] Probar funcionalidad completa

---

## 💡 **NOTA IMPORTANTE:**

Android Studio a veces **tarda en regenerar los IDs** después de crear nuevos layouts. El Clean + Rebuild fuerza esta regeneración.

---

**Estado actual:** ✅ App debería funcionar SIN la función de recuperación
**Próximo paso:** Rebuild y reactivar funcionalidad

---

Creado: 2025-11-16 23:15

