package com.example.myapplication.ui.explore

import android.Manifest
import android.content.Context
import android.os.Build
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePostFragment : Fragment() {

    private lateinit var etPostText: EditText
    private lateinit var rvPhotos: RecyclerView
    private lateinit var btnPublish: MaterialButton
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var btnPickGallery: MaterialButton
    private lateinit var btnTakePhoto: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoGridAdapter

    private lateinit var pickImagesLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var cameraTempUri: Uri? = null

    private val MAX_IMAGES = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lanzador para seleccionar múltiples imágenes desde galería
        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                val spaceLeft = MAX_IMAGES - selectedUris.size
                val toAdd = uris.take(spaceLeft)
                selectedUris.addAll(toAdd)
                photoAdapter.submitList(selectedUris.toList())
            }
        }

        // Lanzador para tomar foto y guardarla en una Uri temporal
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraTempUri != null) {
                cameraTempUri?.let { uri ->
                    if (selectedUris.size < MAX_IMAGES) {
                        selectedUris.add(uri)
                        photoAdapter.submitList(selectedUris.toList())
                    } else {
                        Toast.makeText(requireContext(), "Máximo $MAX_IMAGES imágenes por historia", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                cameraTempUri = null
            }
        }

        // Lanzador para pedir permisos
        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // No hacemos nada automático aquí; la acción puede reintentarse manualmente desde la UI
            val anyGranted = perms.values.any { it }
            if (!anyGranted) {
                Toast.makeText(requireContext(), "Permisos requeridos para usar la cámara/galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
        loadUserData()
    }

    private fun initializeViews(view: View) {
        etPostText = view.findViewById(R.id.etPostText)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        btnPublish = view.findViewById(R.id.btnPublish)
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        btnPickGallery = view.findViewById(R.id.btnPickGallery)
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto)

        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)

        photoAdapter = PhotoGridAdapter(MAX_IMAGES,
            onAddClick = { _ -> openGallery() },
            onRemoveClick = { pos ->
                if (pos >= 0 && pos < selectedUris.size) {
                    selectedUris.removeAt(pos)
                    photoAdapter.submitList(selectedUris.toList())
                }
            }
        )

        rvPhotos.adapter = photoAdapter
    }

    private fun setupListeners() {
        btnPickGallery.setOnClickListener { openGallery() }
        btnTakePhoto.setOnClickListener { takePhotoWithCamera() }
        btnPublish.setOnClickListener { publishStory() }
    }

    private fun loadUserData() {
        // Intentar obtener nombre y foto del perfil para mostrar
        val prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        var email = prefs.getString("user_email", "") ?: ""
        if (email.isNullOrEmpty()) email = FirebaseService.getCurrentUserEmail()

        if (email.isNullOrEmpty()) return

        FirebaseService.getUserProfile(email) { data ->
            data?.let {
                val name = it["name"]?.toString() ?: "Usuario"
                tvUserName.text = name

                val photo = it["photo"]?.toString()
                if (!photo.isNullOrEmpty()) {
                    try {
                        if (photo.startsWith("http://") || photo.startsWith("https://")) {
                            try {
                                com.bumptech.glide.Glide.with(this)
                                    .load(photo)
                                    .circleCrop()
                                    .into(ivUserAvatar)
                            } catch (e: Exception) { /* ignore */ }
                        } else {
                            val decoded = Base64.decode(photo, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            if (bitmap != null) ivUserAvatar.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) { /* ignore */ }
                }
            }
        }
    }

    private fun openGallery() {
        val spaceLeft = MAX_IMAGES - selectedUris.size
        if (spaceLeft <= 0) {
            Toast.makeText(requireContext(), "Máximo $MAX_IMAGES imágenes por historia", Toast.LENGTH_SHORT).show()
            return
        }
        // Pedir permiso lectura si es necesario (condicional según API)
        val readPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionsLauncher.launch(arrayOf(readPerm))
        pickImagesLauncher.launch("image/*")
    }

    private fun takePhotoWithCamera() {
        requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))

        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "No se pudo crear archivo para la foto", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also { file ->
            val authority = requireContext().packageName + ".fileprovider"
            val photoURI: Uri = FileProvider.getUriForFile(requireContext(), authority, file)
            cameraTempUri = photoURI
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun publishStory() {
        val text = etPostText.text.toString().trim()

        if (selectedUris.isEmpty()) {
            Toast.makeText(requireContext(), "Añade al menos una foto para publicar la historia", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublish.isEnabled = false
        btnPublish.text = "Publicando..."

        lifecycleScope.launch {
            val uploadedUrls = mutableListOf<String>()
            var failed = false

            for (uri in selectedUris) {
                val url = withContext(Dispatchers.IO) {
                    try {
                        // Convertir Uri a Bitmap y luego a Base64 con compresión JPEG 80%
                        val input = requireContext().contentResolver.openInputStream(uri) ?: return@withContext null
                        val bitmap = BitmapFactory.decodeStream(input)
                        input.close()

                        if (bitmap != null) {
                            bitmapToBase64(bitmap)
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (url != null) {
                    uploadedUrls.add(url)
                } else {
                    failed = true
                    break
                }
            }

            if (failed) {
                Toast.makeText(requireContext(), "Error subiendo imágenes. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                btnPublish.isEnabled = true
                btnPublish.text = "PUBLICAR"
                return@launch
            }

            val prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
            var email = prefs.getString("user_email", "") ?: ""
            if (email.isEmpty()) {
                email = FirebaseService.getCurrentUserEmail() ?: ""
            }

            // Validar que el email no esté vacío
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Error: Email del usuario vacío", Toast.LENGTH_SHORT).show()
                btnPublish.isEnabled = true
                btnPublish.text = "PUBLICAR"
                return@launch
            }

            android.util.Log.d("CreatePostFragment", "Guardando historia con email: $email")

            // Obtener el perfil del usuario ANTES de crear la historia
            FirebaseService.getUserProfile(email) { profile ->
                val userName = profile?.get("name")?.toString() ?: "Usuario"
                val userPhoto = profile?.get("photo")?.toString() ?: ""

                val storyData = hashMapOf<String, Any>(
                    "userEmail" to email,
                    "images" to uploadedUrls,
                    "text" to text,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "userName" to userName,
                    "userPhoto" to userPhoto
                )

                // Calcular tamaño combinado aproximado de las imágenes Base64 (bytes UTF-8)
                var totalBytes = 0L
                for (b64 in uploadedUrls) {
                    totalBytes += try { b64.toByteArray(Charsets.UTF_8).size.toLong() } catch (_: Exception) { 0L }
                }

                val FIRESTORE_SAFE_THRESHOLD = 700 * 1024 // 700 KB conservador (máximo Firestore es 1MB por documento)

                if (totalBytes <= FIRESTORE_SAFE_THRESHOLD) {
                    // Guardar directamente en el documento posts (colección correcta)
                    android.util.Log.d("CreatePostFragment", "Guardando publicación en collection 'posts': $storyData")
                    db.collection("posts")
                        .add(storyData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Publicación compartida", Toast.LENGTH_SHORT).show()
                            selectedUris.clear()
                            photoAdapter.submitList(selectedUris.toList())
                            etPostText.text.clear()
                            btnPublish.isEnabled = true
                            btnPublish.text = "PUBLICAR"
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CreatePostFragment", "Error guardando publicación: ${e.message}")
                            Toast.makeText(requireContext(), "Error guardando publicación: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnPublish.isEnabled = true
                            btnPublish.text = "PUBLICAR"
                        }

                } else {
                    // Demasiado grande para un solo documento: crear documento posts y guardar imágenes en collection 'postImages'
                    val postDocRef = db.collection("posts").document()
                    // Guardar metadata sin imágenes inicialmente
                    val postNoImages = HashMap(storyData)
                    postNoImages["images"] = listOf<String>()

                    postDocRef.set(postNoImages)
                        .addOnSuccessListener {
                            // Guardar cada imagen como documento independiente en 'postImages'
                            val imageIds = mutableListOf<String>()
                            var remaining = uploadedUrls.size
                            var failed = false

                            if (remaining == 0) {
                                // No hay imágenes (caso raro), actualizar y terminar
                                postDocRef.update("images", imageIds)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Publicación compartida", Toast.LENGTH_SHORT).show()
                                        selectedUris.clear()
                                        photoAdapter.submitList(selectedUris.toList())
                                        etPostText.text.clear()
                                        btnPublish.isEnabled = true
                                        btnPublish.text = "PUBLICAR"
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Error actualizando publicación: ${e.message}", Toast.LENGTH_SHORT).show()
                                        btnPublish.isEnabled = true
                                        btnPublish.text = "PUBLICAR"
                                    }
                                return@addOnSuccessListener
                            }

                            for ((idx, base64) in uploadedUrls.withIndex()) {
                                val imgData = hashMapOf(
                                    "postId" to postDocRef.id,
                                    "image" to base64,
                                    "index" to idx
                                )
                                db.collection("postImages").add(imgData)
                                    .addOnSuccessListener { imgDoc ->
                                        imageIds.add(imgDoc.id)
                                        remaining -= 1
                                        if (remaining == 0 && !failed) {
                                            // Todas las imágenes guardadas: actualizar el post con los IDs
                                            postDocRef.update("images", imageIds)
                                                .addOnSuccessListener {
                                                    Toast.makeText(requireContext(), "Publicación compartida", Toast.LENGTH_SHORT).show()
                                                    selectedUris.clear()
                                                    photoAdapter.submitList(selectedUris.toList())
                                                    etPostText.text.clear()
                                                    btnPublish.isEnabled = true
                                                    btnPublish.text = "PUBLICAR"
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(requireContext(), "Error actualizando publicación: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    btnPublish.isEnabled = true
                                                    btnPublish.text = "PUBLICAR"
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // Marcar fallo pero continuar intentando el resto
                                        failed = true
                                        remaining -= 1
                                        Toast.makeText(requireContext(), "Error subiendo imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                                        if (remaining == 0) {
                                            // Si fallaron todas o algunas, notificamos error y limpiamos/intentamos rollback mínimo
                                            Toast.makeText(requireContext(), "Error subiendo imágenes. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                                            btnPublish.isEnabled = true
                                            btnPublish.text = "PUBLICAR"
                                        }
                                    }
                            }

                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error creando publicación: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnPublish.isEnabled = true
                            btnPublish.text = "PUBLICAR"
                        }
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: android.graphics.Bitmap): String {
        val maxBytes = 800 * 1024 // 800 KB para margen de seguridad (máximo Firestore es 1MB)
        var quality = 70 // Empezar con calidad más baja que en perfil
        
        // Primero intentar con la imagen original a diferentes calidades
        while (quality >= 20) {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            if (base64.toByteArray(Charsets.UTF_8).size <= maxBytes) {
                android.util.Log.d("CreatePostFragment", "Imagen comprimida a calidad $quality, tamaño: ${base64.toByteArray(Charsets.UTF_8).size} bytes")
                return base64
            }

            quality -= 5
        }

        // Si aún es demasiado grande, redimensionar la imagen agresivamente
        var scaledBitmap = bitmap
        var scaleFactor = 0.8f

        while (scaleFactor > 0.2f) {
            val newWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(100)
            val newHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(100)

            scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            quality = 70

            while (quality >= 20) {
                val baos = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                if (base64.toByteArray(Charsets.UTF_8).size <= maxBytes) {
                    if (scaledBitmap !== bitmap) scaledBitmap.recycle()
                    android.util.Log.d("CreatePostFragment", "Imagen escalada a ${newWidth}x${newHeight} con calidad $quality, tamaño: ${base64.toByteArray(Charsets.UTF_8).size} bytes")
                    return base64
                }

                quality -= 5
            }

            if (scaledBitmap !== bitmap) scaledBitmap.recycle()
            scaleFactor -= 0.1f
        }

        // Último recurso: comprimir agresivamente a baja resolución
        val finalBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 400, 300, true)
        val baos = java.io.ByteArrayOutputStream()
        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 15, baos)
        finalBitmap.recycle()
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        android.util.Log.d("CreatePostFragment", "Imagen comprimida al máximo: tamaño ${base64.toByteArray(Charsets.UTF_8).size} bytes")
        return base64
    }
}
