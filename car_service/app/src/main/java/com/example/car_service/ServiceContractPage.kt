package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ServiceContractPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_contract_page)

        findViewById<ImageButton>(R.id.btnServiceContractBack).setOnClickListener {
            finish()
        }

        // Example: First contract option
        findViewById<ImageButton>(R.id.btnServiceContractOption1Detail).setOnClickListener {
            val intent = Intent(this, BookingPage::class.java).apply {
                putExtra("service_id", ServiceConstants.SERVICE_CONTRACT)
                putExtra("service_name", "1-Year Service Contract")
                putExtra("base_price", 1315) // From your XML
            }
            startActivity(intent)
        }
    }
}