package com.example.myapplication.ui.explore

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class KnowFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_know, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = arguments?.getString("user_email")
        Log.d("KnowFragment", "Cargando perfil de usuario: $userEmail")

        if (!userEmail.isNullOrEmpty()) {
            loadUserProfile(view, userEmail!!)
        } else {
            Log.e("KnowFragment", "Email de usuario no proporcionado")
        }

        val btnBack = view.findViewById<ImageButton>(R.id.btnBackFromKnow)
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadUserProfile(view: View, email: String) {
        // Buscar en collection userProfiles primero
        db.collection("userProfiles")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { userProfileDocs ->
                if (userProfileDocs.documents.isNotEmpty()) {
                    val userDoc = userProfileDocs.documents[0]
                    displayUserProfile(view, userDoc.data ?: emptyMap())
                } else {
                    // Si no está, buscar en usuarios
                    db.collection("usuarios")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { usuariosDocs ->
                            if (usuariosDocs.documents.isNotEmpty()) {
                                val userDoc = usuariosDocs.documents[0]
                                displayUserProfile(view, userDoc.data ?: emptyMap())
                            } else {
                                Log.e("KnowFragment", "Usuario no encontrado: $email")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("KnowFragment", "Error buscando en usuarios: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("KnowFragment", "Error buscando en userProfiles: ${e.message}")
            }
    }

    private fun displayUserProfile(view: View, userData: Map<String, Any>) {
        try {
            val name = userData["name"]?.toString() ?: "Usuario"
            val age = userData["age"]?.toString() ?: "N/A"
            val birthDate = userData["birthDate"]?.toString() ?: "No especificada"
            val city = userData["city"]?.toString() ?: "No especificada"
            val description = userData["description"]?.toString() ?: "Sin descripción"
            var photoBase64 = userData["photo"]?.toString()
            val interests = (userData["interests"] as? List<*>) ?: emptyList<Any>()
            val email = userData["email"]?.toString() ?: ""

            val tvUserNameProfile = view.findViewById<TextView>(R.id.tvUserNameProfile)
            tvUserNameProfile?.text = name

            val tvAgeProfile = view.findViewById<TextView>(R.id.tvAgeProfile)
            tvAgeProfile?.text = if (age != "N/A") "$age años" else "Edad no especificada"

            val tvBirthDateProfile = view.findViewById<TextView>(R.id.tvBirthDateProfile)
            tvBirthDateProfile?.text = "Nacimiento: $birthDate"

            val tvCityProfile = view.findViewById<TextView>(R.id.tvCityProfile)
            tvCityProfile?.text = "📍 $city"

            val tvDescriptionProfile = view.findViewById<TextView>(R.id.tvDescriptionProfile)
            tvDescriptionProfile?.text = description

            val ivUserPhotoKnow = view.findViewById<ImageView>(R.id.ivUserPhotoKnow)
            if (!photoBase64.isNullOrEmpty()) {
                try {
                    // Si la cadena incluye 'data:image/...;base64,' la recortamos
                    if (photoBase64.contains(",")) {
                        val parts = photoBase64.split(",", limit = 2)
                        if (parts.size == 2 && parts[0].startsWith("data:")) {
                            photoBase64 = parts[1]
                        }
                    }

                    val decoded = Base64.decode(photoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(this)
                            .load(bmp)
                            .centerCrop()
                            .into(ivUserPhotoKnow)
                        Log.d("KnowFragment", "Foto cargada para: $name")
                    }
                } catch (e: Exception) {
                    Log.w("KnowFragment", "Error cargando foto: ${e.message}")
                }
            }

            ivUserPhotoKnow?.setOnClickListener {
                showPhotoDialog(photoBase64, name)
            }

            val llInterestsContainer = view.findViewById<LinearLayout>(R.id.llInterestsContainer)
            llInterestsContainer?.removeAllViews()

            if (interests.isNotEmpty()) {
                val tvInterestsLabel = TextView(requireContext())
                tvInterestsLabel.text = "Intereses:"
                tvInterestsLabel.textSize = 16f
                tvInterestsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.tamats_pink))
                tvInterestsLabel.setPadding(16, 16, 16, 8)
                llInterestsContainer?.addView(tvInterestsLabel)

                for (interest in interests) {
                    val interestText = interest?.toString() ?: continue
                    val tvInterest = TextView(requireContext())
                    tvInterest.text = "• $interestText"
                    tvInterest.textSize = 14f
                    tvInterest.setTextColor(android.graphics.Color.WHITE)
                    tvInterest.setPadding(32, 8, 16, 8)
                    llInterestsContainer?.addView(tvInterest)
                }
            }

            val llPublicationsContainer = view.findViewById<LinearLayout>(R.id.llPublicationsContainer)
            llPublicationsContainer?.removeAllViews()

            // Cargar publicaciones del usuario
            if (email.isNotEmpty()) {
                loadUserPublications(email, llPublicationsContainer)
            }

        } catch (e: Exception) {
            Log.e("KnowFragment", "Error mostrando perfil: ${e.message}", e)
        }
    }

    private fun loadUserPublications(email: String, container: LinearLayout?) {
        if (container == null) return

        db.collection("posts")
            .whereEqualTo("userEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { postDocs ->
                container.removeAllViews()

                if (postDocs.isEmpty) {
                    val tvNoPost = TextView(requireContext())
                    tvNoPost.text = "Este usuario aún no ha creado publicaciones"
                    tvNoPost.textSize = 14f
                    tvNoPost.setTextColor(android.graphics.Color.GRAY)
                    tvNoPost.gravity = android.view.Gravity.CENTER
                    tvNoPost.setPadding(16, 32, 16, 32)
                    container.addView(tvNoPost)
                    return@addOnSuccessListener
                }

                val tvPublicationsLabel = TextView(requireContext())
                tvPublicationsLabel.text = "Publicaciones:"
                tvPublicationsLabel.textSize = 16f
                tvPublicationsLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.tamats_pink))
                tvPublicationsLabel.setPadding(16, 16, 16, 8)
                container.addView(tvPublicationsLabel)

                for (postDoc in postDocs.documents) {
                    try {
                        val postData = postDoc.data ?: continue
                        val postText = postData["text"]?.toString() ?: ""
                        val postImages = postData["images"] as? List<*>

                        val postView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_publication_know, container, false)

                        val tvPostText = postView.findViewById<TextView>(R.id.tvPostText)
                        tvPostText?.text = postText

                        val llPostImagesContainer = postView.findViewById<LinearLayout>(R.id.llPostImagesContainer)
                        llPostImagesContainer?.removeAllViews()

                        if (!postImages.isNullOrEmpty()) {
                            for (imageUrl in postImages) {
                                val ivImage = ImageView(requireContext())
                                ivImage.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    400
                                ).apply {
                                    setMargins(0, 8, 0, 8)
                                }
                                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                                ivImage.contentDescription = "Imagen de publicación"

                                Glide.with(this)
                                    .load(imageUrl?.toString() ?: "")
                                    .centerCrop()
                                    .into(ivImage)

                                llPostImagesContainer.addView(ivImage)
                            }
                        }

                        container.addView(postView)
                    } catch (e: Exception) {
                        Log.e("KnowFragment", "Error mostrando publicación: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("KnowFragment", "Error cargando publicaciones: ${e.message}")
            }
    }

    private fun showPhotoDialog(photoBase64: String?, name: String) {
        if (photoBase64.isNullOrEmpty()) return

        var p = photoBase64
        if (p.contains(",")) {
            val parts = p.split(",", limit = 2)
            if (parts.size == 2 && parts[0].startsWith("data:")) p = parts[1]
        }

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo, null, false)
        dialog.setContentView(dialogView)

        val ivPhoto = dialogView.findViewById<ImageView>(R.id.ivPhotoDialog)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)

        try {
            val decoded = Base64.decode(p, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            if (bmp != null) {
                Glide.with(this)
                    .load(bmp)
                    .centerInside()
                    .into(ivPhoto)
                ivPhoto?.contentDescription = "Foto de $name"
            }
        } catch (e: Exception) {
            Log.e("KnowFragment", "Error mostrando foto en diálogo: ${e.message}")
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        fun newInstance(userEmail: String): KnowFragment {
            return KnowFragment().apply {
                arguments = Bundle().apply {
                    putString("user_email", userEmail)
                }
            }
        }
    }
}
