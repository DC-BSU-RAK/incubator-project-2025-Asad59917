package com.example.car_service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class BookingFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var bookingAdapter: BookingAdapter
    private var cancelBookingBottomSheet: BottomSheetDialog? = null
    private var bookingToCancel: FirebaseHelper.Booking? = null
    private var cancelPosition: Int = -1

    // Pro feature views
    private var btnExportHistory: CardView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseHelper = FirebaseHelper(requireContext())

        initViews(view)
        setupCancelDialog()
        setupRecyclerView()
        setupExportButton()
        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun initViews(view: View) {
        rvBookings = view.findViewById(R.id.rvBookings)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        btnExportHistory = view.findViewById(R.id.btnExportHistory)
    }

    // =========================================================
    // PRO: Export Booking History as PDF
    // =========================================================
    private fun setupExportButton() {
        btnExportHistory?.setOnClickListener {
            exportBookingHistoryPDF()
        }
    }

    private fun updateExportButtonVisibility(hasBookings: Boolean) {
        lifecycleScope.launch {
            val isPremium = firebaseHelper.isPremium()
            btnExportHistory?.visibility = if (isPremium && hasBookings) View.VISIBLE else View.GONE
        }
    }

    private fun exportBookingHistoryPDF() {
        lifecycleScope.launch {
            try {
                val firebaseBookings = firebaseHelper.getAllBookings()
                if (firebaseBookings.isEmpty()) {
                    Toast.makeText(requireContext(), "No bookings to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val customerName = firebaseHelper.getCurrentUserName() ?: "Customer"
                val pdfGenerator = PDFGenerator(requireContext())

                // Convert FirebaseHelper.Booking to PrefsHelper.Booking format for PDF compatibility
                val pdfBookings = firebaseBookings.map { fb ->
                    PrefsHelper.Booking(
                        serviceId = fb.serviceId,
                        serviceName = fb.serviceName,
                        vehicleBrand = fb.vehicleBrand,
                        vehicleModel = fb.vehicleModel,
                        plateNumber = fb.plateNumber,
                        bookingDate = fb.bookingDate,
                        location = fb.location,
                        price = fb.price
                    )
                }

                val file = pdfGenerator.generateBookingHistory(pdfBookings, customerName)

                if (file != null) {
                    Toast.makeText(
                        requireContext(),
                        "Booking history saved to Downloads/AutoMate",
                        Toast.LENGTH_LONG
                    ).show()
                    pdfGenerator.openPDF(file)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            bookings = mutableListOf(),
            onCancelClick = { booking, position ->
                showCancelConfirmation(booking, position)
            }
        )

        rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupCancelDialog() {
        cancelBookingBottomSheet = BottomSheetDialog(requireContext())
        val cancelView = layoutInflater.inflate(R.layout.cancel_booking_bottom_sheet, null)
        cancelBookingBottomSheet?.setContentView(cancelView)

        val btnKeepBooking = cancelView.findViewById<MaterialButton>(R.id.btnKeepBooking)
        val btnConfirmCancel = cancelView.findViewById<MaterialButton>(R.id.btnConfirmCancel)

        btnKeepBooking?.setOnClickListener { cancelBookingBottomSheet?.dismiss() }

        btnConfirmCancel?.setOnClickListener {
            confirmCancelBooking()
        }
    }

    private fun showCancelConfirmation(booking: FirebaseHelper.Booking, position: Int) {
        bookingToCancel = booking
        cancelPosition = position

        val cancelView = cancelBookingBottomSheet?.findViewById<View>(android.R.id.content)
        cancelView?.findViewById<TextView>(R.id.tvCancelServiceName)?.text = booking.serviceName
        cancelView?.findViewById<TextView>(R.id.tvCancelVehicleInfo)?.text =
            "${booking.vehicleBrand} ${booking.vehicleModel} - ${booking.plateNumber}"
        cancelView?.findViewById<TextView>(R.id.tvCancelDateTime)?.text = booking.bookingDate
        cancelView?.findViewById<TextView>(R.id.tvCancelLocation)?.text = booking.location

        cancelBookingBottomSheet?.show()
    }

    private fun confirmCancelBooking() {
        bookingToCancel?.let { booking ->
            lifecycleScope.launch {
                val success = firebaseHelper.cancelBooking(booking.id)

                if (success) {
                    bookingAdapter.removeBooking(cancelPosition)
                    Toast.makeText(requireContext(), "Booking cancelled successfully", Toast.LENGTH_SHORT).show()

                    if (bookingAdapter.isEmpty()) showEmptyState()
                    updateExportButtonVisibility(!bookingAdapter.isEmpty())
                } else {
                    Toast.makeText(requireContext(), "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cancelBookingBottomSheet?.dismiss()
        bookingToCancel = null
        cancelPosition = -1
    }

    private fun loadBookings() {
        lifecycleScope.launch {
            val allBookings = firebaseHelper.getAllBookings()

            // Hide cancelled bookings from user view
            val visibleBookings = allBookings.filter { it.status != "CANCELLED" }

            if (visibleBookings.isEmpty()) {
                showEmptyState()
            } else {
                showBookings(visibleBookings)
            }
            updateExportButtonVisibility(visibleBookings.isNotEmpty())
        }
    }

    private fun showEmptyState() {
        rvBookings.visibility = View.GONE
        emptyStateContainer.visibility = View.VISIBLE
    }

    private fun showBookings(bookings: List<FirebaseHelper.Booking>) {
        emptyStateContainer.visibility = View.GONE
        rvBookings.visibility = View.VISIBLE
        bookingAdapter.updateBookings(bookings)
    }

    fun refreshBookings() {
        loadBookings()
    }
}