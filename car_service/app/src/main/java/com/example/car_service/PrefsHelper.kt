package com.example.car_service

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class PrefsHelper(context: Context) {
    private val context: Context = context.applicationContext
    private val sharedPref: SharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    private val USER_DATA_FILE = "user_data.json"

    data class Booking(
        val serviceId: String,
        val serviceName: String,
        val vehicleBrand: String,
        val vehicleModel: String,
        val plateNumber: String,
        val bookingDate: String,
        val location: String,
        val price: Int,
        val bookingTime: Long = System.currentTimeMillis()
    )

    enum class PasswordUpdateResult {
        SUCCESS,
        WRONG_CURRENT_PASSWORD,
        ERROR
    }

    // =========================================================
    // PREMIUM METHODS — NEW
    // =========================================================

    /**
     * Check if current user has an active premium subscription.
     * Returns true if user is Pro or Elite plan.
     */
    fun isPremium(): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.optBoolean("isPremium", false)
            }
        }
        return false
    }

    /**
     * Get the premium plan name: "pro", "elite", or ""
     */
    fun getPremiumPlan(): String {
        val currentUserEmail = getCurrentUserEmail() ?: return ""
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.optString("premiumPlan", "")
            }
        }
        return ""
    }

    /**
     * Activate premium for the current user.
     * Call this after successful payment.
     * @param plan "pro" or "elite"
     */
    fun setPremium(isPremium: Boolean, plan: String = "") {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("isPremium", isPremium)
                user.put("premiumPlan", plan)
                if (isPremium) {
                    user.put("premiumActivatedAt", System.currentTimeMillis())
                }
                break
            }
        }
        saveUserData(userData)
    }

    /**
     * Cancel the current user's premium subscription.
     * Downgrades them back to the free plan.
     */
    fun cancelPremium(): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("isPremium", false)
                user.put("premiumPlan", "")
                user.put("premiumCancelledAt", System.currentTimeMillis())
                return saveUserData(userData)
            }
        }
        return false
    }

    /**
     * How many scanner scans a free user has left (limit: 1 free scan)
     */
    fun getFreeScanCount(): Int {
        return sharedPref.getInt("FREE_SCAN_COUNT_${getCurrentUserEmail()}", 0)
    }

    fun incrementFreeScanCount() {
        val key = "FREE_SCAN_COUNT_${getCurrentUserEmail()}"
        val current = sharedPref.getInt(key, 0)
        sharedPref.edit().putInt(key, current + 1).apply()
    }

    fun hasFreeScanRemaining(): Boolean {
        return getFreeScanCount() < 1 // 1 free scan per account
    }

    // =========================================================
    // AUTHENTICATION METHODS (unchanged)
    // =========================================================

    fun registerUser(email: String, password: String, fullName: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            if (users.getJSONObject(i).getString("email").equals(email, ignoreCase = true)) {
                return false
            }
        }

        val newUser = JSONObject().apply {
            put("email", email)
            put("password", hashPassword(password))
            put("fullName", fullName)
            put("isLoggedIn", false)
            put("isPremium", false)
            put("premiumPlan", "")
            put("locations", JSONObject().apply {
                put("selectedLocationType", "Current")
                put("selectedAddress", "Dubai, U.A.E")
                put("homeAddress", "")
                put("workAddress", "")
                put("currentAddress", "Dubai, U.A.E")
            })
            put("vehicles", JSONArray())
            put("bookings", JSONArray())
        }

        users.put(newUser)
        return saveUserData(userData)
    }

    fun loginUser(email: String, password: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email").equals(email, ignoreCase = true) &&
                user.getString("password") == hashPassword(password)) {

                user.put("isLoggedIn", true)
                if (saveUserData(userData)) {
                    sharedPref.edit().putString("CURRENT_USER", email).apply()
                    return true
                }
            }
        }
        return false
    }

    fun isLoggedIn(): Boolean {
        val currentUserEmail = sharedPref.getString("CURRENT_USER", null) ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.getBoolean("isLoggedIn")) {
                return true
            }
        }
        return false
    }

    fun getCurrentUserEmail(): String? = sharedPref.getString("CURRENT_USER", null)

    fun getCurrentUserName(): String? {
        val currentUserEmail = getCurrentUserEmail() ?: return null
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.optString("fullName", null)
            }
        }
        return null
    }

    fun logout(clearRememberMe: Boolean = false) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("isLoggedIn", false)
                saveUserData(userData)
                break
            }
        }

        sharedPref.edit().remove("CURRENT_USER").apply()
        if (clearRememberMe) clearRememberMe()
    }

    // =========================================================
    // PROFILE UPDATE METHODS (unchanged)
    // =========================================================

    fun updateUserName(newName: String): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("fullName", newName)
                return saveUserData(userData)
            }
        }
        return false
    }

    fun updateUserPassword(currentPassword: String, newPassword: String): PasswordUpdateResult {
        val currentUserEmail = getCurrentUserEmail() ?: return PasswordUpdateResult.ERROR
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                if (user.getString("password") != hashPassword(currentPassword)) {
                    return PasswordUpdateResult.WRONG_CURRENT_PASSWORD
                }
                user.put("password", hashPassword(newPassword))
                return if (saveUserData(userData)) PasswordUpdateResult.SUCCESS
                else PasswordUpdateResult.ERROR
            }
        }
        return PasswordUpdateResult.ERROR
    }

    // =========================================================
    // REMEMBER ME (unchanged)
    // =========================================================

    fun saveRememberMe(email: String) {
        sharedPref.edit().apply {
            putBoolean("REMEMBER_ME", true)
            putString("REMEMBERED_EMAIL", email)
            apply()
        }
    }

    fun clearRememberMe() {
        sharedPref.edit().apply {
            remove("REMEMBER_ME")
            remove("REMEMBERED_EMAIL")
            apply()
        }
    }

    fun shouldRememberUser(): Boolean = sharedPref.getBoolean("REMEMBER_ME", false)

    fun getRememberedEmail(): String? =
        if (shouldRememberUser()) sharedPref.getString("REMEMBERED_EMAIL", null) else null

    fun canAutoLogin(): Boolean = shouldRememberUser() && getCurrentUserEmail() != null && isLoggedIn()

    // =========================================================
    // LOCATION METHODS (unchanged)
    // =========================================================

    fun saveLocation(locationType: String, address: String) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                val locations = user.getJSONObject("locations")
                locations.put("selectedLocationType", locationType)
                locations.put("selectedAddress", address)
                when (locationType) {
                    "Home" -> locations.put("homeAddress", address)
                    "Work" -> locations.put("workAddress", address)
                    "Current" -> locations.put("currentAddress", address)
                }
                break
            }
        }
        saveUserData(userData)
    }

    fun getLocationType(): String =
        getCurrentUserLocations()?.optString("selectedLocationType", "Current") ?: "Current"

    fun getSelectedAddress(): String {
        val locations = getCurrentUserLocations() ?: return "Dubai, U.A.E"
        val selectedAddress = locations.optString("selectedAddress", null)
        return selectedAddress ?: when (getLocationType()) {
            "Home" -> getHomeLocation().ifEmpty { "Dubai, U.A.E" }
            "Work" -> getWorkLocation().ifEmpty { "Dubai, U.A.E" }
            else -> getCurrentLocation()
        }
    }

    fun saveHomeLocation(address: String) = saveUserLocation("homeAddress", address)
    fun saveWorkLocation(address: String) = saveUserLocation("workAddress", address)
    fun saveCurrentLocation(address: String) = saveUserLocation("currentAddress", address)

    fun getHomeLocation(): String = getUserLocation("homeAddress")
    fun getWorkLocation(): String = getUserLocation("workAddress")
    fun getCurrentLocation(): String = getUserLocation("currentAddress", "Dubai, U.A.E")

    // =========================================================
    // VEHICLE METHODS (unchanged)
    // =========================================================

    fun saveVehicle(vehicle: Vehicle) {
        saveVehicle(vehicle.type, vehicle.brand, vehicle.model,
            vehicle.color, vehicle.chassisNumber, vehicle.plateNumber)
    }

    fun saveVehicle(
        type: String, brand: String, model: String,
        color: String, chassisNumber: String, plateNumber: String
    ) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                val vehicles = user.getJSONArray("vehicles")
                vehicles.put(JSONObject().apply {
                    put("type", type); put("brand", brand); put("model", model)
                    put("color", color); put("chassisNumber", chassisNumber)
                    put("plateNumber", plateNumber)
                })
                break
            }
        }
        saveUserData(userData)
    }

    fun getVehicleCount(): Int = getCurrentUserVehicles()?.length() ?: 0

    fun getVehicle(index: Int): Vehicle? {
        val vehicles = getCurrentUserVehicles() ?: return null
        if (index < 0 || index >= vehicles.length()) return null
        val v = vehicles.getJSONObject(index)
        return Vehicle(
            type = v.getString("type"),
            brand = v.getString("brand"),
            model = v.getString("model"),
            color = v.optString("color", ""),
            chassisNumber = v.optString("chassisNumber", ""),
            plateNumber = v.getString("plateNumber"),
            isSelected = false
        )
    }

    fun getAllVehicles(): List<Vehicle> {
        return (0 until getVehicleCount()).mapNotNull { getVehicle(it) }
    }

    fun getAllVehiclesWithSelection(selectedPlateNumber: String? = null): List<Vehicle> {
        return getAllVehicles().map { it.copy(isSelected = it.plateNumber == selectedPlateNumber) }
    }

    fun clearVehicles() {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("vehicles", JSONArray()); break
            }
        }
        saveUserData(userData)
    }

    // =========================================================
    // BOOKING METHODS (unchanged)
    // =========================================================

    fun saveBooking(booking: Booking) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                if (!user.has("bookings")) user.put("bookings", JSONArray())
                user.getJSONArray("bookings").put(JSONObject().apply {
                    put("serviceId", booking.serviceId)
                    put("serviceName", booking.serviceName)
                    put("vehicleBrand", booking.vehicleBrand)
                    put("vehicleModel", booking.vehicleModel)
                    put("plateNumber", booking.plateNumber)
                    put("bookingDate", booking.bookingDate)
                    put("location", booking.location)
                    put("price", booking.price)
                    put("bookingTime", booking.bookingTime)
                })
                break
            }
        }
        saveUserData(userData)
    }

    fun getAllBookings(): List<Booking> {
        val currentUserEmail = getCurrentUserEmail() ?: return emptyList()
        val userData = loadUserData()
        val users = userData.getJSONArray("users")
        val bookings = mutableListOf<Booking>()

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.has("bookings")) {
                val arr = user.getJSONArray("bookings")
                for (j in 0 until arr.length()) {
                    val b = arr.getJSONObject(j)
                    bookings.add(Booking(
                        b.getString("serviceId"), b.getString("serviceName"),
                        b.getString("vehicleBrand"), b.getString("vehicleModel"),
                        b.getString("plateNumber"), b.getString("bookingDate"),
                        b.getString("location"), b.getInt("price"), b.getLong("bookingTime")
                    ))
                }
                break
            }
        }
        return bookings.sortedByDescending { it.bookingTime }
    }

    fun cancelBooking(bookingToCancel: Booking): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.has("bookings")) {
                val bookings = user.getJSONArray("bookings")
                for (j in bookings.length() - 1 downTo 0) {
                    val b = bookings.getJSONObject(j)
                    if (b.getString("serviceId") == bookingToCancel.serviceId &&
                        b.getString("plateNumber") == bookingToCancel.plateNumber &&
                        b.getLong("bookingTime") == bookingToCancel.bookingTime) {
                        bookings.remove(j)
                        return saveUserData(userData)
                    }
                }
                break
            }
        }
        return false
    }

    fun getBookingByIndex(index: Int): Booking? {
        val all = getAllBookings()
        return if (index in 0 until all.size) all[index] else null
    }

    // =========================================================
    // PRIVATE HELPERS (unchanged)
    // =========================================================

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) { password }
    }

    private fun saveUserLocation(key: String, address: String) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.getJSONObject("locations").put(key, address); break
            }
        }
        saveUserData(userData)
    }

    private fun getUserLocation(key: String, default: String = ""): String =
        getCurrentUserLocations()?.optString(key, default) ?: default

    private fun getCurrentUserLocations(): JSONObject? {
        val email = getCurrentUserEmail() ?: return null
        val users = loadUserData().getJSONArray("users")
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == email) return user.getJSONObject("locations")
        }
        return null
    }

    private fun getCurrentUserVehicles(): JSONArray? {
        val email = getCurrentUserEmail() ?: return null
        val users = loadUserData().getJSONArray("users")
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == email) return user.getJSONArray("vehicles")
        }
        return null
    }

    private fun loadUserData(): JSONObject {
        return try {
            val file = File(context.filesDir, USER_DATA_FILE)
            if (!file.exists()) return JSONObject().apply { put("users", JSONArray()) }
            JSONObject(file.readText())
        } catch (e: Exception) {
            JSONObject().apply { put("users", JSONArray()) }
        }
    }

    private fun saveUserData(data: JSONObject): Boolean {
        return try {
            File(context.filesDir, USER_DATA_FILE).writeText(data.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }
}