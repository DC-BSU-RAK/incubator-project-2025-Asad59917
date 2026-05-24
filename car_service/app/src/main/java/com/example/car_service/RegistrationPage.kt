package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        firebaseHelper = FirebaseHelper(this)

        val etName = findViewById<TextInputEditText>(R.id.full_name_input)
        val etEmail = findViewById<TextInputEditText>(R.id.email_input)
        val etPassword = findViewById<TextInputEditText>(R.id.password_input)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.confirm_password_input)
        val btnSignUp = findViewById<MaterialButton>(R.id.sign_up_button)
        val tvSignIn = findViewById<TextView>(R.id.sign_in_link)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validation
            if (name.isEmpty()) {
                etName.error = "Name required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Invalid email"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be 6+ characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Disable button + show loading
            btnSignUp.isEnabled = false
            btnSignUp.text = "Creating account..."

            lifecycleScope.launch {
                // Pass empty phone since your form doesn't collect it
                val result = firebaseHelper.signUp(email, password, name, "")

                btnSignUp.isEnabled = true
                btnSignUp.text = "Sign up"

                if (result.isSuccess) {
                    Toast.makeText(this@SignUpActivity, "Account created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                    finishAffinity()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Sign up failed"
                    Toast.makeText(this@SignUpActivity, error, Toast.LENGTH_LONG).show()
                }
            }
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}