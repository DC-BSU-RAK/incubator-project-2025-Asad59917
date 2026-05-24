package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class Servicepage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_servicepage) // Replace with your XML file name

        // Find the book now button
        val btnBookNow = findViewById<MaterialButton>(R.id.btnBookNow)

        // Set click listener
        btnBookNow.setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", "engine_oil_change") // Add this line
                putExtra("service_name", "Engine oil change")
                putExtra("base_price", 100)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Also handle the back button if needed
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}