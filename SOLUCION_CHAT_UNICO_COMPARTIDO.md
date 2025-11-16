SOLUCIÓN: CHAT ÚNICO COMPARTIDO - NO DOS CHATS
==============================================

PROBLEMA IDENTIFICADO:
- Se estaban creando DOS chats separados cuando se aceptaba un match
- Usuario A veía su chat
- Usuario B veía otro chat diferente
- Los mensajes NO se sincronizaban porque iban a diferentes documentos

SOLUCIÓN IMPLEMENTADA:
- Se crea UN SOLO chat compartido entre ambos usuarios
- Se utiliza un ID consistente basado en los emails de ambos usuarios

MÉTODO DE GENERACIÓN DE ID:
```
emails = [usuario1@email.com, usuario2@email.com]
emails_sorted = emails.sort()  // Garantiza orden consistente
chatId = "${emails_sorted[0]}_${emails_sorted[1]}"

Ejemplo:
usuario1@email.com = "a@test.com"
usuario2@email.com = "z@test.com"
chatId = "a@test.com_z@test.com"  (siempre el mismo, sin importar quién cree el chat)
```

CAMBIOS REALIZADOS:

1. MatchAdapter.kt
   - Eliminada creación de DOS chats
   - Nuevo método: createSharedChat()
   - Genera ID consistente: emails_ordenados.concat("_")
   - Usa db.collection("chats").document(chatId).set() en lugar de .add()

2. RejectionAdapter.kt
   - Mismo método de ID consistente
   - También crea UN SOLO chat compartido
   - Usa el mismo chatId consistente

3. Chat.kt (Modelo)
   - Agregado campo chatId para referencia

4. ChatsViewModel.kt
   - Busca chats por user1Email Y user2Email
   - Evita duplicados en la lista
   - Muestra el MISMO chat para ambos usuarios

FLUJO CORRECTO AHORA:

Usuario A (Emisor)              Usuario B (Receptor)
   |                                 |
   ├─> Hace match                    |
   |                                 |
   |                      ┌─ Recibe notificación
   |                      |
   |                      └─> Acepta match (Corazón)
   |                          |
   |                          └─> createSharedChat()
   |                              chatId = "a@test.com_z@test.com"
   |                              db.collection("chats")
   |                                .document(chatId)
   |                                .set(chat)
   |
   └─> Recibe notificación
       Entra a Chats
       Busca donde user1Email = suEmail
       ENCUENTRA el chat con chatId = "a@test.com_z@test.com"
       
       
Usuario A escribe mensaje:        Usuario B recibe en TIEMPO REAL:
"Hola, ¿cómo estás?"            - Se escucha el MISMO chatId
   |                             - Mismo documento en Firestore
   v                             - Los mensajes aparecen a la izquierda
db.collection("chats")
  .document("a@test.com_z@test.com")
  .collection("messages")
  .add(message)
       |
       ├─> Timestamp now
       ├─> senderEmail: a@test.com
       ├─> content: "Hola, ¿cómo estás?"
       |
       └─> Ambos usuarios escuchan el MISMO documento
           Firestore notifica a ambos listeners
           Mensaje aparece en AMBAS pantallas


VENTAJAS DE ESTA SOLUCIÓN:
✅ UN SOLO chat compartido
✅ Mensajes bidireccionales en tiempo real
✅ No hay documentos duplicados
✅ El chatId es CONSISTENTE (mismo para ambos usuarios)
✅ Si A busca a B o B busca a A, encuentran el MISMO chat
✅ Los mensajes se sincronizan automáticamente

VERIFICACIÓN EN FIRESTORE:
Antes (INCORRECTO):
/chats/{autoId1} -> user1: A, user2: B
/chats/{autoId2} -> user1: B, user2: A  ❌ DOS DOCUMENTOS

Ahora (CORRECTO):
/chats/a@test.com_z@test.com
  ├─ user1: A
  ├─ user2: B
  └─ /messages
      ├─ {msg1}
      ├─ {msg2}
      └─ {msg3}  ✅ UN SOLO DOCUMENTO CON TODOS LOS MENSAJES

