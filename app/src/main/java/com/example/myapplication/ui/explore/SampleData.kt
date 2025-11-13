package com.example.myapplication.ui.explore

object SampleData {
    fun createSamplePosts(): List<Post> {
        return listOf(
            Post(listOf("https://via.placeholder.com/600", "https://via.placeholder.com/601"), "Post con 2 fotos"),
            Post(listOf("https://via.placeholder.com/602"), "Post con 1 foto"),
            Post(emptyList(), "Post sin fotos")
        )
    }
}

