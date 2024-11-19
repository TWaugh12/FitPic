package com.cs407.fitpic.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cs407.fitpic.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

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


        // Handle Delete Account button
        deleteAccountButton.setOnClickListener {
            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
        }

        // Handle Log Out button
        logOutButton.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        }
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