# Corrección: Carga y Compresión de Imágenes en Historias

## Problemas Identificados

### 1. **Imágenes Demasiado Grandes** 
   - Error: `The value of property "image" is longer than 1048487 bytes`
   - Causa: Las imágenes en CreatePostFragment no se comprimían correctamente
   - Límite de Firestore: **1 MB por documento**

### 2. **Falta de Índices en Firestore**
   - Error: `FAILED_PRECONDITION: The query requires an index`
   - Causa: Consultas con `orderBy()` requieren índices compuestos
   - Solución: Eliminar `orderBy()` y ordenar localmente en la app

### 3. **Historias No Se Visualizan**
   - Causa: Las consultas fallaban por falta de índices, impidiendo cargar las historias

---

## Cambios Realizados

### 1. **CreatePostFragment.kt** - Mejora de Compresión de Imágenes

✅ **Función `bitmapToBase64()` mejorada:**
- Cambio de límite: **900 KB → 800 KB** (margen de seguridad)
- Calidad inicial: **80 → 70** (más agresiva desde el inicio)
- Reducción de calidad: **-10 → -5** (pasos más pequeños, mejor compresión)
- Escalado inicial: **0.9 → 0.8** (redimensionamiento más agresivo)
- Caso extremo: Nueva foto a 400x300 con calidad 15 como último recurso

✅ **Umbral de Firestore:**
- Antes: 900 KB
- Ahora: **700 KB** (máximo conservador considerando overhead del documento)

### 2. **ProfileFragment.kt** - Corrección de Consultas

✅ **`loadStories()`:**
- ❌ ANTES: `.whereEqualTo().orderBy().get()`
- ✅ AHORA: `.whereEqualTo().get()` + ordenamiento local por timestamp

✅ **`loadInitialPosts()` y `loadMorePosts()`:**
- ❌ ANTES: Usaban `postsQueryForCurrentUser()` con `orderBy()`
- ✅ AHORA: Consultas simples sin `orderBy`, ordenamiento local descendente por timestamp
- Removido método `postsQueryForCurrentUser()` innecesario

---

## Flujo Correcto de Guardar Historias

```
Usuario selecciona imágenes
         ↓
CreatePostFragment.publishStory()
         ↓
Para cada imagen:
  - Decodificar Uri → Bitmap
  - bitmapToBase64() → Comprimir automáticamente
    - Intenta calidad 70, 65, 60... si > 800KB
    - Escala a 0.8, 0.7, 0.6... si aún > 800KB
    - Último recurso: 400x300 con calidad 15
         ↓
Calcula totalBytes de todas las imágenes
         ↓
Si totalBytes ≤ 700KB:
  ✅ Guarda en collection "stories" directamente
  
Si totalBytes > 700KB:
  ✅ Crea documento en "stories" sin imágenes
  ✅ Guarda cada imagen en collection "storyImages"
  ✅ Referencia IDs en el documento "stories"
```

---

## Flujo Correcto de Cargar Historias

```
ProfileFragment.loadStories()
         ↓
Consulta: db.collection("stories")
          .whereEqualTo("userEmail", email)
          .get()
         ↓
Recibe documentos (SIN ordenar en Firestore)
         ↓
Ordena LOCALMENTE por timestamp DESC
         ↓
Para cada historia:
  - Si tiene images = [IDs]:
    Carga de collection "storyImages" por ID
  - Si tiene images = [Base64]:
    Decodifica directamente
         ↓
Renderiza en ViewPager2
```

---

## Próximos Pasos Recomendados

### ✅ Pruebas Locales
1. Compilar y ejecutar la aplicación
2. Crear una nueva historia con múltiples imágenes (5 imágenes)
3. Verificar en Firebase Console:
   - Tamaño de documentos en collection "stories"
   - Si documentos están en "storyImages" cuando es necesario
4. Volver a ProfileFragment y verificar que se cargan las historias

### 📋 Firebase Firestore - Limpieza Opcional
Si hay historias antiguas con imágenes demasiado grandes:
1. Ir a Firebase Console → Firestore Database
2. Collection "stories" → eliminar documentos problemáticos
3. Collection "storyImages" → eliminar referencias huérfanas

### 🔧 Configuración de Seguridad (Firebase Rules)
```
Asegurar que las reglas permitan:
- Lectura de "stories" donde userEmail == auth.token.email
- Lectura de "storyImages" donde el storyId existe
- Escritura solo del usuario autenticado
```

---

## Resumen de Cambios de Código

| Archivo | Cambio | Impacto |
|---------|--------|--------|
| CreatePostFragment.kt | Mejorar bitmapToBase64() | ✅ Imágenes siempre < 800KB |
| CreatePostFragment.kt | Cambiar umbral 900→700KB | ✅ Mayor seguridad |
| ProfileFragment.kt | Eliminar orderBy en historias | ✅ Sin error de índice |
| ProfileFragment.kt | Eliminar orderBy en posts | ✅ Sin error de índice |
| ProfileFragment.kt | Ordenamiento local | ✅ Historias ordenadas igual |

---

## Logs Esperados (Correcto)

```
D CreatePostFragment: Imagen comprimida a calidad 70, tamaño: 650000 bytes
D CreatePostFragment: Guardando historia en collection 'stories'
D ProfileFragment: Cargando historias para email: usuario@email.com
D ProfileFragment: Se encontraron 3 historias
D ProfileFragment: Renderizando historia con 3 imágenes
```

## Logs Anteriores (Error)

```
E Firestore: Stream closed with status: INVALID_ARGUMENT - image > 1048487 bytes
E ProfileFragment: Error cargando historias: FAILED_PRECONDITION - The query requires an index
```

---

## Notas Importantes

⚠️ **No eliminar**:
- `storyImages` collection (se usa para historias grandes)
- Código de carga de historias antiguas
- Funcionalidad de editar perfil (usa compresión correcta)

✅ **Verificar**:
- Que las imágenes en perfil se siguen cargando correctamente
- Que las nuevas historias aparecen inmediatamente
- Que no hay pérdida de calidad inaceptable en imágenes

