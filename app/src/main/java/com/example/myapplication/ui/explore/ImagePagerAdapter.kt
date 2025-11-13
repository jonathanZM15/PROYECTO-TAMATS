package com.example.myapplication.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

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
        val url = images[position]
        Glide.with(holder.image.context)
            .load(url)
            .centerCrop()
            .into(holder.image)
    }

    override fun getItemCount(): Int = images.size
}

