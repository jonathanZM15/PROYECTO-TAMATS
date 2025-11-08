# Funcionalidad de Edición de Perfil de Usuario

## Descripción

Se ha agregado una completa funcionalidad de edición de perfil de usuario con las siguientes características:

### Características Implementadas

1. **Edición de Perfil (EditProfileActivity)**
   - Nombre de usuario (editable)
   - Edad (campo numérico)
   - Ciudad (editable)
   - Descripción personal (opcional, multilinea)
   - Foto de perfil (subida desde galería o capturada con cámara)

2. **Gestión de Fotos**
   - Opción de tomar foto con cámara
   - Opción de seleccionar foto desde galería
   - Las fotos se guardan en:
     - Galería del dispositivo
     - Firebase (codificadas en Base64)

3. **Selección de Intereses**
   - Deporte
   - Literatura
   - Música
   - Viajar
   - Fotografía
   - Arte
   - Cine
   - Cocina
   - Yoga
   - Tecnología
   - Lectura
   - Moda
   
   Los intereses se almacenan como una lista en Firebase.

4. **Almacenamiento de Datos**
   - Todos los datos se guardan en Firebase Firestore
   - Las fotos se codifican en Base64 para almacenamiento en Firebase
   - Los perfiles se pueden editar en cualquier momento

5. **Visualización de Perfil (ViewProfileActivity)**
   - Ver todos los datos del perfil completado
   - Ver foto de perfil
   - Ver intereses seleccionados
   - Opción para editar perfil
   - Opción para cerrar sesión

## Archivos Creados/Modificados

### Layouts XML
- `activity_edit_profile.xml` - Interfaz de edición de perfil
- `activity_view_profile.xml` - Interfaz de visualización de perfil
- `edit_text_background.xml` - Estilo para campos de entrada
- `button_rounded_red.xml` - Estilo para botón de cámara
- `file_paths.xml` - Configuración de FileProvider

### Activities Kotlin
- `EditProfileActivity.kt` - Lógica de edición de perfil
- `ViewProfileActivity.kt` - Lógica de visualización de perfil

### Modificaciones
- `FirebaseService.kt` - Agregados métodos:
  - `saveUserProfile()` - Guardar/actualizar perfil en Firebase
  - `getUserProfile()` - Obtener perfil del usuario
- `AndroidManifest.xml` - Agregados permisos y activities

## Permisos Requeridos

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## Cómo Usar

### Acceder desde LoginActivity
```kotlin
val intent = Intent(this, EditProfileActivity::class.java)
intent.putExtra("userEmail", userEmail)
startActivity(intent)
```

### Guardar Perfil
El botón "Guardar Perfil" valida que los campos obligatorios estén completos y guarda todo en Firebase.

### Ver Perfil
Acceder a `ViewProfileActivity` para ver el perfil completado del usuario.

### Editar Perfil
Desde la pantalla de visualización, hacer click en "Editar Perfil" para volver a la pantalla de edición.

## Estructura de Datos en Firebase

```json
{
  "userProfiles": {
    "email@example.com": {
      "name": "María González",
      "age": 28,
      "city": "Madrid",
      "description": "Amante del café...",
      "interests": ["Deporte", "Fotografía", "Viajar"],
      "photo": "base64_encoded_image_data",
      "email": "email@example.com",
      "lastUpdated": 1704067200000
    }
  }
}
```

## Validaciones

- **Nombre**: Requerido
- **Edad**: Requerido, numérico
- **Ciudad**: Requerido
- **Descripción**: Opcional
- **Foto**: Opcional
- **Intereses**: Opcional (al menos uno recomendado)

## Notas Técnicas

- Las fotos se comprimen a JPEG con calidad 80 antes de codificar a Base64
- Se utilizan Activity Result APIs (registerForActivityResult) en lugar de startActivityForResult
- Se implementa manejo de permisos en tiempo de ejecución (Runtime Permissions)
- El FileProvider se utiliza para acceso seguro a archivos de cámara
- Las imágenes se guardan automáticamente en la galería del dispositivo

