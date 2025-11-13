package com.example.myapplication.ui.explore

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

/**
 * Adapter que gestiona internamente la lista de URIs y aplica DiffUtil de forma síncrona.
 * Evita condiciones de carrera del AsyncListDiffer de ListAdapter cuando se actualiza
 * la lista rápidamente (p. ej. seleccionar varias fotos a la vez).
 */
class PhotoGridAdapter(
    private val maxPhotos: Int,
    private val onAddClick: (position: Int) -> Unit,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        // Habilitar ids estables para ayudar a RecyclerView a mantener coherencia
        setHasStableIds(true)
    }

    companion object {
        private const val TYPE_PHOTO = 0
        private const val TYPE_ADD_BUTTON = 1
        private const val ADD_BUTTON_ID = Long.MIN_VALUE + 1
    }

    private var items: List<Uri> = emptyList()

    // Exponer la lista actual de forma inmutable (equivalente a ListAdapter.currentList)
    val currentList: List<Uri>
        get() = items

    override fun getItemId(position: Int): Long {
        return if (position == items.size && items.size < maxPhotos) {
            // Id estable para el botón "añadir"
            ADD_BUTTON_ID
        } else if (position < items.size) {
            // Usar hashCode de la Uri como id estable
            items[position].hashCode().toLong()
        } else {
            // Fallback
            position.toLong()
        }
    }

    fun submitList(newList: List<Uri>) {
        // Para evitar inconsistencias entre el número real de items del adapter
        // (que incluye el slot de "Agregar" cuando aplica) y las notificaciones
        // generadas por DiffUtil, actualizamos la lista y refrescamos toda la vista.
        // Las listas son pequeñas (máx $maxPhotos), por lo que el coste es aceptable.
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && items.size < maxPhotos) {
            TYPE_ADD_BUTTON
        } else {
            TYPE_PHOTO
        }
    }

    override fun getItemCount(): Int {
        return if (items.size < maxPhotos) items.size + 1 else items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD_BUTTON) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_grid, parent, false)
            AddPhotoViewHolder(view, onAddClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_grid, parent, false)
            PhotoViewHolder(view, onRemoveClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PhotoViewHolder -> {
                if (position < items.size) holder.bind(items[position])
            }
            is AddPhotoViewHolder -> holder.bind()
        }
    }

    inner class PhotoViewHolder(itemView: View, val onRemoveClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivGridPhoto)
        private val btnRemove: android.widget.ImageButton = itemView.findViewById(R.id.btnRemovePhoto)
        private val btnAddSlot: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAddSlot)

        fun bind(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(ivPhoto)

            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    onRemoveClick(pos)
                }
            }

            btnAddSlot.visibility = View.GONE
        }
    }

    inner class AddPhotoViewHolder(itemView: View, val onAddClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val btnAddSlot: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAddSlot)
        private val btnRemove: android.widget.ImageButton = itemView.findViewById(R.id.btnRemovePhoto)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivGridPhoto)

        fun bind() {
            btnAddSlot.visibility = View.VISIBLE
            btnAddSlot.setOnClickListener {
                var pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) pos = items.size
                if (pos > items.size) pos = items.size
                onAddClick(pos)
            }

            btnRemove.visibility = View.GONE
            ivPhoto.setImageResource(android.R.color.transparent)
        }
    }
}
