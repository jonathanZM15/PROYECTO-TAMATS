package com.example.myapplication.ui.explore

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.cloud.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ExploreActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos el layout de Explore que muestra únicamente publicaciones
        setContentView(R.layout.activity_explore)

        // Referencias para poblar posts
        val llPosts = findViewById<LinearLayout>(R.id.llPostsContainer)
        if (llPosts == null) {
            android.util.Log.e("ExploreActivity", "Error: llPostsContainer no encontrado en el layout activity_explore.xml")
            return
        }

        // Escuchar cambios en la colección 'posts' ordenados por timestamp descendente
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    android.util.Log.e("ExploreActivity", "Error escuchando posts: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                // Limpiar contenedor y volver a poblar
                llPosts.removeAllViews()

                if (snapshots.documents.isEmpty()) {
                    // Mostrar mensaje de que no hay más publicaciones
                    val emptyView = LayoutInflater.from(this).inflate(R.layout.item_post, llPosts, false)
                    val tvEmpty = emptyView.findViewById<TextView>(R.id.tvThinking)
                    tvEmpty.text = "No hay más publicaciones"
                    llPosts.addView(emptyView)
                }

                for (doc in snapshots.documents) {
                    try {
                        val data = doc.data ?: continue
                        val text = data["text"]?.toString() ?: ""
                        val photos = (data["photos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        val authorName = data["authorName"]?.toString() ?: ""
                        val authorEmail = data["authorEmail"]?.toString() ?: ""

                        // Inflar item_post y rellenar
                        val itemView = LayoutInflater.from(this).inflate(R.layout.item_post, llPosts, false)

                        // Configurar avatar y nombre del autor
                        val ivAuthorAvatar = itemView.findViewById<ImageView>(R.id.ivAuthorAvatar)
                        val tvAuthor = itemView.findViewById<TextView>(R.id.tvAuthor)

                        tvAuthor.text = authorName.ifEmpty { "Usuario Anónimo" }

                        // Cargar foto de perfil del autor si existe
                        if (authorEmail.isNotEmpty()) {
                            FirebaseService.getUserProfile(authorEmail) { profileData ->
                                runOnUiThread {
                                    if (profileData != null) {
                                        val photoBase64 = profileData["photo"]?.toString()
                                        if (!photoBase64.isNullOrEmpty()) {
                                            try {
                                                val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                                                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                                if (bmp != null) {
                                                    Glide.with(this@ExploreActivity)
                                                        .load(bmp)
                                                        .circleCrop()
                                                        .into(ivAuthorAvatar)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.w("ExploreActivity", "Error cargando foto de autor: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Configurar ViewPager2 para el carrusel de fotos
                        val vpPhotos = itemView.findViewById<ViewPager2>(R.id.vpPhotos)
                        val llPageIndicator = itemView.findViewById<LinearLayout>(R.id.llPageIndicator)

                        if (photos.isEmpty()) {
                            vpPhotos.visibility = View.GONE
                            llPageIndicator.visibility = View.GONE
                        } else {
                            vpPhotos.adapter = ImagePagerAdapter(photos)
                            vpPhotos.visibility = View.VISIBLE

                            // Crear indicadores de página (puntos)
                            llPageIndicator.removeAllViews()
                            if (photos.size > 1) {
                                llPageIndicator.visibility = View.VISIBLE
                                for (i in photos.indices) {
                                    val dot = View(this)
                                    val dotParams = LinearLayout.LayoutParams(12, 12)
                                    dotParams.marginStart = 6
                                    dotParams.marginEnd = 6
                                    dot.layoutParams = dotParams
                                    dot.setBackgroundResource(R.drawable.dot_indicator)

                                    if (i == 0) {
                                        dot.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                                    } else {
                                        dot.setBackgroundColor(ContextCompat.getColor(this, R.color.dot_inactive))
                                    }
                                    llPageIndicator.addView(dot)
                                }

                                // Actualizar indicadores cuando se cambia de página
                                vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        for (j in photos.indices) {
                                            val dot = llPageIndicator.getChildAt(j)
                                            if (j == position) {
                                                dot?.setBackgroundColor(ContextCompat.getColor(this@ExploreActivity, R.color.white))
                                            } else {
                                                dot?.setBackgroundColor(ContextCompat.getColor(this@ExploreActivity, R.color.dot_inactive))
                                            }
                                        }
                                    }
                                })
                            } else {
                                llPageIndicator.visibility = View.GONE
                            }
                        }

                        val tvThinking = itemView.findViewById<TextView>(R.id.tvThinking)
                        tvThinking.text = text

                        llPosts.addView(itemView)
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreActivity", "Error al renderizar post ${doc.id}: ${e.message}", e)
                    }
                }
                // Hacer scroll al inicio para mostrar el post más reciente
                try {
                    val svPosts = findViewById<ScrollView>(R.id.svPosts)
                    svPosts?.post { svPosts.fullScroll(View.FOCUS_UP) }
                } catch (_: Exception) { /* ignore */ }
            }

        // Referencias al bottom nav incluido (image + text por item)
        val ivExplore = findViewById<ImageView>(R.id.ivNavExplore)
        val tvExplore = findViewById<TextView>(R.id.tvNavExplore)

        val ivMatches = findViewById<ImageView>(R.id.ivNavMatches)
        val tvMatches = findViewById<TextView>(R.id.tvNavMatches)

        val ivChats = findViewById<ImageView>(R.id.ivNavChats)
        val tvChats = findViewById<TextView>(R.id.tvNavChats)

        val ivProfile = findViewById<ImageView>(R.id.ivNavPerfil)
        val tvProfile = findViewById<TextView>(R.id.tvNavPerfil)

        val fabCenter = findViewById<com.google.android.material.button.MaterialButton>(R.id.fabCenter)

        fun setActive(iv: ImageView, tv: TextView) {
            val activeColor = ContextCompat.getColor(this, R.color.bottom_nav_active)
            iv.imageTintList = ColorStateList.valueOf(activeColor)
            tv.setTextColor(activeColor)
        }

        fun setInactive(iv: ImageView, tv: TextView) {
            val inactiveColor = ContextCompat.getColor(this, R.color.bottom_nav_inactive)
            iv.imageTintList = ColorStateList.valueOf(inactiveColor)
            tv.setTextColor(inactiveColor)
        }

        // Estado inicial: Explore activo
        setActive(ivExplore, tvExplore)
        setInactive(ivMatches, tvMatches)
        setInactive(ivChats, tvChats)
        setInactive(ivProfile, tvProfile)

        // Listeners para navegación (ajusta Intents a tus Activities reales)
        val goToMatches = {
            // startActivity(Intent(this, MatchesActivity::class.java))
        }
        val goToChats = {
            // startActivity(Intent(this, ChatsActivity::class.java))
        }
        val goToProfile = {
            startActivity(Intent(this, com.example.myapplication.ui.simulacion.ViewProfileActivity::class.java))
        }

        ivExplore.setOnClickListener {
            // ya estás en Explore: tal vez refrescar
            setActive(ivExplore, tvExplore)
            setInactive(ivMatches, tvMatches)
            setInactive(ivChats, tvChats)
            setInactive(ivProfile, tvProfile)
        }

        ivMatches.setOnClickListener {
            goToMatches()
        }

        ivChats.setOnClickListener {
            goToChats()
        }

        ivProfile.setOnClickListener {
            goToProfile()
        }

        fabCenter?.setOnClickListener {
            // Acción del '+' central: abrir CreatePostActivity
            startActivity(Intent(this, CreatePostActivity::class.java))
        }
    }
}
