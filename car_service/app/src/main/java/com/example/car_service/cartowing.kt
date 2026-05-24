package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class cartowing : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cartowing)

        findViewById<ImageButton>(R.id.btnRoadSideBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnRoadSideCallNow).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.CAR_TOWING)
                putExtra("service_name", "Roadside Assistance")
                putExtra("base_price", ServiceConstants.CAR_TOWING_PRICE)
            }
            startActivity(intent)
        }
    }
}