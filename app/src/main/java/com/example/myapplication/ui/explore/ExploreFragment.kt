package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class ExploreFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var currentView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentView = view
        loadUserProfiles(view)
    }

    private fun loadUserProfiles(view: View) {
        val llPosts = view.findViewById<LinearLayout>(R.id.llPostsContainer)
        val allProfiles = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

        // Obtener el email del usuario actual
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""
        Log.d("ExploreFragment", "Email del usuario actual: $currentUserEmail")

        // Cargar desde userProfiles primero
        db.collection("userProfiles")
            .get()
            .addOnSuccessListener { userProfileSnapshots ->
                Log.d("ExploreFragment", "userProfiles: encontrados ${userProfileSnapshots.size()} documentos")
                allProfiles.addAll(userProfileSnapshots.documents)

                // Luego cargar desde usuarios
                db.collection("usuarios")
                    .get()
                    .addOnSuccessListener { usuariosSnapshots ->
                        Log.d("ExploreFragment", "usuarios: encontrados ${usuariosSnapshots.size()} documentos")

                        // Agregar usuarios que no estén ya en userProfiles
                        val userProfileEmails = allProfiles.mapNotNull { it.data?.get("email")?.toString() }.toSet()
                        for (doc in usuariosSnapshots.documents) {
                            val email = doc.data?.get("email")?.toString() ?: ""
                            if (email !in userProfileEmails) {
                                allProfiles.add(doc)
                            }
                        }

                        // Filtrar el perfil del usuario actual
                        val filteredProfiles = allProfiles.filter {
                            it.data?.get("email")?.toString() != currentUserEmail
                        }
                        Log.d("ExploreFragment", "Perfiles después de filtrar el usuario actual: ${filteredProfiles.size}")

                        llPosts.removeAllViews()
                        displayProfiles(filteredProfiles, llPosts)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ExploreFragment", "Error cargando desde usuarios: ${e.message}")

                        // Filtrar el perfil del usuario actual incluso en caso de error
                        val filteredProfiles = allProfiles.filter {
                            it.data?.get("email")?.toString() != currentUserEmail
                        }
                        llPosts.removeAllViews()
                        displayProfiles(filteredProfiles, llPosts)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ExploreFragment", "Error cargando desde userProfiles: ${e.message}")
                // Intentar solo desde usuarios como fallback
                db.collection("usuarios")
                    .get()
                    .addOnSuccessListener { usuariosSnapshots ->
                        Log.d("ExploreFragment", "Fallback: usuarios encontrados ${usuariosSnapshots.size()}")

                        // Filtrar el perfil del usuario actual
                        val filteredProfiles = usuariosSnapshots.documents.filter {
                            it.data?.get("email")?.toString() != currentUserEmail
                        }

                        llPosts.removeAllViews()
                        displayProfiles(filteredProfiles, llPosts)
                    }
                    .addOnFailureListener { e2 ->
                        Log.e("ExploreFragment", "Error en fallback: ${e2.message}")
                    }
            }
    }

    private fun displayProfiles(documents: List<com.google.firebase.firestore.DocumentSnapshot>, llPosts: LinearLayout) {
        try {
            if (documents.isEmpty()) {
                Log.d("ExploreFragment", "No hay perfiles disponibles")
                val emptyView = TextView(requireContext())
                emptyView.text = "No hay perfiles disponibles aún.\nCompleta tu perfil para que otros te vean."
                emptyView.setTextColor(android.graphics.Color.WHITE)
                emptyView.textSize = 16f
                emptyView.gravity = android.view.Gravity.CENTER
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(32, 100, 32, 0)
                emptyView.layoutParams = params
                llPosts.addView(emptyView)
                return
            }

            for (doc in documents) {
                try {
                    val profileData = doc.data ?: continue

                    // Extraer datos del perfil - con valores por defecto si no existen
                    val userName = profileData["name"]?.toString() ?: "Usuario"
                    val userEmail = profileData["email"]?.toString() ?: ""
                    val userCity = profileData["city"]?.toString() ?: "Ciudad no especificada"
                    val userDescription = profileData["description"]?.toString() ?: "Sin descripción"
                    val photoBase64 = profileData["photo"]?.toString()

                    Log.d("ExploreFragment", "Cargando perfil: $userName (ciudad: $userCity)")

                    // Inflar el layout de tarjeta de perfil
                    val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_user_profile_card, llPosts, false)

                    // Configurar nombre de usuario con listener para ir al perfil
                    val tvUserName = itemView.findViewById<TextView>(R.id.tvUserName)
                    tvUserName.text = userName
                    tvUserName.setOnClickListener {
                        if (userEmail.isNotEmpty()) {
                            openUserProfile(userEmail)
                        }
                    }
                    // Hacer el nombre visible como enlace
                    tvUserName.setTextColor(resources.getColor(R.color.tamats_pink, null))
                    tvUserName.isClickable = true
                    tvUserName.isFocusable = true

                    // Configurar ciudad
                    val tvUserCity = itemView.findViewById<TextView>(R.id.tvUserCity)
                    tvUserCity.text = userCity

                    // Configurar descripción
                    val tvUserDescription = itemView.findViewById<TextView>(R.id.tvUserDescription)
                    tvUserDescription.text = userDescription

                    // Cargar foto de perfil si existe
                    val ivUserProfilePhoto = itemView.findViewById<ImageView>(R.id.ivUserProfilePhoto)
                    if (!photoBase64.isNullOrEmpty()) {
                        try {
                            val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            if (bmp != null) {
                                Glide.with(this@ExploreFragment)
                                    .load(bmp)
                                    .centerCrop()
                                    .into(ivUserProfilePhoto)
                                Log.d("ExploreFragment", "Foto cargada para: $userName")
                            } else {
                                Log.w("ExploreFragment", "No se pudo decodificar foto de perfil para $userName")
                            }
                        } catch (e: Exception) {
                            Log.w("ExploreFragment", "Error cargando foto de perfil: ${e.message}")
                        }
                    } else {
                        Log.d("ExploreFragment", "Sin foto para: $userName")
                    }

                    llPosts.addView(itemView)
                } catch (e: Exception) {
                    Log.e("ExploreFragment", "Error al renderizar perfil ${doc.id}: ${e.message}", e)
                }
            }

            Log.d("ExploreFragment", "Total de perfiles cargados: ${llPosts.childCount}")

            // Hacer scroll al inicio
            try {
                val svPosts = currentView?.findViewById<ScrollView>(R.id.svPosts)
                svPosts?.post { svPosts.fullScroll(View.FOCUS_UP) }
            } catch (_: Exception) {
                // ignore
            }
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Error general al cargar perfiles: ${e.message}", e)
        }
    }

    private fun openUserProfile(userEmail: String) {
        val knowFragment = KnowFragment.newInstance(userEmail)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, knowFragment)
            .addToBackStack("know_${userEmail}")
            .commit()
    }
}

