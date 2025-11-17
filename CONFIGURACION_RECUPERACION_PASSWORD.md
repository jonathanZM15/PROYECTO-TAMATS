# 🔐 CONFIGURACIÓN DE RECUPERACIÓN DE CONTRASEÑA - FIREBASE

## ✅ **LO QUE YA FUNCIONA:**

1. ✅ Diálogo para ingresar correo
2. ✅ Envío automático de correo de recuperación
3. ✅ Pantalla en la app para cambiar contraseña
4. ✅ Redirección automática al login después de cambiar

---

## 📧 **CONFIGURAR PLANTILLA DE CORREO EN FIREBASE CONSOLE:**

### **Paso 1: Ir a Firebase Console**
1. Abre https://console.firebase.google.com/
2. Selecciona tu proyecto: **myapplication-b2be5**

### **Paso 2: Configurar Plantilla de Email**
1. En el menú izquierdo, ve a **Authentication** (Autenticación)
2. Click en la pestaña **Templates** (Plantillas)
3. Selecciona **Password reset** (Restablecimiento de contraseña)

### **Paso 3: Personalizar el Correo**
Cambia la plantilla por este texto personalizado:

---

**Asunto del correo:**
```
🔐 Recupera tu cuenta de TAMATS
```

**Cuerpo del correo:**
```
¡Hola!

Recibimos una solicitud para restablecer la contraseña de tu cuenta en TAMATS.

Si NO solicitaste este cambio, ignora este correo y tu contraseña permanecerá segura.

Para crear una nueva contraseña, haz clic en el siguiente botón:

%LINK%

Este enlace expirará en 1 hora por motivos de seguridad.

---

💜 Gracias por ser parte de TAMATS
El equipo de TAMATS

© 2025 TAMATS. Todos los derechos reservados.
```

### **Paso 4: Configurar URL de Redirección**

En la misma pantalla de plantillas, busca:
- **Action URL**: Cambiar a tu dominio de Firebase
- Por defecto será: `https://myapplication-b2be5.firebaseapp.com`

---

## 🎯 **FLUJO COMPLETO DESPUÉS DE LA CONFIGURACIÓN:**

1. ✅ Usuario toca "¿Olvidaste tu contraseña?"
2. ✅ Ingresa su correo
3. ✅ Recibe correo personalizado de TAMATS
4. ✅ Toca el link en el correo
5. ✅ **Se abre la app automáticamente** (no el navegador)
6. ✅ Ve la pantalla de "Nueva Contraseña" dentro de la app
7. ✅ Ingresa nueva contraseña (mínimo 6 caracteres)
8. ✅ Toca "Guardar Nueva Contraseña"
9. ✅ **Automáticamente redirige al Login**
10. ✅ Inicia sesión con la nueva contraseña

---

## 🔧 **CARACTERÍSTICAS IMPLEMENTADAS:**

### **Validaciones:**
- ✅ Contraseña mínimo 6 caracteres
- ✅ Verificación de que ambas contraseñas coincidan
- ✅ Link expira después de 1 hora
- ✅ Detección de links inválidos o expirados

### **Diseño:**
- ✅ Pantalla moderna con gradiente morado
- ✅ Icono de candado 🔐
- ✅ Card blanco elevado
- ✅ Campos con TextInputLayout de Material Design
- ✅ Botón morado estilo TAMATS
- ✅ Botón "Ver contraseña" (ojo)

### **Seguridad:**
- ✅ Link único por usuario
- ✅ Expiración automática
- ✅ Validación de código en servidor (Firebase)
- ✅ No se puede reutilizar el mismo link

---

## 📱 **CÓMO PROBAR:**

1. En la app, toca "¿Olvidaste tu contraseña?"
2. Ingresa un correo registrado
3. Revisa tu bandeja de entrada (también spam)
4. Toca el link del correo
5. **La app se abrirá automáticamente** mostrando la pantalla de cambio
6. Ingresa nueva contraseña y confirma
7. Serás redirigido al login automáticamente

---

## ⚠️ **NOTAS IMPORTANTES:**

- El link del correo **solo funciona UNA vez**
- Si el link expira, solicita uno nuevo
- El correo puede tardar hasta 2 minutos en llegar
- Revisa la carpeta de spam si no lo ves

---

## 🎨 **PRÓXIMOS PASOS: EMAIL DE BIENVENIDA**

Ahora que la recuperación funciona, implementaremos:
- 📧 Email automático al registrarse
- 💌 Mensaje de bienvenida personalizado
- ✨ Plantilla HTML profesional

---

¡TODO LISTO MI REY! 🔥👑

