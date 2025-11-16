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
    private lateinit var tvWordCount: TextView
    private lateinit var btnChangePhoto: ImageButton
    private lateinit var btnSaveProfile: MaterialButton

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

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath != null) {
                val path = currentPhotoPath
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(path)
                        if (bmp != null) {
                            selectedBitmap = bmp
                            withContext(Dispatchers.Main) {
                                ivProfilePhoto.setImageBitmap(bmp)
                            }
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

    private val pickPictureLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
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
                                Toast.makeText(this@EditProfileActivity, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        userEmail = intent.getStringExtra("userEmail") ?: ""

        initializeViews()
        setupListeners()
        loadProfileData()

        val saveContainer = findViewById<LinearLayout>(R.id.saveButtonContainer)
        saveContainer?.bringToFront()

        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun initializeViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        etProfileName = findViewById(R.id.etProfileName)
        etProfileAge = findViewById(R.id.etProfileAge)
        etProfileCity = findViewById(R.id.etProfileCity)
        etProfileDescription = findViewById(R.id.etProfileDescription)
        tvWordCount = findViewById(R.id.tvWordCount)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

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
        btnChangePhoto.setOnClickListener { showPhotoOptions() }
        btnSaveProfile.setOnClickListener { saveProfile() }

        etProfileDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateWordCount()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        try {
            val fabCenter = findViewById<MaterialButton>(R.id.fabCenter)
            fabCenter?.setOnClickListener {
                val intent = Intent(this, com.example.myapplication.MainActivity::class.java)
                intent.putExtra("fragment", "create_post")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        } catch (_: Exception) {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
            val photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                MediaStore.Images.Media.insertImage(contentResolver, bitmap, filename, "Foto de perfil")
            }
        } catch (e: Exception) {
            android.util.Log.w("EditProfileActivity", "Error guardando foto: ${e.message}")
        }
    }

    private fun loadProfileData() {
        if (userEmail.isEmpty()) {
            val prefs = getSharedPreferences("user_data", 0)
            userEmail = prefs.getString("user_email", "") ?: ""
        }
        if (userEmail.isEmpty()) return

        FirebaseService.getUserProfile(userEmail) { profileData ->
            runOnUiThread {
                try {
                    if (profileData != null && profileData.isNotEmpty()) {
                        profileData["name"]?.toString()?.let { if (it.isNotEmpty()) etProfileName.setText(it) }
                        profileData["age"]?.toString()?.let { if (it.isNotEmpty()) etProfileAge.setText(it) }
                        profileData["city"]?.toString()?.let { if (it.isNotEmpty()) etProfileCity.setText(it) }
                        profileData["description"]?.toString()?.let {
                            if (it.isNotEmpty()) {
                                etProfileDescription.setText(it)
                                updateWordCount()
                            }
                        }

                        profileData["photo"]?.toString()?.let { photoBase64 ->
                            if (photoBase64.isNotEmpty()) {
                                try {
                                    val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    if (bitmap != null) {
                                        selectedBitmap = bitmap
                                        ivProfilePhoto.setImageBitmap(bitmap)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("EditProfileActivity", "Error decodificando foto: ${e.message}")
                                }
                            }
                        }

                        @Suppress("UNCHECKED_CAST")
                        val interests = profileData["interests"] as? List<String> ?: emptyList()
                        if (interests.isNotEmpty()) setSelectedInterests(interests)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProfileActivity", "Error cargando perfil: ${e.message}")
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
        if (selectedBitmap == null) {
            Toast.makeText(this, "Por favor selecciona una foto de perfil", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
            return
        }

        val wordCount = description.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        if (wordCount > 30) {
            Toast.makeText(this, "La descripción no puede exceder 30 palabras. Actualmente tiene $wordCount palabras", Toast.LENGTH_SHORT).show()
            return
        }

        val interests = getSelectedInterests()
        val photoBase64 = bitmapToBase64(selectedBitmap!!)
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val emailToUse = if (userEmail.isNotBlank()) userEmail else (prefs.getString("user_email", "") ?: "")

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
        // Usar calidad 60 para reducir significativamente el tamaño del archivo
        // Esto evita el error TransactionTooLargeException
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun updateWordCount() {
        val description = etProfileDescription.text.toString().trim()
        val wordCount = if (description.isEmpty()) {
            0
        } else {
            description.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        }

        val color = if (wordCount > 30) {
            android.graphics.Color.RED
        } else if (wordCount >= 25) {
            android.graphics.Color.parseColor("#FF9800")
        } else {
            android.graphics.Color.DKGRAY
        }

        tvWordCount.setTextColor(color)
        tvWordCount.text = "$wordCount/30 palabras"
    }

    private fun saveProfileToFirebase(profileData: Map<String, Any>, email: String) {
        FirebaseService.saveUserProfile(email, profileData) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()
                    try {
                        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
                        prefs.edit(commit = true) {
                            if (email.isNotBlank()) putString("user_email", email)
                            val name = profileData["name"]?.toString() ?: ""
                            if (name.isNotEmpty()) putString("user_name", name)
                            // NO guardar la foto Base64 en SharedPreferences para evitar TransactionTooLargeException
                            // La foto se cargará desde Firebase cuando sea necesaria
                        }
                    } catch (_: Exception) {
                    }
                    val intent = Intent(this, com.example.myapplication.MainActivity::class.java)
                    intent.putExtra("fragment", "profile")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
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
        val saveContainer = findViewById<LinearLayout>(R.id.saveButtonContainer)
        saveContainer?.bringToFront()
    }
}
