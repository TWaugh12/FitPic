package com.cs407.fitpic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cs407.fitpic.R

// adapter to link items in database.




class ClothingAdapter(private val items: List<ClothingItem>,
                      private val onLongClick: (ClothingItem) -> Unit
    ) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_clothing_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = items[position]
        val imageUrl = item.imageUrl

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
        // placeholder is the icon displaying while loading
        //TODO: FIND BETTER LOADING ICON
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = items.size
}
