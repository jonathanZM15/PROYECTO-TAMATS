package com.example.myapplication.ui.explore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class MatchesFragment : Fragment() {

    private lateinit var matchesViewModel: MatchesViewModel
    private lateinit var rejectionsViewModel: RejectionNotificationsViewModel
    private lateinit var acceptanceNotificationsViewModel: MatchAcceptanceNotificationsViewModel
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var rejectionAdapter: RejectionAdapter
    private lateinit var acceptanceNotificationAdapter: MatchAcceptanceNotificationAdapter
    private lateinit var rvMatches: RecyclerView
    private lateinit var rvRejections: RecyclerView
    private lateinit var rvAcceptanceNotifications: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateMatches: LinearLayout
    private lateinit var emptyStateRejections: LinearLayout
    private lateinit var emptyStateAcceptance: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_matches, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar ViewModels
        matchesViewModel = ViewModelProvider(this).get(MatchesViewModel::class.java)
        rejectionsViewModel = ViewModelProvider(this).get(RejectionNotificationsViewModel::class.java)
        acceptanceNotificationsViewModel = ViewModelProvider(this).get(MatchAcceptanceNotificationsViewModel::class.java)

        // Configurar RecyclerViews
        rvMatches = view.findViewById(R.id.rvMatches)
        rvRejections = view.findViewById(R.id.rvRejections)
        rvAcceptanceNotifications = view.findViewById(R.id.rvAcceptanceNotifications)
        emptyStateLayout = view.findViewById(R.id.llEmptyState)
        emptyStateMatches = view.findViewById(R.id.llEmptyStateMatches)
        emptyStateRejections = view.findViewById(R.id.llEmptyStateRejections)
        emptyStateAcceptance = view.findViewById(R.id.llEmptyStateAcceptance)

        rvMatches.layoutManager = LinearLayoutManager(requireContext())
        rvRejections.layoutManager = LinearLayoutManager(requireContext())
        rvAcceptanceNotifications.layoutManager = LinearLayoutManager(requireContext())

        matchAdapter = MatchAdapter(
            onProfileClick = { email ->
                openUserProfile(email)
            },
            onMatchRemoved = {
                updateEmptyStates()
            }
        )

        rejectionAdapter = RejectionAdapter(
            onRejectionRemoved = {
                updateEmptyStates()
            }
        )

        acceptanceNotificationAdapter = MatchAcceptanceNotificationAdapter(
            onNotificationRemoved = {
                updateEmptyStates()
            }
        )

        rvMatches.adapter = matchAdapter
        rvRejections.adapter = rejectionAdapter
        rvAcceptanceNotifications.adapter = acceptanceNotificationAdapter

        // Obtener email del usuario actual
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""

        if (currentUserEmail.isNotEmpty()) {
            // Cargar matches, rechazos y notificaciones de aceptación
            matchesViewModel.loadMatches(currentUserEmail)
            rejectionsViewModel.loadRejections(currentUserEmail)
            acceptanceNotificationsViewModel.loadNotifications(currentUserEmail)

            // Observar cambios en los matches
            matchesViewModel.matches.observe(viewLifecycleOwner) { matches ->
                matchAdapter.setMatches(matches)
                updateEmptyStates()
            }

            // Observar cambios en los rechazos
            rejectionsViewModel.rejections.observe(viewLifecycleOwner) { rejections ->
                rejectionAdapter.setRejections(rejections)
                updateEmptyStates()
            }

            // Observar cambios en las notificaciones de aceptación
            acceptanceNotificationsViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
                acceptanceNotificationAdapter.setNotifications(notifications)
                updateEmptyStates()
            }
        } else {
            Log.e("MatchesFragment", "Email del usuario no encontrado")
        }
    }

    private fun updateEmptyStates() {
        val matchesCount = rvMatches.adapter?.itemCount ?: 0
        val rejectionsCount = rvRejections.adapter?.itemCount ?: 0
        val acceptanceCount = rvAcceptanceNotifications.adapter?.itemCount ?: 0
        val totalCount = matchesCount + rejectionsCount + acceptanceCount

        // Mostrar/ocultar empty states de matches
        emptyStateMatches.visibility = if (matchesCount == 0) View.VISIBLE else View.GONE
        rvMatches.visibility = if (matchesCount == 0) View.GONE else View.VISIBLE

        // Mostrar/ocultar empty states de rechazos
        emptyStateRejections.visibility = if (rejectionsCount == 0) View.VISIBLE else View.GONE
        rvRejections.visibility = if (rejectionsCount == 0) View.GONE else View.VISIBLE

        // Mostrar/ocultar empty states de aceptaciones
        emptyStateAcceptance.visibility = if (acceptanceCount == 0) View.VISIBLE else View.GONE
        rvAcceptanceNotifications.visibility = if (acceptanceCount == 0) View.GONE else View.VISIBLE

        // Mostrar/ocultar empty state general
        emptyStateLayout.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
    }

    private fun openUserProfile(email: String) {
        // Navegar al perfil del usuario
        val bundle = Bundle().apply {
            putString("user_email", email)
            putString("sourceScreen", "matches")
        }

        val knowFragment = KnowFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, knowFragment)
            .addToBackStack("matches")
            .commit()
    }
}

