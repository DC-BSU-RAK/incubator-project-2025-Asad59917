package com.example.car_service

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class AddVehicleActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private var selectedType: String = "Car"

    private lateinit var carTextView: TextView
    private lateinit var motorcycleTextView: TextView
    private lateinit var boatTextView: TextView
    private lateinit var otherTextView: TextView
    private lateinit var selectionIndicator: MaterialCardView
    private lateinit var carContainer: FrameLayout
    private lateinit var motorcycleContainer: FrameLayout
    private lateinit var boatContainer: FrameLayout
    private lateinit var otherContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        firebaseHelper = FirebaseHelper(this)

        // Form fields
        val colorInput = findViewById<EditText>(R.id.colorInput)
        val makeInput = findViewById<EditText>(R.id.makeInput)
        val modelInput = findViewById<EditText>(R.id.modelInput)
        val chassisInput = findViewById<EditText>(R.id.chassisInput)
        val licenseInput = findViewById<EditText>(R.id.licenseInput)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Type selector views
        carContainer = findViewById(R.id.carContainer)
        motorcycleContainer = findViewById(R.id.motorcycleContainer)
        boatContainer = findViewById(R.id.boatContainer)
        otherContainer = findViewById(R.id.otherContainer)
        carTextView = findViewById(R.id.carTextView)
        motorcycleTextView = findViewById(R.id.motorcycleTextView)
        boatTextView = findViewById(R.id.boatTextView)
        otherTextView = findViewById(R.id.otherTextView)
        selectionIndicator = findViewById(R.id.selectionIndicator)

        backButton?.setOnClickListener { finish() }

        // Type selection
        carContainer.setOnClickListener { selectType("Car", carContainer) }
        motorcycleContainer.setOnClickListener { selectType("Motorcycle", motorcycleContainer) }
        boatContainer.setOnClickListener { selectType("Boat", boatContainer) }
        otherContainer.setOnClickListener { selectType("Other", otherContainer) }

        // Set "Car" as default after layout is ready
        carContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                carContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                selectType("Car", carContainer)
            }
        })

        saveButton.setOnClickListener {
            val color = colorInput.text.toString().trim()
            val make = makeInput.text.toString().trim()
            val model = modelInput.text.toString().trim()
            val chassis = chassisInput.text.toString().trim()
            val license = licenseInput.text.toString().trim()

            if (make.isEmpty() || model.isEmpty() || license.isEmpty()) {
                Toast.makeText(this, "Make, Model and License plate are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveButton.isEnabled = false
            saveButton.text = "Saving..."

            // ✅ Vehicle with ALL fields matching your existing data class
            val vehicle = Vehicle(
                type = selectedType,
                brand = make,
                model = model,
                color = color,
                chassisNumber = chassis,
                plateNumber = license
            )

            lifecycleScope.launch {
                val success = firebaseHelper.saveVehicle(vehicle)

                saveButton.isEnabled = true
                saveButton.text = "Save"

                if (success) {
                    Toast.makeText(this@AddVehicleActivity, "Vehicle added!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddVehicleActivity, "Failed to save vehicle", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun selectType(type: String, selectedContainer: FrameLayout) {
        selectedType = type

        carTextView.setTextColor(0xFFAAAAAA.toInt())
        motorcycleTextView.setTextColor(0xFFAAAAAA.toInt())
        boatTextView.setTextColor(0xFFAAAAAA.toInt())
        otherTextView.setTextColor(0xFFAAAAAA.toInt())

        when (type) {
            "Car" -> carTextView.setTextColor(0xFF000000.toInt())
            "Motorcycle" -> motorcycleTextView.setTextColor(0xFF000000.toInt())
            "Boat" -> boatTextView.setTextColor(0xFF000000.toInt())
            "Other" -> otherTextView.setTextColor(0xFF000000.toInt())
        }

        if (selectedContainer.width > 0) {
            val params = selectionIndicator.layoutParams as android.widget.FrameLayout.LayoutParams
            params.width = selectedContainer.width
            params.height = selectedContainer.height
            params.leftMargin = selectedContainer.left
            selectionIndicator.layoutParams = params
            selectionIndicator.visibility = View.VISIBLE
        }
    }
}