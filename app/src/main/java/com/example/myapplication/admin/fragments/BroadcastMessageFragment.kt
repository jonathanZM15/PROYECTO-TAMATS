package com.example.myapplication.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.admin.viewmodels.AdminMessagesViewModel
import com.example.myapplication.admin.viewmodels.AdminViewModel
import com.google.firebase.auth.FirebaseAuth

class BroadcastMessageFragment : Fragment() {

    private lateinit var messagesViewModel: AdminMessagesViewModel
    private lateinit var adminViewModel: AdminViewModel
    private lateinit var etBroadcastMessage: EditText
    private lateinit var btnSendBroadcast: Button
    private lateinit var btnCancel: ImageButton
    private lateinit var tvRecipientCount: TextView
    private lateinit var progressBar: ProgressBar
    private var currentUserEmail: String = ""
    private var currentUserName: String = ""
    private var userEmailsList: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_broadcast_message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener email del admin actual
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Admin"

        // Inicializar vistas
        etBroadcastMessage = view.findViewById(R.id.etBroadcastMessage)
        btnSendBroadcast = view.findViewById(R.id.btnSendBroadcast)
        btnCancel = view.findViewById(R.id.btnCancelBroadcast)
        tvRecipientCount = view.findViewById(R.id.tvRecipientCount)
        progressBar = view.findViewById(R.id.progressBarBroadcast)

        // Inicializar ViewModels
        messagesViewModel = ViewModelProvider(this).get(AdminMessagesViewModel::class.java)
        adminViewModel = ViewModelProvider(this).get(AdminViewModel::class.java)

        // Cargar usuarios
        adminViewModel.loadUsers()

        // Observar usuarios
        adminViewModel.filteredUsers.observe(viewLifecycleOwner) { users ->
            userEmailsList = users.map { it.email }
            tvRecipientCount.text = "Destinatarios: ${userEmailsList.size} usuarios"
        }

        // Botón enviar
        btnSendBroadcast.setOnClickListener {
            sendBroadcastMessage()
        }

        // Botón cancelar
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun sendBroadcastMessage() {
        val messageContent = etBroadcastMessage.text.toString().trim()

        if (messageContent.isEmpty()) {
            Toast.makeText(requireContext(), "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        if (userEmailsList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay usuarios para enviar el mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSendBroadcast.isEnabled = false

        messagesViewModel.sendBroadcastMessage(
            adminEmail = currentUserEmail,
            adminName = currentUserName,
            content = messageContent,
            userEmails = userEmailsList,
            onSuccess = {
                progressBar.visibility = View.GONE
                btnSendBroadcast.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Mensaje enviado a ${userEmailsList.size} usuarios",
                    Toast.LENGTH_LONG
                ).show()
                etBroadcastMessage.text.clear()
                parentFragmentManager.popBackStack()
            },
            onFailure = { e ->
                progressBar.visibility = View.GONE
                btnSendBroadcast.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Error enviando mensaje: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("BroadcastMessageFragment", "Error: ${e.message}")
            }
        )
    }
}

