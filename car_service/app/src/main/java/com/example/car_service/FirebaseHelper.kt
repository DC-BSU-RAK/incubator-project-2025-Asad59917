package com.example.car_service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseHelper(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val prefs: SharedPreferences = context.getSharedPreferences("automate_local", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseHelper"
        const val USERS = "users"
        const val VEHICLES = "vehicles"
        const val BOOKINGS = "bookings"
        const val NOTIFICATIONS = "notifications"
    }

    // ========================================================================
    // DATA CLASSES
    // ========================================================================
    data class User(
        val uid: String = "",
        val name: String = "",
        val email: String = "",
        val phone: String = "",
        val isPremium: Boolean = false,
        val premiumPlan: String = "",
        val premiumActivatedAt: Long = 0L,
        val premiumCancelledAt: Long = 0L,
        val freeScanCount: Int = 0,
        val createdAt: Long = System.currentTimeMillis()
    )

    data class Booking(
        val id: String = "",
        val userId: String = "",
        val userName: String = "",
        val userEmail: String = "",
        val serviceId: String = "",
        val serviceName: String = "",
        val vehicleBrand: String = "",
        val vehicleModel: String = "",
        val plateNumber: String = "",
        val bookingDate: String = "",
        val location: String = "",
        val price: Int = 0,
        val originalPrice: Int = 0,
        val isProDiscount: Boolean = false,
        val status: String = "PENDING",
        val createdAt: Long = System.currentTimeMillis()
    )

    data class FirebaseVehicle(
        val id: String = "",
        val userId: String = "",
        val type: String = "Car",
        val brand: String = "",
        val model: String = "",
        val color: String = "",
        val chassisNumber: String = "",
        val plateNumber: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    data class AppNotification(
        val id: String = "",
        val title: String = "",
        val message: String = "",
        val sentTo: String = "All Users",
        val audience: String = "all",
        val targetUserId: String = "",
        val recipients: Int = 0,
        val sentAt: Long = 0L,
        val createdAt: Long = System.currentTimeMillis()
    )

    // ========================================================================
    // AUTH METHODS
    // ========================================================================
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun signOut() {
        auth.signOut()
        prefs.edit().clear().apply()
    }

    suspend fun signUp(email: String, password: String, name: String, phone: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Sign up failed")

            val user = User(uid = uid, name = name, email = email, phone = phone)
            db.collection(USERS).document(uid).set(user).await()

            prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putString("phone", phone)
                .putBoolean("isPremium", false)
                .apply()

            Result.success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Sign in failed")

            val userDoc = db.collection(USERS).document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.let {
                prefs.edit()
                    .putString("name", it.name)
                    .putString("email", it.email)
                    .putString("phone", it.phone)
                    .putBoolean("isPremium", it.isPremium)
                    .apply()
            }

            Result.success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // USER METHODS
    // ========================================================================
    suspend fun getCurrentUser(): User? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            val user = doc.toObject(User::class.java)
            user?.let {
                prefs.edit().putBoolean("isPremium", it.isPremium).apply()
            }
            user
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUser failed", e)
            null
        }
    }

    fun getCurrentUserName(): String? = prefs.getString("name", null)

    suspend fun updateUserName(newName: String): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            db.collection(USERS).document(uid).update("name", newName).await()
            prefs.edit().putString("name", newName).apply()
            true
        } catch (e: Exception) { false }
    }

    // ========================================================================
    // PREMIUM METHODS
    // ========================================================================
    suspend fun isPremium(): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            val premium = doc.getBoolean("isPremium") ?: false
            Log.d(TAG, "isPremium() = $premium for uid=$uid")
            prefs.edit().putBoolean("isPremium", premium).apply()
            premium
        } catch (e: Exception) {
            Log.e(TAG, "isPremium() failed, falling back to cache", e)
            prefs.getBoolean("isPremium", false)
        }
    }

    fun isPremiumCached(): Boolean = prefs.getBoolean("isPremium", false)

    suspend fun setPremium(isPremium: Boolean, plan: String = "pro"): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val updates = mutableMapOf<String, Any>(
                "isPremium" to isPremium,
                "premiumPlan" to plan
            )
            if (isPremium) updates["premiumActivatedAt"] = System.currentTimeMillis()
            db.collection(USERS).document(uid).update(updates).await()
            prefs.edit().putBoolean("isPremium", isPremium).apply()
            Log.d(TAG, "setPremium($isPremium) success")
            true
        } catch (e: Exception) { Log.e(TAG, "setPremium failed", e); false }
    }

    suspend fun cancelPremium(): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            db.collection(USERS).document(uid).update(
                mapOf(
                    "isPremium" to false,
                    "premiumPlan" to "",
                    "premiumCancelledAt" to System.currentTimeMillis()
                )
            ).await()
            prefs.edit().putBoolean("isPremium", false).apply()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getFreeScanCount(): Int = getCurrentUser()?.freeScanCount ?: 0

    suspend fun incrementFreeScanCount(): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val user = getCurrentUser() ?: return false
            db.collection(USERS).document(uid).update("freeScanCount", user.freeScanCount + 1).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun hasFreeScanRemaining(): Boolean = getFreeScanCount() < 1

    // ========================================================================
    // VEHICLE METHODS
    // ========================================================================
    suspend fun saveVehicle(vehicle: Vehicle): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val docRef = db.collection(VEHICLES).document()
            val firebaseVehicle = FirebaseVehicle(
                id = docRef.id,
                userId = uid,
                type = vehicle.type,
                brand = vehicle.brand,
                model = vehicle.model,
                color = vehicle.color,
                chassisNumber = vehicle.chassisNumber,
                plateNumber = vehicle.plateNumber
            )
            docRef.set(firebaseVehicle).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveVehicle failed", e); false
        }
    }

    suspend fun getAllVehicles(): List<Vehicle> {
        val uid = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection(VEHICLES)
                .whereEqualTo("userId", uid)
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                val fv = doc.toObject(FirebaseVehicle::class.java) ?: return@mapNotNull null
                Vehicle(
                    type = fv.type,
                    brand = fv.brand,
                    model = fv.model,
                    color = fv.color,
                    chassisNumber = fv.chassisNumber,
                    plateNumber = fv.plateNumber
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllVehicles failed", e); emptyList()
        }
    }

    // ========================================================================
    // BOOKING METHODS
    // ========================================================================
    suspend fun saveBooking(booking: Booking): Boolean {
        return try {
            val docRef = db.collection(BOOKINGS).document()
            val newBooking = booking.copy(id = docRef.id)
            docRef.set(newBooking).await()
            true
        } catch (e: Exception) { Log.e(TAG, "saveBooking failed", e); false }
    }

    suspend fun getAllBookings(): List<Booking> {
        val uid = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection(BOOKINGS)
                .whereEqualTo("userId", uid)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) { Log.e(TAG, "getAllBookings failed", e); emptyList() }
    }

    suspend fun cancelBooking(bookingId: String): Boolean {
        return try {
            db.collection(BOOKINGS).document(bookingId).update("status", "CANCELLED").await()
            true
        } catch (e: Exception) { false }
    }

    // ========================================================================
    // NOTIFICATION METHODS
    // ========================================================================
    suspend fun getAllNotifications(): List<AppNotification> {
        val uid = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection(NOTIFICATIONS).get().await()
            snapshot.documents.mapNotNull { doc ->
                val notif = doc.toObject(AppNotification::class.java)
                val targetUserId = doc.getString("targetUserId") ?: ""

                // Show notification if it's for everyone OR specifically for this user
                if (targetUserId.isEmpty() || targetUserId == uid) {
                    notif?.copy(id = doc.id)
                } else null
            }.sortedByDescending { it.sentAt }
        } catch (e: Exception) {
            Log.e(TAG, "getAllNotifications failed", e)
            emptyList()
        }
    }

    suspend fun saveNotification(
        title: String,
        message: String,
        audience: String = "user",
        targetUserId: String? = null
    ): Boolean {
        return try {
            val docRef = db.collection(NOTIFICATIONS).document()
            val notif = hashMapOf(
                "id" to docRef.id,
                "title" to title,
                "message" to message,
                "audience" to audience,
                "sentTo" to (targetUserId ?: "All Users"),
                "targetUserId" to (targetUserId ?: ""),
                "sentAt" to System.currentTimeMillis(),
                "createdAt" to System.currentTimeMillis(),
                "recipients" to 1
            )
            docRef.set(notif).await()
            Log.d(TAG, "Notification saved: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveNotification failed", e)
            false
        }
    }

    // ========================================================================
    // LOCATION METHODS
    // ========================================================================
    fun saveLocation(type: String, address: String): Boolean {
        return prefs.edit().putString("loc_$type", address).commit()
    }

    fun getCurrentLocation(): String = prefs.getString("loc_Current", "") ?: ""
    fun getHomeLocation(): String = prefs.getString("loc_Home", "") ?: ""
    fun getWorkLocation(): String = prefs.getString("loc_Work", "") ?: ""
    fun getSelectedAddress(): String {
        return prefs.getString("loc_Current", null)
            ?: prefs.getString("loc_Home", null)
            ?: "Dubai, U.A.E"
    }
}