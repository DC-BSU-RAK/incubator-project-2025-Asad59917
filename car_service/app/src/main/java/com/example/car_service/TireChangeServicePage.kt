package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class TireChangeServicePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tire_change_service_page)

        findViewById<ImageButton>(R.id.btnTireChangeServiceBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnTireChangeServiceBookNow).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.TIRE_CHANGE)
                putExtra("service_name", "Tire Change")
                putExtra("base_price", ServiceConstants.TIRE_CHANGE_PRICE)
            }
            startActivity(intent)
        }
    }
    }
