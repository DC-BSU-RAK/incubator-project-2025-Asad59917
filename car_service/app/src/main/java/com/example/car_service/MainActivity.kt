package com.example.car_service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper(this)

        // Check if user is logged in via Firebase, if not redirect to SignInActivity
        if (!firebaseHelper.isLoggedIn()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()

        // Load home fragment by default
        if (savedInstanceState == null) {
            // Check if we should show bookings tab (e.g., after completing a booking)
            val showBookings = intent.getBooleanExtra("SHOW_BOOKINGS", false)
            if (showBookings) {
                loadFragment(BookingFragment())
                bottomNavigation.selectedItemId = R.id.nav_booking
            } else {
                loadFragment(HomeFragment())
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_notification -> {
                    loadFragment(NotificationFragment())
                    true
                }
                R.id.nav_booking -> {
                    loadFragment(BookingFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onBackPressed() {
        // Handle back press for fragments if needed
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (currentFragment is HomeFragment) {
            // If we're on home fragment, exit the app
            if (firebaseHelper.isLoggedIn()) {
                super.onBackPressed()
            } else {
                finish()
            }
        } else {
            // If we're on other fragments, go back to home
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }
}