package com.example.myapplication.ui.explore

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import java.io.ByteArrayOutputStream
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager
import android.os.Build

class MessagesFragment : Fragment() {

    private lateinit var messagesViewModel: MessagesViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnGallery: ImageButton
    private lateinit var btnCamera: ImageButton
    private lateinit var tvOtherUserName: TextView
    private lateinit var ivOtherUserPhoto: ImageView
    private lateinit var btnBack: ImageButton

    private var chatId: String = ""
    private var otherUserEmail: String = ""
    private var otherUserName: String = ""
    private var otherUserPhoto: String = ""

    companion object {
        private const val REQUEST_CAMERA = 1
        private const val REQUEST_GALLERY = 2
    }

    private var currentPhotoPath: String? = null

    // Usar la nueva API para seleccionar imagen de galería
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Log.d("MessagesFragment", "Imagen seleccionada de galería: $uri")
            // Procesar en hilo IO para no bloquear UI
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("MessagesFragment", "Abriendo stream de imagen...")
                    val input = try {
                        requireContext().contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        Log.e("MessagesFragment", "Error abriendo input stream: ${e.message}", e)
                        throw e
                    }

                    Log.d("MessagesFragment", "Decodificando bitmap...")
                    val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                    input?.close()

                    if (bitmap != null) {
                        Log.d("MessagesFragment", "Bitmap decodificado exitosamente: ${bitmap.width}x${bitmap.height}")
                        withContext(Dispatchers.Main) {
                            sendImageMessage(bitmap, isFromCamera = false)
                        }
                    } else {
                        Log.e("MessagesFragment", "El bitmap decodificado es null")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessagesFragment", "Error al obtener imagen de galería: ${e.message}", e)
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        try {
                            Toast.makeText(requireContext(), "Error al obtener imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                        } catch (ex: Exception) {
                            Log.e("MessagesFragment", "Error mostrando Toast en pickImageLauncher: ${ex.message}")
                        }
                    }
                }
            }
        } else {
            Log.d("MessagesFragment", "Selección de galería cancelada")
        }
    }

    // Usar la nueva API para capturar imagen con cámara (guarda en archivo)
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        Log.d("MessagesFragment", "Resultado de cámara: success=$success, path=$currentPhotoPath")
        if (success && currentPhotoPath != null) {
            val path = currentPhotoPath
            Log.d("MessagesFragment", "Leyendo foto de: $path")
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("MessagesFragment", "Decodificando archivo de foto...")
                    val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        Log.d("MessagesFragment", "Foto decodificada exitosamente: ${bitmap.width}x${bitmap.height}")
                        withContext(Dispatchers.Main) {
                            sendImageMessage(bitmap, isFromCamera = true)
                        }
                    } else {
                        Log.e("MessagesFragment", "El bitmap decodificado del archivo es null")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error al procesar la foto", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessagesFragment", "Error al cargar foto de cámara: ${e.message}", e)
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        try {
                            Toast.makeText(requireContext(), "Error al procesar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        } catch (ex: Exception) {
                            Log.e("MessagesFragment", "Error mostrando Toast en takePictureLauncher: ${ex.message}")
                        }
                    }
                }
            }
        } else {
            Log.w("MessagesFragment", "Error: success=$success, currentPhotoPath=$currentPhotoPath")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ocultar la barra de navegación inferior
        val activity = requireActivity()
        val navBar = activity.findViewById<android.view.View>(R.id.layout_bottom_nav)
        navBar?.visibility = android.view.View.GONE

        // Ocultar el botón flotante de crear historia
        val fabCenter = activity.findViewById<android.view.View>(R.id.fabCenter)
        fabCenter?.visibility = android.view.View.GONE

        // ...existing code...
        val fragmentContainer = activity.findViewById<android.view.View>(R.id.fragmentContainer)
        val layoutParams = fragmentContainer?.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (layoutParams != null) {
            layoutParams.bottomMargin = 0
            fragmentContainer.layoutParams = layoutParams
        }

        // Obtener argumentos
        chatId = arguments?.getString("chat_id") ?: ""
        otherUserEmail = arguments?.getString("other_user_email") ?: ""
        otherUserName = arguments?.getString("other_user_name") ?: ""
        otherUserPhoto = arguments?.getString("other_user_photo") ?: ""

        // Inicializar ViewModel
        messagesViewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)

        // Obtener referencias de vistas
        rvMessages = view.findViewById(R.id.rvMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSendMessage)
        btnGallery = view.findViewById(R.id.btnGallery)
        btnCamera = view.findViewById(R.id.btnCamera)
        tvOtherUserName = view.findViewById(R.id.tvOtherUserName)
        ivOtherUserPhoto = view.findViewById(R.id.ivOtherUserPhoto)
        btnBack = view.findViewById(R.id.btnBackMessages)

        // Configurar RecyclerView
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""

        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        messagesAdapter = MessagesAdapter(currentUserEmail)
        rvMessages.adapter = messagesAdapter

        if (chatId.isNotEmpty()) {
            // Cargar mensajes
            messagesViewModel.loadMessages(chatId)
            messagesViewModel.loadChat(chatId)

            // Observar cambios en el chat para obtener información actualizada
            messagesViewModel.chat.observe(viewLifecycleOwner) { chat ->
                // Determinar quién es el otro usuario dinámicamente
                val displayUserName: String
                val displayUserPhoto: String

                if (currentUserEmail == chat.user1Email) {
                    // El usuario logeado es user1, mostrar info de user2
                    displayUserName = chat.user2Name
                    displayUserPhoto = chat.user2Photo
                } else {
                    // El usuario logeado es user2, mostrar info de user1
                    displayUserName = chat.user1Name
                    displayUserPhoto = chat.user1Photo
                }

                // Actualizar la UI con la información del otro usuario
                tvOtherUserName.text = displayUserName
                loadUserPhoto(displayUserPhoto)
            }

            // Observar cambios en los mensajes
            messagesViewModel.messages.observe(viewLifecycleOwner) { messages ->
                messagesAdapter.setMessages(messages)
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        // Botones de acción
        btnSend.setOnClickListener {
            sendMessage()
        }

        btnGallery.setOnClickListener {
            pickImageFromGallery()
        }

        btnCamera.setOnClickListener {
            takePhoto()
        }

        btnBack.setOnClickListener {
            goBack()
        }


        ivOtherUserPhoto.setOnClickListener {
            goBack()
        }
    }

    private fun loadUserPhoto(photoBase64: String) {
        if (!photoBase64.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                if (bmp != null) {
                    Glide.with(this)
                        .load(bmp)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(ivOtherUserPhoto)
                }
            } catch (e: Exception) {
                Log.w("MessagesFragment", "Error cargando foto: ${e.message}")
                ivOtherUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            ivOtherUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) {
            Toast.makeText(requireContext(), "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""
        val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

        messagesViewModel.sendMessage(
            chatId,
            currentUserEmail,
            currentUserName,
            messageText,
            onSuccess = {
                etMessage.text.clear()
                Toast.makeText(requireContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun pickImageFromGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            // Permiso ya está otorgado
            pickImageLauncher.launch("image/*")
        } else {
            // Solicitar permiso
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(permission),
                REQUEST_GALLERY
            )
        }
    }

    private fun takePhoto() {
        val permission = Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            // Permiso ya está otorgado
            launchCamera()
        } else {
            // Solicitar permiso
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(permission),
                REQUEST_CAMERA
            )
        }
    }

    private fun launchCamera() {
        try {
            val context = requireContext() ?: return
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("MessagesFragment", "Error al acceder a la cámara: ${e.message}", e)
            try {
                Toast.makeText(requireContext(), "Error al acceder a la cámara", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Log.e("MessagesFragment", "No se pudo mostrar Toast: ${ex.message}")
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val context = requireContext()
        val storageDir = context.cacheDir
        val image = File.createTempFile("CHAT_${timeStamp}_", ".jpg", storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    launchCamera()
                }
                REQUEST_GALLERY -> {
                    pickImageLauncher.launch("image/*")
                }
            }
        } else {
            Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendImageMessage(bitmap: android.graphics.Bitmap, isFromCamera: Boolean = false) {
        // Validar chatId
        if (chatId.isEmpty()) {
            Log.e("MessagesFragment", "ERROR CRÍTICO: chatId está vacío")
            Toast.makeText(requireContext(), "Error: ChatID no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Capturar datos en hilo principal ANTES de cambiar de hilo
        val context = try { requireContext() } catch (e: Exception) {
            Log.e("MessagesFragment", "Error obteniendo contexto: ${e.message}", e)
            null
        } ?: return

        val prefs = try {
            context.getSharedPreferences("user_data", 0)
        } catch (e: Exception) {
            Log.e("MessagesFragment", "Error obteniendo prefs: ${e.message}", e)
            null
        } ?: return

        val currentUserEmail = prefs.getString("user_email", "") ?: ""
        val currentUserName = prefs.getString("user_name", "Usuario") ?: "Usuario"

        Log.d("MessagesFragment", "Iniciando envío de imagen. Usuario: $currentUserEmail, ChatID: $chatId, isFromCamera: $isFromCamera")

        // Mostrar indicador de envío en hilo principal
        try {
            Toast.makeText(context, "Enviando imagen...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MessagesFragment", "Error mostrando Toast inicial: ${e.message}")
        }

        // Procesar imagen en hilo IO
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MessagesFragment", "Iniciando compresión de imagen...")

                // Compresión iterativa: reducir tamaño hasta alcanzar objetivo
                var compressionQuality = 50
                var maxWidth = 500
                var imageBase64 = "" // Inicializar variable
                var resizedBitmap = bitmap // Inicializar con el bitmap original

                // Loop hasta que el Base64 sea lo suficientemente pequeño (< 150KB)
                while (maxWidth >= 300 && compressionQuality >= 10) {
                    Log.d("MessagesFragment", "Intentando: maxWidth=$maxWidth, quality=$compressionQuality")

                    resizedBitmap = resizeBitmap(bitmap, maxWidth)
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                    Log.d("MessagesFragment", "Tamaño: ${imageBytes.size} bytes, Base64: ${imageBase64.length} caracteres")

                    if (imageBase64.length <= 150000) {
                        Log.d("MessagesFragment", "✓ Tamaño aceptable alcanzado: ${imageBase64.length} caracteres")
                        break
                    }

                    // Reducir parámetros para próximo intento
                    compressionQuality -= 5
                    if (compressionQuality < 10) {
                        maxWidth -= 50
                        compressionQuality = 40
                    }
                }

                Log.d("MessagesFragment", "Imagen final Base64: ${imageBase64.length} caracteres (${imageBase64.length / 1024} KB)")

                if (imageBase64.length > 150000) {
                    Log.e("MessagesFragment", "ERROR CRÍTICO: No se pudo comprimir la imagen lo suficiente")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Imagen demasiado grande para enviar", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Guardar foto en galería SOLO si es de cámara
                if (isFromCamera) {
                    try {
                        Log.d("MessagesFragment", "Guardando foto en galería...")
                        savePhotoToGallery(resizedBitmap)
                        Log.d("MessagesFragment", "Foto guardada en galería exitosamente")
                    } catch (e: Exception) {
                        Log.w("MessagesFragment", "Error guardando en galería (no es crítico): ${e.message}", e)
                    }
                }

                Log.d("MessagesFragment", "Enviando mensaje a Firestore...")
                messagesViewModel.sendMessage(
                    chatId,
                    currentUserEmail,
                    currentUserName,
                    "[Imagen]",
                    imageBase64,
                    onSuccess = {
                        Log.d("MessagesFragment", "Imagen enviada exitosamente")
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                Toast.makeText(context, "Imagen enviada", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("MessagesFragment", "Error mostrando Toast de éxito: ${e.message}")
                            }
                        }
                    },
                    onFailure = { e ->
                        Log.e("MessagesFragment", "Error enviando imagen: ${e.message}", e)
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                Toast.makeText(context, "Error al enviar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                            } catch (ex: Exception) {
                                Log.e("MessagesFragment", "Error mostrando Toast de error: ${ex.message}")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("MessagesFragment", "Error CRÍTICO al procesar imagen: ${e.message}", e)
                e.printStackTrace()
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        Toast.makeText(context, "Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        Log.e("MessagesFragment", "Error mostrando Toast de error crítico: ${ex.message}")
                    }
                }
            }
        }
    }

    private fun resizeBitmap(bitmap: android.graphics.Bitmap, maxWidth: Int): android.graphics.Bitmap {
        return if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            val newHeight = (bitmap.height * ratio).toInt()
            android.graphics.Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun savePhotoToGallery(bitmap: android.graphics.Bitmap) {
        try {
            val context = requireContext()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "Chat_$timeStamp.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${context.getString(R.string.app_name)}")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { out ->
                        out?.let { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } else {
                MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, filename, "Foto de chat")
            }
            Log.d("MessagesFragment", "Foto guardada en galería")
        } catch (e: Exception) {
            Log.w("MessagesFragment", "Error guardando foto en galería: ${e.message}")
        }
    }

    private fun goBack() {
        // Mostrar la barra de navegación al salir del chat
        val activity = requireActivity()
        val navBar = activity.findViewById<android.view.View>(R.id.layout_bottom_nav)
        navBar?.visibility = android.view.View.VISIBLE

        // Mostrar el botón flotante de crear historia al salir del chat
        val fabCenter = activity.findViewById<android.view.View>(R.id.fabCenter)
        fabCenter?.visibility = android.view.View.VISIBLE

        // Restaurar el margen del fragmentContainer
        val fragmentContainer = activity.findViewById<android.view.View>(R.id.fragmentContainer)
        val layoutParams = fragmentContainer?.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (layoutParams != null) {
            layoutParams.bottomMargin = 224  // 56dp en píxeles (56 * 4 = 224dp aproximadamente, pero usamos 56dp)
            fragmentContainer.layoutParams = layoutParams
        }

        parentFragmentManager.popBackStack()
    }
}

