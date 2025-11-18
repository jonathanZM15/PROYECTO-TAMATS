package com.example.myapplication.ui.explore

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import android.annotation.SuppressLint

class KnowFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_know, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = arguments?.getString("user_email")
        Log.d("KnowFragment", "Cargando perfil de usuario: $userEmail")

        if (!userEmail.isNullOrEmpty()) {
            loadUserProfile(view, userEmail!!)
        } else {
            Log.e("KnowFragment", "Email de usuario no proporcionado")
        }

        val btnBack = view.findViewById<ImageButton>(R.id.btnBackFromKnow)
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadUserProfile(view: View, email: String) {
        // Buscar en collection userProfiles primero
        db.collection("userProfiles")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { userProfileDocs ->
                if (userProfileDocs.documents.isNotEmpty()) {
                    val userDoc = userProfileDocs.documents[0]
                    displayUserProfile(view, userDoc.data ?: emptyMap())
                } else {
                    // Si no está, buscar en usuarios
                    db.collection("usuarios")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { usuariosDocs ->
                            if (usuariosDocs.documents.isNotEmpty()) {
                                val userDoc = usuariosDocs.documents[0]
                                displayUserProfile(view, userDoc.data ?: emptyMap())
                            } else {
                                Log.e("KnowFragment", "Usuario no encontrado: $email")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("KnowFragment", "Error buscando en usuarios: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("KnowFragment", "Error buscando en userProfiles: ${e.message}")
            }
    }

    private fun displayUserProfile(view: View, userData: Map<String, Any>) {
        try {
            val name = userData["name"]?.toString() ?: "Usuario"
            val age = userData["age"]?.toString() ?: "N/A"
            val birthDate = userData["birthDate"]?.toString() ?: "No especificada"
            val city = userData["city"]?.toString() ?: "No especificada"
            val description = userData["description"]?.toString() ?: "Sin descripción"
            var photoBase64 = userData["photo"]?.toString()
            val interests = (userData["interests"] as? List<*>) ?: emptyList<Any>()
            val email = userData["email"]?.toString() ?: ""

            val tvUserNameProfile = view.findViewById<TextView>(R.id.tvUserNameProfile)
            tvUserNameProfile?.text = name

            val tvAgeProfile = view.findViewById<TextView>(R.id.tvAgeProfile)
            tvAgeProfile?.text = if (age != "N/A") getString(R.string.age_years, age) else getString(R.string.age_not_specified)

            val tvBirthDateProfile = view.findViewById<TextView>(R.id.tvBirthDateProfile)
            tvBirthDateProfile?.text = getString(R.string.birthdate_label, birthDate)

            val tvCityProfile = view.findViewById<TextView>(R.id.tvCityProfile)
            tvCityProfile?.text = getString(R.string.city_with_pin, city)

            val tvDescriptionProfile = view.findViewById<TextView>(R.id.tvDescriptionProfile)
            tvDescriptionProfile?.text = description

            val ivUserPhotoKnow = view.findViewById<ImageView>(R.id.ivUserPhotoKnow)
            if (!photoBase64.isNullOrEmpty()) {
                try {
                    // Si la cadena incluye 'data:image/...;base64,' la recortamos
                    if (photoBase64.contains(",")) {
                        val parts = photoBase64.split(",", limit = 2)
                        if (parts.size == 2 && parts[0].startsWith("data:")) {
                            photoBase64 = parts[1]
                        }
                    }

                    val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(this)
                            .load(bmp)
                            .centerCrop()
                            .into(ivUserPhotoKnow)
                        Log.d("KnowFragment", "Foto cargada para: $name")
                    }
                } catch (e: Exception) {
                    Log.w("KnowFragment", "Error cargando foto: ${e.message}")
                }
            }

            ivUserPhotoKnow?.setOnClickListener {
                showPhotoDialog(photoBase64, name)
            }

            val llInterestsContainer = view.findViewById<LinearLayout>(R.id.llInterestsContainer)
            llInterestsContainer?.removeAllViews()

            if (interests.isNotEmpty()) {
                val tvInterestsLabel = TextView(requireContext())
                tvInterestsLabel.text = getString(R.string.interests_label)
                tvInterestsLabel.textSize = 16f
                tvInterestsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.tamats_pink))
                tvInterestsLabel.setPadding(16, 16, 16, 8)
                llInterestsContainer?.addView(tvInterestsLabel)

                for (interest in interests) {
                    val interestText = interest?.toString() ?: continue
                    val tvInterest = TextView(requireContext())
                    tvInterest.text = getString(R.string.interest_bullet, interestText)
                    tvInterest.textSize = 14f
                    tvInterest.setTextColor(android.graphics.Color.WHITE)
                    tvInterest.setPadding(32, 8, 16, 8)
                    llInterestsContainer?.addView(tvInterest)
                }
            }

            val llPublicationsContainer = view.findViewById<LinearLayout>(R.id.llPublicationsContainer)
            llPublicationsContainer?.removeAllViews()

            // Cargar publicaciones del usuario
            // IMPORTANTE: Usar userEmail del argumento en lugar del email de userData para evitar discrepancias
            val emailParaBuscar = if (!userEmail.isNullOrEmpty()) userEmail!! else email
            Log.d("KnowFragment", "displayUserProfile: usando email = '$emailParaBuscar' (userEmail arg: $userEmail, userData email: $email)")

            if (emailParaBuscar.isNotEmpty()) {
                Log.d("KnowFragment", "Llamando loadUserPublications con email: $emailParaBuscar")
                loadUserPublications(emailParaBuscar, llPublicationsContainer)
            } else {
                Log.e("KnowFragment", "Email vacío en displayUserProfile")
            }

            val rvUserStories = view.findViewById<RecyclerView>(R.id.rvUserStories)
            rvUserStories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            // Evitar el warning 'No adapter attached; skipping layout' asignando un adapter vacío inicialmente
            rvUserStories.adapter = StoryAdapter(emptyList(), this)

            // Cargar historias del usuario
            loadUserStories(email, rvUserStories, view)

        } catch (e: Exception) {
            Log.e("KnowFragment", "Error mostrando perfil: ${e.message}", e)
        }
    }

    private fun loadUserPublications(email: String, container: LinearLayout?) {
        if (container == null) {
            Log.e("KnowFragment", "loadUserPublications: container es null")
            return
        }

        Log.d("KnowFragment", "===== CARGANDO PUBLICACIONES =====")
        Log.d("KnowFragment", "Email a buscar: '$email'")

        // Buscar en la colección 'stories' donde se guardan las publicaciones
        db.collection("stories")
            .get()
            .addOnSuccessListener { allDocs ->
                Log.d("KnowFragment", "Total de stories en BD: ${allDocs.size()}")

                // Filtrar manualmente para encontrar publicaciones del usuario
                val userPosts = allDocs.documents.filter { doc ->
                    val docEmail = doc.get("userEmail")?.toString() ?: ""
                    docEmail.equals(email, ignoreCase = true)
                }.sortedByDescending { doc ->
                    (doc.get("timestamp") as? Number)?.toLong() ?: 0L
                }

                Log.d("KnowFragment", "Publicaciones encontradas para usuario: ${userPosts.size}")

                container.removeAllViews()

                if (userPosts.isEmpty()) {
                    Log.d("KnowFragment", "Sin publicaciones para mostrar")
                    val tvNoPost = TextView(requireContext())
                    tvNoPost.text = getString(R.string.no_publications_message)
                    tvNoPost.textSize = 14f
                    tvNoPost.setTextColor(android.graphics.Color.GRAY)
                    tvNoPost.gravity = Gravity.CENTER
                    tvNoPost.setPadding(16, 32, 16, 32)
                    container.addView(tvNoPost)
                    return@addOnSuccessListener
                }

                // Mostrar header de publicaciones
                val tvPublicationsLabel = TextView(requireContext())
                tvPublicationsLabel.text = "Publicaciones"
                tvPublicationsLabel.textSize = 16f
                tvPublicationsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.tamats_pink))
                tvPublicationsLabel.setPadding(16, 16, 16, 8)
                container.addView(tvPublicationsLabel)

                // Mostrar cada publicación
                for (postDoc in userPosts) {
                    try {
                        val postData = postDoc.data ?: continue
                        val postText = postData["text"]?.toString() ?: ""
                        val postImages = postData["images"] as? List<*>

                        Log.d("KnowFragment", "Mostrando publicación: text='$postText', imágenes=${postImages?.size ?: 0}")

                        val postView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_publication_know, container, false)

                        val tvPostText = postView.findViewById<TextView>(R.id.tvPostText)
                        tvPostText?.text = postText

                        val llPostImagesContainer = postView.findViewById<LinearLayout>(R.id.llPostImagesContainer)
                        llPostImagesContainer?.removeAllViews()

                        if (!postImages.isNullOrEmpty()) {
                            for (imageUrl in postImages) {
                                val ivImage = ImageView(requireContext())
                                ivImage.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    400
                                ).apply {
                                    setMargins(0, 8, 0, 8)
                                }
                                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                                ivImage.contentDescription = "Imagen de publicación"

                                try {
                                    val imageStr = imageUrl?.toString() ?: ""
                                    if (imageStr.isNotEmpty()) {
                                        // Verificar si es Base64 o una URL
                                        if (imageStr.startsWith("http://") || imageStr.startsWith("https://")) {
                                            // Es una URL, cargar directamente
                                            Glide.with(this)
                                                .load(imageStr)
                                                .centerCrop()
                                                .into(ivImage)
                                        } else {
                                            // Es Base64, decodificar a Bitmap
                                            val decoded = Base64.decode(imageStr, Base64.DEFAULT)
                                            val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                            if (bitmap != null) {
                                                Glide.with(this)
                                                    .load(bitmap)
                                                    .centerCrop()
                                                    .into(ivImage)
                                                Log.d("KnowFragment", "Imagen Base64 cargada correctamente")
                                            } else {
                                                Log.w("KnowFragment", "Error decodificando imagen Base64")
                                                ivImage.setImageResource(android.R.drawable.ic_menu_report_image)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("KnowFragment", "Error cargando imagen de publicación: ${e.message}")
                                    ivImage.setImageResource(android.R.drawable.ic_menu_report_image)
                                }

                                llPostImagesContainer.addView(ivImage)
                            }
                        }

                        container.addView(postView)
                    } catch (e: Exception) {
                        Log.e("KnowFragment", "Error mostrando publicación: ${e.message}")
                    }
                }

                Log.d("KnowFragment", "Publicaciones renderizadas exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("KnowFragment", "Error cargando publicaciones de Firebase: ${e.message}")
            }
    }


    private fun loadUserStories(email: String, recyclerView: RecyclerView, parentView: View) {
        val tvNoStories = parentView.findViewById<TextView>(R.id.tvNoStories)
        Log.d("KnowFragment", "loadUserStories: iniciando consulta por userEmail=$email")
        db.collection("stories")
            .whereEqualTo("userEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { storyDocs ->
                Log.d("KnowFragment", "loadUserStories: consulta inicial devuelve ${storyDocs.size()} documentos")
                val stories = mutableListOf<Story>()

                for (doc in storyDocs.documents) {
                    val data = doc.data ?: continue
                    val images = (data["images"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val userName = data["userName"]?.toString() ?: "Usuario"
                    val userPhoto = data["userPhoto"]?.toString() ?: ""

                    // Extraer timestamp de forma robusta
                    val tsAny = data["timestamp"]
                    var tsSeconds: Long? = null
                    try {
                        if (tsAny != null) {
                            when (tsAny) {
                                is Timestamp -> tsSeconds = tsAny.seconds
                                is Number -> tsSeconds = tsAny.toLong()
                                is String -> tsAny.toLongOrNull()
                                else -> {
                                    // tipo no reconocido
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Log.w("KnowFragment", "loadUserStories: no pudo extraer timestamp del doc ${doc.id}: ${ex.message}")
                    }

                    stories.add(Story(images, tsSeconds, userName, userPhoto))
                }

                // asegurar orden descendente por timestamp (nulls al final)
                val sorted = stories.sortedWith(compareByDescending<Story> { it.tsSeconds ?: Long.MIN_VALUE })

                Log.d("KnowFragment", "loadUserStories: historias procesadas (inicial): ${sorted.size}")

                if (sorted.isEmpty()) {
                    tvNoStories?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvNoStories?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    val adapter = StoryAdapter(sorted, this@KnowFragment)
                    recyclerView.adapter = adapter
                    Log.d("KnowFragment", "loadUserStories: adaptador asignado con ${sorted.size} historias para $email")
                }
            }
            .addOnFailureListener { e ->
                Log.e("KnowFragment", "Error cargando historias (inicial): ${e.message}")
                // Fallback: si falla por índice, hacemos la consulta sin orderBy y ordenamos localmente
                val msg = e.message ?: ""
                if (msg.contains("requires an index", ignoreCase = true) || msg.contains("requires an index", ignoreCase = true) || msg.contains("The query requires an index", ignoreCase = true) || msg.contains("index", ignoreCase = true)) {
                    Log.d("KnowFragment", "loadUserStories: usando fallback sin orderBy por userEmail=$email")
                    db.collection("stories")
                        .whereEqualTo("userEmail", email)
                        .get()
                        .addOnSuccessListener { docs ->
                            Log.d("KnowFragment", "loadUserStories (fallback): devuelve ${docs.size()} documentos")
                            val stories = mutableListOf<Story>()
                            for (doc in docs.documents) {
                                val data = doc.data ?: continue
                                val images = (data["images"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val userName = data["userName"]?.toString() ?: "Usuario"
                                val userPhoto = data["userPhoto"]?.toString() ?: ""
                                var tsSeconds: Long? = null
                                val tsAny = data["timestamp"]
                                try {
                                    if (tsAny != null) {
                                        when (tsAny) {
                                            is Timestamp -> tsSeconds = tsAny.seconds
                                            is Number -> tsSeconds = tsAny.toLong()
                                            is String -> tsAny.toLongOrNull()
                                            else -> {}
                                        }
                                    }
                                } catch (ex: Exception) {
                                    Log.w("KnowFragment", "loadUserStories(fallback): error extrayendo timestamp doc ${doc.id}: ${ex.message}")
                                }
                                stories.add(Story(images, tsSeconds, userName, userPhoto))
                            }
                            val sorted = stories.sortedWith(compareByDescending<Story> { it.tsSeconds ?: Long.MIN_VALUE })
                            Log.d("KnowFragment", "loadUserStories (fallback): historias procesadas: ${sorted.size}")
                            if (sorted.isEmpty()) {
                                tvNoStories?.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                            } else {
                                tvNoStories?.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                recyclerView.adapter = StoryAdapter(sorted, this@KnowFragment)
                                Log.d("KnowFragment", "loadUserStories (fallback): adaptador asignado con ${sorted.size} historias para $email")
                            }
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("KnowFragment", "Fallback error cargando historias: ${e2.message}")
                            tvNoStories?.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                } else {
                    tvNoStories?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            }
    }

    @SuppressLint("InflateParams")
    private fun showPhotoDialog(photoBase64: String?, name: String) {
        if (photoBase64.isNullOrEmpty()) return

        var p = photoBase64
        if (p.contains(",")) {
            val parts = p.split(",", limit = 2)
            if (parts.size == 2 && parts[0].startsWith("data:")) p = parts[1]
        }

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo, null, false)
        dialog.setContentView(dialogView)

        val ivPhoto = dialogView.findViewById<ImageView>(R.id.ivPhotoDialog)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)

        try {
            val decoded = Base64.decode(p, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            if (bmp != null) {
                Glide.with(this)
                    .load(bmp)
                    .centerInside()
                    .into(ivPhoto)
                ivPhoto?.contentDescription = getString(R.string.photo_of, name)
            }
        } catch (e: Exception) {
            Log.e("KnowFragment", "Error mostrando foto en diálogo: ${e.message}")
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showFullscreenImageDialog(imageData: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_fullscreen_image, null, false)
        dialog.setContentView(dialogView)

        val ivFullscreenImage = dialogView.findViewById<ImageView>(R.id.ivFullscreenImage)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseFullscreenImage)

        try {
            // Detectar si es Base64 o URL
            val isBase64 = imageData.length > 500 || imageData.contains(",") || !imageData.startsWith("http")

            if (isBase64) {
                // Es Base64 - decodificar
                var base64String = imageData

                if (base64String.contains(",")) {
                    val parts = base64String.split(",", limit = 2)
                    if (parts.size == 2) {
                        base64String = parts[1]
                    }
                }

                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap != null) {
                    Glide.with(this)
                        .load(bitmap)
                        .centerInside()
                        .into(ivFullscreenImage)
                    Log.d("KnowFragment", "Imagen Base64 mostrada en pantalla completa")
                }
            } else {
                // Es URL
                Glide.with(this)
                    .load(imageData)
                    .centerInside()
                    .into(ivFullscreenImage)
                Log.d("KnowFragment", "Imagen URL mostrada en pantalla completa")
            }
        } catch (e: Exception) {
            Log.e("KnowFragment", "Error mostrando imagen en pantalla completa: ${e.message}")
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        fun newInstance(userEmail: String): KnowFragment {
            return KnowFragment().apply {
                arguments = Bundle().apply {
                    putString("user_email", userEmail)
                }
            }
        }
    }

    // Modelo simple de historia
    private data class Story(
        val images: List<String>,
        val tsSeconds: Long? = null,
        val userName: String = "",
        val userPhoto: String = ""
    )

    // Adapter horizontal que muestra tarjetas de historias (cada tarjeta usa item_story.xml)
    private class StoryAdapter(
        private val stories: List<Story>,
        private val fragment: KnowFragment
    ) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false)
            return StoryViewHolder(view, fragment)
        }

        override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
            holder.bind(stories[position])
        }

        override fun getItemCount(): Int = stories.size

        class StoryViewHolder(itemView: View, private val fragment: KnowFragment) : RecyclerView.ViewHolder(itemView) {
            private val vpImages = itemView.findViewById<ViewPager2>(R.id.vpStoryImages)
            private val llDots = itemView.findViewById<LinearLayout>(R.id.vpStoryDots)
            private val tvUserName = itemView.findViewById<TextView>(R.id.tvStoryUserName)
            private val ivUserPhoto = itemView.findViewById<ImageView>(R.id.ivStoryUserPhoto)
            private val tvStoryText = itemView.findViewById<TextView>(R.id.tvStoryText)

            fun bind(story: Story) {
                // Si el story tiene imágenes, inicializar ViewPager2
                val images = story.images

                // Mostrar nombre y foto del usuario
                tvUserName.text = story.userName

                // Cargar foto del usuario si existe
                if (story.userPhoto.isNotEmpty()) {
                    try {
                        // Si es Base64 con prefijo, recortarlo
                        var photoData = story.userPhoto
                        if (photoData.contains(",")) {
                            val parts = photoData.split(",", limit = 2)
                            if (parts.size == 2 && parts[0].startsWith("data:")) {
                                photoData = parts[1]
                            }
                        }

                        // Decodificar Base64 si es necesario
                        if (photoData.length > 100) { // Probablemente sea Base64
                            val decoded = Base64.decode(photoData, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            if (bmp != null) {
                                ivUserPhoto.setImageBitmap(bmp)
                            }
                        } else {
                            // Si es URL, usar Glide
                            Glide.with(itemView.context)
                                .load(story.userPhoto)
                                .centerCrop()
                                .into(ivUserPhoto)
                        }
                    } catch (e: Exception) {
                        Log.w("KnowFragment", "Error cargando foto del usuario: ${e.message}")
                        ivUserPhoto.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    ivUserPhoto.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Validar que hay imágenes antes de mostrar ViewPager2
                if (images.isNotEmpty()) {
                    vpImages.adapter = ImagesPagerAdapter(images) { imageData ->
                        fragment.showFullscreenImageDialog(imageData)
                    }
                    setupDots(images.size)

                    // Actualizar dots según página
                    vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            updateDots(position)
                        }
                    })
                } else {
                    // Si no hay imágenes, mostrar placeholder
                    vpImages.visibility = View.GONE
                    llDots.visibility = View.GONE
                }

                // Ocultar texto si no hay
                if (tvStoryText.text.isNullOrEmpty()) tvStoryText.visibility = View.GONE
            }

            private fun setupDots(count: Int) {
                llDots.removeAllViews()
                if (count <= 1) {
                    llDots.visibility = View.GONE
                    return
                }
                llDots.visibility = View.VISIBLE
                for (i in 0 until count) {
                    val dot = View(itemView.context)
                    val size = (6 * itemView.resources.displayMetrics.density).toInt()
                    val params = LinearLayout.LayoutParams(size, size)
                    params.setMargins(6, 0, 6, 0)
                    dot.layoutParams = params
                    dot.setBackgroundResource(R.drawable.dot_indicator)
                    dot.alpha = if (i == 0) 1f else 0.4f
                    llDots.addView(dot)
                }
            }

            private fun updateDots(selected: Int) {
                for (i in 0 until llDots.childCount) {
                    llDots.getChildAt(i).alpha = if (i == selected) 1f else 0.4f
                }
            }
        }
    }

    // Adapter para las imágenes dentro del ViewPager2 (item simple con ImageView)
    private class ImagesPagerAdapter(
        private val images: List<String>,
        private val onImageClick: (String) -> Unit
    ) : RecyclerView.Adapter<ImagesPagerAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(images[position])
        }

        override fun getItemCount(): Int = images.size

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)

            fun bind(imageData: String) {
                Log.d("ImagesPagerAdapter", "Cargando imagen de longitud: ${imageData.length}")

                if (imageData.isEmpty()) {
                    Log.w("ImagesPagerAdapter", "Imagen vacía")
                    ivImage.setImageResource(R.drawable.ic_image_placeholder)
                    return
                }

                try {
                    // Detectar si es Base64 o URL
                    val isBase64 = imageData.length > 500 || imageData.contains(",") || !imageData.startsWith("http")

                    if (isBase64) {
                        // Es Base64 - decodificar directamente
                        Log.d("ImagesPagerAdapter", "Detectado como Base64, decodificando...")

                        var base64String = imageData

                        // Si tiene prefijo data URI, recortarlo
                        if (base64String.contains(",")) {
                            val parts = base64String.split(",", limit = 2)
                            if (parts.size == 2) {
                                base64String = parts[1]
                            }
                        }

                        // Decodificar Base64 a Bitmap
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (bitmap != null) {
                            Glide.with(itemView.context)
                                .load(bitmap)
                                .centerCrop()
                                .into(ivImage)
                            Log.d("ImagesPagerAdapter", "Imagen Base64 decodificada y cargada correctamente")
                        } else {
                            Log.e("ImagesPagerAdapter", "No se pudo decodificar el bitmap")
                            ivImage.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    } else {
                        // Es URL - usar Glide normalmente
                        Log.d("ImagesPagerAdapter", "Detectado como URL, cargando con Glide: $imageData")

                        Glide.with(itemView.context)
                            .load(imageData)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(ivImage)
                        Log.d("ImagesPagerAdapter", "Imagen URL cargada con Glide")
                    }
                } catch (e: Exception) {
                    Log.e("ImagesPagerAdapter", "Error cargando imagen: ${e.message}", e)
                    ivImage.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Añadir click listener para mostrar imagen en pantalla completa
                ivImage.setOnClickListener {
                    onImageClick(imageData)
                }
            }
        }
    }
}
