package com.example.car_service

import android.os.Bundle
import android.widget.ImageButton
import android.content.Intent

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class BatteryChangeServicePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_change_service_page)

        // Back Button
        findViewById<ImageButton>(R.id.btnBatteryChangeServiceBack).setOnClickListener {
            finish()
        }

        // Book Now Button
        findViewById<MaterialButton>(R.id.btnBatteryChangeServiceBookNow).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.BATTERY_CHANGE)
                putExtra("service_name", "Battery Change")
                putExtra("base_price", ServiceConstants.BATTERY_CHANGE_PRICE)
            }
            startActivity(intent)
        }
    }
}