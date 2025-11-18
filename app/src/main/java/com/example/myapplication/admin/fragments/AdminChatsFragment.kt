package com.example.myapplication.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.admin.adapters.AdminChatsAdapter
import com.example.myapplication.admin.viewmodels.AdminChatsViewModel
import com.example.myapplication.model.Chat
import com.google.firebase.auth.FirebaseAuth

class AdminChatsFragment : Fragment() {

    private lateinit var adminChatsViewModel: AdminChatsViewModel
    private lateinit var adminChatsAdapter: AdminChatsAdapter
    private lateinit var rvChats: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var searchView: SearchView
    private var allChats: List<Chat> = emptyList()
    private var currentUserEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener email del admin actual
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (currentUserEmail.isEmpty()) {
            Log.e("AdminChatsFragment", "No se pudo obtener el email del admin")
            return
        }

        // Inicializar ViewModel
        adminChatsViewModel = ViewModelProvider(this).get(AdminChatsViewModel::class.java)

        // Configurar RecyclerView
        rvChats = view.findViewById(R.id.rvAdminChats)
        emptyStateLayout = view.findViewById(R.id.llEmptyStateAdminChats)
        searchView = view.findViewById(R.id.svSearchAdminChats)

        rvChats.layoutManager = LinearLayoutManager(requireContext())

        // Configurar adaptador
        adminChatsAdapter = AdminChatsAdapter(
            onChatClick = { chat ->
                openAdminMessagesScreen(chat)
            },
            onLoadMore = {
                adminChatsViewModel.loadMoreChats(currentUserEmail)
            }
        )

        rvChats.adapter = adminChatsAdapter

        // Configurar SearchView
        setupSearchView()

        // Cargar chats
        adminChatsViewModel.loadSupportChats(currentUserEmail)

        // Observar cambios en los chats
        adminChatsViewModel.adminChats.observe(viewLifecycleOwner) { chats ->
            allChats = chats
            if (chats.isEmpty()) {
                adminChatsAdapter.setChats(emptyList())
            } else {
                adminChatsAdapter.setChats(chats)
            }
            updateEmptyState(chats.isNotEmpty())
        }

        // Observar estado de carga
        adminChatsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                adminChatsAdapter.setLoading(true)
            } else {
                adminChatsAdapter.setLoading(false)
            }
        }

        // Observar resultados de búsqueda
        adminChatsViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adminChatsAdapter.setChats(results)
            updateEmptyState(results.isNotEmpty())
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    adminChatsAdapter.setChats(allChats)
                    updateEmptyState(allChats.isNotEmpty())
                } else {
                    adminChatsViewModel.searchChats(currentUserEmail, newText)
                }
                return true
            }
        })
    }

    private fun updateEmptyState(hasChats: Boolean) {
        emptyStateLayout.visibility = if (hasChats) View.GONE else View.VISIBLE
        rvChats.visibility = if (hasChats) View.VISIBLE else View.GONE
    }

    private fun openAdminMessagesScreen(chat: Chat) {
        val fragment = AdminMessagesFragment().apply {
            arguments = Bundle().apply {
                putString("chatId", chat.id)
                putString("otherUserEmail", chat.user2Email)
                putString("otherUserName", chat.user2Name)
                putString("otherUserPhoto", chat.user2Photo)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

