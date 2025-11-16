package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.util.ImageOpenHelper

data class Post(val images: List<String>, val text: String)

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vpPhotos: ViewPager2 = view.findViewById(R.id.vpPhotos)
        val tvThinking: TextView = view.findViewById(R.id.tvThinking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.tvThinking.text = post.text
        if (post.images.isEmpty()) {
            holder.vpPhotos.visibility = View.GONE
        } else {
            holder.vpPhotos.visibility = View.VISIBLE

            // Detectar si son Base64 directo o IDs de storyImages
            val firstImage = post.images[0]
            if (isBase64(firstImage)) {
                // Son Base64 directo
                holder.vpPhotos.adapter = ImagePagerAdapter(post.images)
            } else {
                // Son IDs de documentos, cargar de storyImages
                loadStoryImagesFromIds(post.images, holder.vpPhotos)
            }
        }
    }

    override fun getItemCount(): Int = posts.size

    // Detectar si un string es Base64 o un ID de documento
    private fun isBase64(str: String): Boolean {
        if (str.length > 200) return true // Probablemente Base64
        if (str.contains("/") || str.contains("+") || str.contains("=")) return true // Caracteres típicos de Base64
        return false
    }

    // Cargar imágenes desde collection storyImages usando sus IDs
    private fun loadStoryImagesFromIds(imageIds: List<String>, viewPager: ViewPager2) {
        val loadedImages = mutableMapOf<Int, String>() // index -> base64
        var remaining = imageIds.size

        for (imgId in imageIds) {
            db.collection("storyImages").document(imgId)
                .get()
                .addOnSuccessListener { doc ->
                    try {
                        val imageBase64 = doc.getString("image")
                        val index = doc.getLong("index")?.toInt() ?: -1

                        if (!imageBase64.isNullOrEmpty()) {
                            if (index >= 0) {
                                loadedImages[index] = imageBase64
                            } else {
                                loadedImages[loadedImages.size] = imageBase64
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PostAdapter", "Error extrayendo imagen: ${e.message}")
                    }

                    remaining--
                    if (remaining == 0 && loadedImages.isNotEmpty()) {
                        // Ordenar por clave (índice) y crear lista ordenada
                        val sortedImages = loadedImages.toSortedMap().values.toList()
                        viewPager.adapter = ImagePagerAdapter(sortedImages)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PostAdapter", "Error cargando imagen $imgId: ${e.message}")
                    remaining--
                    if (remaining == 0 && loadedImages.isNotEmpty()) {
                        val sortedImages = loadedImages.toSortedMap().values.toList()
                        viewPager.adapter = ImagePagerAdapter(sortedImages)
                    }
                }
        }
    }

    private class ImagePagerAdapter(private val images: List<String>) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
            return ImageViewHolder(v)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val src = images[position]
            try {
                if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("content://") || src.startsWith("file://")) {
                    Glide.with(holder.image.context)
                        .load(src)
                        .centerCrop()
                        .into(holder.image)
                } else if (src.startsWith("data:")) {
                    // data:[<mediatype>][;base64],<data>
                    val comma = src.indexOf(',')
                    val b64 = if (comma >= 0 && comma + 1 < src.length) src.substring(comma + 1) else src
                    val decoded = Base64.decode(b64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    Glide.with(holder.image.context)
                        .load(bmp)
                        .centerCrop()
                        .into(holder.image)
                } else {
                    // intentar interpretar como base64 puro
                    val decoded = try {
                        Base64.decode(src, Base64.DEFAULT)
                    } catch (_: Exception) {
                        null
                    }
                    if (decoded != null) {
                        val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        if (bmp != null) {
                            Glide.with(holder.image.context)
                                .load(bmp)
                                .centerCrop()
                                .into(holder.image)
                        } else {
                            holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    } else {
                        holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            } catch (_: Exception) {
                holder.image.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Añadir click que abra la lista completa usando ImageOpenHelper
            holder.image.setOnClickListener {
                try {
                    ImageOpenHelper.openFullScreen(holder.image.context, images, position)
                } catch (e: Exception) {
                    android.util.Log.w("PostAdapter", "Error abriendo FullScreen: ${e.message}")
                }
            }
        }

        override fun getItemCount(): Int = images.size
    }
}
