package com.cs407.fitpic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.fitpic.R


class SectionAdapter : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

    private val sections = mutableListOf<Section>()

    fun submitList(newSections: List<Section>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.text_section_title)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView_horizontal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.title.text = section.title
        holder.recyclerView.layoutManager = LinearLayoutManager(
            holder.recyclerView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        holder.recyclerView.adapter = ClothingAdapter(section.items)
    }

    override fun getItemCount(): Int = sections.size
}
