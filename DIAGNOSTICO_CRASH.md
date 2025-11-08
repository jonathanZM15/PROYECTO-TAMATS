# Guía de Diagnóstico - App Se Cierra Automáticamente

## Cambios Realizados para Prevenir Crashes

He agregado manejo de excepciones en:

1. **MyApplication.kt** - Ahora captura errores de inicialización de Firebase
2. **ProfileActivity.kt** - Protección contra elementos faltantes del layout
3. **EditProfileActivity.kt** - Manejo de errores en onCreate
4. **ViewProfileActivity.kt** - Manejo de errores en onCreate

## Causas Comunes del Crash

### 1. **Archivo google-services.json Faltante**
   - **Síntoma**: App se cierra inmediatamente al abrir
   - **Solución**: Asegúrate de tener el archivo `google-services.json` en la carpeta `app/`
   - **Ubicación correcta**: `C:\Users\CompuStore\Desktop\proyecto_moviles\app\google-services.json`

### 2. **Elementos del Layout Faltantes**
   - **Síntoma**: NullPointerException en ProfileActivity
   - **Solución**: Verifica que el archivo `activity_profile.xml` tenga los IDs:
     - `tvProfileName`
     - `tvProfileEmail`
     - `tvProfileBirthDate`
     - `tvProfileGender`
     - `tvWelcomeTitle`
     - `btnShowFirebaseData`

### 3. **Dependencias Faltantes**
   - **Síntoma**: ClassNotFoundException
   - **Solución**: Asegúrate de haber ejecutado `gradle sync` después de los cambios

### 4. **Problemas con la Base de Datos Room**
   - **Síntoma**: RuntimeException al inicializar la base de datos
   - **Solución**: Verifica que el `AppDatabase` se inicializa correctamente

## Cómo Ver los Logs de Error

### Opción 1: Android Studio
1. Abre Android Studio
2. Ve a View → Tool Windows → Logcat
3. Busca mensajes con `CRASH` o `Error`
4. Copia el stack trace completo

### Opción 2: Comando ADB (si tienes ADB instalado)
```bash
adb logcat | grep -i "error\|crash\|exception"
```

## Verificación de Archivos Necesarios

Asegúrate de que existen estos archivos:

✓ `google-services.json` - Debe estar en `app/`
✓ `activity_profile.xml` - Con todos los elementos necesarios
✓ `activity_edit_profile.xml` - Nuevo layout creado
✓ `activity_view_profile.xml` - Nuevo layout creado
✓ `file_paths.xml` - Para FileProvider
✓ `colors.xml` - Con `purple_700` agregado

## Pasos para Resolver el Crash

### Paso 1: Verificar google-services.json
Confirma que el archivo existe en:
```
proyecto_moviles/app/google-services.json
```

Si no existe, descárgalo desde:
- Firebase Console → Proyecto → Settings → Descargar google-services.json

### Paso 2: Limpiar y Reconstruir
```bash
./gradlew clean
./gradlew build
```

### Paso 3: Reinstalar la App
```bash
adb uninstall com.example.myapplication
adb install path/to/app-debug.apk
```

### Paso 4: Ver Logs
Ejecuta la app y observa los logs en Logcat para ver el error exacto.

## Debugging Avanzado

Si aún tienes problemas, puedes agregar un log inicial en SplashActivity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    android.util.Log.d("SplashActivity", "Splash iniciado correctamente")
    
    try {
        setContentView(R.layout.activity_splash)
        // ... resto del código
    } catch (e: Exception) {
        android.util.Log.e("SplashActivity", "Error: ${e.message}", e)
        e.printStackTrace()
    }
}
```

## Posibles Soluciones Rápidas

1. **Borra la carpeta `build/`** y reconstruye
2. **Invalida caché** en Android Studio: File → Invalidate Caches
3. **Actualiza todas las dependencias** en `libs.versions.toml`
4. **Verifica que compileSdk = 36** sea compatible con tu targetSdk

## Información que Necesito

Por favor, comparte:
1. El **stack trace completo** del error (del Logcat)
2. Confirma que **google-services.json** existe en `app/`
3. El **nivel de API** de tu dispositivo/emulador
4. La **versión de Android Studio** que usas

Con esta información podré proporcionar una solución más específica.

