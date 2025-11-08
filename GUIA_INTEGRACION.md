
### La foto no aparece en Firebase
- Verifica que la foto se haya capturado correctamente
- Comprueba que tienes permisos de WRITE_EXTERNAL_STORAGE
- Revisa que Firebase esté configurado correctamente

### Los datos no se guardan en Firebase
- Verifica que Firebase esté inicializado
- Comprueba las reglas de Firestore (permite lectura/escritura)
- Revisa los logs de Firebase para errores

### Problemas con permisos
- Los permisos se solicitan automáticamente en tiempo de ejecución
- En Android 12+, asegúrate de tener READ_MEDIA_IMAGES en lugar de READ_EXTERNAL_STORAGE

## Estructura del Proyecto

```
app/src/main/
├── java/.../ui/simulacion/
│   ├── EditProfileActivity.kt (NUEVO)
│   ├── ViewProfileActivity.kt (NUEVO)
│   ├── ProfileActivity.kt
│   └── ...
├── res/
│   ├── layout/
│   │   ├── activity_edit_profile.xml (NUEVO)
│   │   ├── activity_view_profile.xml (NUEVO)
│   │   └── ...
│   ├── drawable/
│   │   ├── edit_text_background.xml (NUEVO)
│   │   ├── button_rounded_red.xml (NUEVO)
│   │   └── ...
│   └── xml/
│       └── file_paths.xml (NUEVO)
└── AndroidManifest.xml (MODIFICADO)
```
# Guía de Integración - Edición de Perfil de Usuario

## Paso 1: Modificar LoginActivity para Redirigir a Edición de Perfil

Después de validar las credenciales del usuario por primera vez, redirigir a `EditProfileActivity`:

```kotlin
// En tu LoginActivity.kt, después de validación exitosa:

val intent = if (isFirstLogin) {
    // Primera vez que inicia sesión - ir a editar perfil
    Intent(this, EditProfileActivity::class.java).apply {
        putExtra("userEmail", email)
    }
} else {
    // Ya tiene perfil - ir a ver perfil
    Intent(this, ViewProfileActivity::class.java).apply {
        putExtra("userEmail", email)
    }
}
startActivity(intent)
```

## Paso 2: Opción de Menú en ProfileActivity

Si deseas agregar un botón en ProfileActivity para editar el perfil:

```kotlin
// En ProfileActivity.kt
val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)
btnEditProfile.setOnClickListener {
    val intent = Intent(this, EditProfileActivity::class.java)
    intent.putExtra("userEmail", userEmail) // obtén el email del usuario actual
    startActivity(intent)
}
```

## Paso 3: Verificar Sincronización con Firebase

La aplicación automáticamente:
1. Guarda la foto en Base64 en Firebase
2. Guarda la foto en la galería del dispositivo
3. Almacena todos los datos en la colección "userProfiles" de Firestore

## Paso 4: Cargar Perfil Existente en Edición

Si deseas permitir editar un perfil existente, añade este código a `EditProfileActivity.loadProfileData()`:

```kotlin
private fun loadProfileData() {
    FirebaseService.getUserProfile(userEmail) { profileData ->
        runOnUiThread {
            if (profileData != null) {
                etProfileName.setText(profileData["name"]?.toString() ?: "")
                etProfileAge.setText(profileData["age"]?.toString() ?: "")
                etProfileCity.setText(profileData["city"]?.toString() ?: "")
                etProfileDescription.setText(profileData["description"]?.toString() ?: "")
                
                // Cargar foto
                val photoBase64 = profileData["photo"]?.toString()
                if (!photoBase64.isNullOrEmpty()) {
                    val decodedString = Base64.decode(photoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    selectedBitmap = bitmap
                    ivProfilePhoto.setImageBitmap(bitmap)
                }
                
                // Cargar intereses
                @Suppress("UNCHECKED_CAST")
                val interests = profileData["interests"] as? List<String> ?: emptyList()
                setSelectedInterests(interests)
            }
        }
    }
}

private fun setSelectedInterests(interests: List<String>) {
    cbDeporte.isChecked = "Deporte" in interests
    cbLiteratura.isChecked = "Literatura" in interests
    cbMusica.isChecked = "Música" in interests
    cbViajar.isChecked = "Viajar" in interests
    cbFotografia.isChecked = "Fotografía" in interests
    cbArte.isChecked = "Arte" in interests
    cbCine.isChecked = "Cine" in interests
    cbCocina.isChecked = "Cocina" in interests
    cbYoga.isChecked = "Yoga" in interests
    cbTecnologia.isChecked = "Tecnología" in interests
    cbLectura.isChecked = "Lectura" in interests
    cbModa.isChecked = "Moda" in interests
}
```

## Paso 5: Flujo de Usuario Recomendado

### Primera vez que inicia sesión:
1. Usuario completa Login
2. Se redirige a EditProfileActivity
3. Usuario completa su perfil y hace click en "Guardar Perfil"
4. Se guarda en Firebase y se redirige a ViewProfileActivity
5. Usuario puede ver su perfil completado

### Inicia sesión nuevamente:
1. Usuario completa Login
2. Se redirige directamente a ViewProfileActivity
3. Usuario puede ver su perfil o editarlo

## Paso 6: Pruebas Recomendadas

```kotlin
// Prueba 1: Verificar que la foto se guarda en Firebase
fun testPhotoUpload() {
    FirebaseService.getUserProfile(userEmail) { profileData ->
        val photoBase64 = profileData?.get("photo")?.toString()
        assert(!photoBase64.isNullOrEmpty()) // Debe tener foto
    }
}

// Prueba 2: Verificar que los intereses se guardan
fun testInterestsSaved() {
    FirebaseService.getUserProfile(userEmail) { profileData ->
        @Suppress("UNCHECKED_CAST")
        val interests = profileData?.get("interests") as? List<String>
        assert(interests != null && interests.isNotEmpty())
    }
}
```

## Solución de Problemas

