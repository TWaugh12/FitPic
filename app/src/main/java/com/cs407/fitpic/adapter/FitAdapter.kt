package com.cs407.fitpic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cs407.fitpic.R

// Adapter to display fit titles in a RecyclerView
class FitAdapter(
    private val fits: List<Fit>,
    private val context: Context,
    private val onFitClick: (Fit) -> Unit // Callback for clicks
) : RecyclerView.Adapter<FitAdapter.FitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FitViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_fit, parent, false)
        return FitViewHolder(view)
    }

    override fun onBindViewHolder(holder: FitViewHolder, position: Int) {
        val fit = fits[position]
        holder.titleTextView.text = fit.title

        // Set click listener
        holder.titleTextView.setOnClickListener {
            onFitClick(fit) // Trigger callback
        }
    }

    override fun getItemCount(): Int {
        return fits.size
    }

    inner class FitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.fit_title)
    }
}

