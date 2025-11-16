FLUJO COMPLETO DE MATCHES BILATERALES
====================================

1. USUARIO EMISOR (En Explorar - Hace match con el corazón)
   - Se crea un registro en "matches" con:
     * fromUserEmail: email del emisor
     * toUserEmail: email del receptor
     * estado: "pendiente"

2. USUARIO RECEPTOR (En Matches - Ve la notificación)
   - Ve la notificación en "Nuevos Matches"
   - Puede hacer dos cosas:

   OPCIÓN A: RECHAZA (Botón X)
   - Se marca como "rejected" en la DB
   - Se crea una "RejectionNotification" para el emisor
   - La notificación original se elimina del receptor
   - El emisor verá la notificación de rechazo en "Rechazos"

   OPCIÓN B: ACEPTA (Botón Corazón) ✅
   - Se ejecuta este flujo:
     1. Se crea un registro en "acceptedMatches" con ambos usuarios
     2. Se elimina la notificación original del receptor (desaparece en tiempo real)
     3. Se crea un "matchAcceptanceNotification" para el emisor
     4. Se crea un "chat" bilateralmente

3. USUARIO EMISOR (Recibe notificación de aceptación)
   - Ve una nueva notificación en "Matches Aceptados"
   - Muestra: "[Nombre del usuario] ¡Aceptó tu match!"
   - Solo tiene botón X para eliminar la notificación

4. AMBOS USUARIOS (Pueden chatear)
   - El chat está disponible SOLO si:
     * Existe un registro en "acceptedMatches"
     * Ambos usuarios tienen referencias en ese registro
   - El chat permite:
     * Enviar mensajes de texto
     * Enviar imágenes desde cámara o galería
     * Ver mensaje más reciente y timestamp

COLECCIONES EN FIRESTORE
========================
- matches: Notificaciones iniciales de match (pendientes)
- acceptedMatches: Matches aceptados mutuamente (constancia bilateral)
- matchAcceptanceNotifications: Notificaciones para el emisor
- rejectionNotifications: Notificaciones de rechazo
- chats: Chats bilaterales (solo accesibles si hay acceptedMatch)
- chats/{chatId}/messages: Mensajes dentro de cada chat

GARANTÍAS
=========
✅ Si el receptor NO acepta: No hay chat
✅ Si el receptor ACEPTA: Se crea constancia en "acceptedMatches"
✅ Ambos usuarios ven el mismo chat
✅ La notificación desaparece en tiempo real para el receptor
✅ El emisor recibe notificación de aceptación
✅ Sin constancia bilateral = Sin chat disponible

