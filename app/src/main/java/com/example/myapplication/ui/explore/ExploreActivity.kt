package com.example.myapplication.ui.explore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExploreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos temporalmente el layout fijo para validar el centrado de FABs
        setContentView(R.layout.activity_explore)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_explore
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> true
                R.id.nav_matches -> {
                    // TODO: navegar a Matches
                    true
                }
                R.id.nav_chats -> {
                    // TODO: navegar a Chats
                    true
                }
                R.id.nav_profile -> {
                    // TODO: navegar a Perfil
                    true
                }
                else -> false
            }
        }
    }
}
