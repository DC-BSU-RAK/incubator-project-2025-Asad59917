package com.example.car_service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var prefsHelper: PrefsHelper  // Still used for locations (local cache)
    private lateinit var locationBottomSheet: BottomSheetDialog
    private lateinit var addLocationBottomSheet: BottomSheetDialog
    private lateinit var vehicleBottomSheet: BottomSheetDialog
    private var selectedVehicleIndex = -1

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseHelper = FirebaseHelper(requireContext())
        prefsHelper = PrefsHelper(requireContext())

        if (!firebaseHelper.isLoggedIn()) {
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
            return
        }

        loadUserLocation()
        setupNavigation()
        setupLocationAndVehicles()
        setupServiceCards()
        setupSpecialOffers()
        setupPremiumScanner()

        setupLocationBottomSheet()
        setupAddLocationBottomSheet()
        setupVehicleBottomSheet()
    }

    // =========================================================
    // PREMIUM SCANNER SETUP (now using Firebase)
    // =========================================================
    private fun setupPremiumScanner() {
        val scannerBanner = view?.findViewById<CardView>(R.id.premiumScannerBanner)
        val scanNowButton = view?.findViewById<CardView>(R.id.scanNowButton)

        // Check premium status from Firebase (async)
        lifecycleScope.launch {
            val isPremium = firebaseHelper.isPremium()

            if (isPremium) {
                // Premium user - go directly to scanner
                scanNowButton?.setOnClickListener { openScanner() }
                scannerBanner?.setOnClickListener { openScanner() }
            } else {
                // Free user - show upgrade dialog
                scanNowButton?.setOnClickListener { showPremiumUpgradeDialog() }
                scannerBanner?.setOnClickListener { showPremiumUpgradeDialog() }
            }
        }
    }

    private fun openScanner() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(requireContext(), ScannerActivity::class.java))
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    private fun showPremiumUpgradeDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_premium_upgrade, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById<CardView>(R.id.upgradeProButton)?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), PaymentActivity::class.java))
        }

        dialogView.findViewById<TextView>(R.id.maybeLaterButton)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(requireContext(), ScannerActivity::class.java))
        } else {
            Toast.makeText(requireContext(),
                "Camera permission required for scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vehicleBottomSheet.isInitialized && vehicleBottomSheet.isShowing) {
            setupVehicleBottomSheet()
        }
        // Refresh premium banner on resume (in case user just upgraded)
        setupPremiumScanner()
    }

    private fun loadUserLocation() {
        val locationTitle = view?.findViewById<TextView>(R.id.locationTitle)
        val address = prefsHelper.getSelectedAddress()

        if (address.isNotEmpty() && address != "Dubai, U.A.E") {
            locationTitle?.text = address
        } else {
            locationTitle?.text = "Select location"
        }
    }

    private fun setupLocationBottomSheet() {
        locationBottomSheet = BottomSheetDialog(requireContext())
        val locationView = layoutInflater.inflate(R.layout.location_bottom_sheet, null)
        locationBottomSheet.setContentView(locationView)

        val currentLocationItem = locationView.findViewById<View>(R.id.currentLocationItem)
        val homeLocationItem = locationView.findViewById<View>(R.id.homeLocationItem)
        val addWorkItem = locationView.findViewById<View>(R.id.addWorkItem)
        val selectButton = locationView.findViewById<View>(R.id.selectButton)
        val cancelButton = locationView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = locationView.findViewById<TextView>(R.id.addNewButton)

        val currentLocationText = locationView.findViewById<TextView>(R.id.currentLocationText)
        val homeLocationText = locationView.findViewById<TextView>(R.id.homeLocationText)
        val workLocationText = locationView.findViewById<TextView>(R.id.workLocationText)
        val workLocationContainer = locationView.findViewById<View>(R.id.workLocationContainer)
        val workTitleText = locationView.findViewById<TextView>(R.id.workTitleText)

        currentLocationText.text = prefsHelper.getCurrentLocation().ifEmpty { "No location set" }
        homeLocationText.text = prefsHelper.getHomeLocation().ifEmpty { "No home location set" }

        val savedWorkLocation = prefsHelper.getWorkLocation()
        if (savedWorkLocation.isNotEmpty()) {
            workLocationContainer.visibility = View.VISIBLE
            workLocationText.text = savedWorkLocation
            addWorkItem.visibility = View.GONE
        } else {
            workLocationContainer.visibility = View.GONE
            addWorkItem.visibility = View.VISIBLE
            workTitleText.text = "Add work"
        }

        currentLocationItem.setOnClickListener {
            if (prefsHelper.getCurrentLocation().isNotEmpty()) {
                updateLocation("Current", prefsHelper.getCurrentLocation())
            } else {
                showAddLocationBottomSheet("Current")
            }
            locationBottomSheet.dismiss()
        }

        homeLocationItem.setOnClickListener {
            if (prefsHelper.getHomeLocation().isNotEmpty()) {
                updateLocation("Home", prefsHelper.getHomeLocation())
            } else {
                showAddLocationBottomSheet("Home")
            }
            locationBottomSheet.dismiss()
        }

        addWorkItem.setOnClickListener {
            showAddLocationBottomSheet("Work")
            locationBottomSheet.dismiss()
        }

        workLocationContainer.setOnClickListener {
            updateLocation("Work", prefsHelper.getWorkLocation())
            locationBottomSheet.dismiss()
        }

        selectButton.setOnClickListener { locationBottomSheet.dismiss() }
        cancelButton.setOnClickListener { locationBottomSheet.dismiss() }
        addNewButton.setOnClickListener {
            showAddLocationBottomSheet(null)
            locationBottomSheet.dismiss()
        }
    }

    private fun setupAddLocationBottomSheet() {
        addLocationBottomSheet = BottomSheetDialog(requireContext())
        val addLocationView = layoutInflater.inflate(R.layout.add_location_bottom_sheet, null)
        addLocationBottomSheet.setContentView(addLocationView)

        val cancelButton = addLocationView.findViewById<TextView>(R.id.cancelAddLocationButton)
        val saveButton = addLocationView.findViewById<CardView>(R.id.saveButton)
        val saveTopButton = addLocationView.findViewById<TextView>(R.id.saveLocationButton)
        val locationTypeGroup = addLocationView.findViewById<RadioGroup>(R.id.locationTypeGroup)
        val addressInput = addLocationView.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationView.findViewById<EditText>(R.id.cityInput)
        val countryInput = addLocationView.findViewById<EditText>(R.id.countryInput)

        countryInput.setText("U.A.E")

        val saveAction = saveAction@{
            val address = addressInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val country = countryInput.text.toString().trim()

            if (address.isEmpty() || city.isEmpty() || country.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@saveAction
            }

            val fullAddress = if (address.contains(city)) {
                "$address · $country"
            } else {
                "$address · $city, $country"
            }

            val selectedLocationType = when (locationTypeGroup.checkedRadioButtonId) {
                R.id.radioHome -> "Home"
                R.id.radioWork -> "Work"
                R.id.radioCurrent -> "Current"
                else -> "Current"
            }

            prefsHelper.saveLocation(selectedLocationType, fullAddress)
            updateLocation(selectedLocationType, fullAddress)

            Toast.makeText(requireContext(), "$selectedLocationType location saved", Toast.LENGTH_SHORT).show()
            addLocationBottomSheet.dismiss()
            setupLocationBottomSheet()
        }

        cancelButton.setOnClickListener { addLocationBottomSheet.dismiss() }
        saveButton.setOnClickListener { saveAction() }
        saveTopButton.setOnClickListener { saveAction() }
    }

    private fun setupVehicleBottomSheet() {
        vehicleBottomSheet = BottomSheetDialog(requireContext())
        val vehicleView = layoutInflater.inflate(R.layout.vehicle_bottom_sheet, null)
        vehicleBottomSheet.setContentView(vehicleView)

        val cancelButton = vehicleView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = vehicleView.findViewById<TextView>(R.id.addNewButton)
        val selectButton = vehicleView.findViewById<MaterialButton>(R.id.selectButton)
        val vehicleListContainer = vehicleView.findViewById<LinearLayout>(R.id.vehicleListContainer)
        val emptyStateView = vehicleView.findViewById<TextView>(R.id.emptyStateText)

        // ✅ Vehicle list is now loaded async from Firebase
        fun refreshVehicleList() {
            lifecycleScope.launch {
                val vehicles = firebaseHelper.getAllVehicles()
                vehicleListContainer.removeAllViews()

                if (vehicles.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                    vehicleListContainer.visibility = View.GONE
                } else {
                    emptyStateView.visibility = View.GONE
                    vehicleListContainer.visibility = View.VISIBLE

                    vehicles.forEachIndexed { index, vehicle ->
                        val vehicleItemView = layoutInflater.inflate(R.layout.item_vehicle, null)

                        vehicleItemView.findViewById<TextView>(R.id.vehicleBrandModel).text = "${vehicle.brand} ${vehicle.model}"
                        vehicleItemView.findViewById<TextView>(R.id.vehiclePlate).text = vehicle.plateNumber

                        if (index == selectedVehicleIndex) {
                            vehicleItemView.setBackgroundResource(R.drawable.selected_vehicle_background)
                        }

                        vehicleItemView.setOnClickListener {
                            selectedVehicleIndex = index
                            refreshVehicleList()
                        }

                        vehicleListContainer.addView(vehicleItemView)

                        if (index < vehicles.size - 1) {
                            val divider = View(requireContext())
                            divider.layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 1
                            ).apply { setMargins(0, 16.dpToPx(), 0, 16.dpToPx()) }
                            divider.setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.divider_gray)
                            )
                            vehicleListContainer.addView(divider)
                        }
                    }
                }
            }
        }

        refreshVehicleList()

        addNewButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddVehicleActivity::class.java))
            vehicleBottomSheet.dismiss()
        }

        cancelButton.setOnClickListener { vehicleBottomSheet.dismiss() }

        selectButton.setOnClickListener {
            lifecycleScope.launch {
                val vehicles = firebaseHelper.getAllVehicles()
                if (selectedVehicleIndex in 0 until vehicles.size) {
                    val selectedVehicle = vehicles[selectedVehicleIndex]
                    Toast.makeText(requireContext(),
                        "Selected: ${selectedVehicle.brand} ${selectedVehicle.model}",
                        Toast.LENGTH_SHORT).show()
                    vehicleBottomSheet.dismiss()
                } else if (vehicles.isNotEmpty()) {
                    selectedVehicleIndex = 0
                    refreshVehicleList()
                    Toast.makeText(requireContext(),
                        "Selected: ${vehicles[0].brand} ${vehicles[0].model}",
                        Toast.LENGTH_SHORT).show()
                    vehicleBottomSheet.dismiss()
                } else {
                    Toast.makeText(requireContext(), "No vehicles available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddLocationBottomSheet(locationType: String?) {
        if (!this::addLocationBottomSheet.isInitialized) {
            setupAddLocationBottomSheet()
        }

        val radioHome = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioHome)
        val radioWork = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioWork)
        val radioCurrent = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioCurrent)
        val addressInput = addLocationBottomSheet.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationBottomSheet.findViewById<EditText>(R.id.cityInput)

        when (locationType) {
            "Home" -> radioHome?.isChecked = true
            "Work" -> radioWork?.isChecked = true
            "Current" -> radioCurrent?.isChecked = true
        }

        addressInput?.setText("")
        cityInput?.setText("")
        addLocationBottomSheet.show()
    }

    private fun updateLocation(locationType: String, address: String) {
        view?.findViewById<TextView>(R.id.locationTitle)?.text = address
        prefsHelper.saveLocation(locationType, address)
        Toast.makeText(requireContext(), "$locationType location selected", Toast.LENGTH_SHORT).show()
    }

    private fun setupNavigation() {
        view?.findViewById<ImageButton>(R.id.menuButton)?.setOnClickListener {
            Toast.makeText(requireContext(), "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        view?.findViewById<ImageButton>(R.id.notificationButton)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        view?.findViewById<TextView>(R.id.locationTitle)?.setOnClickListener {
            locationBottomSheet.show()
        }
    }

    private fun setupLocationAndVehicles() {
        view?.findViewById<CardView>(R.id.myLocationButton)?.setOnClickListener {
            locationBottomSheet.show()
        }

        view?.findViewById<CardView>(R.id.myVehiclesButton)?.setOnClickListener {
            vehicleBottomSheet.show()
        }
    }

    private fun setupServiceCards() {
        val serviceCards = listOf(
            view?.findViewById<CardView>(R.id.serviceCard1),
            view?.findViewById<CardView>(R.id.serviceCard2),
            view?.findViewById<CardView>(R.id.serviceCard3),
            view?.findViewById<CardView>(R.id.serviceCard4),
            view?.findViewById<CardView>(R.id.serviceCard5),
            view?.findViewById<CardView>(R.id.serviceCard6),
            view?.findViewById<CardView>(R.id.serviceCard7),
            view?.findViewById<CardView>(R.id.serviceCard8)
        )

        val serviceTypes = listOf(
            "Service", "Car Towing", "Brake Service", "Car Wash",
            "Fuel Up", "Tire Change", "Battery Change", "Service Contract"
        )

        serviceCards.forEachIndexed { index, cardView ->
            cardView?.setOnClickListener { navigateToService(serviceTypes[index]) }
        }
    }

    private fun navigateToService(serviceName: String) {
        val intent = when (serviceName) {
            "Service" -> Intent(requireContext(), Servicepage::class.java)
            "Car Towing" -> Intent(requireContext(), cartowing::class.java)
            "Brake Service" -> Intent(requireContext(), BrakeService::class.java)
            "Car Wash" -> Intent(requireContext(), CarWashServicePage::class.java)
            "Tire Change" -> Intent(requireContext(), TireChangeServicePage::class.java)
            "Battery Change" -> Intent(requireContext(), BatteryChangeServicePage::class.java)
            "Service Contract" -> Intent(requireContext(), ServiceContractPage::class.java)
            "Fuel Up" -> Intent(requireContext(), FuelUPPage::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }

    private fun setupSpecialOffers() {
        view?.findViewById<CardView>(R.id.oilChangeOffer)?.setOnClickListener {
            startActivity(Intent(requireContext(), Servicepage::class.java))
        }

        view?.findViewById<CardView>(R.id.carWashOffer)?.setOnClickListener {
            startActivity(Intent(requireContext(), CarWashServicePage::class.java))
        }
    }

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}