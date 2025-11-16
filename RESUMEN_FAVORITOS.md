# Funcionalidad de Favoritos - Sistema de Exploración

## 🌟 Características Implementadas

### 1. **Botón de Estrella Funcional**
- ✅ Click en la estrella para agregar/quitar favoritos
- ✅ **Máximo de 3 favoritos** por usuario
- ✅ Color **rojo** cuando es favorito
- ✅ Color **gris** cuando NO es favorito
- ✅ Cambio de color inmediato al hacer click

### 2. **Persistencia en Base de Datos**
Se crea automáticamente la colección `favorites` en Firebase con documentos como:

```json
{
  "fromUserEmail": "usuario@email.com",
  "toUserEmail": "perfil-favorito@email.com",
  "timestamp": "2025-11-16T10:30:00Z",
  "position": 0
}
```

**Campo "position"**: Mantiene el orden en que fueron seleccionados los favoritos (0, 1, 2)

### 3. **Orden de Visualización**
Los perfiles se muestran en el siguiente orden:

1. **Primeros**: Los 3 favoritos (en el orden que fueron seleccionados)
2. **Después**: El resto de los perfiles ordenados por:
   - Compatibilidad de intereses (descendente)
   - Rango de edad similar ±3 años
   - Fecha de creación (más reciente primero)

### 4. **Actualizaciones en Tiempo Real**
- El ViewModel mantiene un conjunto de emails de favoritos: `favoriteEmails`
- Cuando se agrega/elimina un favorito, se actualiza inmediatamente
- El botón cambia de color instantáneamente
- La lista se reorganiza si es necesario

---

## 📊 Estructura de Base de Datos

### Colección: `favorites`

```
favorites/
├── doc1/
│   ├── fromUserEmail: "juan@email.com"
│   ├── toUserEmail: "maria@email.com"
│   ├── timestamp: 2025-11-16T10:30:00Z
│   └── position: 0  (primer favorito)
├── doc2/
│   ├── fromUserEmail: "juan@email.com"
│   ├── toUserEmail: "ana@email.com"
│   ├── timestamp: 2025-11-16T10:35:00Z
│   └── position: 1  (segundo favorito)
└── doc3/
    ├── fromUserEmail: "juan@email.com"
    ├── toUserEmail: "laura@email.com"
    ├── timestamp: 2025-11-16T10:40:00Z
    └── position: 2  (tercer favorito)
```

---

## 🔄 Flujo de Funcionamiento

### Al Abrir la Pantalla:
1. Se cargan los favoritos del usuario (ordenados por position)
2. Se cargan todos los demás perfiles
3. Se crea una lista ordenada: favoritos primero, luego otros
4. Se guarda el conjunto de emails de favoritos en el ViewModel
5. Se muestra visualmente el estado del botón (rojo/gris)

### Al Agregar un Favorito:
1. Usuario hace click en la estrella
2. Se verifica que no haya más de 3 favoritos
3. Se guarda en Firebase con la posición correspondiente
4. El botón cambia a **rojo**
5. Se actualiza el ViewModel
6. Se muestra confirmación al usuario

### Al Eliminar un Favorito:
1. Usuario hace click nuevamente en la estrella (que está en rojo)
2. Se elimina de la colección `favorites` en Firebase
3. El botón cambia a **gris**
4. Se actualiza el ViewModel
5. Se muestra confirmación al usuario

### Búsqueda con Favoritos:
- Los favoritos siempre aparecen primero en los resultados de búsqueda
- Mantienen su orden
- El botón de estrella mantiene su estado (rojo si es favorito)

---

## 🎨 Estados Visuales del Botón

| Estado | Color | Significado |
|--------|-------|-------------|
| Favorito | 🔴 Rojo | Es un favorito del usuario |
| No Favorito | ⚫ Gris | No es favorito |

---

## 📝 Archivos Modificados

### 1. **ExploreViewModel.kt**
- ✅ Agregada variable: `favoriteEmails: Set<String>`
- Permite acceso rápido a los emails de favoritos

### 2. **ProfileAdapter.kt**
- ✅ Parámetro adicional: `viewModel: ExploreViewModel?`
- ✅ Método nuevo: `updateFavoriteButtonState()`
- ✅ Método nuevo: `toggleFavorite()`
- ✅ Actualización de estado en tiempo real

### 3. **ExploreFragment.kt**
- ✅ Método nuevo: `loadFavoriteProfiles()`
- ✅ Actualizado: `loadUserProfiles()`
- ✅ Actualizado: `displayProfiles()` para mostrar favoritos primero
- ✅ Paso del ViewModel al ProfileAdapter

---

## 📋 Restricciones y Validaciones

1. **Máximo de 3 favoritos**: Si el usuario intenta agregar un 4to, recibe mensaje de error
2. **Sin duplicados**: No se puede marcar el mismo perfil dos veces como favorito
3. **Persistencia**: Los favoritos se mantienen incluso si cierras y abres la app
4. **Independencia**: Los favoritos de un usuario NO afectan a otros usuarios

---

## 🧪 Testing Recomendado

### Agregar Favoritos:
```
✓ Click en estrella de perfil 1 → Color rojo
✓ Click en estrella de perfil 2 → Color rojo
✓ Click en estrella de perfil 3 → Color rojo
✓ Click en estrella de perfil 4 → Mensaje "Máximo de 3"
```

### Eliminar Favoritos:
```
✓ Click en estrella roja → Color gris
✓ Verificar que desaparece del top
```

### Orden de Visualización:
```
✓ Los 3 favoritos aparecen primero en el mismo orden que se seleccionaron
✓ El resto de perfiles después
✓ Al recargar, el orden se mantiene
```

### Búsqueda:
```
✓ Buscar por nombre → Los favoritos que coincidan aparecen primero
✓ El color del botón de estrella se muestra correctamente
```

### Persistencia:
```
✓ Marcar 3 favoritos
✓ Cerrar la app completamente
✓ Abrir nuevamente
✓ Los 3 favoritos siguen siendo favoritos (botón en rojo)
```

---

## 🔐 Seguridad

- Solo el usuario autenticado puede ver y modificar sus propios favoritos
- Los datos se guardan vinculados al `fromUserEmail` del usuario
- Se usa Firestore Security Rules (recomendado configurar)

---

## 🚀 Próximas Mejoras Sugeridas

1. Mostrar badge con número de favoritos (ej: "★ 2/3")
2. Reordenar favoritos mediante drag & drop
3. Mostrar una sección separada de "Tus Favoritos"
4. Notificar cuando un favorito está activo (ha hecho match)
5. Permitir exportar lista de favoritos


