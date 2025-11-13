package com.example.myapplication.ui.explore

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapplication.R
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

                for (doc in snapshots.documents) {
                    try {
                        val data = doc.data ?: continue
                        val text = data["text"]?.toString() ?: ""
                        val photos = (data["photos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        val authorName = data["authorName"]?.toString() ?: ""

                        // Inflar item_post y rellenar
                        val itemView = LayoutInflater.from(this).inflate(R.layout.item_post, llPosts, false)

                        // vpPhotos: usar ImagePagerAdapter para mostrar fotos
                        val vpPhotos = itemView.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpPhotos)
                        if (photos.isEmpty()) {
                            vpPhotos.visibility = View.GONE
                        } else {
                            vpPhotos.adapter = ImagePagerAdapter(photos)
                            vpPhotos.visibility = View.VISIBLE
                        }

                        val tvAuthor = itemView.findViewById<TextView>(R.id.tvAuthor)
                        if (authorName.isNotEmpty()) {
                            tvAuthor.visibility = View.VISIBLE
                            tvAuthor.text = authorName
                        } else {
                            tvAuthor.visibility = View.GONE
                        }

                        val tvThinking = itemView.findViewById<TextView>(R.id.tvThinking)
                        // Mostrar el texto de la publicación
                        tvThinking.text = text

                        // actualmente mostramos el texto de la publicación en tvThinking

                        llPosts.addView(itemView)
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreActivity", "Error al renderizar post ${doc.id}: ${e.message}", e)
                        // seguir con el siguiente post
                    }
                }
                // Hacer scroll al inicio para mostrar el post más reciente
                try {
                    val svPosts = findViewById<android.widget.ScrollView>(R.id.svPosts)
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
