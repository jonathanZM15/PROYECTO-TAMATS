package com.example.myapplication.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.admin.adapters.AdminMessagesAdapter
import com.example.myapplication.admin.viewmodels.AdminMessagesViewModel
import com.google.firebase.auth.FirebaseAuth
import android.graphics.BitmapFactory
import android.util.Base64

class AdminMessagesFragment : Fragment() {

    private lateinit var messagesViewModel: AdminMessagesViewModel
    private lateinit var messagesAdapter: AdminMessagesAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvOtherUserName: TextView
    private lateinit var ivOtherUserPhoto: ImageView
    private lateinit var btnBack: ImageButton

    private var chatId: String = ""
    private var otherUserEmail: String = ""
    private var otherUserName: String = ""
    private var otherUserPhoto: String = ""
    private var currentUserEmail: String = ""
    private var currentUserName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener datos del bundle
        chatId = arguments?.getString("chatId") ?: ""
        otherUserEmail = arguments?.getString("otherUserEmail") ?: ""
        otherUserName = arguments?.getString("otherUserName") ?: ""
        otherUserPhoto = arguments?.getString("otherUserPhoto") ?: ""

        // Obtener email del admin actual
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Admin"

        if (chatId.isEmpty()) {
            Log.e("AdminMessagesFragment", "Chat ID no proporcionado")
            return
        }

        // Inicializar vistas
        rvMessages = view.findViewById(R.id.rvAdminMessages)
        etMessage = view.findViewById(R.id.etAdminMessage)
        btnSend = view.findViewById(R.id.btnAdminSendMessage)
        tvOtherUserName = view.findViewById(R.id.tvAdminOtherUserName)
        ivOtherUserPhoto = view.findViewById(R.id.ivAdminOtherUserPhoto)
        btnBack = view.findViewById(R.id.btnAdminBackMessages)

        // Configurar RecyclerView
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        messagesViewModel = ViewModelProvider(this).get(AdminMessagesViewModel::class.java)
        messagesAdapter = AdminMessagesAdapter(currentUserEmail)
        rvMessages.adapter = messagesAdapter

        // Configurar header
        tvOtherUserName.text = otherUserName
        if (!otherUserPhoto.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(otherUserPhoto, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                if (bmp != null) {
                    Glide.with(this)
                        .load(bmp)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(ivOtherUserPhoto)
                }
            } catch (e: Exception) {
                Log.w("AdminMessagesFragment", "Error cargando foto: ${e.message}")
                ivOtherUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            ivOtherUserPhoto.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Botón de retroceso
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Botón de enviar
        btnSend.setOnClickListener {
            sendMessage()
        }

        // Cargar mensajes
        messagesViewModel.loadMessages(chatId)

        // Observar cambios en mensajes
        messagesViewModel.messages.observe(viewLifecycleOwner) { messages ->
            messagesAdapter.setMessages(messages)
            // Scroll automático al último mensaje
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val messageContent = etMessage.text.toString().trim()

        if (messageContent.isEmpty()) {
            Toast.makeText(requireContext(), "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        messagesViewModel.sendMessage(
            chatId = chatId,
            senderEmail = currentUserEmail,
            senderName = currentUserName,
            content = messageContent,
            onSuccess = {
                etMessage.text.clear()
                Log.d("AdminMessagesFragment", "Mensaje enviado")
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Error enviando mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdminMessagesFragment", "Error: ${e.message}")
            }
        )
    }
}

