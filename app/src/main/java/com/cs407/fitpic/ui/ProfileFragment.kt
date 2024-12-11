package com.cs407.fitpic.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cs407.fitpic.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.android.volley.Request
import com.cs407.fitpic.adapter.ClothingItem
import com.cs407.fitpic.adapter.Fit
import com.cs407.fitpic.adapter.FitAdapter
import org.json.JSONObject
import com.cs407.fitpic.ui.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    var weather_url1 = ""
    private var auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    var api_key = "a30081f64c8fb1b31af9262d1dbf69d5"

    private lateinit var fitsRecyclerView: RecyclerView
    private lateinit var fitsAdapter: FitAdapter
    private lateinit var fitsList: MutableList<Fit>
    private lateinit var clothingItemsMap: MutableMap<String, ClothingItem>
    private lateinit var weather_button: Button
    private lateinit var weather_text: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val darkModeSwitch: Switch = view.findViewById(R.id.switch_dark_mode)
        val usernameTextView: TextView = view.findViewById(R.id.text_username)
        val emailTextView: TextView = view.findViewById(R.id.text_email)
        val deleteAccountButton: Button = view.findViewById(R.id.button_delete_account)
        val logOutButton: Button = view.findViewById(R.id.button_log_out)

        fetchUsername { fetchedUsername ->
            usernameTextView.text = fetchedUsername
        }

        val email = auth.currentUser?.email
        emailTextView.text = email ?: "email unavailable"

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("settings", 0)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode
        applyDarkMode(isDarkMode)

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            applyDarkMode(isChecked)
        }

        val isDarkTheme = isDarkTheme()
        val primaryTextColor = if (isDarkTheme) {
            ContextCompat.getColor(requireContext(), R.color.text_color_dark)
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_color_light)
        }

        val titleTextView: TextView = view.findViewById(R.id.fitpic_title)
        val weatherTextView: TextView = view.findViewById(R.id.text_todays_weather)
        titleTextView.setTextColor(primaryTextColor)
        weatherTextView.setTextColor(primaryTextColor)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        weather_button = view.findViewById(R.id.weather_button)
        weather_text = view.findViewById(R.id.weather_text)

        weather_button.setOnClickListener {
            checkForPermission()
        }

        // Initialize RecyclerView with the FitsAdapter
        fitsList = mutableListOf() // Initialize with an empty list
        fitsAdapter = FitAdapter(fitsList, requireContext()) // Pass only the list and context
        fitsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        fitsRecyclerView.adapter = fitsAdapter

// Fetch user fits from Firebase
        fetchUserFits { userFits ->
            fitsList.clear()
            fitsList.addAll(userFits)
            fitsAdapter.notifyDataSetChanged() // Notify the adapter when data is updated
        }

        auth = FirebaseAuth.getInstance()
        logOutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged Out", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        deleteAccountButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid)
                    .delete()
                    .addOnSuccessListener {
                        currentUser.delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Account Deleted", Toast.LENGTH_SHORT).show()
                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to Delete Account", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to Delete User Data", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "No User Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchUserFits(onResult: (List<Fit>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("fits").document(userId).collection("fits")
                .get()
                .addOnSuccessListener { result ->
                    val userFits = mutableListOf<Fit>()
                    for (document in result) {
                        val title = document.getString("title") ?: ""
                        val clothingItemIds = document.get("clothing_item_ids") as? List<String> ?: emptyList()

                        // Only add the fit if the title is not empty
                        if (title.isNotEmpty()) {
                            val fit = Fit(title, clothingItemIds)
                            userFits.add(fit)
                        }
                    }
                    onResult(userFits)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
        } else {
            onResult(emptyList())
        }
    }

    private fun fetchClothingItems(onResult: (Map<String, ClothingItem>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("clothingItems").whereArrayContains("userIds", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val clothingItems = mutableMapOf<String, ClothingItem>()
                    for (document in result) {
                        val documentId = document.id
                        val filename = document.getString("filename") ?: ""
                        val type = document.getString("type") ?: ""
                        val imageUrl = document.getString("imageUrl") ?: ""
                        val clothingItem = ClothingItem(documentId, filename, type, imageUrl)
                        clothingItems[documentId] = clothingItem
                    }
                    onResult(clothingItems)
                }
                .addOnFailureListener {
                    onResult(emptyMap())
                }
        } else {
            onResult(emptyMap())
        }
    }

    private fun checkForPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            obtainLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtainLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to fetch weather data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtainLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    weather_url1 = "https://api.openweathermap.org/data/3.0/onecall?lat=${location.latitude}&lon=${location.longitude}&units=imperial&appid=${api_key}"
                    Log.d("WeatherURL", weather_url1)
                    getTemp()
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching location: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getTemp() {
        val queue = Volley.newRequestQueue(requireContext())
        val stringReq = StringRequest(Request.Method.GET, weather_url1, { response ->
            try {
                val obj = JSONObject(response)
                val current = obj.getJSONObject("current")
                val temperature = current.getDouble("temp")
                val weatherDescription = current.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("description")
                weather_text.text = "$temperature°F, $weatherDescription"
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error parsing weather data", Toast.LENGTH_SHORT).show()
            }
        },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val message = error.message
                Toast.makeText(requireContext(), "Error fetching weather: $message (Code: $statusCode)", Toast.LENGTH_SHORT).show()
            })
        queue.add(stringReq)
    }

    private fun applyDarkMode(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun fetchUsername(onResult: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val username = document?.getString("username") ?: "username unavailable"
                    onResult(username)
                }
                .addOnFailureListener {
                    onResult("username unavailable")
                }
        } else {
            onResult("username unavailable")
        }
    }
}
