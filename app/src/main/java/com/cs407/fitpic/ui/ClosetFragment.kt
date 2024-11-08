package com.cs407.fitpic.ui

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.fitpic.R

class ClosetFragment : Fragment() {


//  ORIGIONAL FUNCTION LAYOUT

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_closet, container, false)
//    }


//      BELOW STILL NEEDS ACCESS TO THE DATABASE SOMEHOW

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view, R.id.recyclerView_fits, getFitsItems())
        setupRecyclerView(view, R.id.recyclerView_tops, getTopsItems())
        setupRecyclerView(view, R.id.recyclerView_layers, getLayersItems())
        setupRecyclerView(view, R.id.recyclerView_bottoms, getBottomsItems())
        setupRecyclerView(view, R.id.recyclerView_shoes, getShoesItems())
        setupRecyclerView(view, R.id.recyclerView_hats, getHatsItems())
    }


//     sets up the recyclerview to create horizontal grid system. STILL NEED item adapter
//          to collect data from database.

    private fun setupRecyclerView(view: View, recyclerViewId: Int, items: List<ClipData.Item>) {
        val recyclerView = view.findViewById<RecyclerView>(recyclerViewId)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        //TODO: create an item adapter that will grab items from database
        //recyclerView.adapter = StaggeredItemAdapter(items)
    }

    // NOTE FROM BOOBS: Imported android.content.ClipData to accomodate items,
    //                  MIGHT NEED TO CHANGE


    private fun getFitsItems(): List<ClipData.Item> = listOf(/* Add items here */)
    private fun getTopsItems(): List<ClipData.Item> = listOf(/* Add items here */)
    private fun getLayersItems(): List<ClipData.Item> = listOf(/* Add items here */)
    private fun getBottomsItems(): List<ClipData.Item> = listOf(/* Add items here */)
    private fun getShoesItems(): List<ClipData.Item> = listOf(/* Add items here */)
    private fun getHatsItems(): List<ClipData.Item> = listOf(/* Add items here */)
}