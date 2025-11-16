# Sistema de Reordenamiento Automático de Favoritos

## 🎯 Funcionalidad Implementada

Se ha implementado un sistema inteligente de posicionamiento automático de favoritos en el explorar que funciona exactamente como lo solicitaste:

### **Lógica de Posicionamiento**

#### **Al Agregar un Favorito:**
1. Se busca la primera posición disponible (0, 1 o 2)
2. Se asigna automáticamente esa posición al nuevo favorito
3. El perfil aparece inmediatamente en ese lugar en el explorar

**Ejemplo:**
```
Inicial: []
Agrego "Maria" → [María (posición 0)]
Agrego "Ana" → [María (pos 0), Ana (pos 1)]
Agrego "Laura" → [María (pos 0), Ana (pos 1), Laura (pos 2)]
Intento agregar "Sofia" → Mensaje de límite alcanzado
```

#### **Al Eliminar un Favorito:**
1. Se identifica la posición del favorito eliminado
2. Todos los favoritos en posiciones posteriores se mueven una posición hacia arriba
3. Los perfiles se reorganizan automáticamente en tiempo real

**Ejemplo:**
```
Antes: [María (pos 0), Ana (pos 1), Laura (pos 2)]
Elimino Ana (pos 1):
- Laura se mueve: pos 2 → pos 1
Después: [María (pos 0), Laura (pos 1)]
```

---

## 📊 Estructura en Firebase

### Campo `position` en la colección `favorites`

```json
{
  "fromUserEmail": "usuario@email.com",
  "toUserEmail": "maria@email.com",
  "position": 0,
  "timestamp": "2025-11-16T10:30:00Z"
}
```

**Valores válidos de `position`:**
- `0` → Primer favorito (aparece de primero)
- `1` → Segundo favorito (aparece de segundo)
- `2` → Tercer favorito (aparece de tercero)

---

## 🔄 Flujo Técnico

### **Método: `toggleFavorite()` en ProfileAdapter**

#### Cuando se **AGREGA** un favorito:

```kotlin
// 1. Contar favoritos existentes
val allFavorites = db.collection("favorites")
    .whereEqualTo("fromUserEmail", currentUserEmail)
    .get()

// 2. Si hay menos de 3, agregar en la siguiente posición
val nuevaPosicion = allFavorites.size()  // 0, 1 o 2
val favoriteData = hashMapOf(
    "position" to nuevaPosicion,  // Posición automática
    // ... otros campos
)
```

#### Cuando se **ELIMINA** un favorito:

```kotlin
// 1. Obtener la posición del favorito que se elimina
val posicionEliminada = favoriteDoc.data?.get("position")

// 2. Llamar a reorganizarFavoritosAlEliminar()
reorganizarFavoritosAlEliminar(currentUserEmail, posicionEliminada)

// 3. Decrementar la posición de todos los que estaban después
for (doc in favoriteDocs) {
    if (currentPosition > posicionEliminada) {
        val newPosition = currentPosition - 1
        db.collection("favorites").document(doc.id).update("position", newPosition)
    }
}
```

### **Método: `loadFavoriteProfiles()` en ExploreFragment**

```kotlin
// 1. Cargar favoritos ordenados por posición
db.collection("favorites")
    .whereEqualTo("fromUserEmail", currentUserEmail)
    .orderBy("position")  // CRUCIAL: ordenar por posición
    .get()

// 2. Para cada favorito, obtener el perfil completo
for (favoriteDoc in favoriteDocs) {
    val position = favoriteDoc.data?.get("position")
    // Guardar posición para reordenar después
    favoritePositions[email] = position
}

// 3. Devolver ordenados garantizando el orden correcto
val orderedFavorites = favoriteSnapshots.sortedBy { doc ->
    val email = doc.data?.get("email")?.toString()
    favoritePositions[email] ?: 999
}
callback(orderedFavorites)
```

---

## 📋 Cambios Realizados

### **1. ProfileAdapter.kt**

✅ **Método `toggleFavorite()` actualizado:**
- Guarda automáticamente la posición correcta al agregar
- Captura la posición del favorito eliminado
- Llama a `reorganizarFavoritosAlEliminar()` después de eliminar

✅ **Nuevo método `reorganizarFavoritosAlEliminar()`:**
- Decrementa la posición de todos los favoritos posteriores
- Asegura que no queden huecos en el ordenamiento
- Maneja errores de actualización

### **2. ExploreFragment.kt**

✅ **Método `loadFavoriteProfiles()` mejorado:**
- Carga favoritos ordenados por `position` desde Firebase
- Mantiene un mapa de posiciones para garantizar el orden correcto
- Devuelve los favoritos en el orden exacto de posición

---

## ✨ Ejemplo de Uso Completo

### **Paso 1: Usuario agrega 3 favoritos**

```
Usuario hace click en ⭐ de María
→ Se guarda: { toUserEmail: "maria@email.com", position: 0 }
→ María aparece de PRIMERA en explorar

Usuario hace click en ⭐ de Ana
→ Se guarda: { toUserEmail: "ana@email.com", position: 1 }
→ Ana aparece de SEGUNDA en explorar

Usuario hace click en ⭐ de Laura
→ Se guarda: { toUserEmail: "laura@email.com", position: 2 }
→ Laura aparece de TERCERA en explorar

Orden actual en explorar:
1. María (favorito 1)
2. Ana (favorito 2)
3. Laura (favorito 3)
4. [Otros perfiles ordenados por compatibilidad...]
```

### **Paso 2: Usuario elimina a Ana (posición 1)**

```
Usuario hace click en ⭐ de Ana (que está en rojo)
→ Se elimina el documento de Ana
→ Se llama reorganizarFavoritosAlEliminar(..., 1)
→ Laura (que estaba en posición 2) se actualiza a posición 1

Firebase ahora tiene:
- María: position 0
- Laura: position 1  (fue 2)

Orden actual en explorar:
1. María (favorito 1)
2. Laura (favorito 2)  ← Automáticamente subió
3. [Otros perfiles ordenados por compatibilidad...]
```

### **Paso 3: Usuario agrega a Sofia**

```
Usuario hace click en ⭐ de Sofia
→ Se guarda: { toUserEmail: "sofia@email.com", position: 2 }
→ Sofia aparece de TERCERA en explorar

Orden actual en explorar:
1. María (favorito 1)
2. Laura (favorito 2)
3. Sofia (favorito 3)
4. [Otros perfiles...]
```

---

## 🎨 Evidencia Visual

### **En la Pantalla de Explorar**

```
┌─────────────────────────┐
│  EXPLORAR               │
├─────────────────────────┤
│ [Favoritos ordenados]   │
│ ┌───────────────────┐   │
│ │ María        ⭐🔴 │   │ ← Posición 1 (rojo)
│ │ 25, Madrid        │   │
│ └───────────────────┘   │
│ ┌───────────────────┐   │
│ │ Laura        ⭐🔴 │   │ ← Posición 2 (rojo, subió)
│ │ 23, Barcelona     │   │
│ └───────────────────┘   │
│ ┌───────────────────┐   │
│ │ Sofia        ⭐🔴 │   │ ← Posición 3 (rojo)
│ │ 24, Valencia      │   │
│ └───────────────────┘   │
│                         │
│ [Otros perfiles...]     │
│ ┌───────────────────┐   │
│ │ Andrea      ⭐⚫  │   │ ← No favorito (gris)
│ │ 26, Sevilla       │   │
│ └───────────────────┘   │
└─────────────────────────┘
```

---

## 🧪 Testing Recomendado

### **Test 1: Agregar favoritos en orden**
```
✓ Agregar María → posición 0
✓ Agregar Ana → posición 1
✓ Agregar Laura → posición 2
✓ Verificar orden en explorar: María, Ana, Laura
✓ Recargar app → orden se mantiene
```

### **Test 2: Eliminar del medio**
```
✓ Tengo: [María (0), Ana (1), Laura (2)]
✓ Elimino Ana
✓ Verificar: [María (0), Laura (1)]
✓ Laura se movió automáticamente a posición 1
✓ Recargar app → orden correcto
```

### **Test 3: Eliminar del inicio**
```
✓ Tengo: [María (0), Ana (1), Laura (2)]
✓ Elimino María
✓ Verificar: [Ana (0), Laura (1)]
✓ Ambas se reorganizaron automáticamente
```

### **Test 4: Búsqueda con favoritos**
```
✓ Buscar nombre
✓ Los favoritos que coincidan aparecen primero en su posición
✓ Luego otros perfiles
```

---

## 🔐 Validaciones

✅ **Máximo 3 favoritos** - No permite agregar más
✅ **Posiciones secuenciales** - No hay huecos (0, 1, 2)
✅ **Reorganización automática** - Al eliminar, los demás suben
✅ **Persistencia** - Los cambios se guardan en Firebase
✅ **Orden garantizado** - Los favoritos siempre aparecen primero en el orden correcto


