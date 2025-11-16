package com.example.myapplication.ui.explore

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.RejectionNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RejectionNotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _rejections = MutableLiveData<List<RejectionNotification>>()
    val rejections: LiveData<List<RejectionNotification>> = _rejections

    private var rejectionsListener: ListenerRegistration? = null

    fun loadRejections(currentUserEmail: String) {
        // Escuchar en tiempo real las notificaciones de rechazo para el usuario actual
        rejectionsListener = db.collection("rejectionNotifications")
            .whereEqualTo("toUserEmail", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RejectionViewModel", "Error cargando rechazos: ${e.message}", e)
                    _rejections.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rejectionList = mutableListOf<RejectionNotification>()
                    for (doc in snapshot.documents) {
                        try {
                            val rejection = doc.toObject(RejectionNotification::class.java)?.copy(id = doc.id)
                            if (rejection != null) {
                                rejectionList.add(rejection)
                            }
                        } catch (ex: Exception) {
                            Log.e("RejectionViewModel", "Error parseando rechazo: ${ex.message}", ex)
                        }
                    }
                    // Ordenar por timestamp descendente (más recientes primero)
                    rejectionList.sortByDescending { it.timestamp.toDate() }
                    _rejections.value = rejectionList
                    Log.d("RejectionViewModel", "Rechazos cargados: ${rejectionList.size}")
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        rejectionsListener?.remove()
    }
}

