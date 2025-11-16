package com.example.myapplication.ui.explore

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
import com.example.myapplication.model.Chat

class ChatsFragment : Fragment() {

    private lateinit var chatsViewModel: ChatsViewModel
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var rvChats: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var searchView: SearchView
    private var allChats: List<Chat> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar ViewModel
        chatsViewModel = ViewModelProvider(this).get(ChatsViewModel::class.java)

        // Configurar RecyclerView
        rvChats = view.findViewById(R.id.rvChats)
        emptyStateLayout = view.findViewById(R.id.llEmptyStateChats)
        searchView = view.findViewById(R.id.svSearchChats)

        rvChats.layoutManager = LinearLayoutManager(requireContext())

        // Configurar SearchView
        setupSearchView()

        // Obtener email del usuario actual
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        val currentUserEmail = prefs.getString("user_email", "") ?: ""

        chatsAdapter = ChatsAdapter(
            currentUserEmail = currentUserEmail,
            onChatClick = { chat ->
                // Determinar quién es el otro usuario
                val otherUserEmail: String
                val otherUserName: String
                val otherUserPhoto: String

                if (currentUserEmail == chat.user1Email) {
                    // El usuario logeado es user1, pasar info de user2
                    otherUserEmail = chat.user2Email
                    otherUserName = chat.user2Name
                    otherUserPhoto = chat.user2Photo
                } else {
                    // El usuario logeado es user2, pasar info de user1
                    otherUserEmail = chat.user1Email
                    otherUserName = chat.user1Name
                    otherUserPhoto = chat.user1Photo
                }

                openMessagesScreen(chat.id, otherUserEmail, otherUserName, otherUserPhoto)
            }
        )

        rvChats.adapter = chatsAdapter

        if (currentUserEmail.isNotEmpty()) {
            // Cargar chats
            chatsViewModel.loadChats(currentUserEmail)

            // Observar cambios en los chats
            chatsViewModel.chats.observe(viewLifecycleOwner) { chats ->
                allChats = chats
                chatsAdapter.setChats(chats)
                updateEmptyState(chats.isNotEmpty())
            }
        } else {
            Log.e("ChatsFragment", "Email del usuario no encontrado")
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChats(newText ?: "")
                return true
            }
        })
    }

    private fun filterChats(query: String) {
        val filteredChats = if (query.isEmpty()) {
            allChats
        } else {
            allChats.filter { chat ->
                val otherUserName = if (chat.user1Email == getUserEmail()) {
                    chat.user2Name
                } else {
                    chat.user1Name
                }
                otherUserName.contains(query, ignoreCase = true)
            }
        }

        chatsAdapter.setChats(filteredChats)
        updateEmptyState(filteredChats.isNotEmpty())
    }

    private fun getUserEmail(): String {
        val prefs = requireContext().getSharedPreferences("user_data", 0)
        return prefs.getString("user_email", "") ?: ""
    }

    private fun updateEmptyState(hasChats: Boolean) {
        emptyStateLayout.visibility = if (hasChats) View.GONE else View.VISIBLE
        rvChats.visibility = if (hasChats) View.VISIBLE else View.GONE
    }

    private fun openMessagesScreen(
        chatId: String,
        otherUserEmail: String,
        otherUserName: String,
        otherUserPhoto: String
    ) {
        val bundle = Bundle().apply {
            putString("chat_id", chatId)
            putString("other_user_email", otherUserEmail)
            putString("other_user_name", otherUserName)
            // NO pasar otherUserPhoto en el Bundle para evitar TransactionTooLargeException
            // La foto se cargará desde Firebase en MessagesFragment usando el email
        }

        val messagesFragment = MessagesFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, messagesFragment)
            .addToBackStack("chats")
            .commit()
    }
}

