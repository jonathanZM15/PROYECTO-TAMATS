SISTEMA DE CHAT BILATERAL - RESUMEN DE IMPLEMENTACIÓN
=====================================================

✅ CAMBIOS REALIZADOS

1. CORRECCIÓN DEL SCROLL DE MENSAJES
   - Se agregó validación para evitar scrollear si la lista está vacía
   - Ahora solo scrollea a la última posición si hay mensajes

2. MEJORAS EN LOS LAYOUTS DE MENSAJES
   
   a) Mensajes de Texto:
      - item_message_sent.xml: Aparecen a la DERECHA con color rosa (#FF1493)
      - item_message_received.xml: Aparecen a la IZQUIERDA con fondo gris
      - Ambos muestran timestamp (hora del mensaje)
      - Agregado breakStrategy para mejor visualización del texto

   b) Imágenes:
      - item_image_sent.xml: Imágenes a la DERECHA con CardView redondeado
      - item_image_received.xml: Imágenes a la IZQUIERDA con CardView redondeado
      - Ambas con bordes redondeados (12dp) y elevación
      - Imágenes de 200x200dp para buena visualización

3. MEJORAS EN EL LAYOUT PRINCIPAL (fragment_messages.xml)
   - Agregado fondo morado (#C7439F) al área de mensajes
   - EditText mejorado con soporte para múltiples líneas (máximo 3)
   - Mejor espaciado y padding en todos los elementos
   - Botones de cámara y galería mejor organizados

📊 FLUJO BILATERAL DE MENSAJES

Usuario A (Emisor)                    Usuario B (Receptor)
    |                                      |
    |---> Escribe mensaje                   |
    |---> Presiona enviar                   |
    |                                      |
    v                                      v
    Mensaje guardado en:              Escucha en tiempo real:
    /chats/{chatId}/messages/         /chats/{chatId}/messages/
         |                                  |
         |---> Timestamp ahora            |---> Mensaje aparece a la izquierda
         |---> senderEmail: A            |---> Marca como recibido
         |                                |
         v                                v
    Aparece a la DERECHA          Puede responder
    con color rosa                con mensaje nuevo

🔄 SINCRONIZACIÓN EN TIEMPO REAL

- Firestore addSnapshotListener() escucha cambios en tiempo real
- MessagesViewModel.loadMessages() se actualiza automáticamente
- MessagesAdapter.setMessages() actualiza la lista
- RecyclerView scrollea automáticamente al último mensaje

✨ CARACTERÍSTICAS DEL CHAT

1. Mensajes de Texto:
   - Envío bilateral
   - Aparecer a derecha (enviados) o izquierda (recibidos)
   - Mostrar timestamp
   - Colores diferenciados

2. Imágenes:
   - Captura desde cámara
   - Seleccionar desde galería
   - Conversión a Base64
   - Visualización con bordes redondeados
   - Sincronización bilateral en tiempo real

3. Interfaz de Usuario:
   - Header con nombre y foto del otro usuario
   - Botón retroceder (X)
   - Botones de cámara y galería
   - EditText para escribir mensajes
   - Botón enviar (corazón)
   - Scroll automático al último mensaje

🎯 VALIDACIONES IMPLEMENTADAS

✅ Solo se scrollea si hay mensajes
✅ Imágenes con soporte a Base64
✅ Mensajes vacíos no se envían
✅ Timestamp en todos los mensajes
✅ Diferenciación clara entre enviados y recibidos
✅ Sincronización en tiempo real en ambas direcciones

