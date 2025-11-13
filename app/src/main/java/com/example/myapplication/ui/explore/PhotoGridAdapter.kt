package com.example.myapplication.ui.explore

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

/**
 * Adapter de fotos que usa AsyncListDiffer (DiffUtil) para aplicar cambios eficientemente.
 *
 * Comportamiento:
 * - Siempre presenta exactamente [maxPhotos] celdas para mantener la estabilidad del layout.
 * - Si no hay fotos, muestra un botón visible SOLO en la posición 0 y placeholders invisibles en las demás celdas.
 * - Si hay fotos, muestra las fotos en las posiciones 0..(n-1) y botones "Añadir" en las posiciones restantes.
 *
 * Uso: llamar a [submitList] desde la Activity con la lista actualizada de Uris (por ejemplo: selectedUris.toList()).
 *
 * @param maxPhotos número máximo de fotos soportadas (y número de celdas a renderizar)
 * @param onAddClick callback cuando el usuario pulsa el botón 'Añadir' en una celda vacía
 * @param onRemoveClick callback cuando el usuario pulsa el botón eliminar en una celda con foto
 */
class PhotoGridAdapter(
    private val maxPhotos: Int,
    private val onAddClick: (position: Int) -> Unit,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder>() {

    // DiffUtil para Uri (compara por toString())
    private val diffCallback = object : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem.toString() == newItem.toString()
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    init {
        // Mejora la estabilidad: RecyclerView puede mantener mejor referencias si los ids son estables
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val list = differ.currentList
        return if (position < list.size) list[position].hashCode().toLong() else ("empty_slot_$position").hashCode().toLong()
    }

    /** Envía una nueva lista de fotos al adapter (copia inmutable recomendada). */
    fun submitList(list: List<Uri>) {
        differ.submitList(list.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_grid, parent, false)
        return PhotoViewHolder(v)
    }

    override fun getItemCount(): Int = maxPhotos

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val items = differ.currentList
        if (items.isEmpty()) {
            if (position == 0) {
                holder.bindEmpty()
                holder.btnRemove.visibility = View.GONE
                holder.btnAddSlot.visibility = View.VISIBLE
                holder.btnAddSlot.setOnClickListener { onAddClick(0) }
                holder.itemView.isClickable = true
            } else {
                holder.bindEmpty()
                holder.btnRemove.visibility = View.GONE
                holder.btnAddSlot.visibility = View.INVISIBLE
                holder.itemView.isClickable = false
            }
        } else {
            if (position < items.size) {
                val uri = items[position]
                holder.bindPhoto(uri)
                holder.btnRemove.visibility = View.VISIBLE
                holder.btnRemove.setOnClickListener { onRemoveClick(position) }
                holder.btnAddSlot.visibility = View.GONE
                holder.itemView.isClickable = false
            } else {
                holder.bindEmpty()
                holder.btnRemove.visibility = View.GONE
                holder.btnAddSlot.visibility = View.VISIBLE
                holder.btnAddSlot.setOnClickListener { onAddClick(position) }
                holder.itemView.isClickable = true
            }
        }
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.container)
        val btnAddSlot: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAddSlot)
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivGridPhoto)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemovePhoto)

        fun bindPhoto(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .override(256, 256)
                .disallowHardwareConfig()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.sample_profile)
                .error(R.drawable.sample_profile)
                .into(ivPhoto)

            ivPhoto.visibility = View.VISIBLE
            btnAddSlot.visibility = View.GONE
            container.setBackgroundResource(R.drawable.bg_rounded_item)
        }

        fun bindEmpty() {
            ivPhoto.setImageDrawable(null)
            ivPhoto.visibility = View.INVISIBLE
            btnAddSlot.visibility = View.VISIBLE
            container.setBackgroundResource(R.drawable.bg_add_photo_slot)
        }
    }
}
