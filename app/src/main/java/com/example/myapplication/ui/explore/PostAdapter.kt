package com.example.myapplication.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication.R

data class Post(val images: List<String>, val text: String)

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

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
            holder.vpPhotos.adapter = ImagePagerAdapter(post.images)
        }
    }

    override fun getItemCount(): Int = posts.size

    private class ImagePagerAdapter(private val images: List<String>) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
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
}

