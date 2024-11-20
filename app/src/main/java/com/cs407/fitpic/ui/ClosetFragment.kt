package com.cs407.fitpic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.fitpic.R
import com.cs407.fitpic.adapter.ClothingItem
import com.cs407.fitpic.adapter.Section
import com.cs407.fitpic.adapter.SectionAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ClosetFragment : Fragment() {


    // TODO: CHANGE CURRENT USER ID TO DYNAMICALLY ALLOCATED ONE

    private lateinit var recyclerView: RecyclerView
    private val sectionAdapter = SectionAdapter()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = "IuqWnM0nT0Qfpv2MZOtrekxoSJf1"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_closet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_closet)
        recyclerView.layoutManager = LinearLayoutManager(context) // Vertical layout for sections
        recyclerView.adapter = sectionAdapter

        fetchClothingItems()
    }

    private fun fetchClothingItems() {
        firestore.collection("products")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val sections = organizeClothingByCategory(querySnapshot.toObjects(ClothingItem::class.java))
                sectionAdapter.submitList(sections)
            }
            .addOnFailureListener {
                // TODO: INSERT ERROR
            }
    }

    private fun organizeClothingByCategory(clothingItems: List<ClothingItem>): List<Section> {
        val sectionMap = mutableMapOf<String, MutableList<ClothingItem>>()

        for (item in clothingItems) {
            val category = item.category ?: "Uncategorized"
            sectionMap.getOrPut(category) { mutableListOf() }.add(item)
        }

        return sectionMap.map { (category, items) -> Section(category, items) }
    }
}
