package com.example.myapplication.ui.simulacion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var etProfileName: EditText
    private lateinit var etProfileAge: EditText
    private lateinit var etProfileCity: EditText
    private lateinit var etProfileDescription: EditText
    private lateinit var btnChangePhoto: ImageButton
    private lateinit var btnSaveProfile: MaterialButton

    // CheckBoxes de intereses
    private lateinit var cbDeporte: CheckBox
    private lateinit var cbLiteratura: CheckBox
    private lateinit var cbMusica: CheckBox
    private lateinit var cbViajar: CheckBox
    private lateinit var cbFotografia: CheckBox
    private lateinit var cbArte: CheckBox
    private lateinit var cbCine: CheckBox
    private lateinit var cbCocina: CheckBox
    private lateinit var cbYoga: CheckBox
    private lateinit var cbTecnologia: CheckBox
    private lateinit var cbLectura: CheckBox
    private lateinit var cbModa: CheckBox

    private var currentPhotoPath: String? = null
    private var selectedBitmap: Bitmap? = null
    private var userEmail: String = ""

    // Registrar resultado de la cámara
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath != null) {
                // Decodificar la imagen en background para evitar bloquear la UI
                val path = currentPhotoPath
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(path)
                        if (bmp != null) {
                            selectedBitmap = bmp
                            withContext(Dispatchers.Main) {
                                ivProfilePhoto.setImageBitmap(bmp)
                            }
                            // Guardar en galería (no bloqueante)
                            savePhotoToGallery(bmp)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EditProfileActivity, "Error al procesar la foto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditProfileActivity, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
             }
         }

    // Registrar resultado de la galería
    private val pickPictureLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // Decodificar en background para evitar bloquear la UI
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val input = contentResolver.openInputStream(uri)
                        val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        input?.close()
                        if (bmp != null) {
                            selectedBitmap = bmp
                            withContext(Dispatchers.Main) {
                                ivProfilePhoto.setImageBitmap(bmp)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EditProfileActivity, "Error al procesar la imagen seleccionada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditProfileActivity, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
             }
         }

    // Registrar permisos
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchGallery()
            } else {
                Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
            }
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_edit_profile)

            // Obtener el email del intent
            userEmail = intent.getStringExtra("userEmail") ?: ""

            initializeViews()
            setupListeners()
            loadProfileData()

            // Asegurar que el contenedor del botón Guardar quede por encima del FAB
            val saveContainer = findViewById<LinearLayout>(R.id.saveButtonContainer)
            saveContainer?.bringToFront()

            // Evitar que el teclado oculte el contenido
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            android.util.Log.e("EditProfileActivity", "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al cargar el perfil: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        etProfileName = findViewById(R.id.etProfileName)
        etProfileAge = findViewById(R.id.etProfileAge)
        etProfileCity = findViewById(R.id.etProfileCity)
        etProfileDescription = findViewById(R.id.etProfileDescription)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // Inicializar checkboxes
        cbDeporte = findViewById(R.id.cbDeporte)
        cbLiteratura = findViewById(R.id.cbLiteratura)
        cbMusica = findViewById(R.id.cbMusica)
        cbViajar = findViewById(R.id.cbViajar)
        cbFotografia = findViewById(R.id.cbFotografia)
        cbArte = findViewById(R.id.cbArte)
        cbCine = findViewById(R.id.cbCine)
        cbCocina = findViewById(R.id.cbCocina)
        cbYoga = findViewById(R.id.cbYoga)
        cbTecnologia = findViewById(R.id.cbTecnologia)
        cbLectura = findViewById(R.id.cbLectura)
        cbModa = findViewById(R.id.cbModa)
    }

    private fun setupListeners() {
        btnChangePhoto.setOnClickListener {
            showPhotoOptions()
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Botón central '+' en la barra inferior (si está incluido en el layout)
        try {
            val fabCenter = findViewById<com.google.android.material.button.MaterialButton>(R.id.fabCenter)
            fabCenter?.setOnClickListener {
                try {
                    val intent = Intent(this, com.example.myapplication.ui.explore.CreatePostActivity::class.java)
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this, "Crear publicación no disponible", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            // No hacer nada si no existe el FAB en este layout
        }

        // Intent directo para el item Explorar del bottom nav (si está incluido)
        try {
            val navExplorar = findViewById<LinearLayout>(R.id.navExplorar)
            navExplorar?.setOnClickListener {
                startActivity(Intent(this, com.example.myapplication.ui.explore.ExploreActivity::class.java))
            }
        } catch (_: Exception) {
            // ignore if layout doesn't include bottom nav
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> requestGalleryPermission()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchGallery()
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al acceder a la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGallery() {
        pickPictureLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(null)
        val image = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun savePhotoToGallery(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "Profile_$timeStamp.jpg"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${getString(R.string.app_name)}")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = contentResolver
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { out ->
                        out?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } else {
                // Fallback antiguo (deprecated) pero funcional en versiones antiguas
                MediaStore.Images.Media.insertImage(contentResolver, bitmap, filename, "Foto de perfil")
            }
        } catch (e: Exception) {
            android.util.Log.w("EditProfileActivity", "No se pudo guardar foto en galería: ${e.message}")
        }
    }

    private fun loadProfileData() {
        android.util.Log.d("EditProfileActivity", "Iniciando carga de datos para email: $userEmail")

        if (userEmail.isEmpty()) {
            android.util.Log.e("EditProfileActivity", "Email vacío, no se pueden cargar datos")
            return
        }

        // Consultar Firebase
        FirebaseService.getUserProfile(userEmail) { profileData ->
            android.util.Log.d("EditProfileActivity", "Datos recibidos de Firebase: ${profileData?.size} campos")

            // Usar un pequeño delay para asegurar que la UI esté lista
            runOnUiThread {
                try {
                    if (profileData != null && profileData.isNotEmpty()) {
                        android.util.Log.d("EditProfileActivity", "Autocarga: Llenando campos...")

                        // Cargar nombre
                        val name = profileData["name"]?.toString() ?: ""
                        android.util.Log.d("EditProfileActivity", "Nombre en Firebase: '$name'")
                        if (name.isNotEmpty()) {
                            etProfileName.setText(name)
                            android.util.Log.d("EditProfileActivity", "✓ Nombre cargado en UI: $name")
                        }

                        // Cargar edad
                        val age = profileData["age"]?.toString() ?: ""
                        android.util.Log.d("EditProfileActivity", "Edad en Firebase: '$age'")
                        if (age.isNotEmpty()) {
                            etProfileAge.setText(age)
                            android.util.Log.d("EditProfileActivity", "✓ Edad cargada en UI: $age")
                        }

                        // Cargar ciudad
                        val city = profileData["city"]?.toString() ?: ""
                        android.util.Log.d("EditProfileActivity", "Ciudad en Firebase: '$city'")
                        if (city.isNotEmpty()) {
                            etProfileCity.setText(city)
                            android.util.Log.d("EditProfileActivity", "✓ Ciudad cargada en UI: $city")
                        }

                        // Cargar descripción
                        val description = profileData["description"]?.toString() ?: ""
                        android.util.Log.d("EditProfileActivity", "Descripción en Firebase: '$description'")
                        if (description.isNotEmpty()) {
                            etProfileDescription.setText(description)
                            android.util.Log.d("EditProfileActivity", "✓ Descripción cargada en UI")
                        }

                        // Cargar foto
                        val photoBase64 = profileData["photo"]?.toString()
                        android.util.Log.d("EditProfileActivity", "Foto en Firebase: ${if (photoBase64.isNullOrEmpty()) "NO" else "SÍ"}")
                        if (!photoBase64.isNullOrEmpty()) {
                            try {
                                val decodedString = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                if (bitmap != null) {
                                    selectedBitmap = bitmap
                                    ivProfilePhoto.setImageBitmap(bitmap)
                                    android.util.Log.d("EditProfileActivity", "✓ Foto cargada correctamente")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("EditProfileActivity", "Error decodificando foto: ${e.message}", e)
                            }
                        }

                        // Cargar intereses
                        @Suppress("UNCHECKED_CAST")
                        val interests = profileData["interests"] as? List<String> ?: emptyList()
                        android.util.Log.d("EditProfileActivity", "Intereses en Firebase: $interests")
                        if (interests.isNotEmpty()) {
                            setSelectedInterests(interests)
                            android.util.Log.d("EditProfileActivity", "✓ Intereses cargados: $interests")
                        }

                        android.util.Log.d("EditProfileActivity", "✓✓✓ AUTOCARGA COMPLETADA EXITOSAMENTE ✓✓✓")
                    } else {
                        android.util.Log.d("EditProfileActivity", "⚠ profileData es null o vacío - Usuario SIN perfil previo")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProfileActivity", "❌ Error cargando datos: ${e.message}", e)
                    e.printStackTrace()
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

        // Obtener intereses seleccionados
        val interests = getSelectedInterests()

        // Convertir foto a Base64
        val photoBase64 = if (selectedBitmap != null) {
            bitmapToBase64(selectedBitmap!!)
        } else {
            ""
        }

        // Determinar email a usar (preferir userEmail de la Activity, sino el guardado local)
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val emailToUse = if (userEmail.isNotBlank()) userEmail else (prefs.getString("user_email", "") ?: "")

        // Crear objeto de perfil (usar emailToUse para no pasar un email vacío al servidor)
        val profileData: Map<String, Any> = mapOf(
            "name" to name,
            "age" to (age.toIntOrNull() ?: 0),
            "city" to city,
            "description" to description,
            "interests" to interests,
            "photo" to photoBase64,
            "email" to emailToUse,
            "lastUpdated" to (System.currentTimeMillis() as Any)
        )

        // Guardar en Firebase (pasando el email utilizado)
        saveProfileToFirebase(profileData, emailToUse)
    }

    private fun getSelectedInterests(): List<String> {
        val interests = mutableListOf<String>()

        if (cbDeporte.isChecked) interests.add("Deporte")
        if (cbLiteratura.isChecked) interests.add("Literatura")
        if (cbMusica.isChecked) interests.add("Música")
        if (cbViajar.isChecked) interests.add("Viajar")
        if (cbFotografia.isChecked) interests.add("Fotografía")
        if (cbArte.isChecked) interests.add("Arte")
        if (cbCine.isChecked) interests.add("Cine")
        if (cbCocina.isChecked) interests.add("Cocina")
        if (cbYoga.isChecked) interests.add("Yoga")
        if (cbTecnologia.isChecked) interests.add("Tecnología")
        if (cbLectura.isChecked) interests.add("Lectura")
        if (cbModa.isChecked) interests.add("Moda")

        return interests
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveProfileToFirebase(profileData: Map<String, Any>, email: String) {
        FirebaseService.saveUserProfile(email, profileData) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()
                    // Guardar datos localmente para uso inmediato en otras pantallas
                    try {
                        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                        prefs.edit(commit = true) {
                            if (email.isNotBlank()) putString("user_email", email)
                            val name = profileData["name"]?.toString() ?: ""
                            val photo = profileData["photo"]?.toString() ?: ""
                            if (name.isNotEmpty()) putString("user_name", name)
                            if (photo.isNotEmpty()) putString("user_photo", photo)
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                     // Navegar a ViewProfileActivity para mostrar el perfil completado
                     val intent = Intent(this, ViewProfileActivity::class.java)
                     if (email.isNotBlank()) intent.putExtra("userEmail", email)
                     startActivity(intent)
                     finish()
                 } else {
                     Toast.makeText(this, "Error al guardar el perfil", Toast.LENGTH_SHORT).show()
                 }
             }
         }
     }

    override fun onResume() {
        super.onResume()
        // Asegurar que el contenedor del botón Guardar esté al frente después de layout
        val saveContainer = findViewById<LinearLayout>(R.id.saveButtonContainer)
        saveContainer?.bringToFront()
        saveContainer?.requestLayout()
        saveContainer?.invalidate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val saveContainer = findViewById<LinearLayout>(R.id.saveButtonContainer)
            saveContainer?.bringToFront()
            // aseguremos que la elevación sea grande
            saveContainer?.translationZ = 200f
            saveContainer?.invalidate()
        }
    }
}
