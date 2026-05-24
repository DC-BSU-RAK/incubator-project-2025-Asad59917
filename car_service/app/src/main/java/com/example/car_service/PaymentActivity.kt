package com.example.car_service

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseHelper

    private lateinit var tvCardNumber: TextView
    private lateinit var tvCardHolder: TextView
    private lateinit var tvCardExpiry: TextView

    private lateinit var etCardNumber: EditText
    private lateinit var etCardHolder: EditText
    private lateinit var etExpiry: EditText
    private lateinit var etCVV: EditText

    private lateinit var btnPay: CardView
    private lateinit var btnBack: ImageButton

    // Payment mode: "pro" (default) or "booking"
    private var paymentMode: String = "pro"

    // Booking-mode extras
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var vehicleBrand: String = ""
    private var vehicleModel: String = ""
    private var plateNumber: String = ""
    private var bookingDate: String = ""
    private var bookingLocation: String = ""
    private var bookingPrice: Int = 0
    private var bookingOriginalPrice: Int = 0
    private var isProDiscount: Boolean = false

    /**
     * Helper that finds a view by its String ID name (e.g. "tvHeaderTitle").
     * Returns null if the ID doesn't exist in the XML — so we can safely
     * try to update optional labels without crashing.
     */
    private fun <T : View> findViewByName(idName: String): T? {
        val resId = resources.getIdentifier(idName, "id", packageName)
        return if (resId == 0) null else findViewById<T?>(resId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        firebaseHelper = FirebaseHelper(this)

        paymentMode = intent.getStringExtra("payment_mode") ?: "pro"
        if (paymentMode == "booking") {
            serviceId            = intent.getStringExtra("service_id") ?: ""
            serviceName          = intent.getStringExtra("service_name") ?: "Service"
            vehicleBrand         = intent.getStringExtra("vehicle_brand") ?: ""
            vehicleModel         = intent.getStringExtra("vehicle_model") ?: ""
            plateNumber          = intent.getStringExtra("plate_number") ?: ""
            bookingDate          = intent.getStringExtra("booking_date") ?: ""
            bookingLocation      = intent.getStringExtra("location") ?: ""
            bookingPrice         = intent.getIntExtra("price", 0)
            bookingOriginalPrice = intent.getIntExtra("original_price", 0)
            isProDiscount        = intent.getBooleanExtra("is_pro_discount", false)
        }

        initViews()
        applyModeUI()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        tvCardNumber  = findViewById(R.id.tvCardNumber)
        tvCardHolder  = findViewById(R.id.tvCardHolder)
        tvCardExpiry  = findViewById(R.id.tvCardExpiry)
        etCardNumber  = findViewById(R.id.etCardNumber)
        etCardHolder  = findViewById(R.id.etCardHolder)
        etExpiry      = findViewById(R.id.etExpiry)
        etCVV         = findViewById(R.id.etCVV)
        btnPay  = findViewById(R.id.btnPay)
        btnBack = findViewById(R.id.btnBack)
    }

    /**
     * Tries to swap labels for booking mode using the optional XML IDs.
     * If you didn't add the IDs, this silently does nothing — payment
     * page still says "AED 39 — Activate Pro" in that case.
     *
     * For the booking mode to LOOK right (showing service name & price),
     * just add these IDs to your activity_payment.xml on the matching
     * existing TextViews:
     *   tvHeaderTitle, tvHeaderSubtitle,
     *   tvPlanTitle, tvPlanSubtitle, tvPlanPrice,
     *   tvPayLabel
     */
    private fun applyModeUI() {
        if (paymentMode == "booking") {
            findViewByName<TextView>("tvHeaderTitle")?.text = "Secure Payment"
            findViewByName<TextView>("tvHeaderSubtitle")?.text = "Service Booking"
            findViewByName<TextView>("tvPlanTitle")?.text = "🔧 $serviceName"
            findViewByName<TextView>("tvPlanSubtitle")?.text = "$vehicleBrand $vehicleModel · $plateNumber"
            findViewByName<TextView>("tvPlanPrice")?.text = "AED $bookingPrice"
            findViewByName<TextView>("tvPayLabel")?.text = "Pay AED $bookingPrice — Confirm Booking"

            // Optional: hide the Pro features list if you tagged its container
            findViewByName<View>("planFeaturesCard")?.visibility = View.GONE
        }
        // "pro" mode = leave existing XML labels as-is (they already say "AutoMate Pro" etc.)
    }

    private fun setupTextWatchers() {
        etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().replace(" ", "").take(16)
                val formatted = digits.chunked(4).joinToString(" ")
                etCardNumber.setText(formatted)
                etCardNumber.setSelection(formatted.length)
                val display = digits.padEnd(16, '•').chunked(4).joinToString(" ")
                tvCardNumber.text = display
                isFormatting = false
            }
        })

        etCardHolder.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvCardHolder.text = if (s.isNullOrBlank()) "FULL NAME" else s.toString().uppercase()
            }
        })

        etExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().replace("/", "").take(4)
                val formatted = if (digits.length >= 2) {
                    "${digits.substring(0, 2)}/${digits.substring(2)}"
                } else digits
                etExpiry.setText(formatted)
                etExpiry.setSelection(formatted.length)
                tvCardExpiry.text = if (formatted.isEmpty()) "MM/YY" else formatted
                isFormatting = false
            }
        })
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnPay.setOnClickListener {
            if (validateInputs()) {
                processPayment()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val cardNumber = etCardNumber.text.toString().replace(" ", "")
        val cardHolder = etCardHolder.text.toString().trim()
        val expiry     = etExpiry.text.toString().trim()
        val cvv        = etCVV.text.toString().trim()

        if (cardNumber.length < 16) {
            etCardNumber.error = "Enter a valid 16-digit card number"
            etCardNumber.requestFocus(); return false
        }
        if (cardHolder.isEmpty()) {
            etCardHolder.error = "Enter cardholder name"
            etCardHolder.requestFocus(); return false
        }
        if (expiry.length < 5) {
            etExpiry.error = "Enter valid expiry (MM/YY)"
            etExpiry.requestFocus(); return false
        }
        if (cvv.length < 3) {
            etCVV.error = "Enter valid CVV"
            etCVV.requestFocus(); return false
        }
        return true
    }

    private fun processPayment() {
        btnPay.isEnabled = false
        lifecycleScope.launch {
            if (paymentMode == "booking") processBookingPayment() else processProPayment()
        }
    }

    private suspend fun processProPayment() {
        val success = firebaseHelper.setPremium(true, "pro")
        if (success) {
            firebaseHelper.saveNotification(
                title = "Welcome to AutoMate Pro! ⭐",
                message = "Your Pro membership is active. Enjoy 15% off all services + premium features.",
                audience = "user",
                targetUserId = firebaseHelper.getCurrentUserId()
            )
            showPaymentSuccessSheet(isPro = true)
        } else {
            btnPay.isEnabled = true
            Toast.makeText(this, "Failed to activate Pro. Try again.", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun processBookingPayment() {
        try {
            val user = firebaseHelper.getCurrentUser()
            val booking = FirebaseHelper.Booking(
                userId = firebaseHelper.getCurrentUserId() ?: "",
                userName = user?.name ?: "",
                userEmail = user?.email ?: "",
                serviceId = serviceId,
                serviceName = serviceName,
                vehicleBrand = vehicleBrand,
                vehicleModel = vehicleModel,
                plateNumber = plateNumber,
                bookingDate = bookingDate,
                location = bookingLocation,
                price = bookingPrice,
                originalPrice = bookingOriginalPrice,
                isProDiscount = isProDiscount,
                status = "PENDING"
            )

            val saved = firebaseHelper.saveBooking(booking)
            if (!saved) {
                btnPay.isEnabled = true
                Toast.makeText(this, "Booking failed. Please try again.", Toast.LENGTH_LONG).show()
                return
            }

            firebaseHelper.saveNotification(
                title = "Payment Successful ✅",
                message = "Your payment of AED $bookingPrice for $serviceName has been processed. Awaiting confirmation.",
                audience = "user",
                targetUserId = firebaseHelper.getCurrentUserId()
            )

            showPaymentSuccessSheet(isPro = false)
        } catch (e: Exception) {
            btnPay.isEnabled = true
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPaymentSuccessSheet(isPro: Boolean) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_payment_success, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)

        // Optional: only updates the message if your XML has this id (won't crash if missing)
        val msgResId = resources.getIdentifier("tvSuccessMessage", "id", packageName)
        if (msgResId != 0) {
            view.findViewById<TextView?>(msgResId)?.text = if (isPro) {
                "Welcome to AutoMate Pro!"
            } else {
                "Booking Placed! We'll confirm shortly."
            }
        }

        view.findViewById<CardView>(R.id.btnGoHome).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, MainActivity::class.java).apply {
                if (!isPro) putExtra("SHOW_BOOKINGS", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}