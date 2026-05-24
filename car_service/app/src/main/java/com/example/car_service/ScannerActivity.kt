package com.example.car_service

import android.util.Log
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ScannerActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    // UI Elements
    private lateinit var btnTakePhoto: CardView
    private lateinit var btnChooseGallery: CardView
    private lateinit var ivPreview: ImageView
    private lateinit var layoutResult: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvSymbolName: TextView
    private lateinit var tvSeverity: TextView
    private lateinit var tvSeverityBadge: TextView
    private lateinit var tvWhatItMeans: TextView
    private lateinit var tvCauses: TextView
    private lateinit var tvDiyFix: TextView
    private lateinit var btnBookTechnician: CardView
    private lateinit var btnScanAgain: CardView

    private var currentPhotoPath: String = ""
    private var capturedBitmap: Bitmap? = null

    // ⚠️ REPLACE WITH YOUR GEMINI API KEY
    // Get free key from: https://ai.google.dev (Google AI Studio)
    private val GEMINI_API_KEY = "AIzaSyCFqxk2cdBJ0L0MDPaft8PR2kyROBxD_Kw"

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_GALLERY = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        prefsHelper = PrefsHelper(this)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnChooseGallery = findViewById(R.id.btnChooseGallery)
        ivPreview = findViewById(R.id.ivPreview)
        layoutResult = findViewById(R.id.layoutResult)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvSymbolName = findViewById(R.id.tvSymbolName)
        tvSeverity = findViewById(R.id.tvSeverity)
        tvSeverityBadge = findViewById(R.id.tvSeverityBadge)
        tvWhatItMeans = findViewById(R.id.tvWhatItMeans)
        tvCauses = findViewById(R.id.tvCauses)
        tvDiyFix = findViewById(R.id.tvDiyFix)
        btnBookTechnician = findViewById(R.id.btnBookTechnician)
        btnScanAgain = findViewById(R.id.btnScanAgain)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        showEmptyState()
    }

    private fun setupClickListeners() {
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnChooseGallery.setOnClickListener { chooseFromGallery() }

        btnScanAgain.setOnClickListener {
            showEmptyState()
            capturedBitmap = null
        }

        btnBookTechnician.setOnClickListener {
            startActivity(Intent(this, Servicepage::class.java))
            finish()
        }
    }

    // =========================================================
    // CAMERA / GALLERY
    // =========================================================
    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("SCAN_${timeStamp}_", ".jpg", storageDir).also {
            currentPhotoPath = it.absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                capturedBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                capturedBitmap?.let {
                    ivPreview.setImageBitmap(it)
                    ivPreview.visibility = View.VISIBLE
                    findViewById<LinearLayout>(R.id.layoutCameraPlaceholder)?.visibility = View.GONE
                    analyzeImage(it)
                }
            }
            REQUEST_IMAGE_GALLERY -> {
                val uri: Uri? = data?.data
                uri?.let {
                    val inputStream = contentResolver.openInputStream(it)
                    capturedBitmap = BitmapFactory.decodeStream(inputStream)
                    capturedBitmap?.let { bmp ->
                        ivPreview.setImageBitmap(bmp)
                        ivPreview.visibility = View.VISIBLE
                        findViewById<LinearLayout>(R.id.layoutCameraPlaceholder)?.visibility = View.GONE
                        analyzeImage(bmp)
                    }
                }
            }
        }
    }

    // =========================================================
    // GEMINI AI ANALYSIS
    // =========================================================
    private fun analyzeImage(bitmap: Bitmap) {
        showLoading()

        if (!prefsHelper.isPremium()) {
            prefsHelper.incrementFreeScanCount()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val result = callGeminiVisionAPI(base64Image)

                withContext(Dispatchers.Main) {
                    displayResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Analysis failed: ${e.message}")
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize to reduce payload size
        val resized = Bitmap.createScaledBitmap(bitmap, 800, 600, true)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun callGeminiVisionAPI(base64Image: String): ScanResult {
        // Gemini 2.5 Flash — free tier, fast, supports vision
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 30000
        }

        val prompt = """
            Analyze this car dashboard warning light image. Be concise.
            
            Return ONLY this JSON (no markdown, no extra text):
            {
                "symbolName": "Short name (max 5 words)",
                "severity": "LOW or MEDIUM or HIGH or UNKNOWN",
                "whatItMeans": "Brief explanation in 1 sentence",
                "possibleCauses": ["cause 1", "cause 2", "cause 3"],
                "diyFix": "Brief DIY steps in 2-3 sentences max",
                "needsTechnician": true,
                "urgency": "Can wait or Fix soon or Stop driving immediately"
            }
            
            Keep all text fields SHORT and concise. If image is not a car warning light, set symbolName to "Not a warning light".
        """.trimIndent()

        // Build Gemini request body
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // Text prompt
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        // Image
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 2048)
                put("responseMimeType", "application/json")
            })
        }.toString()

        connection.outputStream.use { it.write(requestBody.toByteArray()) }

        val responseCode = connection.responseCode
        val responseText = if (responseCode == 200) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val errorMsg = connection.errorStream?.bufferedReader()?.readText() ?: "Error $responseCode"
            throw Exception("API Error $responseCode: $errorMsg")
        }

        return parseGeminiResponse(responseText)
    }

    private fun parseGeminiResponse(responseText: String): ScanResult {
        // Log the full response for debugging
        Log.d("ScannerActivity", "Full Gemini response: $responseText")

        return try {
            val responseJson = JSONObject(responseText)

            // Check if there's an error in the response
            if (responseJson.has("error")) {
                val error = responseJson.getJSONObject("error")
                val errorMsg = error.optString("message", "Unknown API error")
                throw Exception("Gemini API: $errorMsg")
            }

            // Gemini response structure: candidates[0].content.parts[0].text
            val candidates = responseJson.getJSONArray("candidates")

            if (candidates.length() == 0) {
                throw Exception("No candidates in response")
            }

            val firstCandidate = candidates.getJSONObject(0)

            // Check if blocked due to safety
            if (firstCandidate.has("finishReason")) {
                val finishReason = firstCandidate.getString("finishReason")
                if (finishReason == "SAFETY" || finishReason == "OTHER") {
                    throw Exception("Response blocked: $finishReason")
                }
            }

            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            var textContent = parts.getJSONObject(0).getString("text").trim()

            Log.d("ScannerActivity", "Extracted text: $textContent")

            // Aggressive cleanup — Gemini sometimes wraps JSON in markdown
            textContent = textContent
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Find the JSON object in the text (in case there's extra text around it)
            val jsonStart = textContent.indexOf('{')
            val jsonEnd = textContent.lastIndexOf('}')
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                textContent = textContent.substring(jsonStart, jsonEnd + 1)
            }

            Log.d("ScannerActivity", "Cleaned JSON: $textContent")

            // Parse the JSON inside the text response
            val resultJson = JSONObject(textContent)

            // Parse causes — handle both array and missing field
            val causes = mutableListOf<String>()
            if (resultJson.has("possibleCauses")) {
                val causesArray = resultJson.getJSONArray("possibleCauses")
                for (i in 0 until causesArray.length()) {
                    causes.add(causesArray.getString(i))
                }
            }

            ScanResult(
                symbolName = resultJson.optString("symbolName", "Unknown Symbol"),
                severity = resultJson.optString("severity", "UNKNOWN").uppercase(),
                whatItMeans = resultJson.optString("whatItMeans", "No description available"),
                possibleCauses = causes,
                diyFix = resultJson.optString("diyFix", "Consult a professional technician"),
                needsTechnician = resultJson.optBoolean("needsTechnician", false),
                urgency = resultJson.optString("urgency", "N/A")
            )
        } catch (e: Exception) {
            Log.e("ScannerActivity", "Parse error: ${e.message}", e)
            Log.e("ScannerActivity", "Raw response was: $responseText")
            ScanResult(
                symbolName = "Analysis Error",
                severity = "UNKNOWN",
                whatItMeans = "Could not parse the analysis result. Error: ${e.message}",
                possibleCauses = listOf("Try taking a clearer photo", "Make sure the warning light is visible", "Check your internet connection"),
                diyFix = "Take a closer, well-lit photo of the dashboard warning light.",
                needsTechnician = false,
                urgency = "N/A"
            )
        }
    }

    // =========================================================
    // UI STATE MANAGEMENT
    // =========================================================
    private fun showEmptyState() {
        layoutEmpty.visibility = View.VISIBLE
        layoutLoading.visibility = View.GONE
        layoutResult.visibility = View.GONE
        ivPreview.visibility = View.GONE
        findViewById<LinearLayout>(R.id.layoutCameraPlaceholder)?.visibility = View.VISIBLE
    }

    private fun showLoading() {
        layoutEmpty.visibility = View.GONE
        layoutLoading.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE
    }

    private fun displayResult(result: ScanResult) {
        layoutEmpty.visibility = View.GONE
        layoutLoading.visibility = View.GONE
        layoutResult.visibility = View.VISIBLE

        tvSymbolName.text = result.symbolName
        tvWhatItMeans.text = result.whatItMeans
        tvDiyFix.text = result.diyFix

        tvSeverityBadge.text = result.severity
        tvSeverity.text = "• ${result.urgency}"

        when (result.severity) {
            "HIGH" -> {
                tvSeverityBadge.setBackgroundResource(R.drawable.severity_high_background)
                tvSeverityBadge.setTextColor(resources.getColor(android.R.color.white, null))
                tvSeverity.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
            }
            "MEDIUM" -> {
                tvSeverityBadge.setBackgroundResource(R.drawable.severity_medium_background)
                tvSeverityBadge.setTextColor(resources.getColor(android.R.color.white, null))
                tvSeverity.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            "LOW" -> {
                tvSeverityBadge.setBackgroundResource(R.drawable.severity_low_background)
                tvSeverityBadge.setTextColor(resources.getColor(android.R.color.white, null))
                tvSeverity.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            else -> {
                tvSeverityBadge.setBackgroundResource(R.drawable.severity_unknown_background)
                tvSeverityBadge.setTextColor(resources.getColor(android.R.color.white, null))
            }
        }

        tvCauses.text = if (result.possibleCauses.isNotEmpty())
            result.possibleCauses.joinToString("\n") { "• $it" }
        else "No causes detected"

        btnBookTechnician.visibility = if (result.needsTechnician) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // =========================================================
    // DATA CLASS
    // =========================================================
    data class ScanResult(
        val symbolName: String,
        val severity: String,
        val whatItMeans: String,
        val possibleCauses: List<String>,
        val diyFix: String,
        val needsTechnician: Boolean,
        val urgency: String
    )
}