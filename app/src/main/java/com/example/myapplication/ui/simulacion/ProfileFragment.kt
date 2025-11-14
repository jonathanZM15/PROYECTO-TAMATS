package com.example.myapplication.ui.simulacion

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.example.myapplication.ui.explore.Post
import com.example.myapplication.ui.explore.PostAdapter
import com.example.myapplication.ui.explore.ImagePagerAdapter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.PopupWindow

class ProfileFragment : Fragment() {

    private lateinit var ivProfilePhotoView: ImageView
    private lateinit var tvViewName: TextView
    private lateinit var tvViewAge: TextView
    private lateinit var tvViewCity: TextView
    private lateinit var tvViewDescription: TextView
    private lateinit var interestsViewContainer: LinearLayout

    // Botón de menú superior
    private lateinit var btnMenuProfile: ImageButton

    // RecyclerView para publicaciones
    private lateinit var rvProfilePosts: RecyclerView
    private lateinit var tvNoPostsMessage: TextView
    private val posts = mutableListOf<Post>()
    private var adapter: PostAdapter? = null

    // Firestore
    private val db = FirebaseFirestore.getInstance()

    // Paginación
    private val pageSize = 6
    private var isLoading = false
    private var noMore = false
    private var lastVisible: DocumentSnapshot? = null

    // Email del usuario actual
    private var currentUserEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
        loadProfileData()

        // Inicializar RecyclerView para posts
        rvProfilePosts.layoutManager = LinearLayoutManager(requireContext())
        adapter = PostAdapter(posts)
        rvProfilePosts.adapter = adapter

        rvProfilePosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisiblePos = lm.findLastVisibleItemPosition()

                // Cargar cuando estemos cerca del final
                if (!isLoading && !noMore && lastVisiblePos >= total - 3) {
                    loadMorePosts()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Recargar historias cuando el usuario regresa a esta pantalla
        if (currentUserEmail.isNotEmpty()) {
            android.util.Log.d("ProfileFragment", "onResume: Recargando historias")
            loadStories()
        }
    }

    private fun initializeViews(view: View) {
        try {
            ivProfilePhotoView = view.findViewById(R.id.ivProfilePhotoView)
            tvViewName = view.findViewById(R.id.tvViewName)
            tvViewAge = view.findViewById(R.id.tvViewAge)
            tvViewCity = view.findViewById(R.id.tvViewCity)
            tvViewDescription = view.findViewById(R.id.tvViewDescription)
            interestsViewContainer = view.findViewById(R.id.interestsViewContainer)
            rvProfilePosts = view.findViewById(R.id.rvProfilePosts)
            tvNoPostsMessage = view.findViewById(R.id.tvNoPostsMessage)

            // nueva vista: botón de menú
            btnMenuProfile = view.findViewById(R.id.btnMenuProfile)
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error inicializando vistas: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        // Mostrar un PopupWindow anclado al botón superior para que aparezca justo debajo
        try {
            btnMenuProfile.setOnClickListener { anchorView ->
                try {
                    val inflater = LayoutInflater.from(requireContext())
                    val popupView = inflater.inflate(R.layout.popup_profile_menu, null)

                    // Crear PopupWindow con wrap_content para ancho/alto
                    val popupWindow = PopupWindow(
                        popupView,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        true
                    )

                    // Permitir tocar fuera para cerrar y dar fondo no nulo para que funcione outside touch
                    popupWindow.isOutsideTouchable = true
                    popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    // Opcional: sombra/elevación en API >= 21
                    try {
                        popupWindow.elevation = 8f
                    } catch (ignored: Throwable) { }

                    // Asociar botones del layout del popup
                    val btnEdit = popupView.findViewById<TextView>(R.id.btn_edit_profile)
                    val btnLogout = popupView.findViewById<TextView>(R.id.btn_logout)

                    btnEdit.setOnClickListener {
                        popupWindow.dismiss()
                        startActivity(Intent(requireActivity(), EditProfileActivity::class.java))
                    }

                    btnLogout.setOnClickListener {
                        popupWindow.dismiss()
                        FirebaseService.logout(requireContext())
                        val prefs = requireContext().getSharedPreferences("user_data", 0)
                        prefs.edit { clear() }
                        val intent = Intent(requireActivity(), com.example.myapplication.ui.login.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }

                    // Mostrar el popup justo debajo del botón (showAsDropDown asegura el anclaje).
                    // Ajustes de offset (x,y) pueden calibrarse si hace falta.
                    popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.NO_GRAVITY)

                } catch (e: Exception) {
                    android.util.Log.w("ProfileFragment", "Error mostrando popup personalizado: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ProfileFragment", "setupListeners: ${e.message}")
        }
    }

    private fun loadProfileData() {
        // Primero intentar obtener el email de SharedPreferences (más confiable que FirebaseAuth)
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        currentUserEmail = prefs.getString("user_email", null) ?: ""

        // Si no hay en SharedPreferences, intentar FirebaseAuth
        if (currentUserEmail.isEmpty()) {
            currentUserEmail = FirebaseService.getCurrentUserEmail()
        }

        if (currentUserEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Las historias se cargan en onResume(), no aquí

        // Buscar perfil en userProfiles primero (donde se guardan los perfiles editados)
        val sanitizedEmail = currentUserEmail.replace("/", "_")
        db.collection("userProfiles").document(sanitizedEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.data ?: return@addOnSuccessListener

                    // Cargar datos básicos
                    tvViewName.text = userData["name"]?.toString() ?: "Sin nombre"
                    tvViewAge.text = userData["age"]?.toString() ?: "N/A"
                    tvViewCity.text = userData["city"]?.toString() ?: "N/A"
                    tvViewDescription.text = userData["description"]?.toString() ?: "Sin descripción"

                    // Cargar foto de perfil
                    val profilePhotoBase64 = userData["photo"]?.toString()
                    if (!profilePhotoBase64.isNullOrEmpty()) {
                        try {
                            val decoded = Base64.decode(profilePhotoBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            if (bitmap != null) {
                                Glide.with(this@ProfileFragment)
                                    .load(bitmap)
                                    .into(ivProfilePhotoView)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ProfileFragment", "Error cargando foto: ${e.message}")
                        }
                    }

                    // Cargar intereses
                    val interests = userData["interests"] as? List<*> ?: emptyList<Any>()
                    interestsViewContainer.removeAllViews()
                    for (interest in interests) {
                        val interestText = interest.toString()
                        val chipView = createInterestChip(interestText)
                        interestsViewContainer.addView(chipView)
                    }

                    // Cargar publicaciones del usuario (inicial)
                    loadInitialPosts()
                } else {
                    // Si no existe en userProfiles, intenta en usuarios
                    db.collection("usuarios")
                        .whereEqualTo("email", currentUserEmail)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                Toast.makeText(requireContext(), "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            val doc = documents.documents[0]
                            val userData = doc.data ?: return@addOnSuccessListener

                            // Cargar datos básicos
                            tvViewName.text = userData["name"]?.toString() ?: "Sin nombre"
                            tvViewAge.text = userData["age"]?.toString() ?: "N/A"
                            tvViewCity.text = userData["city"]?.toString() ?: "N/A"
                            tvViewDescription.text = userData["description"]?.toString() ?: "Sin descripción"

                            // Cargar foto de perfil
                            val profilePhotoBase64 = userData["photo"]?.toString()
                            if (!profilePhotoBase64.isNullOrEmpty()) {
                                try {
                                    val decoded = Base64.decode(profilePhotoBase64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    if (bitmap != null) {
                                        Glide.with(this@ProfileFragment)
                                            .load(bitmap)
                                            .into(ivProfilePhotoView)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("ProfileFragment", "Error cargando foto: ${e.message}")
                                }
                            }

                            // Cargar intereses
                            val interests = userData["interests"] as? List<*> ?: emptyList<Any>()
                            interestsViewContainer.removeAllViews()
                            for (interest in interests) {
                                val interestText = interest.toString()
                                val chipView = createInterestChip(interestText)
                                interestsViewContainer.addView(chipView)
                            }

                            // Cargar publicaciones del usuario (inicial)
                            loadInitialPosts()
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("ProfileFragment", "Error loading profile: ${exception.message}")
                            Toast.makeText(requireContext(), "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("ProfileFragment", "Error loading profile from userProfiles: ${exception.message}")
                // Fallback: intentar en usuarios
                db.collection("usuarios")
                    .whereEqualTo("email", currentUserEmail)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Toast.makeText(requireContext(), "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val doc = documents.documents[0]
                        val userData = doc.data ?: return@addOnSuccessListener

                        tvViewName.text = userData["name"]?.toString() ?: "Sin nombre"
                        tvViewAge.text = userData["age"]?.toString() ?: "N/A"
                        tvViewCity.text = userData["city"]?.toString() ?: "N/A"
                        tvViewDescription.text = userData["description"]?.toString() ?: "Sin descripción"

                        // Cargar publicaciones del usuario (inicial)
                        loadInitialPosts()
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e("ProfileFragment", "Error loading profile: ${exception.message}")
                        Toast.makeText(requireContext(), "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun loadStories() {
        try {
            val container = view?.findViewById<LinearLayout>(R.id.storiesContainer)
            if (container == null) {
                android.util.Log.w("ProfileFragment", "storiesContainer es null, retornando")
                return
            }

            container.removeAllViews()

            if (currentUserEmail.isEmpty()) {
                android.util.Log.w("ProfileFragment", "currentUserEmail vacío en loadStories")
                return
            }

            android.util.Log.d("ProfileFragment", "Cargando historias para email: $currentUserEmail")

            // Usar query más simple sin orderBy para evitar requisito de índice
            db.collection("stories")
                .whereEqualTo("userEmail", currentUserEmail)
                .get()
                .addOnSuccessListener { snapshot ->
                    android.util.Log.d("ProfileFragment", "Se encontraron ${snapshot.size()} historias")

                    if (snapshot.isEmpty) {
                        // No hay historias
                        android.util.Log.d("ProfileFragment", "No hay historias para este usuario")
                        return@addOnSuccessListener
                    }

                    // Ordenar localmente por timestamp descendente
                    val stories = snapshot.documents
                        .mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                val timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                                Pair(doc, timestamp)
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileFragment", "Error procesando documento: ${e.message}")
                                null
                            }
                        }
                        .sortedByDescending { it.second }
                        .map { it.first }

                    for (doc in stories) {
                        try {
                            val data = doc.data ?: continue
                            val imageIds = (data["images"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val text = data["text"]?.toString() ?: ""
                            val userName = data["userName"]?.toString() ?: ""
                            val userPhoto = data["userPhoto"]?.toString() ?: ""

                            android.util.Log.d("ProfileFragment", "Renderizando historia con ${imageIds.size} imágenes")

                            // Inflar item_story
                            val storyView = LayoutInflater.from(requireContext()).inflate(R.layout.item_story, container, false)
                            val ivUserPhoto = storyView.findViewById<ImageView>(R.id.ivStoryUserPhoto)
                            val tvUserName = storyView.findViewById<TextView>(R.id.tvStoryUserName)
                            val vp = storyView.findViewById<ViewPager2>(R.id.vpStoryImages)
                            val tvText = storyView.findViewById<TextView>(R.id.tvStoryText)

                            tvUserName.text = if (userName.isNotEmpty()) userName else tvViewName.text

                            // Cargar foto del usuario (Base64 o URL)
                            if (userPhoto.isNotEmpty()) {
                                try {
                                    if (userPhoto.startsWith("http://") || userPhoto.startsWith("https://")) {
                                        Glide.with(this@ProfileFragment).load(userPhoto).circleCrop().into(ivUserPhoto)
                                    } else {
                                        val decoded = Base64.decode(userPhoto, Base64.DEFAULT)
                                        val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                        if (bmp != null) ivUserPhoto.setImageBitmap(bmp)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileFragment", "Error cargando foto usuario: ${e.message}")
                                }
                            }

                            // Procesar imágenes: pueden ser Base64 directo o IDs de storyImages
                            if (imageIds.isNotEmpty()) {
                                // Verificar si es Base64 directo o IDs de documentos
                                val firstImage = imageIds[0]
                                if (isBase64(firstImage)) {
                                    // Son Base64 directos, mostrar directamente
                                    android.util.Log.d("ProfileFragment", "Imágenes Base64 directo en documento")
                                    vp.adapter = ImagePagerAdapter(imageIds)
                                } else {
                                    // Son IDs de documentos, cargar de storyImages
                                    android.util.Log.d("ProfileFragment", "IDs de documentos detectados, cargando desde storyImages")
                                    loadStoryImagesFromCollection(imageIds, vp)
                                }
                            }

                            // Texto de la historia (opcional)
                            if (text.isNotBlank()) {
                                tvText.visibility = View.VISIBLE
                                tvText.text = text
                            } else {
                                tvText.visibility = View.GONE
                            }

                            container.addView(storyView)

                        } catch (e: Exception) {
                            android.util.Log.w("ProfileFragment", "Error renderizando historia: ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileFragment", "Error cargando historias: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "loadStories error: ${e.message}")
        }
    }

    // Publicaciones (paginación)
    private fun loadInitialPosts() {
        if (currentUserEmail.isEmpty()) return
        isLoading = true
        noMore = false
        lastVisible = null
        posts.clear()
        val prevSize = posts.size
        posts.clear()
        if (prevSize > 0) adapter?.notifyItemRangeRemoved(0, prevSize)

        db.collection("posts")
            .whereEqualTo("userEmail", currentUserEmail)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    noMore = true
                    tvNoPostsMessage.visibility = View.VISIBLE
                    rvProfilePosts.visibility = View.GONE
                } else {
                    tvNoPostsMessage.visibility = View.GONE
                    rvProfilePosts.visibility = View.VISIBLE
                    lastVisible = snapshot.documents.last()

                    // Ordenar localmente por timestamp descendente
                    val new = snapshot.documents
                        .mapNotNull { doc ->
                            try {
                                val text = doc.getString("text") ?: ""
                                val images = (doc.get("images") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                Triple(Post(images, text), timestamp, doc)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        .sortedByDescending { it.second }
                        .map { it.first }

                    // Actualizar lastVisible al documento más reciente
                    if (new.isNotEmpty()) {
                        lastVisible = snapshot.documents
                            .mapNotNull { doc ->
                                try {
                                    val timestamp = doc.getLong("timestamp") ?: 0L
                                    Pair(doc, timestamp)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            .minByOrNull { it.second }?.first
                    }

                    val start = posts.size
                    posts.addAll(new)
                    adapter?.notifyItemRangeInserted(start, new.size)
                    if (new.size < pageSize) noMore = true
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                android.util.Log.e("ProfileFragment", "Error cargando posts iniciales: ${e.message}")
            }
    }

    private fun loadMorePosts() {
        if (currentUserEmail.isEmpty() || lastVisible == null) return
        isLoading = true

        db.collection("posts")
            .whereEqualTo("userEmail", currentUserEmail)
            .startAfter(lastVisible!!)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    noMore = true
                } else {
                    lastVisible = snapshot.documents.last()

                    // Ordenar localmente por timestamp descendente
                    val new = snapshot.documents
                        .mapNotNull { doc ->
                            try {
                                val text = doc.getString("text") ?: ""
                                val images = (doc.get("images") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                Triple(Post(images, text), timestamp, doc)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        .sortedByDescending { it.second }
                        .map { it.first }

                    // Actualizar lastVisible al documento más reciente
                    if (new.isNotEmpty()) {
                        lastVisible = snapshot.documents
                            .mapNotNull { doc ->
                                try {
                                    val timestamp = doc.getLong("timestamp") ?: 0L
                                    Pair(doc, timestamp)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            .minByOrNull { it.second }?.first
                    }

                    val insertPos = posts.size
                    posts.addAll(new)
                    adapter?.notifyItemRangeInserted(insertPos, new.size)
                    if (new.size < pageSize) noMore = true
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                android.util.Log.e("ProfileFragment", "Error cargando más posts: ${e.message}")
            }
    }

    private fun createInterestChip(text: String): View {
        val chip = TextView(requireContext())
        chip.text = text
        chip.textSize = 12f
        chip.setTextColor(requireContext().getColor(R.color.white))
        chip.setBackgroundResource(R.drawable.interest_chip_background)
        chip.setPadding(16, 8, 16, 8)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = 8
        params.bottomMargin = 8
        chip.layoutParams = params

        return chip
    }

    // Detectar si un string es Base64 o un ID de documento
    private fun isBase64(str: String): Boolean {
        // Base64 típicamente es más largo y contiene / + = caracteres
        // Los IDs de Firestore son más cortos y alfanuméricos
        if (str.length > 200) return true // Probablemente Base64
        if (str.contains("/") || str.contains("+") || str.contains("=")) return true // Caracteres típicos de Base64
        return false
    }

    // Cargar imágenes desde collection storyImages usando sus IDs
    private fun loadStoryImagesFromCollection(imageIds: List<String>, viewPager: ViewPager2) {
        val loadedImages = mutableMapOf<Int, String>() // index -> base64
        var remaining = imageIds.size

        for (imgId in imageIds) {
            db.collection("storyImages").document(imgId)
                .get()
                .addOnSuccessListener { doc ->
                    try {
                        val imageBase64 = doc.getString("image")
                        val index = doc.getLong("index")?.toInt() ?: -1

                        if (!imageBase64.isNullOrEmpty()) {
                            if (index >= 0) {
                                loadedImages[index] = imageBase64
                            } else {
                                // Si no tiene índice, agregarlo al final
                                loadedImages[loadedImages.size] = imageBase64
                            }
                            android.util.Log.d("ProfileFragment", "Imagen cargada en índice $index")
                        } else {
                            android.util.Log.w("ProfileFragment", "Campo 'image' vacío en storyImage $imgId")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileFragment", "Error extrayendo imagen de storyImages: ${e.message}")
                    }

                    remaining--
                    if (remaining == 0) {
                        // Todas las imágenes cargadas, ordenar y establecer adapter
                        android.util.Log.d("ProfileFragment", "Todas las imágenes cargadas: ${loadedImages.size} de ${imageIds.size}")
                        if (loadedImages.isNotEmpty()) {
                            // Ordenar por clave (índice) y crear lista ordenada
                            val sortedImages = loadedImages.toSortedMap().values.toList()
                            android.util.Log.d("ProfileFragment", "Estableciendo adapter con ${sortedImages.size} imágenes ordenadas")
                            viewPager.adapter = ImagePagerAdapter(sortedImages)
                        } else {
                            android.util.Log.w("ProfileFragment", "No se pudieron cargar imágenes de storyImages")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileFragment", "Error cargando imagen $imgId: ${e.message}")
                    remaining--
                    if (remaining == 0 && loadedImages.isNotEmpty()) {
                        val sortedImages = loadedImages.toSortedMap().values.toList()
                        viewPager.adapter = ImagePagerAdapter(sortedImages)
                    }
                }
        }
    }
}
