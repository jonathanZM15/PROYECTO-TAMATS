# SISTEMA DE CHATS DE SOPORTE Y ADMINISTRACIÓN - DOCUMENTACIÓN COMPLETA

## 📋 RESUMEN DE IMPLEMENTACIÓN

Se ha implementado un sistema completo de chats de soporte con las siguientes características:

### 1. **CHAT DE SOPORTE POR DEFECTO (Usuarios Comunes)**

- **Creación Automática**: Se crea automáticamente cuando el usuario completa su perfil en `EditProfileActivity.kt`
- **Características**:
  - Chat directo entre usuario y administrador (`yendermejia0@gmail.com`)
  - Permanece fijado al inicio de la lista (no se mueve)
  - Mostrará un badge "SOPORTE" para identificarlo fácilmente
  - Primer mensaje de bienvenida: "¡Bienvenido! Este es tu chat de soporte."

**Archivos Modificados/Creados:**
- `model/Chat.kt` - Agregados campos `isSupportChat` e `isPinned`
- `model/BroadcastMessage.kt` - Nuevo modelo para mensajes difundidos
- `ui/simulacion/EditProfileActivity.kt` - Función `createSupportChat()` agregada

---

### 2. **PANTALLA DE CHATS PARA USUARIOS COMUNES**

- **Orden de Chats**:
  1. Chats de soporte (fijados)
  2. Chats regulares con otros usuarios (ordenados por último mensaje)

- **Características**:
  - Barra de búsqueda para filtrar chats
  - Badge "SOPORTE" visible en el chat de soporte
  - Click para abrir el chat y ver mensajes

**Archivos Modificados:**
- `ui/explore/ChatsViewModel.kt` - Actualizado `loadChats()` para ordenar soporte primero
- `ui/explore/ChatsAdapter.kt` - Agregado soporte para badge de soporte

---

### 3. **PANTALLA DE CHATS DEL ADMINISTRADOR**

- **Ubicación**: En el menú dropdown del AdminActivity
- **Características**:
  - Lista de todos los chats de soporte (uno por usuario registrado)
  - Scroll lazy loading: carga 10 chats al inicio, luego de 10 en 10 al bajar
  - Barra de búsqueda para encontrar usuarios específicos
  - Muestra foto, nombre y último mensaje de cada usuario
  - Click para abrir el chat y conversar

**Archivos Creados:**
- `admin/fragments/AdminChatsFragment.kt` - Pantalla principal de chats
- `admin/fragments/AdminMessagesFragment.kt` - Pantalla de conversación
- `admin/viewmodels/AdminChatsViewModel.kt` - Lógica de carga y búsqueda
- `admin/viewmodels/AdminMessagesViewModel.kt` - Lógica de mensajes
- `admin/adapters/AdminChatsAdapter.kt` - Adapter para la lista de chats
- `admin/adapters/AdminMessagesAdapter.kt` - Adapter para los mensajes

**Layout XML Creados:**
- `fragment_admin_chats.xml` - Layout de la lista de chats
- `fragment_admin_messages.xml` - Layout de la conversación
- `item_loading.xml` - Item de carga para lazy loading

---

### 4. **MENSAJES DIFUNDIDOS A TODOS LOS USUARIOS**

- **Ubicación**: En el menú dropdown del AdminActivity - "Enviar Mensaje a Todos"
- **Características**:
  - Editor de texto para escribir el mensaje
  - Muestra cantidad de destinatarios (todos los usuarios registrados)
  - Botón para enviar el mensaje a todos
  - El mensaje se crea en cada chat de soporte automáticamente
  - Confirmación de envío exitoso

**Archivos Creados:**
- `admin/fragments/BroadcastMessageFragment.kt` - Pantalla de mensaje masivo

**Layout XML Creado:**
- `fragment_broadcast_message.xml` - Layout de la pantalla de broadcast

---

## 🔧 CAMBIOS EN ARQUITECTURA

### Actualización de AdminActivity

El `AdminActivity.kt` ahora:
1. Contiene un `FrameLayout` como contenedor de fragmentos
2. Mantiene la vista original de usuarios debajo (se oculta cuando hay fragmentos)
3. Tiene métodos para mostrar/ocultar fragmentos:
   - `openAdminChats()` - Abre la pantalla de chats
   - `openBroadcastMessage()` - Abre la pantalla de mensajes masivos
   - `hideUsersView()` - Oculta la lista de usuarios
   - `showUsersView()` - Muestra la lista de usuarios
4. Listener automático para volver a mostrar usuarios cuando se cierre un fragment

### Actualización de Menú Admin

El menú dropdown ahora tiene 3 opciones:
1. **Chats de Soporte** - Acceso a la pantalla de chats
2. **Enviar Mensaje a Todos** - Acceso a broadcast de mensajes
3. **Cerrar Sesión** - Logout (existente)

**Archivos Modificados:**
- `admin/activities/AdminActivity.kt` - Métodos agregados
- `menu/admin_dropdown_menu.xml` - Opciones del menú

---

## 📱 FLUJO DE USUARIO

### Para Usuarios Comunes:
1. Se registran y completan su perfil
2. Automáticamente se crea un chat de soporte
3. En la pantalla de chats, ven el chat de soporte fijado al inicio
4. Pueden clicar para escribir al administrador

### Para el Administrador:
1. En el menu dropdown, selecciona "Chats de Soporte"
2. Ve una lista de todos los usuarios (cargados en scroll lazy)
3. Puede buscar un usuario específico
4. Al clicar en un usuario, abre la conversación
5. Puede escribir mensajes al usuario individual
6. Opcionalmente, selecciona "Enviar Mensaje a Todos" para notificaciones masivas

---

## 🎨 RECURSOS CREADOS

### Drawables:
- `badge_support_background.xml` - Fondo naranja para badge de soporte
- `button_background.xml` - Fondo para botones
- `ic_send.xml` - Icono de enviar
- `ic_chat.xml` - Icono de chat

### Valores:
- `colors.xml` - Colores agregados para mensajes y soporte
- `strings.xml` - Cadenas de texto para el sistema de chats

---

## 💾 MODELOS DE DATOS

### Chat (Actualizado)
```kotlin
data class Chat(
    val id: String = "",
    val user1Email: String = "",
    val user1Name: String = "",
    val user1Photo: String = "",
    val user2Email: String = "",
    val user2Name: String = "",
    val user2Photo: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val isSupportChat: Boolean = false,    // ← NUEVO
    val isPinned: Boolean = false          // ← NUEVO
)
```

### Message (Sin cambios)
```kotlin
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text"
)
```

### BroadcastMessage (Nuevo)
```kotlin
data class BroadcastMessage(
    val id: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val recipients: List<String> = emptyList()
)
```

---

## 🔐 SEGURIDAD

- Solo el email `yendermejia0@gmail.com` puede enviar mensajes masivos
- Los chats de soporte solo pueden ser modificados por admin y el usuario correspondiente
- Verificación de permisos de admin en AdminActivity
- Los mensajes son privados y no son visible para otros usuarios

---

## ✅ CARACTERÍSTICAS COMPLETADAS

- ✅ Chat de soporte por defecto para cada usuario
- ✅ Chat fijado al inicio (no se mueve)
- ✅ Badge "SOPORTE" identificador
- ✅ Pantalla de chats para admin con scroll lazy loading
- ✅ Carga de 10 en 10 usuarios
- ✅ Barra de búsqueda en pantalla de admin
- ✅ Pantalla de conversación individual
- ✅ Opción de enviar mensaje a todos los usuarios
- ✅ Integración con menú dropdown del admin
- ✅ Navegación entre vistas usando fragments
- ✅ Retorno automático a vista de usuarios

---

## 📝 NOTAS IMPORTANTES

1. **Email del Admin**: Es `yendermejia0@gmail.com` - cambiar si es necesario en:
   - `EditProfileActivity.kt`
   - `AdminChatsViewModel.kt`

2. **Lazy Loading**: Se cargan 10 chats inicialmente y 10 más al hacer scroll

3. **Búsqueda**: Busca por nombre o email del usuario

4. **Mensajes Masivos**: Se crean automáticamente en cada chat de soporte del usuario

5. **Base de Datos**: Todos los datos se guardan en Firestore con las colecciones:
   - `chats` - Almacena los chats
   - `messages` - Almacena los mensajes
   - `broadcasts` - Almacena los mensajes difundidos

---

## 🚀 PRÓXIMAS MEJORAS (Opcionales)

- Notificaciones push cuando se recibe un mensaje
- Indicador de "escribiendo..."
- Archivos adjuntos en chats
- Historial de mensajes eliminados
- Reacciones a mensajes (emojis)
- Chats grupales

---

Fin de la documentación. El sistema está completamente implementado y listo para usar.

