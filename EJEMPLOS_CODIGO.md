# Ejemplos de Código para Integración

## 1. Modificar LoginActivity para Primera Vez

Ejemplo completo de cómo modificar tu LoginActivity para detectar si es la primera vez que el usuario inicia sesión:

```kotlin
package com.example.myapplication.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.example.myapplication.ui.simulacion.EditProfileActivity
import com.example.myapplication.ui.simulacion.ViewProfileActivity
import com.example.myapplication.util.EncryptionUtil

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar si el usuario existe
            FirebaseService.findUserByEmail(email) { user ->
                runOnUiThread {
                    if (user != null) {
                        // Verificar contraseña
                        val encryptedPassword = EncryptionUtil.encryptPassword(password)
                        if (encryptedPassword == user.passwordHash) {
                            // Contraseña correcta - verificar si tiene perfil
                            checkAndNavigateToProfile(email)
                        } else {
                            Toast.makeText(this, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkAndNavigateToProfile(email: String) {
        // Verificar si el usuario ya tiene un perfil completado
        FirebaseService.getUserProfile(email) { profileData ->
            runOnUiThread {
                val intent = if (profileData != null) {
                    // Ya tiene perfil - ir a verlo
                    Intent(this, ViewProfileActivity::class.java)
                } else {
                    // Primera vez - ir a editar/completar perfil
                    Intent(this, EditProfileActivity::class.java)
                }
                intent.putExtra("userEmail", email)
                startActivity(intent)
                finish() // Cerrar LoginActivity
            }
        }
    }
}
```

## 2. Agregar Botón en Explorar para Ir al Perfil

Si tienes una sección de exploración, puedes agregar un botón para ir al perfil:

```kotlin
// En tu ExploreActivity o similar
val btnProfile = findViewById<ImageView>(R.id.btnProfile)
btnProfile.setOnClickListener {
    // Obtener el email del usuario actual (guárdalo en SharedPreferences o Firebase Auth)
    val userEmail = getCurrentUserEmail() // Tu método para obtener el email actual
    
    val intent = Intent(this, ViewProfileActivity::class.java)
    intent.putExtra("userEmail", userEmail)
    startActivity(intent)
}

private fun getCurrentUserEmail(): String {
    // Ejemplo usando SharedPreferences
    val preferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
    return preferences.getString("user_email", "") ?: ""
}
```

## 3. Guardar Email del Usuario Actual

Es recomendable guardar el email del usuario actual en SharedPreferences cuando inicia sesión:

```kotlin
// En LoginActivity, después de validación exitosa:
private fun saveUserEmailLocally(email: String) {
    val preferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
    preferences.edit().putString("user_email", email).apply()
}

// Luego, en cualquier Activity, puedes recuperarlo:
private fun getCurrentUserEmail(): String {
    val preferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
    return preferences.getString("user_email", "") ?: ""
}
```

## 4. Agregar Método de Logout

En ViewProfileActivity, cuando el usuario cierra sesión, limpiar los datos:

```kotlin
// En ViewProfileActivity.kt
private fun logout() {
    // Limpiar email guardado
    val preferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
    preferences.edit().clear().apply()
    
    // Volver a LoginActivity
    val intent = Intent(this, LoginActivity::class.java)
    startActivity(intent)
    finish()
}
```

## 5. Agregar Validación de Foto Obligatoria (Opcional)

Si deseas hacer la foto obligatoria:

```kotlin
// En EditProfileActivity.kt, modificar saveProfile():
private fun saveProfile() {
    val name = etProfileName.text.toString().trim()
    val age = etProfileAge.text.toString().trim()
    val city = etProfileCity.text.toString().trim()
    val description = etProfileDescription.text.toString().trim()

    // Validar campos obligatorios
    if (name.isEmpty()) {
        Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show()
        return
    }

    if (age.isEmpty()) {
        Toast.makeText(this, "Por favor ingresa tu edad", Toast.LENGTH_SHORT).show()
        return
    }

    if (city.isEmpty()) {
        Toast.makeText(this, "Por favor ingresa tu ciudad", Toast.LENGTH_SHORT).show()
        return
    }

    // NUEVO: Validar que tenga foto
    if (selectedBitmap == null) {
        Toast.makeText(this, "Por favor sube una foto de perfil", Toast.LENGTH_SHORT).show()
        return
    }

    // ... resto del código ...
}
```

## 6. Agregar Indicador de Carga

Para mejorar la UX mientras se guarda en Firebase:

```kotlin
// En EditProfileActivity.kt
private var isLoading = false
private lateinit var progressBar: ProgressBar

private fun saveProfile() {
    // ... validaciones ...

    // Mostrar progreso
    isLoading = true
    progressBar.visibility = View.VISIBLE
    btnSaveProfile.isEnabled = false

    // ... código de guardado ...
}

private fun saveProfileToFirebase(profileData: Map<String, Any>) {
    FirebaseService.saveUserProfile(userEmail, profileData) { success ->
        runOnUiThread {
            isLoading = false
            progressBar.visibility = View.GONE
            btnSaveProfile.isEnabled = true

            if (success) {
                Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar el perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

## 7. Agregar ProgressBar al Layout

Añade esto a `activity_edit_profile.xml` dentro del ScrollView:

```xml
<ProgressBar
    android:id="@+id/progressBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="center"
    android:visibility="gone"
    android:layout_margin="16dp" />
```

## 8. Manejo de Errores Mejorado

Para mejor manejo de errores en Firebase:

```kotlin
// En EditProfileActivity.kt
private fun saveProfileToFirebase(profileData: Map<String, Any>) {
    if (userEmail.isEmpty()) {
        Toast.makeText(this, "Error: email no disponible", Toast.LENGTH_SHORT).show()
        return
    }

    FirebaseService.saveUserProfile(userEmail, profileData) { success ->
        runOnUiThread {
            if (success) {
                Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()
                // Navegar a ViewProfileActivity
                val intent = Intent(this, ViewProfileActivity::class.java)
                intent.putExtra("userEmail", userEmail)
                startActivity(intent)
                finish()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("No se pudo guardar el perfil. Verifica tu conexión a internet e intenta de nuevo.")
                    .setPositiveButton("Reintentar") { _, _ ->
                        saveProfile()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }
}
```

## 9. Agregar Atajo en Menú de Navegación

Si usas un menú de navegación inferior (BottomNavigationView), añade un item para Perfil:

```xml
<!-- res/menu/bottom_nav_menu.xml -->
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/navigation_explore"
        android:icon="@drawable/ic_explore"
        android:title="Explorar" />
    <item
        android:id="@+id/navigation_likes"
        android:icon="@drawable/ic_heart_outline"
        android:title="Matches" />
    <item
        android:id="@+id/navigation_chats"
        android:icon="@drawable/ic_chat"
        android:title="Chats" />
    <item
        android:id="@+id/navigation_profile"
        android:icon="@drawable/ic_person"
        android:title="Perfil" />
</menu>
```

```kotlin
// En tu Activity principal
bottomNavView.setOnItemSelectedListener { menuItem ->
    when (menuItem.itemId) {
        R.id.navigation_profile -> {
            val userEmail = getCurrentUserEmail()
            val intent = Intent(this, ViewProfileActivity::class.java)
            intent.putExtra("userEmail", userEmail)
            startActivity(intent)
            true
        }
        // ... otros items ...
        else -> false
    }
}
```

## 10. Sincronizar con Firebase Authentication (Opcional)

Si quieres usar Firebase Authentication en lugar de tu sistema de login actual:

```kotlin
// En EditProfileActivity.kt, al inicio de onCreate:
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_edit_profile)

    // Obtener usuario de Firebase Auth
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        userEmail = currentUser.email ?: intent.getStringExtra("userEmail") ?: ""
    } else {
        userEmail = intent.getStringExtra("userEmail") ?: ""
    }

    initializeViews()
    setupListeners()
    loadProfileData()
}
```

Estos son los principales ejemplos de integración que necesitas para completar la funcionalidad de edición de perfil en tu aplicación.

