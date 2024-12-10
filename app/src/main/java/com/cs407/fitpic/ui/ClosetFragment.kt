package com.cs407.fitpic.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.fitpic.R
import com.cs407.fitpic.adapter.ClothingItem
import com.cs407.fitpic.adapter.Section
import com.cs407.fitpic.adapter.SectionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ClosetFragment : Fragment() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val sectionAdapter by lazy {
        SectionAdapter(requireContext()) { clothingItem ->
            showDeleteDialog(clothingItem)
        }
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_closet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_closet)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = sectionAdapter

        val isDarkMode = isDarkTheme()
        sectionAdapter.setTextColorForTheme(isDarkMode)

        fetchClothingItems()
    }

    private fun fetchClothingItems() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("ClosetFragment", "No user logged in")
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val clothingItemsRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("clothingItems")

        clothingItemsRef.get()
            .addOnSuccessListener { querySnapshot ->
                handleClothingItemsSuccess(querySnapshot)
            }
            .addOnFailureListener { exception ->
                Log.e("ClosetFragment", "Error fetching clothing items", exception)
                Toast.makeText(requireContext(), "Failed to load closet data. Please try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleClothingItemsSuccess(querySnapshot: QuerySnapshot) {
        val clothingItems = querySnapshot.documents.mapNotNull { document ->
            document.toObject(ClothingItem::class.java)?.copy(documentId = document.id)
        }

        val sections = organizeClothingByCategory(clothingItems)
        sectionAdapter.submitList(sections)
    }

    private fun organizeClothingByCategory(clothingItems: List<ClothingItem>): List<Section> {
        return clothingItems.groupBy { it.type ?: "Unknown" }
            .map { (category, items) -> Section(category, items) }
    }

    private fun showDeleteDialog(clothingItem: ClothingItem) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.delete_button)?.setOnClickListener {
            deleteClothingItem(clothingItem)
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.cancel_button)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun deleteClothingItem(clothingItem: ClothingItem) {
        val currentUser = auth.currentUser ?: return

        val clothingItemRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("clothingItems")
            .document(clothingItem.documentId)

        clothingItemRef.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
                fetchClothingItems() // Refresh the data after deletion
            }
            .addOnFailureListener { exception ->
                Log.e("ClosetFragment", "Failed to delete item", exception)
                Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
