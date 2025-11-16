package com.example.myapplication.ui.explore

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot

class ExploreViewModel : ViewModel() {

    // Datos del usuario actual
    var currentUserAge: Int = 0
    var currentUserInterests: List<String> = emptyList()

    // Cache de perfiles
    var cachedProfiles: List<DocumentSnapshot> = emptyList()

    // Estado de carga
    var profilesLoaded: Boolean = false

    // Paginación
    companion object {
        const val PROFILES_PER_PAGE = 5
    }

    var currentPage: Int = 0
    var totalProfiles: Int = 0
    var isLoadingMore: Boolean = false


    fun resetPagination() {
        currentPage = 0
        isLoadingMore = false
    }
}

