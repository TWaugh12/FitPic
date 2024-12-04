package com.cs407.fitpic.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cs407.fitpic.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText1)
        val emailEditText = findViewById<EditText>(R.id.emailEditText1)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText1)
        val signUpButton = findViewById<Button>(R.id.signUpButton1)

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        // Add username and email to Firestore
                        val db = FirebaseFirestore.getInstance()
                        val userMap = hashMapOf(
                            "username" to username,
                            "email" to email
                        )

                        db.collection("users").document(userId!!)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sign-Up Successful!", Toast.LENGTH_SHORT).show()
                                finish() // Navigate back to LoginActivity
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Sign-Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

