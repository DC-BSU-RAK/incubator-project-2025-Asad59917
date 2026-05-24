package com.example.car_service

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var tvProfileName: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var backButton: ImageButton
    private lateinit var cvSignOut: CardView
    private lateinit var cvTermsConditions: CardView
    private lateinit var cvChangeName: CardView
    private lateinit var cvChangePassword: CardView

    // Premium views
    private lateinit var layoutProBadge: LinearLayout
    private lateinit var cvProBadgeAvatar: CardView
    private lateinit var cvActivePro: CardView
    private lateinit var cvUpgradeToPro: CardView
    private lateinit var btnCancelSubscription: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseHelper = FirebaseHelper(requireContext())

        initViews(view)
        loadUserData()
        updatePremiumUI()
        setClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh premium status every time fragment becomes visible
        updatePremiumUI()
        loadUserData()
    }

    private fun initViews(view: View) {
        tvProfileName    = view.findViewById(R.id.tvProfileName)
        tvUserName       = view.findViewById(R.id.tvUserName)
        tvUserEmail      = view.findViewById(R.id.tvUserEmail)
        backButton       = view.findViewById(R.id.backButton)
        cvSignOut        = view.findViewById(R.id.cvSignOut)
        cvTermsConditions= view.findViewById(R.id.cvTermsConditions)
        cvChangeName     = view.findViewById(R.id.cvChangeName)
        cvChangePassword = view.findViewById(R.id.cvChangePassword)

        // Premium views
        layoutProBadge   = view.findViewById(R.id.layoutProBadge)
        cvProBadgeAvatar = view.findViewById(R.id.cvProBadgeAvatar)
        cvActivePro      = view.findViewById(R.id.cvActivePro)
        cvUpgradeToPro   = view.findViewById(R.id.cvUpgradeToPro)
        btnCancelSubscription = view.findViewById(R.id.btnCancelSubscription)
    }

    // =========================================================
    // PREMIUM UI (now async from Firebase)
    // =========================================================
    private fun updatePremiumUI() {
        lifecycleScope.launch {
            val isPremium = firebaseHelper.isPremium()

            if (isPremium) {
                layoutProBadge.visibility   = View.VISIBLE
                cvProBadgeAvatar.visibility = View.VISIBLE
                cvActivePro.visibility      = View.VISIBLE
                cvUpgradeToPro.visibility   = View.GONE
            } else {
                layoutProBadge.visibility   = View.GONE
                cvProBadgeAvatar.visibility = View.GONE
                cvActivePro.visibility      = View.GONE
                cvUpgradeToPro.visibility   = View.VISIBLE
            }
        }
    }

    // =========================================================
    // LOAD USER DATA (from Firebase)
    // =========================================================
    private fun loadUserData() {
        lifecycleScope.launch {
            val user = firebaseHelper.getCurrentUser()

            if (user != null && user.email.isNotEmpty()) {
                tvUserEmail.text = user.email

                val displayName = if (user.name.isNotEmpty()) {
                    user.name
                } else {
                    formatName(user.email.substringBefore("@"))
                }

                tvProfileName.text = displayName
                tvUserName.text    = displayName
            } else {
                tvProfileName.text = "Guest User"
                tvUserName.text    = "Guest User"
                tvUserEmail.text   = "No email available"
            }
        }
    }

    private fun formatName(nameFromEmail: String): String {
        val cleanName = nameFromEmail
            .replace(Regex("[0-9]"), "")
            .replace(".", " ").replace("_", " ").replace("-", " ")

        return cleanName.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }.ifEmpty { "User" }
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================
    private fun setClickListeners() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Upgrade card → go to payment
        cvUpgradeToPro.setOnClickListener {
            startActivity(Intent(requireContext(), PaymentActivity::class.java))
        }

        // Cancel subscription button
        btnCancelSubscription.setOnClickListener {
            showCancelSubscriptionDialog()
        }

        cvTermsConditions.setOnClickListener {
            startActivity(Intent(requireContext(), TermsConditionsActivity::class.java))
        }

        cvChangeName.setOnClickListener { showChangeNameDialog() }

        cvChangePassword.setOnClickListener { showChangePasswordDialog() }

        cvSignOut.setOnClickListener { showSignOutDialog() }
    }

    // =========================================================
    // CANCEL SUBSCRIPTION (Firebase)
    // =========================================================
    private fun showCancelSubscriptionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Cancel Pro Subscription?")
        builder.setMessage(
            "Are you sure you want to cancel your AutoMate Pro subscription?\n\n" +
                    "You will lose access to:\n" +
                    "• Camera Warning Light Scanner\n" +
                    "• Digital Vehicle Health Reports\n" +
                    "• Booking History Export\n" +
                    "• Service Reminder AI\n\n" +
                    "This action cannot be undone."
        )
        builder.setPositiveButton("Yes, Cancel") { dialog, _ ->
            lifecycleScope.launch {
                val success = firebaseHelper.cancelPremium()
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Your Pro subscription has been cancelled",
                        Toast.LENGTH_LONG
                    ).show()
                    updatePremiumUI()  // Refresh UI to hide Pro badges
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to cancel subscription. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Keep Pro") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // =========================================================
    // CHANGE NAME (Firebase)
    // =========================================================
    private fun showChangeNameDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_change_name)
        configureDialogWindow(dialog)

        val etNewName  = dialog.findViewById<EditText>(R.id.etNewName)
        val btnSave    = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel  = dialog.findViewById<Button>(R.id.btnCancel)

        // Pre-fill current name
        val currentName = firebaseHelper.getCurrentUserName()
        if (!currentName.isNullOrEmpty()) {
            etNewName.setText(currentName)
            etNewName.setSelection(currentName.length)
        }

        btnSave.setOnClickListener {
            val newName = etNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = firebaseHelper.updateUserName(newName)
                    if (success) {
                        Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show()
                        loadUserData()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // =========================================================
    // CHANGE PASSWORD (Firebase Auth)
    // =========================================================
    private fun showChangePasswordDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_change_password)
        configureDialogWindow(dialog)

        val etCurrentPassword = dialog.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword     = dialog.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialog.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave           = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel         = dialog.findViewById<Button>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword     = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            when {
                currentPassword.isEmpty() ->
                    Toast.makeText(requireContext(), "Please enter your current password", Toast.LENGTH_SHORT).show()
                newPassword.isEmpty() ->
                    Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                newPassword.length < 6 ->
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                newPassword != confirmPassword ->
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                else -> {
                    changeFirebasePassword(currentPassword, newPassword, dialog)
                }
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Change Firebase Auth password.
     * Firebase requires re-authenticating the user before changing password.
     */
    private fun changeFirebasePassword(currentPassword: String, newPassword: String, dialog: Dialog) {
        val user = Firebase.auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Step 1: Re-authenticate with current password
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()

                // Step 2: Update password
                user.updatePassword(newPassword).await()

                Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("password is invalid", ignoreCase = true) == true ->
                        "Current password is incorrect"
                    e.message?.contains("credential is malformed", ignoreCase = true) == true ->
                        "Current password is incorrect"
                    else -> "Failed to update password: ${e.message}"
                }
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================
    // SIGN OUT (Firebase)
    // =========================================================
    private fun showSignOutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_signout_confirmation)
        configureDialogWindow(dialog)

        dialog.findViewById<Button>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            signOut()
        }
        dialog.findViewById<Button>(R.id.btnNo).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun configureDialogWindow(dialog: Dialog) {
        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setGravity(Gravity.BOTTOM)
            it.setWindowAnimations(R.style.DialogSlideAnimation)
            it.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.transparent))
        }
    }

    private fun signOut() {
        firebaseHelper.signOut()
        startActivity(Intent(requireContext(), SignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        requireActivity().finish()
    }
}