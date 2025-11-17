package com.example.myapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.ui.explore.CreatePostFragment
import com.example.myapplication.ui.explore.ExploreFragment
import com.example.myapplication.ui.explore.MatchesFragment
import com.example.myapplication.ui.explore.ChatsFragment
import com.example.myapplication.ui.simulacion.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var ivExplore: ImageView
    private lateinit var tvExplore: TextView
    private lateinit var ivMatches: ImageView
    private lateinit var tvMatches: TextView
    private lateinit var ivChats: ImageView
    private lateinit var tvChats: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var tvProfile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar si el usuario está autenticado
        val prefs = getSharedPreferences("user_data", MODE_PRIVATE)
        val userEmail = prefs.getString("user_email", null)
        if (userEmail.isNullOrEmpty()) {
            // Usuario no autenticado, ir a LoginActivity
            val intent = Intent(this, com.example.myapplication.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }

        // Inicializar referencias de la barra de navegación
        ivExplore = findViewById(R.id.ivNavExplore)
        tvExplore = findViewById(R.id.tvNavExplore)
        ivMatches = findViewById(R.id.ivNavMatches)
        tvMatches = findViewById(R.id.tvNavMatches)
        ivChats = findViewById(R.id.ivNavChats)
        tvChats = findViewById(R.id.tvNavChats)
        ivProfile = findViewById(R.id.ivNavPerfil)
        tvProfile = findViewById(R.id.tvNavPerfil)

        // Configurar listeners de navegación
        setupNavigationListeners()

        // Determinar fragment a mostrar según intent extra (permite abrir CreatePost desde otras Activities)
        val fragmentToOpen = intent?.getStringExtra("fragment") ?: "explore"

        if (savedInstanceState == null) {
            when (fragmentToOpen) {
                "explore" -> {
                    loadFragment(ExploreFragment(), "Explore")
                    setActive(ivExplore, tvExplore)
                }
                "profile" -> {
                    loadFragment(ProfileFragment(), "Profile")
                    setActive(ivProfile, tvProfile)
                }
                "create_post" -> {
                    loadFragment(CreatePostFragment(), "CreatePost")
                    // No cambiar el estado del bottom nav (mantener el item anterior seleccionado)
                }
                else -> {
                    loadFragment(ExploreFragment(), "Explore")
                    setActive(ivExplore, tvExplore)
                }
            }
        }
    }

    private fun setupNavigationListeners() {
        val fabCenter = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabCenter)

        ivExplore.setOnClickListener {
            loadFragment(ExploreFragment(), "Explore")
            updateNavigation(ivExplore, tvExplore)
        }

        ivMatches.setOnClickListener {
            loadFragment(MatchesFragment(), "Matches")
            updateNavigation(ivMatches, tvMatches)
        }

        ivChats.setOnClickListener {
            loadFragment(ChatsFragment(), "Chats")
            updateNavigation(ivChats, tvChats)
        }

        ivProfile.setOnClickListener {
            loadFragment(ProfileFragment(), "Profile")
            updateNavigation(ivProfile, tvProfile)
        }

        fabCenter?.setOnClickListener {
            // Abrir el fragment de creación de post dentro de MainActivity sin recrear la barra
            loadFragment(CreatePostFragment(), "CreatePost")
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        // Reemplazar fragment SIN apilar en backstack para mantener barra de nav fija
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    private fun updateNavigation(activeIv: ImageView, activeTv: TextView) {
        // Desactivar todos
        setInactive(ivExplore, tvExplore)
        setInactive(ivMatches, tvMatches)
        setInactive(ivChats, tvChats)
        setInactive(ivProfile, tvProfile)

        // Activar el seleccionado
        setActive(activeIv, activeTv)
    }

    private fun setActive(iv: ImageView, tv: TextView) {
        val activeColor = ContextCompat.getColor(this, R.color.bottom_nav_active)
        iv.imageTintList = ColorStateList.valueOf(activeColor)
        tv.setTextColor(activeColor)
    }

    private fun setInactive(iv: ImageView, tv: TextView) {
        val inactiveColor = ContextCompat.getColor(this, R.color.bottom_nav_inactive)
        iv.imageTintList = ColorStateList.valueOf(inactiveColor)
        tv.setTextColor(inactiveColor)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Manejar intents entrantes cuando la Activity ya está en foreground
        val fragmentToOpen = intent.getStringExtra("fragment") ?: return
        when (fragmentToOpen) {
            "explore" -> {
                loadFragment(ExploreFragment(), "Explore")
                updateNavigation(ivExplore, tvExplore)
            }
            "profile" -> {
                loadFragment(ProfileFragment(), "Profile")
                updateNavigation(ivProfile, tvProfile)
            }
            "create_post" -> {
                loadFragment(CreatePostFragment(), "CreatePost")
            }
        }
    }
}
