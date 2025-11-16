package com.example.myapplication.ui.explore

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Match
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MatchesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _matches = MutableLiveData<List<Match>>()
    val matches: LiveData<List<Match>> = _matches

    private var matchesListener: ListenerRegistration? = null

    fun loadMatches(currentUserEmail: String) {
        // Escuchar en tiempo real los matches para el usuario actual
        matchesListener = db.collection("matches")
            .whereEqualTo("toUserEmail", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MatchesViewModel", "Error cargando matches: ${e.message}", e)
                    _matches.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val matchList = mutableListOf<Match>()
                    for (doc in snapshot.documents) {
                        try {
                            val match = doc.toObject(Match::class.java)?.copy(id = doc.id)
                            // Solo incluir matches que NO han sido rechazados
                            if (match != null && !match.rejected) {
                                matchList.add(match)
                            }
                        } catch (ex: Exception) {
                            Log.e("MatchesViewModel", "Error parseando match: ${ex.message}", ex)
                        }
                    }
                    // Ordenar por timestamp descendente (más recientes primero)
                    matchList.sortByDescending { it.timestamp.toDate() }
                    _matches.value = matchList
                    Log.d("MatchesViewModel", "Matches cargados: ${matchList.size}")
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        matchesListener?.remove()
    }
}

