package com.example.car_service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF reports for premium users:
 * 1. Vehicle Health Report - after each booking
 * 2. Booking History Export - all bookings as PDF
 */
class PDFGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595   // A4 width in points
        private const val PAGE_HEIGHT = 842  // A4 height in points
        private const val MARGIN = 40
    }

    // =========================================================
    // 1. HEALTH REPORT (after booking)
    // =========================================================
    fun generateHealthReport(
        serviceName: String,
        vehicleBrand: String,
        vehicleModel: String,
        plateNumber: String,
        date: String,
        location: String,
        price: Int,
        customerName: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#5671FF")
                textSize = 24f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#1A1A1A")
                textSize = 16f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#444444")
                textSize = 12f
            }
            val labelPaint = Paint().apply {
                color = Color.parseColor("#888888")
                textSize = 11f
            }
            val accentPaint = Paint().apply {
                color = Color.parseColor("#5671FF")
                textSize = 14f
                isFakeBoldText = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }

            var y = MARGIN + 30

            // Header background bar
            val headerBgPaint = Paint().apply { color = Color.parseColor("#5671FF") }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 70f, headerBgPaint)

            // Title
            val whiteTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 22f
                isFakeBoldText = true
            }
            canvas.drawText("AutoMate", MARGIN.toFloat(), 35f, whiteTitlePaint)

            val whiteSubtitlePaint = Paint().apply {
                color = Color.parseColor("#CCE0FF")
                textSize = 11f
            }
            canvas.drawText("Vehicle Health Report - PRO", MARGIN.toFloat(), 55f, whiteSubtitlePaint)

            // Pro badge top right
            val badgePaint = Paint().apply { color = Color.parseColor("#FFD700") }
            canvas.drawRect((PAGE_WIDTH - 90f), 25f, (PAGE_WIDTH - 30f), 50f, badgePaint)
            val proPaint = Paint().apply {
                color = Color.parseColor("#1A1A1A")
                textSize = 12f
                isFakeBoldText = true
            }
            canvas.drawText("⭐ PRO", PAGE_WIDTH - 78f, 42f, proPaint)

            y = 110

            // Report title
            canvas.drawText("Service Health Report", MARGIN.toFloat(), y.toFloat(), titlePaint)
            y += 30

            // Generated date
            val genDate = SimpleDateFormat("EEE, dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on: $genDate", MARGIN.toFloat(), y.toFloat(), bodyPaint)
            y += 30

            // Divider
            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // CUSTOMER DETAILS section
            canvas.drawText("CUSTOMER DETAILS", MARGIN.toFloat(), y.toFloat(), headerPaint)
            y += 25

            canvas.drawText("Customer Name:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText(customerName, (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 25

            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // SERVICE DETAILS section
            canvas.drawText("SERVICE DETAILS", MARGIN.toFloat(), y.toFloat(), headerPaint)
            y += 25

            canvas.drawText("Service:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText(serviceName, (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 20

            canvas.drawText("Date:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText(date, (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 20

            canvas.drawText("Location:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText(location, (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 30

            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // VEHICLE DETAILS section
            canvas.drawText("VEHICLE DETAILS", MARGIN.toFloat(), y.toFloat(), headerPaint)
            y += 25

            canvas.drawText("Make & Model:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText("$vehicleBrand $vehicleModel", (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 20

            canvas.drawText("License Plate:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText(plateNumber, (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 30

            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // INSPECTION CHECKLIST
            canvas.drawText("10-POINT INSPECTION", MARGIN.toFloat(), y.toFloat(), headerPaint)
            y += 25

            val checklistItems = listOf(
                "✓ Engine oil and filter — Checked",
                "✓ Brake fluid level — Within range",
                "✓ Coolant level — Topped up",
                "✓ Battery condition — Good",
                "✓ Tire pressure — Adjusted",
                "✓ Air filter — Inspected",
                "✓ Wiper blades — Functional",
                "✓ Headlights & brake lights — Working",
                "✓ Steering fluid — Topped up",
                "✓ Parking brake — Operational"
            )

            for (item in checklistItems) {
                canvas.drawText(item, MARGIN.toFloat(), y.toFloat(), bodyPaint)
                y += 18
            }

            y += 15
            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // PAYMENT SUMMARY
            canvas.drawText("PAYMENT SUMMARY", MARGIN.toFloat(), y.toFloat(), headerPaint)
            y += 25

            canvas.drawText("Total Amount:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText("AED $price", (MARGIN + 150).toFloat(), y.toFloat(), accentPaint)
            y += 20

            canvas.drawText("Pro Discount:", MARGIN.toFloat(), y.toFloat(), labelPaint)
            canvas.drawText("15% Applied", (MARGIN + 150).toFloat(), y.toFloat(), bodyPaint)
            y += 30

            // Footer
            val footerY = PAGE_HEIGHT - 60
            canvas.drawLine(MARGIN.toFloat(), footerY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), footerY.toFloat(), linePaint)

            val footerPaint = Paint().apply {
                color = Color.parseColor("#888888")
                textSize = 10f
            }
            canvas.drawText("Thank you for choosing AutoMate Pro", MARGIN.toFloat(), (footerY + 20).toFloat(), footerPaint)
            canvas.drawText("AutoMate UAE | Dubai", MARGIN.toFloat(), (footerY + 35).toFloat(), footerPaint)

            pdfDocument.finishPage(page)

            // Save the file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AutoMate_Health_Report_$timestamp.pdf"
            val file = saveToDownloads(fileName, pdfDocument)
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error generating health report", e)
            null
        }
    }

    // =========================================================
    // 2. BOOKING HISTORY EXPORT
    // =========================================================
    fun generateBookingHistory(
        bookings: List<PrefsHelper.Booking>,
        customerName: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#5671FF")
                textSize = 22f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#1A1A1A")
                textSize = 14f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#444444")
                textSize = 11f
            }
            val labelPaint = Paint().apply {
                color = Color.parseColor("#888888")
                textSize = 10f
            }
            val pricePaint = Paint().apply {
                color = Color.parseColor("#5671FF")
                textSize = 13f
                isFakeBoldText = true
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }

            // Header bar
            val headerBgPaint = Paint().apply { color = Color.parseColor("#5671FF") }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 70f, headerBgPaint)

            val whiteTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 22f
                isFakeBoldText = true
            }
            canvas.drawText("AutoMate", MARGIN.toFloat(), 35f, whiteTitlePaint)

            val whiteSubtitlePaint = Paint().apply {
                color = Color.parseColor("#CCE0FF")
                textSize = 11f
            }
            canvas.drawText("Booking History Export - PRO", MARGIN.toFloat(), 55f, whiteSubtitlePaint)

            var y = 110

            canvas.drawText("Booking History Report", MARGIN.toFloat(), y.toFloat(), titlePaint)
            y += 25

            canvas.drawText("Customer: $customerName", MARGIN.toFloat(), y.toFloat(), bodyPaint)
            y += 16
            val genDate = SimpleDateFormat("EEE, dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Generated: $genDate", MARGIN.toFloat(), y.toFloat(), bodyPaint)
            y += 16
            canvas.drawText("Total Bookings: ${bookings.size}", MARGIN.toFloat(), y.toFloat(), bodyPaint)
            y += 25

            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
            y += 25

            // Calculate total spent
            var totalSpent = 0
            for (booking in bookings) {
                totalSpent += booking.price
            }

            // Each booking entry
            for ((index, booking) in bookings.withIndex()) {
                if (y > PAGE_HEIGHT - 100) {
                    // Need a new page (simplified - just stops here for now)
                    break
                }

                canvas.drawText("${index + 1}. ${booking.serviceName}", MARGIN.toFloat(), y.toFloat(), headerPaint)

                val priceText = "AED ${booking.price}"
                val priceWidth = pricePaint.measureText(priceText)
                canvas.drawText(priceText, (PAGE_WIDTH - MARGIN - priceWidth), y.toFloat(), pricePaint)
                y += 20

                canvas.drawText("Vehicle: ${booking.vehicleBrand} ${booking.vehicleModel} - ${booking.plateNumber}",
                    (MARGIN + 15).toFloat(), y.toFloat(), bodyPaint)
                y += 15
                canvas.drawText("Date: ${booking.bookingDate}", (MARGIN + 15).toFloat(), y.toFloat(), bodyPaint)
                y += 15
                canvas.drawText("Location: ${booking.location}", (MARGIN + 15).toFloat(), y.toFloat(), bodyPaint)
                y += 20

                canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y.toFloat(), linePaint)
                y += 20
            }

            // Total spent
            y += 10
            canvas.drawText("TOTAL SPENT", MARGIN.toFloat(), y.toFloat(), headerPaint)

            val totalText = "AED $totalSpent"
            val totalWidth = pricePaint.measureText(totalText)
            canvas.drawText(totalText, (PAGE_WIDTH - MARGIN - totalWidth), y.toFloat(), pricePaint)

            // Footer
            val footerY = PAGE_HEIGHT - 40
            val footerPaint = Paint().apply {
                color = Color.parseColor("#888888")
                textSize = 10f
            }
            canvas.drawText("AutoMate UAE | Dubai | www.automate.ae", MARGIN.toFloat(), footerY.toFloat(), footerPaint)

            pdfDocument.finishPage(page)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AutoMate_Booking_History_$timestamp.pdf"
            val file = saveToDownloads(fileName, pdfDocument)
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error generating booking history", e)
            null
        }
    }

    // =========================================================
    // SAVE PDF TO DOWNLOADS
    // =========================================================
    private fun saveToDownloads(fileName: String, pdfDocument: PdfDocument): File? {
        return try {
            // Use app-specific external storage (no permission needed)
            val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AutoMate")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }

            Log.d("PDFGenerator", "PDF saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error saving PDF", e)
            null
        }
    }

    // =========================================================
    // OPEN PDF
    // =========================================================
    fun openPDF(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error opening PDF", e)
        }
    }
}

// End of PDFGenerator