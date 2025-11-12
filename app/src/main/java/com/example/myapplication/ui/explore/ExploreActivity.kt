package com.example.myapplication.ui.explore

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R

class ExploreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos temporalmente el layout fijo para validar el centrado de FABs
        setContentView(R.layout.activity_explore)

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
            // Acción del '+' central
            // startActivity(Intent(this, CreateContentActivity::class.java))
        }
    }
}
