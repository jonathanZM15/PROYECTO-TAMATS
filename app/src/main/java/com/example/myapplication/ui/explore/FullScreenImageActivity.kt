package com.example.myapplication.ui.explore

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.Color
import androidx.core.graphics.toColorInt

class FullScreenImageActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pantalla completa (ocultar barra de notificaciones)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_fullscreen_image)

        try { supportActionBar?.hide() } catch (_: Exception) {}

        val vp = findViewById<ViewPager2>(R.id.fullscreenViewPager)
        val dotsContainer = findViewById<LinearLayout>(R.id.dotsContainer)

        // Compatibilidad: aceptar lista 'imageUris' o single 'imageData'
        val imageUris = intent.getStringArrayListExtra("imageUris")
        val fallback = intent.getStringExtra("imageData")
        val images = when {
            imageUris != null && imageUris.isNotEmpty() -> imageUris.toList()
            !fallback.isNullOrEmpty() -> listOf(fallback)
            else -> emptyList()
        }

        if (images.isEmpty()) {
            finish()
            return
        }

        // Adaptador local nombrado para evitar errores de referencia
        class FSAdapter(private val imgs: List<String>) : RecyclerView.Adapter<FSAdapter.ImageViewHolder>() {
            inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val image: ImageView = view.findViewById(R.id.image)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
                // Asegurar tamaño
                v.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                return ImageViewHolder(v)
            }

            override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
                val src = imgs[position]
                try {
                    if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("content://") || src.startsWith("file://")) {
                        Glide.with(holder.image.context).load(src).into(holder.image)
                    } else if (src.startsWith("data:")) {
                        val comma = src.indexOf(',')
                        val b64 = if (comma >= 0 && comma + 1 < src.length) src.substring(comma + 1) else src
                        val decoded = Base64.decode(b64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        Glide.with(holder.image.context).load(bmp).into(holder.image)
                    } else {
                        // intentar como Base64
                        val decoded = try { Base64.decode(src, Base64.DEFAULT) } catch (_: Exception) { null }
                        if (decoded != null) {
                            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                            Glide.with(holder.image.context).load(bmp).into(holder.image)
                        } else {
                            holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                }

                // Cerrar al tocar la imagen
                holder.image.setOnClickListener { finish() }
            }

            override fun getItemCount(): Int = imgs.size
        }

        vp.adapter = FSAdapter(images)

        // Dots
        dotsContainer.removeAllViews()
        val dotViews = mutableListOf<TextView>()
        for (i in images.indices) {
            val dot = TextView(this)
            dot.text = "●"
            dot.textSize = 12f
            dot.setTextColor(if (i == 0) Color.WHITE else "#66FFFFFF".toColorInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(8, 0, 8, 0)
            dotsContainer.addView(dot, lp)
            dotViews.add(dot)
        }

        vp.setCurrentItem(intent.getIntExtra("startIndex", 0), false)

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                for ((i, d) in dotViews.withIndex()) {
                    d.setTextColor(if (i == position) Color.WHITE else "#66FFFFFF".toColorInt())
                }
            }
        })
    }
}
