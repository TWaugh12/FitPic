package com.cs407.fitpic.ui

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.cs407.fitpic.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class AddFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var addImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var clothingTypeSpinner: Spinner
    private var selectedImageUri: Uri? = null

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var openAI: OpenAI? = null

    companion object {
        private const val TAG = "AddFragment"
        private const val oai_key = "add key"
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
            saveButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeOpenAI()
    }

    private fun initializeOpenAI() {
        try {
            if (oai_key.isNotEmpty()) {
                openAI = OpenAI(
                    token = oai_key,
                    timeout = Timeout(socket = 60.seconds)
                )
            } else {
                Log.e(TAG, "OpenAI API key is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OpenAI client", e)
            Toast.makeText(context, "Error initializing image analysis", Toast.LENGTH_SHORT).show()
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

        val isDarkTheme = isDarkTheme()
        val primaryTextColor = if (isDarkTheme) {
            ContextCompat.getColor(requireContext(), R.color.text_color_dark)
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_color_light)
        }

        val titleTextView: TextView = view.findViewById(R.id.fitpic_title)
        titleTextView.setTextColor(primaryTextColor)
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

    private suspend fun analyzeImageWithGPT(imageUrl: String): JSONObject? {
        return try {
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-4o"),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = "You are a clothing analyzer. " +
                                "You must respond ONLY with a valid JSON object containing three fields " +
                                "- type (type of clothing: Shirts, Pants, Hoodies, Sweaters, Shoes, Accessories)," +
                                " color (main color, choose from rainbow colors), and weather (appropriate temperature (in Fahrenheit) range to wear item)." +
                                " No other text or explanation."
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = listOf(
                            TextPart("Analyze this clothing item and provide details:"),
                            ImagePart(imageUrl)
                        )
                    )
                ),
                responseFormat = ChatResponseFormat.JsonObject
            )

            val openAIInstance = openAI ?: run {
                Log.e(TAG, "OpenAI client not initialized")
                return null
            }

            val response = openAIInstance.chatCompletion(chatCompletionRequest)
            val content = response.choices.first().message.content.orEmpty()

            // Debug log
            Log.d(TAG, "Raw OpenAI response: $content")

            try {
                JSONObject(content)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON response: $content", e)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
            null
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
                        val imageUrl = task.result.toString()
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val analysis = analyzeImageWithGPT(imageUrl)
                                saveToFirestore(filename, imageUrl, analysis)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error during analysis", e)
                                handleUploadError(e)
                            }
                        }
                    } else {
                        handleUploadError(task.exception)
                    }
                }
        } ?: showToast("Please select an image first")
    }

    private suspend fun saveToFirestore(filename: String, downloadUrl: String, analysis: JSONObject?) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showToast("User not logged in. Please log in to save items.")
            return
        }

        try {
            val clothingItem = hashMapOf(
                "type" to (analysis?.optString("type") ?: clothingTypeSpinner.selectedItem.toString()),
                "imageUrl" to downloadUrl,
                "timestamp" to System.currentTimeMillis(),
                "filename" to filename,
                "userId" to currentUser.uid,
                "color" to (analysis?.optString("color") ?: ""),
                "weather" to (analysis?.optString("weather") ?: "")
            )

            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("clothingItems")
                    .add(clothingItem)
                    .await()

                withContext(Dispatchers.Main) {
                    showToast("Upload successful!")
                    resetForm()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                handleUploadError(e)
            }
        }
    }

    private fun handleUploadError(exception: Exception?) {
        val errorMessage = exception?.message ?: "Unknown error occurred"
        Log.e(TAG, "Upload error: $errorMessage", exception)
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

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}