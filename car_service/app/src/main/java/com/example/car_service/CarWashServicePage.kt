package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton


class CarWashServicePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_wash_service_page)

        findViewById<ImageButton>(R.id.btnCarWashServiceBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnCarWashServiceBookNow).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.CAR_WASH)
                putExtra("service_name", "Car Wash")
                putExtra("base_price", ServiceConstants.CAR_WASH_PRICE)
            }
            startActivity(intent)
        }
    }
}