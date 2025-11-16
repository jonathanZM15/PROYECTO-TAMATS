package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.google.firebase.firestore.DocumentSnapshot

data class ProfileItem(
    val documentSnapshot: DocumentSnapshot,
    val name: String,
    val email: String,
    val city: String,
    val description: String,
    val photoBase64: String?
)

class ProfileAdapter(private val onProfileClick: (String) -> Unit) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    private val items = mutableListOf<ProfileItem>()

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserCity: TextView = itemView.findViewById(R.id.tvUserCity)
        private val tvUserDescription: TextView = itemView.findViewById(R.id.tvUserDescription)
        private val ivUserProfilePhoto: ImageView = itemView.findViewById(R.id.ivUserProfilePhoto)

        fun bind(profile: ProfileItem) {
            tvUserName.text = profile.name
            tvUserCity.text = profile.city
            tvUserDescription.text = profile.description

            // Click listener para el nombre
            tvUserName.setOnClickListener {
                if (profile.email.isNotEmpty()) {
                    onProfileClick(profile.email)
                }
            }

            // Cargar foto con Glide - con caché y placeholder
            if (!profile.photoBase64.isNullOrEmpty()) {
                try {
                    val decoded = Base64.decode(profile.photoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bmp != null) {
                        Glide.with(itemView.context)
                            .load(bmp)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivUserProfilePhoto)
                    }
                } catch (e: Exception) {
                    Log.w("ProfileAdapter", "Error cargando foto: ${e.message}")
                    ivUserProfilePhoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivUserProfilePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_profile_card, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun addItems(newItems: List<ProfileItem>) {
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
        Log.d("ProfileAdapter", "Agregados ${newItems.size} items. Total: ${items.size}")
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun setItems(newItems: List<ProfileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

