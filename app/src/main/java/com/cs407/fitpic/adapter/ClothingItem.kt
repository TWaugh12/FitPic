package com.cs407.fitpic.adapter
//data class to define a specific item and the imageUrl's of it
// TODO: MAY NEED TO ADJUST THE CLASS TO CONTAIN MORE INFO

data class ClothingItem(
    val imageUrls: List<String> = emptyList(),
    val category: String? = null // Add other fields if needed
)