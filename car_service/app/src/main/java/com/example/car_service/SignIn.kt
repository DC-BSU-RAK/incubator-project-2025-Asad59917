package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.car_service.databinding.ActivitySignInBinding
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var prefsHelper: PrefsHelper  // Still used for "Remember Me" feature

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseHelper = FirebaseHelper(this)
        prefsHelper = PrefsHelper(this)

        // Auto-login if Firebase user is already signed in
        if (firebaseHelper.isLoggedIn()) {
            navigateToMainActivity()
            return
        }

        // Load remembered email if available
        loadRememberedEmail()

        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val rememberMe = binding.rememberMeSwitch.isChecked

            if (!validateInputs(email, password)) return@setOnClickListener

            // Disable button + show loading
            binding.signInButton.isEnabled = false
            binding.signInButton.text = "Signing in..."

            lifecycleScope.launch {
                val result = firebaseHelper.signIn(email, password)

                binding.signInButton.isEnabled = true
                binding.signInButton.text = "Sign in"

                if (result.isSuccess) {
                    // Handle remember me functionality
                    if (rememberMe) {
                        prefsHelper.saveRememberMe(email)
                    } else {
                        prefsHelper.clearRememberMe()
                    }

                    Toast.makeText(this@SignInActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(
                        this@SignInActivity,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loadRememberedEmail() {
        val rememberedEmail = prefsHelper.getRememberedEmail()
        if (rememberedEmail != null) {
            binding.emailInput.setText(rememberedEmail)
            binding.rememberMeSwitch.isChecked = true
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }
}