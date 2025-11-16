package com.example.myapplication.ui.explore

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.MatchAcceptanceNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MatchAcceptanceNotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _notifications = MutableLiveData<List<MatchAcceptanceNotification>>()
    val notifications: LiveData<List<MatchAcceptanceNotification>> = _notifications

    private var notificationsListener: ListenerRegistration? = null

    fun loadNotifications(currentUserEmail: String) {
        // Escuchar en tiempo real las notificaciones de aceptación para el usuario actual
        notificationsListener = db.collection("matchAcceptanceNotifications")
            .whereEqualTo("toUserEmail", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MatchAcceptanceNotificationsViewModel", "Error cargando notificaciones: ${e.message}", e)
                    _notifications.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notificationList = mutableListOf<MatchAcceptanceNotification>()
                    for (doc in snapshot.documents) {
                        try {
                            val notification = doc.toObject(MatchAcceptanceNotification::class.java)?.copy(id = doc.id)
                            if (notification != null) {
                                notificationList.add(notification)
                            }
                        } catch (ex: Exception) {
                            Log.e("MatchAcceptanceNotificationsViewModel", "Error parseando notificación: ${ex.message}", ex)
                        }
                    }
                    // Ordenar por timestamp descendente (más recientes primero)
                    notificationList.sortByDescending { it.acceptedAt.toDate() }
                    _notifications.value = notificationList
                    Log.d("MatchAcceptanceNotificationsViewModel", "Notificaciones cargadas: ${notificationList.size}")
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}

