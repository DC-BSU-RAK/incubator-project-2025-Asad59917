package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class BrakeService : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brake_service)

        findViewById<ImageButton>(R.id.btnBrakeServiceBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnBrakeServiceBookNow).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.BRAKE_SERVICE)
                putExtra("service_name", "Brake Service")
                putExtra("base_price", ServiceConstants.BRAKE_SERVICE_PRICE)
            }
            startActivity(intent)
        }
    }
}