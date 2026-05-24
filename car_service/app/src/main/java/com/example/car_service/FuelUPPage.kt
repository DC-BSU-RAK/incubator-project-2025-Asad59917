package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FuelUPPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fuel_uppage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fuel_up)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button functionality
        findViewById<ImageButton>(R.id.btnFuelUpBack).setOnClickListener {
            finish()
        }

        // Fuel Option 1 - Special Gasoline
        findViewById<ImageButton>(R.id.btnFuelOption1Detail).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.FUEL_SPECIAL)
                putExtra("service_name", "Special Gasoline")
                putExtra("base_price", 285) // AED 2.85 per liter (in cents)
                putExtra("fuel_type", "special")
                putExtra("octane_rating", "91")
            }
            startActivity(intent)
        }

        // Fuel Option 2 - Super Gasoline
        findViewById<ImageButton>(R.id.btnFuelOption2Detail).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.FUEL_SUPER)
                putExtra("service_name", "Super Gasoline")
                putExtra("base_price", 315) // AED 3.15 per liter (in cents)
                putExtra("fuel_type", "super")
                putExtra("octane_rating", "95")
            }
            startActivity(intent)
        }

        // Fuel Option 3 - Diesel
        findViewById<ImageButton>(R.id.btnFuelOption3Detail).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.FUEL_DIESEL)
                putExtra("service_name", "Diesel Fuel")
                putExtra("base_price", 295) // AED 2.95 per liter (in cents)
                putExtra("fuel_type", "diesel")
                putExtra("octane_rating", "N/A")
            }
            startActivity(intent)
        }
    }
}