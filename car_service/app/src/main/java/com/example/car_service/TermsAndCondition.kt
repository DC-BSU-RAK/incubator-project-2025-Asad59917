package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class TermsConditionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_and_condition)

        setupBackButton()
    }

    private fun setupBackButton() {
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileFragment::class.java)
        startActivity(intent)
        finish() // Optional: removes this activity from the back stack
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Handle device back button press
        navigateToProfile()
    }
}