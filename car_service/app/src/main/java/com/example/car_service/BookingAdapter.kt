package com.example.car_service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookingAdapter(
    private var bookings: MutableList<FirebaseHelper.Booking> = mutableListOf(),
    private val onCancelClick: ((FirebaseHelper.Booking, Int) -> Unit)? = null
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvVehicleInfo: TextView = itemView.findViewById(R.id.tvVehicleInfo)
        val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnCancelBooking: TextView = itemView.findViewById(R.id.btnCancelBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        with(holder) {
            tvServiceName.text = booking.serviceName
            tvPrice.text = "AED ${booking.price}"
            tvVehicleInfo.text = "${booking.vehicleBrand} ${booking.vehicleModel} - ${booking.plateNumber}"
            tvLocation.text = booking.location
            tvBookingDate.text = booking.bookingDate

            // ✅ Show real status from admin: PENDING / CONFIRMED / COMPLETED
            tvStatus.text = booking.status

            // ✅ Only allow cancel for PENDING or CONFIRMED bookings
            if (onCancelClick != null && (booking.status == "PENDING" || booking.status == "CONFIRMED")) {
                btnCancelBooking.visibility = View.VISIBLE
                btnCancelBooking.setOnClickListener {
                    onCancelClick.invoke(booking, position)
                }
            } else {
                btnCancelBooking.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<FirebaseHelper.Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    fun removeBooking(position: Int) {
        if (position in 0 until bookings.size) {
            bookings.removeAt(position)
            notifyItemRemoved(position)
            if (position < bookings.size) {
                notifyItemRangeChanged(position, bookings.size - position)
            }
        }
    }

    fun isEmpty(): Boolean = bookings.isEmpty()
}