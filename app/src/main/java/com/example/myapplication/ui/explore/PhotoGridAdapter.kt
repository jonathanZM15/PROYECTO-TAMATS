package com.example.myapplication.ui.explore

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

/**
 * Adapter simplificado para fotos que evita problemas de IndexOutOfBounds.
 */
class PhotoGridAdapter(
    private val maxPhotos: Int,
    private val onAddClick: (position: Int) -> Unit,
    private val onRemoveClick: (position: Int) -> Unit
) : ListAdapter<Uri, RecyclerView.ViewHolder>(UriDiffCallback()) {

    companion object {
        private const val TYPE_PHOTO = 0
        private const val TYPE_ADD_BUTTON = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == currentList.size && currentList.size < maxPhotos) {
            TYPE_ADD_BUTTON
        } else {
            TYPE_PHOTO
        }
    }

    override fun getItemCount(): Int {
        // Si tenemos menos fotos que el máximo, agregamos 1 para el botón de añadir
        return if (currentList.size < maxPhotos) currentList.size + 1 else currentList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD_BUTTON) {
            // Usar item_photo_grid.xml que tiene btnAddSlot
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_grid, parent, false)
            AddPhotoViewHolder(view, onAddClick)
        } else {
            // Usar item_photo_grid.xml para mostrar fotos
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_grid, parent, false)
            PhotoViewHolder(view, onRemoveClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PhotoViewHolder -> {
                if (position < currentList.size) {
                    holder.bind(currentList[position], position)
                }
            }
            is AddPhotoViewHolder -> {
                holder.bind(position)
            }
        }
    }

    inner class PhotoViewHolder(itemView: View, val onRemoveClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivGridPhoto)
        private val btnRemove: android.widget.ImageButton = itemView.findViewById(R.id.btnRemovePhoto)
        private val btnAddSlot: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAddSlot)

        fun bind(uri: Uri, position: Int) {
            // Mostrar foto
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(ivPhoto)

            // Mostrar botón de eliminar
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                onRemoveClick(position)
            }

            // Ocultar botón de agregar
            btnAddSlot.visibility = View.GONE
        }
    }

    inner class AddPhotoViewHolder(itemView: View, val onAddClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val btnAddSlot: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAddSlot)
        private val btnRemove: android.widget.ImageButton = itemView.findViewById(R.id.btnRemovePhoto)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivGridPhoto)

        fun bind(position: Int) {
            // Mostrar botón de agregar
            btnAddSlot.visibility = View.VISIBLE
            btnAddSlot.setOnClickListener {
                onAddClick(position)
            }

            // Ocultar otros elementos
            btnRemove.visibility = View.GONE
            ivPhoto.setImageResource(android.R.color.transparent)
        }
    }

    class UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    }
}
