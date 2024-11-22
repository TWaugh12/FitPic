package com.cs407.fitpic.ui

import android.Manifest
import android.annotation.SuppressLint
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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cs407.fitpic.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.android.volley.Request
import org.json.JSONObject

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // weather url to get JSON
    var weather_url1 = ""

    // api id for url
    var api_key = "a30081f64c8fb1b31af9262d1dbf69d5"

    private lateinit var weather_button : Button
    private lateinit var weather_text: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        val darkModeSwitch: Switch = view.findViewById(R.id.switch_dark_mode)
        val phoneNumber: TextView = view.findViewById(R.id.text_phone_number)
        val email: TextView = view.findViewById(R.id.text_email)
        val deleteAccountButton: Button = view.findViewById(R.id.button_delete_account)
        val logOutButton: Button = view.findViewById(R.id.button_log_out)

        // Toggle dark mode
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("settings", 0)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode

        // Apply the saved dark mode setting
        applyDarkMode(isDarkMode)

        // Toggle dark mode when the switch is changed
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the new preference
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()

            // Apply dark mode
            applyDarkMode(isChecked)
        }

        val isDarkTheme = isDarkTheme()
        val primaryTextColor = if (isDarkTheme) {
            ContextCompat.getColor(requireContext(), R.color.text_color_dark)
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_color_light)
        }

        // Apply color to TextViews
        val titleTextView: TextView = view.findViewById(R.id.fitpic_title)
        val weatherTextView: TextView = view.findViewById(R.id.text_todays_weather)
        titleTextView.setTextColor(primaryTextColor)
        weatherTextView.setTextColor(primaryTextColor)

        // create an instance of the Fused
        // Location Provider Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        weather_button = view.findViewById(R.id.weather_button)
        weather_text = view.findViewById(R.id.weather_text)

        // add on click listener to the button
        weather_button.setOnClickListener {
            checkForPermission()
        }

        // Handle Delete Account button
        deleteAccountButton.setOnClickListener {
            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
        }

        // Handle Log Out button
        logOutButton.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
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
            // Permissions are already granted, obtain the location
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
                // Permission was granted
                obtainLocation()
            } else {
                // Permission was denied
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
                    getTemp() // Fetch weather after constructing URL
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
        val url: String = weather_url1

        val stringReq = StringRequest(Request.Method.GET, url, { response ->
            try {
                val obj = JSONObject(response)

                // Access the `current` object
                val current = obj.getJSONObject("current")
                val temperature = current.getDouble("temp") // Temperature in Celsius
                val weatherDescription = current.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("description")

                // Display the temperature and weather description
                weather_text.text = "$temperature°F, $weatherDescription"
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error parsing weather data", Toast.LENGTH_SHORT).show()
            }
        },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val message = error.message
                Toast.makeText(requireContext(), "Error fetching weather: $message (Code: $statusCode)", Toast.LENGTH_SHORT).show()
            }
        )
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
}