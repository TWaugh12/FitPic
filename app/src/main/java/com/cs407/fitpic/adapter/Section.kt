package com.cs407.fitpic.adapter
// data class to define a section in the closet, I.E. Shirt, outerwear, etc


data class Section(
    val title: String,
    val items: List<ClothingItem>
)