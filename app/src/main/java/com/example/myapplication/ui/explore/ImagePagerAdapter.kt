package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.util.ImageOpenHelper

class ImagePagerAdapter(private val images: List<String>) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
        // Asegurar LayoutParams match_parent para ViewPager2
        v.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return ImageViewHolder(v)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageData = images[position]

        // Verificar si es una URL o Base64
        if (imageData.startsWith("http://") || imageData.startsWith("https://") || imageData.startsWith("content://") || imageData.startsWith("file://")) {
            // Es una URL o Uri, cargarla con Glide
            Glide.with(holder.image.context)
                .load(imageData)
                .centerCrop()
                .into(holder.image)
        } else {
            // Es Base64, decodificar y mostrar
            try {
                val decoded = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                if (bitmap != null) {
                    Glide.with(holder.image.context)
                        .load(bitmap)
                        .centerCrop()
                        .into(holder.image)
                } else {
                    Log.w("ImagePagerAdapter", "No se pudo decodificar imagen en posición $position")
                    holder.image.setImageResource(android.R.drawable.ic_dialog_alert)
                }
            } catch (e: Exception) {
                Log.e("ImagePagerAdapter", "Error decodificando Base64 en posición $position: ${e.message}", e)
                holder.image.setImageResource(android.R.drawable.ic_dialog_alert)
            }
        }

        // Abrir imagen a pantalla completa al tocarla — usar helper para manejar listas y Base64 grandes
        holder.image.setOnClickListener {
            try {
                ImageOpenHelper.openFullScreen(holder.image.context, images, position)
            } catch (e: Exception) {
                Log.w("ImagePagerAdapter", "Error abriendo FullScreen: ${e.message}")
            }
        }
    }

    override fun getItemCount(): Int = images.size
}
