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
 * Ahora soporta: elegir galería, tomar foto con cámara, y position-based insertion en el grid.
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
    // Launcher para pedir múltiples permisos en tiempo de ejecución
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    // Acción pendiente después de conceder permisos: 0 = nada, 1 = cámara, 2 = galería
    private var pendingPermissionAction: Int = 0

    // Lista local de Uris seleccionadas
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: PhotoGridAdapter

    private val maxPhotos = 5
    private val mainScope = MainScope()

    // Para saber en qué posición se intentó agregar una foto (si se usa cámara)
    private var pendingAddPosition: Int = -1
    private var cameraTempUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        // Registrar launcher de permisos antes de usarlo
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // Si TODOS los permisos solicitados son concedidos, ejecutamos la acción pendiente
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
        // Ajustar el hint para incluir el nombre del usuario (preferir FirebaseAuth > SharedPreferences)
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

        // Inicializar header avatar y nombre
        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)

        // Cargar primero los datos guardados localmente para evitar parpadeo
        loadProfileFromPrefs()

        // Cargar datos de perfil del usuario (nombre y foto) en el header
        loadCurrentUserProfile()

        // Crear el adapter basado en DiffUtil (PhotoGridAdapter) y pasar los callbacks
        adapter = PhotoGridAdapter(
            maxPhotos = maxPhotos,
            onAddClick = { pos -> showAddOptions(pos) },
            onRemoveClick = { pos ->
                if (pos in selectedUris.indices) {
                    selectedUris.removeAt(pos)
                    // Actualizar la lista mediante DiffUtil en el hilo UI
                    rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                }
            }
        )
        // 3 columnas como en el mock
        val gm = GridLayoutManager(this, 3)
        // Hacer que cuando no haya imágenes el único slot ocupe las 3 columnas (solo posición 0)
        gm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val items = selectedUris
                // Si no hay items, solo la posición 0 ocupa las 3 columnas (slot central)
                return if (items.isEmpty() && position == 0) 3 else 1
            }
        }
        rvPhotos.layoutManager = gm
        rvPhotos.adapter = adapter
        // Enviar estado inicial (puede estar vacío) al adapter basado en DiffUtil
        adapter.submitList(selectedUris.toList())

        // Añadir spacing entre items (12dp)
        val spacingPx = (12 * resources.displayMetrics.density).toInt()
        rvPhotos.addItemDecoration(GridSpacingItemDecoration(3, spacingPx, true))

        // Launcher para seleccionar múltiples imágenes desde galería
        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                // Si el usuario seleccionó más de maxPhotos en la galería, avisamos y tomamos solo las primeras
                if (uris.size > maxPhotos) {
                    Toast.makeText(this, "Has seleccionado más de $maxPhotos fotos. Se añadirán las primeras $maxPhotos.", Toast.LENGTH_LONG).show()
                }
                val spaceLeft = maxPhotos - selectedUris.size
                // Si el usuario pasó más URIs que el espacio disponible, avisamos y recortamos
                var toAdd = uris
                if (uris.size > spaceLeft) {
                    toAdd = uris.take(spaceLeft)
                    Toast.makeText(this, "Solo se añadirán $spaceLeft fotos adicionales (máximo $maxPhotos).", Toast.LENGTH_SHORT).show()
                }

                val oldSize = selectedUris.size
                // Insertar en la posición pendiente si existe, manteniendo orden
                if (pendingAddPosition in 0 until maxPhotos) {
                    // Insertar secuencialmente a partir de pendingAddPosition
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
                        // Actualizar la lista usando DiffUtil
                        rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                    }
                } else {
                    selectedUris.addAll(toAdd)
                    val added = selectedUris.size - oldSize
                    if (added > 0) {
                        rvPhotos.post { adapter.submitList(selectedUris.toList()) }
                    }
                }
                // No es necesario llamar a notifyDataSetChanged aquí (ya notificamos rangos especificos)
             } else {
                 // No seleccionó nada
                 Toast.makeText(this, "Debes seleccionar al menos una foto", Toast.LENGTH_SHORT).show()
              }
          }

        // Launcher para tomar foto con cámara (recibe URI donde guardar)
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success && cameraTempUri != null) {
                // Insertar la foto en la posición pendiente o al final
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
                // Notificar con seguridad usando DiffUtil
                rvPhotos.post { adapter.submitList(selectedUris.toList()) }
            } else {
                // falló o cancelado
                cameraTempUri = null
                pendingAddPosition = -1
            }
        }

        btnPublish.setOnClickListener {
            publishPost()
        }

        setupBottomNav()

        // rvPhotos siempre visible; el adaptador gestiona slots vacíos y clicks para agregar
    }

    override fun onResume() {
        super.onResume()
        // Recargar nombre/foto por si el usuario lo actualizó en EditProfile
        loadCurrentUserProfile()
    }

    // Load current user email (FirebaseAuth or SharedPreferences) and request profile from FirebaseService
    private fun loadCurrentUserProfile() {
        // Usar SharedPreferences como fuente principal para email/nombre (evitar dependencia directa de FirebaseAuth aquí)
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)

        val emailToUse = prefs.getString("user_email", null)

        // No usamos FirebaseAuth aquí: tomar el email guardado en prefs
        if (emailToUse.isNullOrEmpty()) {
            // No hay email para consultar en la nube; ya mostramos datos locales si existían
            if (tvUserName.text.isNullOrEmpty()) tvUserName.text = getString(R.string.user_name_demo)
            if (tvUserName.text.isNullOrEmpty()) ivUserAvatar.setImageResource(R.mipmap.ic_launcher)
            return
        }

        // Solicitar datos de perfil desde la nube usando el email determinado
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
                        } catch (_: Exception) { /* ignore */ }
                     }

                    // Actualizar SharedPreferences con los datos de perfil obtenidos
                    try {
                        prefs.edit().apply {
                            if (!name.isNullOrEmpty()) putString("user_name", name)
                            if (!photoBase64.isNullOrEmpty()) putString("user_photo", photoBase64)
                            putString("user_email", emailToUse)
                        }
                    } catch (_: Exception) { /* ignore */ }
                 } else {
                     // Sin perfil en la nube: mantener valores locales ya mostrados
                 }
             }
         }
    }

    // Configurar la barra inferior incluida para navegación básica (A: conecta los botones)
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

        // Marcar Perfil como activo en esta pantalla
        setInactive(ivExplore, tvExplore)
        setInactive(ivMatches, tvMatches)
        setInactive(ivChats, tvChats)
        setActive(ivProfile, tvProfile)

        ivExplore.setOnClickListener {
            // Abrir ExploreActivity directamente
            startActivity(Intent(this, com.example.myapplication.ui.explore.ExploreActivity::class.java))
         }

         ivMatches.setOnClickListener {
             Toast.makeText(this, "Matches - Próximamente", Toast.LENGTH_SHORT).show()
         }

         ivChats.setOnClickListener {
             Toast.makeText(this, "Chats - Próximamente", Toast.LENGTH_SHORT).show()
         }

         ivProfile.setOnClickListener {
            // Abrir ViewProfileActivity directamente
            val intent = Intent(this, com.example.myapplication.ui.simulacion.ViewProfileActivity::class.java)
            // intentar pasar email si lo tenemos
            val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
            val currentEmail = prefs.getString("user_email", null)
            if (!currentEmail.isNullOrEmpty()) intent.putExtra("userEmail", currentEmail)
            startActivity(intent)
         }

        fabCenter.setOnClickListener {
            // Ya estamos en CreatePostActivity (desde FAB), no hacer nada o mostrar mensaje
            Toast.makeText(this, "Ya estás creando una publicación", Toast.LENGTH_SHORT).show()
        }
    }

    // Comprueba y solicita permisos necesarios para la cámara; si ya están concedidos, lanza la cámara
    private fun ensureCameraPermissionsAndLaunch() {
        val perms = mutableListOf<String>()
        // Cámara
        perms.add(Manifest.permission.CAMERA)
        // Lectura de imágenes depende de la API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // En versiones antiguas puede ser necesario WRITE_EXTERNAL_STORAGE
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Si ya están concedidos, ejecutar inmediatamente
        val notGranted = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (notGranted.isEmpty()) {
            launchCamera()
            return
        }

        // Solicitar permisos pendientes
        pendingPermissionAction = 1
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Comprueba y solicita permisos necesarios para galería; si ya están concedidos, lanza picker
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

    // Muestra opciones para agregar: Cámara o Galería
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
                    0 -> { // Cámara
                        pendingAddPosition = position
                        ensureCameraPermissionsAndLaunch()
                    }
                    1 -> { // Galería
                        pendingAddPosition = position
                        ensureGalleryPermissionsAndLaunch()
                    }
                }
            }
            .show()
    }

    // Crear un archivo temporal y lanzar la cámara para tomar foto
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
        if (text.length > 1000) {
            Toast.makeText(this, "El texto no puede superar 1000 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos una foto.", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublish.isEnabled = false
        Toast.makeText(this, "Subiendo publicación...", Toast.LENGTH_SHORT).show()

        // Subir imágenes secuencialmente y obtener URLs usando coroutines
        mainScope.launch {
            try {
                val uploadedUrls = mutableListOf<String>()
                val thumbnailsBase64 = mutableListOf<String>()

                for (uri in selectedUris) {
                    // Generar thumbnail Base64 (max dim 512)
                    val t64 = withContext(Dispatchers.IO) { uriToBase64Thumbnail(uri, 512) }
                    if (t64 == null) throw Exception("Error generando thumbnail")
                    thumbnailsBase64.add(t64)

                    // Subir usando la función suspend nueva
                    val url = FirebaseService.uploadImageSuspend(uri)
                    if (url == null) throw Exception("Error subiendo imagen")
                    uploadedUrls.add(url)
                }

                // Crear objeto publicación y guardarlo en Firestore (incluye ambas representaciones)
                val authorEmail = getCurrentUserEmail()
                val authorName = getCurrentUserName()
                val post = hashMapOf(
                    "text" to text,
                    "photos" to uploadedUrls,
                    "thumbnailsBase64" to thumbnailsBase64,
                    // incluir autor para poder filtrar y mostrar en Explore
                    "authorEmail" to authorEmail,
                    "authorName" to authorName,
                    "timestamp" to System.currentTimeMillis()
                )

                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("posts").add(post).addOnSuccessListener {
                    Toast.makeText(this@CreatePostActivity, "Publicación creada correctamente.", Toast.LENGTH_SHORT).show()
                    // Limpiar el formulario localmente antes de redirigir
                    clearPostForm()
                    // Redirigir automáticamente a ExploreActivity para que el usuario vea su publicación
                    // ExploreActivity escucha la colección 'posts' y mostrará la publicación recién creada.
                    val intent = Intent(this@CreatePostActivity, ExploreActivity::class.java)
                    // Opcional: limpiar el back stack para evitar volver a la pantalla de creación
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    // Finalizar esta Activity para liberar recursos
                    finish()
                 }.addOnFailureListener { e ->
                     Toast.makeText(this@CreatePostActivity, "Error guardando publicación", Toast.LENGTH_LONG).show()
                     btnPublish.isEnabled = true
                 }

            } catch (_: Exception) {
                // Mostrar diálogo con opción de reintentar
                runOnUiThread {
                    btnPublish.isEnabled = true
                    androidx.appcompat.app.AlertDialog.Builder(this@CreatePostActivity)
                        .setTitle("Error al subir imágenes")
                        .setMessage("Ocurrió un error subiendo las imágenes. ¿Quieres reintentar?")
                        .setPositiveButton("Reintentar") { _, _ ->
                            // Reintentar publicación completa
                            publishPost()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    // Crea un thumbnail de tamaño máximo maxDim (px), lo comprime y devuelve la cadena Base64
    private fun uriToBase64Thumbnail(uri: Uri, maxDim: Int): String? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            // Cargar dimensiones primero
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            // Calcular factor de escala
            var scale = 1
            val width = options.outWidth
            val height = options.outHeight
            val max = maxOf(width, height)
            if (max > maxDim) {
                scale = Math.ceil(max.toDouble() / maxDim.toDouble()).toInt()
            }

            // Decodificar con inSampleSize
            val opts2 = BitmapFactory.Options().apply { inSampleSize = scale }
            val input2 = contentResolver.openInputStream(uri) ?: return null
            val bmp = BitmapFactory.decodeStream(input2, null, opts2) ?: return null
            input2.close()

            // Si el bitmap aún es mayor que maxDim, escalarlo exactamente
            val scaled = if (maxOf(bmp.width, bmp.height) > maxDim) {
                val ratio = bmp.width.toFloat() / bmp.height.toFloat()
                if (bmp.width >= bmp.height) {
                    Bitmap.createScaledBitmap(bmp, maxDim, (maxDim / ratio).toInt(), true)
                } else {
                    Bitmap.createScaledBitmap(bmp, (maxDim * ratio).toInt(), maxDim, true)
                }
            } else bmp

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos) // calidad 75 para thumbnails
            val bytes = baos.toByteArray()
            baos.close()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }


    // Helper: obtener email actual preferente (SharedPreferences)
    private fun getCurrentUserEmail(): String {
        // Usar SharedPreferences como fuente fiable del email en este proyecto
        return getSharedPreferences("user_data", MODE_PRIVATE).getString("user_email", "") ?: ""
    }

    // Helper: obtener nombre actual preferente (SharedPreferences)
    private fun getCurrentUserName(): String {
        return getSharedPreferences("user_data", MODE_PRIVATE).getString("user_name", "") ?: ""
    }

    // Cargar y mostrar los datos de perfil guardados en SharedPreferences
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
            } catch (_: Exception) { /* ignore */ }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    // Limpia el formulario de creación de publicación (texto y fotos)
     private fun clearPostForm() {
         try {
             etPostText.setText("")
             selectedUris.clear()
             // Notificar al adaptador para mostrar el slot vacío
             if (::adapter.isInitialized) {
                rvPhotos.post { adapter.submitList(selectedUris.toList()) }
             }
             // Reset cámara temporal/pending
             cameraTempUri = null
             pendingAddPosition = -1
         } catch (_: Exception) { /* ignore */ }
     }
}
