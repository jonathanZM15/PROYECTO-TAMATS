package com.example.myapplication.ui.explore

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager

/**
 * Activity para crear una publicación con texto (máx 1000 caracteres) y hasta 5 fotos.
 * Las imágenes se guardan en Base64 directamente en Firestore.
 */
class CreatePostActivity : AppCompatActivity() {

    private lateinit var etPostText: EditText
    private lateinit var rvPhotos: RecyclerView
    private lateinit var btnPublish: Button

    // UI para avatar y nombre del header
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView

    // Launchers
    private lateinit var pickImagesLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var pendingPermissionAction: Int = 0

    // Lista local de Uris seleccionadas
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: PhotoGridAdapter

    private val maxPhotos = 5
    private val mainScope = MainScope()

    private var pendingAddPosition: Int = -1
    private var cameraTempUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        // Registrar launcher de permisos
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                when (pendingPermissionAction) {
                    1 -> launchCamera()
                    2 -> pickImagesLauncher.launch("image/*")
                }
            } else {
                Toast.makeText(this, "Permisos necesarios para completar la acción", Toast.LENGTH_SHORT).show()
            }
            pendingPermissionAction = 0
        }

        etPostText = findViewById(R.id.etPostText)
        try {
            val currentName = getCurrentUserName()
            if (currentName.isNotEmpty()) {
                etPostText.hint = "¿Qué estás pensando, $currentName?"
            } else {
                etPostText.setHint(R.string.create_post_hint)
            }
        } catch (_: Exception) {
            etPostText.setHint(R.string.create_post_hint)
        }

        rvPhotos = findViewById(R.id.rvPhotos)
        btnPublish = findViewById(R.id.btnPublish)

        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)

        loadProfileFromPrefs()
        loadCurrentUserProfile()

        adapter = PhotoGridAdapter(
            maxPhotos = maxPhotos,
            onAddClick = { pos -> showAddOptions(pos) },
            onRemoveClick = { pos ->
                if (pos in selectedUris.indices) {
                    selectedUris.removeAt(pos)
                    rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                }
            }
        )

        val gm = GridLayoutManager(this, 3)
        gm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return try {
                    val itemCount = adapter.itemCount
                    if (position < 0 || position >= itemCount) return 1
                    // Si hay espacio para el botón de añadir y la posición es la del botón, ocupar las 3 columnas
                    if (adapter.currentList.size < maxPhotos && position == adapter.currentList.size) return 3
                    1
                } catch (_: Exception) {
                    // En caso de cualquier inconsistencia, usar span 1 para seguir seguro
                    1
                }
            }
        }
        rvPhotos.layoutManager = gm
        rvPhotos.adapter = adapter
        // Desactivar animaciones para evitar inconsistencias durante actualizaciones rápidas
        rvPhotos.itemAnimator = null
        adapter.submitList(selectedUris.toList())

        val spacingPx = (12 * resources.displayMetrics.density).toInt()
        rvPhotos.addItemDecoration(GridSpacingItemDecoration(3, spacingPx, true))

        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                if (uris.size > maxPhotos) {
                    Toast.makeText(this, "Has seleccionado más de $maxPhotos fotos. Se añadirán las primeras $maxPhotos.", Toast.LENGTH_LONG).show()
                }
                val spaceLeft = maxPhotos - selectedUris.size
                var toAdd = uris
                if (uris.size > spaceLeft) {
                    toAdd = uris.take(spaceLeft)
                    Toast.makeText(this, "Solo se añadirán $spaceLeft fotos adicionales (máximo $maxPhotos).", Toast.LENGTH_SHORT).show()
                }

                val oldSize = selectedUris.size
                if (pendingAddPosition in 0 until maxPhotos) {
                    var insertIndex = pendingAddPosition
                    var added = 0
                    for (u in toAdd) {
                        if (selectedUris.size < maxPhotos) {
                            if (insertIndex <= selectedUris.size) {
                                selectedUris.add(insertIndex, u)
                            } else {
                                selectedUris.add(u)
                            }
                            insertIndex++
                            added++
                        }
                    }
                    pendingAddPosition = -1
                    if (added > 0) {
                        rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                    }
                } else {
                    selectedUris.addAll(toAdd)
                    val added = selectedUris.size - oldSize
                    if (added > 0) {
                        rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                    }
                }
            } else {
                Toast.makeText(this, "Debes seleccionar al menos una foto", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success && cameraTempUri != null) {
                if (pendingAddPosition in 0 until maxPhotos) {
                    val idx = pendingAddPosition
                    if (idx <= selectedUris.size) {
                        selectedUris.add(idx, cameraTempUri!!)
                    } else {
                        selectedUris.add(cameraTempUri!!)
                    }
                } else {
                    selectedUris.add(cameraTempUri!!)
                }
                cameraTempUri = null
                pendingAddPosition = -1
                rvPhotos.post { adapter.submitList(selectedUris.toList()) }
            } else {
                cameraTempUri = null
                pendingAddPosition = -1
            }
        }

        btnPublish.setOnClickListener {
            publishPost()
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val emailToUse = prefs.getString("user_email", null)

        if (emailToUse.isNullOrEmpty()) {
            if (tvUserName.text.isNullOrEmpty()) tvUserName.text = getString(R.string.user_name_demo)
            return
        }

        FirebaseService.getUserProfile(emailToUse) { profileData ->
            runOnUiThread {
                if (profileData != null) {
                    val name = profileData["name"]?.toString()
                    if (!name.isNullOrEmpty()) tvUserName.text = name

                    val photoBase64 = profileData["photo"]?.toString()
                    if (!photoBase64.isNullOrEmpty()) {
                        try {
                            val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            if (bmp != null) Glide.with(this@CreatePostActivity).load(bmp).circleCrop().into(ivUserAvatar)
                        } catch (_: Exception) { }
                    }

                    try {
                        prefs.edit().apply {
                            if (!name.isNullOrEmpty()) putString("user_name", name)
                            if (!photoBase64.isNullOrEmpty()) putString("user_photo", photoBase64)
                            putString("user_email", emailToUse)
                            commit()
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun setupBottomNav() {
        val ivExplore = findViewById<ImageView>(R.id.ivNavExplore)
        val tvExplore = findViewById<TextView>(R.id.tvNavExplore)
        val ivMatches = findViewById<ImageView>(R.id.ivNavMatches)
        val tvMatches = findViewById<TextView>(R.id.tvNavMatches)
        val ivChats = findViewById<ImageView>(R.id.ivNavChats)
        val tvChats = findViewById<TextView>(R.id.tvNavChats)
        val ivProfile = findViewById<ImageView>(R.id.ivNavPerfil)
        val tvProfile = findViewById<TextView>(R.id.tvNavPerfil)
        val fabCenter = findViewById<MaterialButton>(R.id.fabCenter)

        fun setActive(iv: ImageView, tv: TextView) {
            val activeColor = ContextCompat.getColor(this, R.color.bottom_nav_active)
            iv.imageTintList = android.content.res.ColorStateList.valueOf(activeColor)
            tv.setTextColor(activeColor)
        }

        fun setInactive(iv: ImageView, tv: TextView) {
            val inactiveColor = ContextCompat.getColor(this, R.color.bottom_nav_inactive)
            iv.imageTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            tv.setTextColor(inactiveColor)
        }

        setInactive(ivExplore, tvExplore)
        setInactive(ivMatches, tvMatches)
        setInactive(ivChats, tvChats)
        setActive(ivProfile, tvProfile)

        ivExplore.setOnClickListener {
            startActivity(Intent(this, ExploreActivity::class.java))
        }

        ivMatches.setOnClickListener {
            Toast.makeText(this, "Matches - Próximamente", Toast.LENGTH_SHORT).show()
        }

        ivChats.setOnClickListener {
            Toast.makeText(this, "Chats - Próximamente", Toast.LENGTH_SHORT).show()
        }

        ivProfile.setOnClickListener {
            val intent = Intent(this, com.example.myapplication.ui.simulacion.ViewProfileActivity::class.java)
            val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
            val currentEmail = prefs.getString("user_email", null)
            if (!currentEmail.isNullOrEmpty()) intent.putExtra("userEmail", currentEmail)
            startActivity(intent)
        }

        fabCenter.setOnClickListener {
            Toast.makeText(this, "Ya estás creando una publicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureCameraPermissionsAndLaunch() {
        val perms = mutableListOf<String>()
        perms.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val notGranted = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (notGranted.isEmpty()) {
            launchCamera()
            return
        }

        pendingPermissionAction = 1
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun ensureGalleryPermissionsAndLaunch() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val notGranted = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (notGranted.isEmpty()) {
            pickImagesLauncher.launch("image/*")
            return
        }

        pendingPermissionAction = 2
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun showAddOptions(position: Int) {
        val spaceLeft = maxPhotos - selectedUris.size
        if (spaceLeft <= 0) {
            Toast.makeText(this, "Máximo $maxPhotos fotos.", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("Tomar foto", "Elegir de la galería")
        AlertDialog.Builder(this)
            .setTitle("Agregar Foto")
            .setItems(options) { dialog: DialogInterface, which: Int ->
                when (which) {
                    0 -> {
                        pendingAddPosition = position
                        ensureCameraPermissionsAndLaunch()
                    }
                    1 -> {
                        pendingAddPosition = position
                        ensureGalleryPermissionsAndLaunch()
                    }
                }
            }
            .show()
    }

    private fun launchCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        cameraTempUri = uri
        if (uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(this, "No se pudo crear archivo para la foto.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publishPost() {
        val text = etPostText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Escribe un texto para la publicación.", Toast.LENGTH_SHORT).show()
            return
        }
        if (text.length > 1000) {
            Toast.makeText(this, "El texto no puede superar 1000 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos una foto.", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublish.isEnabled = false
        Toast.makeText(this, "Procesando imágenes...", Toast.LENGTH_SHORT).show()

        mainScope.launch {
            try {
                val imagesBase64 = mutableListOf<String>()
                var totalSize = 0L
                val maxTotalSize = 15 * 1024 * 1024 // 15MB máximo

                for ((index, uri) in selectedUris.withIndex()) {
                    val imageBase64 = withContext(Dispatchers.IO) { uriToBase64Image(uri, 1024) }
                    if (imageBase64 == null) {
                        throw Exception("Error procesando imagen ${index + 1}")
                    }

                    val imageSize = imageBase64.length
                    totalSize += imageSize

                    if (totalSize > maxTotalSize) {
                        throw Exception("Las imágenes son muy grandes. Intenta comprimir o reducir la cantidad.")
                    }

                    imagesBase64.add(imageBase64)
                    android.util.Log.d("CreatePostActivity", "Imagen ${index + 1} procesada: ${(imageSize / 1024)} KB")
                }

                android.util.Log.d("CreatePostActivity", "Total de imágenes: ${imagesBase64.size}, tamaño: ${(totalSize / 1024)} KB")

                val authorEmail = getCurrentUserEmail()
                val authorName = getCurrentUserName()
                val post = hashMapOf(
                    "text" to text,
                    "photos" to imagesBase64,
                    "authorEmail" to authorEmail,
                    "authorName" to authorName,
                    "timestamp" to System.currentTimeMillis()
                )

                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("posts").add(post).addOnSuccessListener {
                    runOnUiThread {
                        Toast.makeText(this@CreatePostActivity, "¡Publicación creada!", Toast.LENGTH_SHORT).show()
                        clearPostForm()
                        val intent = Intent(this@CreatePostActivity, ExploreActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    }
                }.addOnFailureListener { e ->
                    runOnUiThread {
                        android.util.Log.e("CreatePostActivity", "Error guardando: ${e.message}", e)
                        Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        btnPublish.isEnabled = true
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnPublish.isEnabled = true
                    android.util.Log.e("CreatePostActivity", "Error: ${e.message}", e)
                    androidx.appcompat.app.AlertDialog.Builder(this@CreatePostActivity)
                        .setTitle("Error al procesar imágenes")
                        .setMessage(e.message ?: "Ocurrió un error.\n¿Quieres reintentar?")
                        .setPositiveButton("Reintentar") { _, _ ->
                            publishPost()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    private fun uriToBase64Image(uri: Uri, maxDim: Int): String? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            var scale = 1
            val width = options.outWidth
            val height = options.outHeight
            val max = maxOf(width, height)
            if (max > maxDim) {
                scale = Math.ceil(max.toDouble() / maxDim.toDouble()).toInt()
            }

            val opts2 = BitmapFactory.Options().apply { inSampleSize = scale }
            val input2 = contentResolver.openInputStream(uri) ?: return null
            val bmp = BitmapFactory.decodeStream(input2, null, opts2) ?: return null
            input2.close()

            val scaled = if (maxOf(bmp.width, bmp.height) > maxDim) {
                val ratio = bmp.width.toFloat() / bmp.height.toFloat()
                if (bmp.width >= bmp.height) {
                    Bitmap.createScaledBitmap(bmp, maxDim, (maxDim / ratio).toInt(), true)
                } else {
                    Bitmap.createScaledBitmap(bmp, (maxDim * ratio).toInt(), maxDim, true)
                }
            } else bmp

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bytes = baos.toByteArray()
            baos.close()

            android.util.Log.d("CreatePostActivity", "Imagen convertida: ${bytes.size} bytes")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("CreatePostActivity", "Error: ${e.message}", e)
            null
        }
    }

    private fun getCurrentUserEmail(): String {
        return getSharedPreferences("user_data", MODE_PRIVATE).getString("user_email", "") ?: ""
    }

    private fun getCurrentUserName(): String {
        return getSharedPreferences("user_data", MODE_PRIVATE).getString("user_name", "") ?: ""
    }

    private fun loadProfileFromPrefs() {
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val name = prefs.getString("user_name", null)
        val photoBase64 = prefs.getString("user_photo", null)

        if (!name.isNullOrEmpty()) tvUserName.text = name
        if (!photoBase64.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                if (bmp != null) Glide.with(this@CreatePostActivity).load(bmp).circleCrop().into(ivUserAvatar)
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    private fun clearPostForm() {
        try {
            etPostText.setText("")
            selectedUris.clear()
            if (::adapter.isInitialized) {
                rvPhotos.post { adapter.submitList(selectedUris.toList()) }
            }
            cameraTempUri = null
            pendingAddPosition = -1
        } catch (_: Exception) { }
    }
}
