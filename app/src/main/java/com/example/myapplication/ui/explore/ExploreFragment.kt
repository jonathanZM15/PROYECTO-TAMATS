package com.example.myapplication.ui.explore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.util.AgeCalculator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp

class ExploreFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var viewModel: ExploreViewModel
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var searchContainer: View // Contenedor de la barra de búsqueda
    private var allFilteredProfiles: List<DocumentSnapshot> = emptyList()
    private var isSearchBarVisible = true // Estado de visibilidad de la barra

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

        // Inicializar SearchView y su contenedor
        searchView = view.findViewById(R.id.svSearchExplore)
        searchContainer = view.findViewById(R.id.searchBarContainer)
        setupSearchView()

        profileAdapter = ProfileAdapter({ email ->
            openUserProfile(email)
        }, viewModel) {
            // Callback cuando cambian los favoritos
            recargarYReorganizarPerfiles()
        }
        recyclerView.adapter = profileAdapter

        // Configurar paginación y ocultamiento de barra al hacer scroll
        setupScrollListeners()

        // Cargar perfiles solo si no se han cargado antes
        if (!viewModel.profilesLoaded) {
            loadUserProfiles()
        } else if (viewModel.cachedProfiles.isNotEmpty()) {
            // Si ya están cargados, mostrarlos desde el caché
            viewModel.resetPagination()
            loadNextBatch()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // No es necesario hacer nada al presionar enter
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtrar en tiempo real mientras se escribe
                val query = newText?.trim() ?: ""
                Log.d("ExploreFragment", "onQueryTextChange: '$query'")
                filterProfiles(query)
                return true
            }
        })

        // Configurar opciones adicionales del SearchView
        searchView.isIconified = false
        searchView.clearFocus()
    }

    private fun filterProfiles(query: String) {
        val trimmedQuery = query.trim()
        Log.d("ExploreFragment", "Filtrando perfiles con query: '$trimmedQuery', total perfiles disponibles: ${allFilteredProfiles.size}")

        val filteredProfiles = if (trimmedQuery.isEmpty()) {
            allFilteredProfiles
        } else {
            val queryLower = trimmedQuery.lowercase()
            allFilteredProfiles.filter { profile ->
                try {
                    val name = (profile.data?.get("name")?.toString() ?: "").lowercase()
                    val email = (profile.data?.get("email")?.toString() ?: "").lowercase()
                    val city = (profile.data?.get("city")?.toString() ?: "").lowercase()

                    // Buscar en nombre, email o ciudad
                    val matches = name.contains(queryLower) || email.contains(queryLower) || city.contains(queryLower)

                    if (matches) {
                        Log.d("ExploreFragment", "✓ Coincidencia encontrada: ${profile.data?.get("name")}")
                    }

                    matches
                } catch (e: Exception) {
                    Log.w("ExploreFragment", "Error al filtrar perfil: ${e.message}")
                    false
                }
            }
        }

        Log.d("ExploreFragment", "Resultados filtrados: ${filteredProfiles.size} perfiles de ${allFilteredProfiles.size} totales")

        // Aplicar filtro y mostrar
        viewModel.resetPagination()
        viewModel.cachedProfiles = filteredProfiles
        profileAdapter.clearItems()

        if (filteredProfiles.isNotEmpty()) {
            loadNextBatch()
        } else {
            Log.d("ExploreFragment", "No hay perfiles que coincidan con la búsqueda: '$trimmedQuery'")
        }
    }

    private fun setupScrollListeners() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItems = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                // PAGINACIÓN: Si faltan 5 items para llegar al final y no estamos cargando, cargar más
                if (lastVisible >= totalItems - 5 && !viewModel.isLoadingMore &&
                    viewModel.currentPage * ExploreViewModel.PROFILES_PER_PAGE < viewModel.totalProfiles) {
                    Log.d("ExploreFragment", "Detectado scroll hacia abajo, cargando siguiente lote...")
                    loadNextBatch()
                }

                // OCULTAR/MOSTRAR BARRA DE BÚSQUEDA
                if (dy > 20 && isSearchBarVisible) {
                    // Scroll hacia ABAJO - OCULTAR barra de búsqueda
                    hideSearchBar()
                } else if (dy < -20 && !isSearchBarVisible) {
                    // Scroll hacia ARRIBA - MOSTRAR barra de búsqueda
                    showSearchBar()
                }
            }
        })
    }

    private fun hideSearchBar() {
        // Ocultar la barra de búsqueda con animación suave
        searchContainer.animate()
            .translationY(-searchContainer.height.toFloat())
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                searchContainer.visibility = View.GONE
                isSearchBarVisible = false
            }
            .start()
        Log.d("ExploreFragment", "Ocultando barra de búsqueda")
    }

    private fun showSearchBar() {
        // Mostrar la barra de búsqueda con animación suave
        searchContainer.visibility = View.VISIBLE
        searchContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                isSearchBarVisible = true
            }
            .start()
        Log.d("ExploreFragment", "Mostrando barra de búsqueda")
    }

    private fun recargarYReorganizarPerfiles() {
        Log.d("ExploreFragment", "Reorganizando perfiles debido a cambio de favoritos")

        // Obtener los emails de favoritos actuales del ViewModel
        val favoriteEmails = viewModel.favoriteEmails
        Log.d("ExploreFragment", "Favoritos actuales: $favoriteEmails")

        // Usar los perfiles que ya están en caché
        val allProfiles = viewModel.cachedProfiles

        // Separar favoritos de no favoritos
        val favorites = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
        val nonFavorites = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

        for (profile in allProfiles) {
            val email = profile.data?.get("email")?.toString() ?: ""
            if (email in favoriteEmails) {
                favorites.add(profile)
                Log.d("ExploreFragment", "Favorito encontrado: $email")
            } else {
                nonFavorites.add(profile)
            }
        }

        // Crear lista reorganizada: favoritos primero, luego otros
        val reorganizedProfiles = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
        reorganizedProfiles.addAll(favorites)
        reorganizedProfiles.addAll(nonFavorites)

        Log.d("ExploreFragment", "Perfiles reorganizados. Favoritos: ${favorites.size}, Otros: ${nonFavorites.size}")

        // Actualizar el ViewModel
        viewModel.cachedProfiles = reorganizedProfiles

        // Ejecutar en el siguiente frame para evitar el error de scroll
        recyclerView.post {
            // Resetear paginación y mostrar desde el inicio
            viewModel.resetPagination()
            profileAdapter.clearItems()

            // Cargar el primer lote con los perfiles reorganizados
            loadNextBatch()

            Log.d("ExploreFragment", "Lista recargada y reorganizada")
        }
    }

    private fun loadUserProfiles() {
        val allProfiles = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

        // Obtener el email del usuario actual
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""
        Log.d("ExploreFragment", "Email del usuario actual: $currentUserEmail")

        // Primero cargar datos del usuario actual para obtener edad e intereses
        loadCurrentUserData(currentUserEmail) {
            // Cargar los perfiles rechazados y favoritos por el usuario actual
            loadRejectedProfiles(currentUserEmail) { rejectedEmails ->
                Log.d("ExploreFragment", "Perfiles rechazados por este usuario: ${rejectedEmails.size}")

                loadFavoriteProfiles(currentUserEmail) { favoriteProfiles ->
                    Log.d("ExploreFragment", "Perfiles favoritos encontrados: ${favoriteProfiles.size}")

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

                                    // Filtrar el perfil del usuario actual y los perfiles rechazados
                                    val filteredProfiles = allProfiles.filter {
                                        val email = it.data?.get("email")?.toString() ?: ""
                                        email != currentUserEmail && email !in rejectedEmails
                                    }
                                    Log.d("ExploreFragment", "Perfiles después de filtrar el usuario actual: ${filteredProfiles.size}")

                                    // Ordenar y filtrar los perfiles
                                    val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                                    displayProfiles(sortedProfiles, favoriteProfiles)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ExploreFragment", "Error cargando desde usuarios: ${e.message}")

                                    // Filtrar el perfil del usuario actual y perfiles rechazados incluso en caso de error
                                    val filteredProfiles = allProfiles.filter {
                                        val email = it.data?.get("email")?.toString() ?: ""
                                        email != currentUserEmail && email !in rejectedEmails
                                    }
                                    val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                                    displayProfiles(sortedProfiles, favoriteProfiles)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExploreFragment", "Error cargando desde userProfiles: ${e.message}")
                            // Intentar solo desde usuarios como fallback
                            db.collection("usuarios")
                                .get()
                                .addOnSuccessListener { usuariosSnapshots ->
                                    Log.d("ExploreFragment", "Fallback: usuarios encontrados ${usuariosSnapshots.size()}")

                                    // Filtrar el perfil del usuario actual y perfiles rechazados
                                    val filteredProfiles = usuariosSnapshots.documents.filter {
                                        val email = it.data?.get("email")?.toString() ?: ""
                                        email != currentUserEmail && email !in rejectedEmails
                                    }
                                    val sortedProfiles = sortAndFilterProfiles(filteredProfiles)
                                    displayProfiles(sortedProfiles, favoriteProfiles)
                                }
                                .addOnFailureListener { e2 ->
                                    Log.e("ExploreFragment", "Error en fallback: ${e2.message}")
                                }
                        }
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

    private fun loadRejectedProfiles(currentUserEmail: String, callback: (Set<String>) -> Unit) {
        db.collection("rejections")
            .whereEqualTo("fromUserEmail", currentUserEmail)
            .get()
            .addOnSuccessListener { rejectionDocs ->
                val rejectedEmails = mutableSetOf<String>()
                for (doc in rejectionDocs.documents) {
                    val rejectedEmail = doc.data?.get("toUserEmail")?.toString()
                    if (!rejectedEmail.isNullOrEmpty()) {
                        rejectedEmails.add(rejectedEmail)
                    }
                }
                Log.d("ExploreFragment", "Se encontraron ${rejectedEmails.size} perfiles rechazados")
                callback(rejectedEmails)
            }
            .addOnFailureListener { e ->
                Log.e("ExploreFragment", "Error cargando perfiles rechazados: ${e.message}")
                callback(emptySet())
            }
    }

    private fun loadFavoriteProfiles(currentUserEmail: String, callback: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit) {
        db.collection("favorites")
            .whereEqualTo("fromUserEmail", currentUserEmail)
            .orderBy("position")
            .get()
            .addOnSuccessListener { favoriteDocs ->
                val favoriteSnapshots = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                val favoritePositions = mutableMapOf<String, Int>()  // Guardar posiciones para reordenar después

                // Para cada favorito, obtener el perfil completo
                var completedCount = 0
                if (favoriteDocs.documents.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                for ((index, favoriteDoc) in favoriteDocs.documents.withIndex()) {
                    val toUserEmail = favoriteDoc.data?.get("toUserEmail")?.toString() ?: ""
                    val position = favoriteDoc.data?.get("position") as? Number ?: index

                    Log.d("ExploreFragment", "Cargando favorito en posición $position: $toUserEmail")

                    // Buscar en userProfiles primero
                    db.collection("userProfiles")
                        .whereEqualTo("email", toUserEmail)
                        .get()
                        .addOnSuccessListener { userProfileDocs ->
                            if (userProfileDocs.documents.isNotEmpty()) {
                                favoriteSnapshots.add(userProfileDocs.documents[0])
                                favoritePositions[toUserEmail] = position.toInt()
                            } else {
                                // Buscar en usuarios como fallback
                                db.collection("usuarios")
                                    .whereEqualTo("email", toUserEmail)
                                    .get()
                                    .addOnSuccessListener { usuariosDocs ->
                                        if (usuariosDocs.documents.isNotEmpty()) {
                                            favoriteSnapshots.add(usuariosDocs.documents[0])
                                            favoritePositions[toUserEmail] = position.toInt()
                                        }
                                    }
                            }

                            completedCount++
                            if (completedCount == favoriteDocs.documents.size) {
                                // Reordenar los favoritos por posición para garantizar el orden correcto
                                val orderedFavorites = favoriteSnapshots.sortedBy { doc ->
                                    val email = doc.data?.get("email")?.toString() ?: ""
                                    favoritePositions[email] ?: 999
                                }
                                Log.d("ExploreFragment", "Favoritos cargados y ordenados: ${orderedFavorites.size}")
                                callback(orderedFavorites)
                            }
                        }
                        .addOnFailureListener { e ->
                            completedCount++
                            if (completedCount == favoriteDocs.documents.size) {
                                // Reordenar incluso si hay errores
                                val orderedFavorites = favoriteSnapshots.sortedBy { doc ->
                                    val email = doc.data?.get("email")?.toString() ?: ""
                                    favoritePositions[email] ?: 999
                                }
                                callback(orderedFavorites)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExploreFragment", "Error cargando favoritos: ${e.message}")
                callback(emptyList())
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

    private fun displayProfiles(documents: List<com.google.firebase.firestore.DocumentSnapshot>, favorites: List<com.google.firebase.firestore.DocumentSnapshot>) {
        try {
            Log.d("ExploreFragment", "displayProfiles llamado con ${documents.size} perfiles y ${favorites.size} favoritos")

            // Crear lista ordenada: favoritos primero, luego el resto
            val orderedProfiles = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

            // Agregar favoritos primero (en el orden que se cargaron)
            orderedProfiles.addAll(favorites)

            // Agregar los perfiles que no son favoritos
            val favoriteEmails = favorites.mapNotNull { it.data?.get("email")?.toString() }.toSet()
            val nonFavorites = documents.filter { profile ->
                val email = profile.data?.get("email")?.toString() ?: ""
                email !in favoriteEmails
            }
            orderedProfiles.addAll(nonFavorites)

            // Guardar emails de favoritos en el ViewModel para acceso rápido
            viewModel.favoriteEmails = favoriteEmails
            Log.d("ExploreFragment", "Emails de favoritos guardados: $favoriteEmails")

            // Guardar en cache del ViewModel para usar después
            viewModel.cachedProfiles = orderedProfiles
            viewModel.totalProfiles = orderedProfiles.size
            viewModel.resetPagination()

            // Guardar todos los perfiles para la búsqueda
            allFilteredProfiles = orderedProfiles

            if (orderedProfiles.isEmpty()) {
                Log.d("ExploreFragment", "No hay perfiles disponibles")
                profileAdapter.clearItems()
                return
            }

            Log.d("ExploreFragment", "Perfiles cargados correctamente. Total: ${orderedProfiles.size} (${favorites.size} favoritos)")

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

                    // Calcular edad desde birthDate si existe, si no usar age directamente
                    val birthDate = data["birthDate"]?.toString()
                    val age = if (!birthDate.isNullOrEmpty()) {
                        AgeCalculator.calculateAge(birthDate)
                    } else {
                        data["age"]?.toString()?.toIntOrNull() ?: 0
                    }

                    val item = ProfileItem(
                        documentSnapshot = doc,
                        name = data["name"]?.toString() ?: "Usuario",
                        email = data["email"]?.toString() ?: "",
                        city = data["city"]?.toString() ?: "Ciudad no especificada",
                        description = data["description"]?.toString() ?: "Sin descripción",
                        photoBase64 = data["photo"]?.toString(),
                        age = age
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

