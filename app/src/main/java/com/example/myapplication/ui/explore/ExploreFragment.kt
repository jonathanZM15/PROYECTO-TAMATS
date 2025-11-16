package com.example.myapplication.ui.explore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class ExploreFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var viewModel: ExploreViewModel
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(ExploreViewModel::class.java)

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.rvProfiles)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        profileAdapter = ProfileAdapter { email ->
            openUserProfile(email)
        }
        recyclerView.adapter = profileAdapter

        // Configurar paginación al hacer scroll
        setupPaginationListener()

        // Cargar perfiles solo si no se han cargado antes
        if (!viewModel.profilesLoaded) {
            loadUserProfiles()
        } else if (viewModel.cachedProfiles.isNotEmpty()) {
            // Si ya están cargados, mostrarlos desde el caché
            viewModel.resetPagination()
            loadNextBatch()
        }
    }

    private fun setupPaginationListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItems = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                // Si faltan 5 items para llegar al final y no estamos cargando, cargar más
                if (lastVisible >= totalItems - 5 && !viewModel.isLoadingMore &&
                    viewModel.currentPage * ExploreViewModel.PROFILES_PER_PAGE < viewModel.totalProfiles) {
                    Log.d("ExploreFragment", "Detectado scroll hacia abajo, cargando siguiente lote...")
                    loadNextBatch()
                }
            }
        })
    }

    private fun loadUserProfiles() {
        val allProfiles = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

        // Obtener el email del usuario actual
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""
        Log.d("ExploreFragment", "Email del usuario actual: $currentUserEmail")

        // Primero cargar datos del usuario actual para obtener edad e intereses
        loadCurrentUserData(currentUserEmail) {
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

                            // Ordenar y filtrar los perfiles
                            val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                            displayProfiles(sortedProfiles)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExploreFragment", "Error cargando desde usuarios: ${e.message}")

                            // Filtrar el perfil del usuario actual incluso en caso de error
                            val filteredProfiles = allProfiles.filter {
                                it.data?.get("email")?.toString() != currentUserEmail
                            }
                            val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                            displayProfiles(sortedProfiles)
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
                            val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                            displayProfiles(sortedProfiles)
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("ExploreFragment", "Error en fallback: ${e2.message}")
                        }
                }
        }
    }

    private fun loadCurrentUserData(email: String, callback: () -> Unit) {
        // Buscar datos del usuario actual en userProfiles primero
        db.collection("userProfiles")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.documents.isNotEmpty()) {
                    val userData = docs.documents[0].data ?: emptyMap()
                    extractUserData(userData)
                    callback()
                } else {
                    // Buscar en usuarios como fallback
                    db.collection("usuarios")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { usuariosDocs ->
                            if (usuariosDocs.documents.isNotEmpty()) {
                                val userData = usuariosDocs.documents[0].data ?: emptyMap()
                                extractUserData(userData)
                            }
                            callback()
                        }
                        .addOnFailureListener {
                            Log.e("ExploreFragment", "Error cargando usuario actual: ${it.message}")
                            callback()
                        }
                }
            }
            .addOnFailureListener {
                Log.e("ExploreFragment", "Error cargando usuario actual: ${it.message}")
                callback()
            }
    }

    private fun extractUserData(userData: Map<String, Any>) {
        try {
            // Extraer edad
            val age = (userData["age"] as? Number)?.toInt()
                ?: (userData["age"]?.toString()?.toIntOrNull() ?: 0)
            viewModel.currentUserAge = age

            // Extraer intereses
            @Suppress("UNCHECKED_CAST")
            viewModel.currentUserInterests = (userData["interests"] as? List<String>) ?: emptyList()

            Log.d("ExploreFragment", "Usuario actual - Edad: ${viewModel.currentUserAge}, Intereses: ${viewModel.currentUserInterests}")
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Error extrayendo datos del usuario: ${e.message}")
        }
    }

    private fun sortAndFilterProfiles(
        profiles: List<com.google.firebase.firestore.DocumentSnapshot>
    ): List<com.google.firebase.firestore.DocumentSnapshot> {
        return profiles.sortedWith(compareBy(
            // Primero: ordenar por compatibilidad de intereses (descendente, así que negamos)
            { profile ->
                val profileInterests = getProfileInterests(profile)
                val commonInterests = viewModel.currentUserInterests.intersect(profileInterests.toSet()).size
                -commonInterests // Negamos para ordenar descendente
            },
            // Segundo: ordenar por rango de edad similar (±3 años)
            { profile ->
                val profileAge = getProfileAge(profile)
                val ageDifference = kotlin.math.abs(profileAge - viewModel.currentUserAge)
                val isInRange = ageDifference <= 3
                // Los que están en rango (true = 0) van primero que los que no (false = 1)
                if (isInRange) 0 else 1
            },
            // Tercero: ordenar por fecha de creación (más reciente primero)
            { profile ->
                val timestamp = getProfileTimestamp(profile)
                -timestamp // Negamos para ordenar descendente (más reciente primero)
            }
        ))
    }

    private fun getProfileInterests(profile: com.google.firebase.firestore.DocumentSnapshot): List<String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            (profile.data?.get("interests") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            Log.w("ExploreFragment", "Error extrayendo intereses: ${e.message}")
            emptyList()
        }
    }

    private fun getProfileAge(profile: com.google.firebase.firestore.DocumentSnapshot): Int {
        return try {
            val age = profile.data?.get("age")
            when (age) {
                is Number -> age.toInt()
                is String -> age.toIntOrNull() ?: 0
                else -> 0
            }
        } catch (e: Exception) {
            Log.w("ExploreFragment", "Error extrayendo edad: ${e.message}")
            0
        }
    }

    private fun getProfileTimestamp(profile: com.google.firebase.firestore.DocumentSnapshot): Long {
        return try {
            val timestamp = profile.data?.get("createdAt") as? Timestamp
            timestamp?.seconds ?: 0L
        } catch (e: Exception) {
            Log.w("ExploreFragment", "Error extrayendo timestamp: ${e.message}")
            0L
        }
    }

    private fun displayProfiles(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        try {
            Log.d("ExploreFragment", "displayProfiles llamado con ${documents.size} perfiles")

            // Guardar en cache del ViewModel para usar después
            viewModel.cachedProfiles = documents
            viewModel.totalProfiles = documents.size
            viewModel.resetPagination()

            if (documents.isEmpty()) {
                Log.d("ExploreFragment", "No hay perfiles disponibles")
                profileAdapter.clearItems()
                return
            }

            Log.d("ExploreFragment", "Perfiles cargados correctamente. Total: ${documents.size}")

            // Marcar como cargado en el ViewModel
            viewModel.profilesLoaded = true

            // Cargar solo los primeros 5 perfiles
            Log.d("ExploreFragment", "Llamando a loadNextBatch()...")
            loadNextBatch()
        } catch (e: Exception) {
            Log.e("ExploreFragment", "Error general al cargar perfiles: ${e.message}", e)
        }
    }

    private fun loadNextBatch() {
        if (viewModel.isLoadingMore) {
            Log.d("ExploreFragment", "Ya está cargando un lote, ignorando...")
            return
        }

        val currentIndex = viewModel.currentPage * ExploreViewModel.PROFILES_PER_PAGE
        if (currentIndex >= viewModel.totalProfiles) {
            Log.d("ExploreFragment", "Se han cargado todos los perfiles")
            return
        }

        viewModel.isLoadingMore = true

        try {
            val start = viewModel.currentPage * ExploreViewModel.PROFILES_PER_PAGE
            val end = (start + ExploreViewModel.PROFILES_PER_PAGE).coerceAtMost(viewModel.cachedProfiles.size)

            if (start >= viewModel.cachedProfiles.size) {
                Log.d("ExploreFragment", "No hay más perfiles para cargar")
                viewModel.isLoadingMore = false
                return
            }

            val profiles = mutableListOf<ProfileItem>()
            for (i in start until end) {
                try {
                    val doc = viewModel.cachedProfiles[i]
                    val data = doc.data ?: continue

                    val item = ProfileItem(
                        documentSnapshot = doc,
                        name = data["name"]?.toString() ?: "Usuario",
                        email = data["email"]?.toString() ?: "",
                        city = data["city"]?.toString() ?: "Ciudad no especificada",
                        description = data["description"]?.toString() ?: "Sin descripción",
                        photoBase64 = data["photo"]?.toString()
                    )
                    profiles.add(item)
                } catch (e: Exception) {
                    Log.w("ExploreFragment", "Error procesando perfil: ${e.message}")
                    continue
                }
            }

            if (profiles.isNotEmpty()) {
                Log.d("ExploreFragment", "Cargando lote de página ${viewModel.currentPage + 1}: ${profiles.size} perfiles. Total mostrado: ${profileAdapter.itemCount + profiles.size}")
                profileAdapter.addItems(profiles)
                viewModel.currentPage++
            }
        } finally {
            viewModel.isLoadingMore = false
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

