package com.cs407.fitpic.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.cs407.fitpic.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

class AddFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var addImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var clothingTypeSpinner: Spinner
    private var selectedImageUri: Uri? = null

    private val storage = Firebase.storage
    private val firestore = Firebase.firestore

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
            saveButton.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        imageView = view.findViewById(R.id.image_clothing)
        addImageButton = view.findViewById(R.id.button_add_image)
        saveButton = view.findViewById(R.id.button_save_clothing)
        clothingTypeSpinner = view.findViewById(R.id.spinner_clothing_type)
        saveButton.isEnabled = false
    }

    private fun setupClickListeners() {
        addImageButton.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        saveButton.setOnClickListener {
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        selectedImageUri?.let { uri ->
            saveButton.isEnabled = false

            val filename = "clothing_${UUID.randomUUID()}"
            val storageRef = storage.reference.child("clothing_images/$filename")

            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveToFirestore(filename, task.result.toString())
                    } else {
                        handleUploadError(task.exception)
                    }
                }
        } ?: showToast("Please select an image first")
    }

    private fun saveToFirestore(filename: String, downloadUrl: String) {
        val clothingItem = hashMapOf(
            "type" to clothingTypeSpinner.selectedItem.toString(),
            "imageUrl" to downloadUrl,
            "timestamp" to System.currentTimeMillis(),
            "filename" to filename
        )

        firestore.collection("products")
            .add(clothingItem)
            .addOnSuccessListener {
                showToast("Upload successful!")
                resetForm()
            }
            .addOnFailureListener { e ->
                handleUploadError(e)
            }
    }

    private fun handleUploadError(exception: Exception?) {
        val errorMessage = exception?.message ?: "Unknown error occurred"
        showToast("Upload failed: $errorMessage")
        saveButton.isEnabled = true
    }

    private fun resetForm() {
        imageView.setImageResource(0)
        clothingTypeSpinner.setSelection(0)
        selectedImageUri = null
        saveButton.isEnabled = false
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}