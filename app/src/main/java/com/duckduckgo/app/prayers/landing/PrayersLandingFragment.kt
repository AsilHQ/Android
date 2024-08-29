package com.duckduckgo.app.prayers.landing

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.PrayersLandingFragmentBinding
import com.duckduckgo.app.prayers.constants.PrayersConstants
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.ASR
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.DHUHR
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.FAJR
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.ISHA
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.MAGHRIB
import com.duckduckgo.app.prayers.constants.PrayersConstants.PrayerTime.SUNRISE
import com.duckduckgo.app.prayers.fragments.MadhabAndCalculationSelectionBsFragment
import com.duckduckgo.app.prayers.landing.SharedPrefKey.ALARM_SET_AT
import com.duckduckgo.app.prayers.landing.SharedPrefKey.CALCULATION_METHOD_KEY
import com.duckduckgo.app.prayers.landing.SharedPrefKey.MADHAB_METHOD_KEY
import com.duckduckgo.app.prayers.listeners.OnCalculationMethodClickedListener
import com.duckduckgo.app.prayers.listeners.OnMadhabMethodClickedListener
import com.duckduckgo.app.prayers.utils.NotificationUtils
import com.duckduckgo.app.prayers.views.PrayerModel
import com.duckduckgo.app.prayers.views.PrayerModelView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class PrayersLandingFragment : Fragment() {

    private lateinit var binding: PrayersLandingFragmentBinding
    private var prayerModelViews: List<PrayerModelView>? = null
        set(value) {
            value?.let {
                field = value
                preparePrayerTimesView()
            }
        }

    var dateIndex = 1
    private lateinit var todayPrayerTimes: PrayerTimes
    private lateinit var tomorrowPrayerTimes: PrayerTimes
    private lateinit var yesterdayPrayerTimes: PrayerTimes
    private lateinit var params: CalculationParameters
    private lateinit var coordinates: Coordinates
    private var monthNames =
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private var calculationTitles = mutableMapOf<CalculationMethod, String>()
    private var madhabTitles = mutableMapOf<Madhab, String>()
    private var fullAddress: String? = null
    private var cityName: String? = null
    private val dateFormat: SimpleDateFormat by lazy { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var settingsActivityResult: ActivityResultLauncher<Intent>? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var isLocationUpdatesRunning: Boolean = false

    private lateinit var locationManager: LocationManager
    private lateinit var job: Job

    private var sharedPreferences: SharedPreferences? = null
    private val sharedPrefName = "PrayersPreferences"
    private val defaultCalculationMethod = "MUSLIM_WORLD_LEAGUE"
    private val defaultMadhabMethod = "HANAFI"
    private var currentDay = -1

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            prepareLocationInformation(location)
            isLocationUpdatesRunning = false
            locationManager.removeUpdates(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.prayers_landing_fragment, container, false)
        binding = PrayersLandingFragmentBinding.bind(view)
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        settingsActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            requestLocationPermission()
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when (it) {
                true -> {
                    checkIfLocationServicesEnabled()
                }

                else -> {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showAppLocationPermissionNotGranted()
                    } else {
                        requestLocationPermission()
                    }
                }
            }
        }

        sharedPreferences = context?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val calculationMethods = CalculationMethod.entries.toTypedArray()
        val madhabAsrMethods = Madhab.entries.toTypedArray()
        if (calculationTitles.isEmpty()) {
            for (method in calculationMethods) {
                calculationTitles[method] = convertToTitleText(method.name)
            }
        }
        if (madhabTitles.isEmpty()) {
            for (method in madhabAsrMethods) {
                madhabTitles[method] = convertToTitleText(method.name)
            }
        }

        val calendar = Calendar.getInstance()
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        requestLocationPermission()
        NotificationUtils.createNotificationChannel(requireContext())

        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
        if (::job.isInitialized) {
            job.cancel()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkIfLocationServicesEnabled()
        } else {
            requestPermissionLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkIfLocationServicesEnabled() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLastKnownLocation()
        } else {
            showLocationServicesDisabledPopUp()
        }
    }

    private fun getLastKnownLocation() {
        lifecycleScope.launch(Dispatchers.Main) {
            val location = getLastLocation()
            location?.let {
                // Handle the obtained location
                prepareLocationInformation(it)
            }
        }
    }

    private suspend fun getLastLocation(): Location? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val lastKnownLocationGPS =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastKnownLocationNetwork =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                // Choose the best available location
                if (lastKnownLocationGPS != null && lastKnownLocationNetwork != null) {
                    if (lastKnownLocationGPS.time > lastKnownLocationNetwork.time) {
                        lastKnownLocationGPS
                    } else {
                        lastKnownLocationNetwork
                    }
                } else {
                    lastKnownLocationGPS ?: lastKnownLocationNetwork
                }
            } catch (e: SecurityException) {
                null
            }
        }
    }

    private fun showLocationServicesDisabledPopUp() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.kahf_alert))
            .setMessage(getString(R.string.kahf_location_could_not_get))
            .setPositiveButton(
                R.string.kahf_ok,
            ) { d: DialogInterface, w: Int ->
                val packageManager = context?.packageManager
                if (packageManager != null) {
                    val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    if (locationSettingsIntent.resolveActivity(packageManager) != null) {
                        settingsActivityResult?.launch(locationSettingsIntent)
                    }
                }
            }
            .show()
    }

    private fun showAppLocationPermissionNotGranted() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.kahf_alert))
            .setMessage(getString(R.string.kahf_location_could_not_get_app_level))
            .setPositiveButton(
                R.string.kahf_ok,
            ) { d: DialogInterface, w: Int ->
                val packageManager = context?.packageManager
                if (packageManager != null) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    if (intent.resolveActivity(packageManager) != null) {
                        settingsActivityResult?.launch(intent)
                    }
                }
            }
            .show()
    }

    private fun prepareLocationInformation(location: Location) {
        try {
            binding.progressBar.visibility = View.GONE
            coordinates = Coordinates(location.latitude, location.longitude)

            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses: MutableList<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            addresses?.let {
                if (it.isNotEmpty()) {
                    val address: Address = it[0]

                    cityName = address.adminArea
                    val sb = StringBuilder()
                    sb.apply {
                        address.subLocality?.let { str ->
                            append(str)
                            append(", ")
                        }
                        address.thoroughfare?.let { str ->
                            append(str)
                            append(", ")
                        }
                        address.postalCode?.let { str ->
                            append(str)
                        }
                    }
                    this@PrayersLandingFragment.fullAddress = sb.toString()
                }
            }

            prepareData(initialCall = true)
        } catch (error: Exception) {
            prepareData(initialCall = true)
            println("geocoder error " + error.message)
        }
    }

    private fun prepareData(initialCall: Boolean = false) {
        val prayers = mutableListOf<PrayerModelView>()

        getDataFromApi()

        val prayerTimes = when (dateIndex) {
            0 -> yesterdayPrayerTimes
            1 -> todayPrayerTimes
            2 -> tomorrowPrayerTimes
            else -> todayPrayerTimes
        }

        val calendar = Calendar.getInstance()
        calendar.time = prayerTimes.fajr
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    FAJR,
                    convertToTitleText(FAJR.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.fajr),
                    prayerTimes.fajr,
                ),
                this@PrayersLandingFragment,
            ),
        )
        calendar.time = prayerTimes.sunrise
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    SUNRISE,
                    convertToTitleText(SUNRISE.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.sunrise),
                    prayerTimes.sunrise,
                ),
                this@PrayersLandingFragment,
            ),
        )
        calendar.time = prayerTimes.dhuhr
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    DHUHR,
                    convertToTitleText(DHUHR.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.dhuhr),
                    prayerTimes.dhuhr,
                ),
                this@PrayersLandingFragment,
            ),
        )
        calendar.time = prayerTimes.asr
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    ASR,
                    convertToTitleText(ASR.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.asr),
                    prayerTimes.asr,
                ),
                this@PrayersLandingFragment,
            ),
        )
        calendar.time = prayerTimes.maghrib
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    MAGHRIB,
                    convertToTitleText(MAGHRIB.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.maghrib),
                    prayerTimes.maghrib,
                ),
                this@PrayersLandingFragment,
            ),
        )
        calendar.time = prayerTimes.isha
        prayers.add(
            PrayerModelView(
                requireContext(),
                PrayerModel(
                    ISHA,
                    convertToTitleText(ISHA.name),
                    getTimeString(calendar),
                    getNotificationPreference(prayerTimes.isha),
                    prayerTimes.isha,
                ),
                this@PrayersLandingFragment,
            ),
        )

        prayerModelViews = prayers.toList()

        if (initialCall) {
            job = lifecycleScope.launch(Dispatchers.Main) {
                while (isActive) {
                    val cal = Calendar.getInstance()
                    if (cal.get(Calendar.DAY_OF_MONTH) != currentDay) {
                        prepareData()
                        delay(5000)
                    } else {
                        if (dateIndex == 1) {
                            binding.bannerPrayerTime.text = convertToTitleText(prayerTimes.currentPrayer().name)
                            binding.bannerRemainingTimeText.text = getNextPrayerTimeText(Date())
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun getDataFromApi() {
        val savedCalculationMethod = sharedPreferences?.getString(CALCULATION_METHOD_KEY.value, defaultCalculationMethod) ?: defaultCalculationMethod
        val calculationMethod = CalculationMethod.valueOf(savedCalculationMethod)
        val savedMadhabMethod = sharedPreferences?.getString(MADHAB_METHOD_KEY.value, defaultMadhabMethod) ?: defaultMadhabMethod
        val madhabMethod = Madhab.valueOf(savedMadhabMethod)
        params = calculationMethod.parameters
        params.madhab = madhabMethod

        yesterdayPrayerTimes = PrayerTimes(coordinates, DateComponents.from(getNDaysBeforeOrAfter(-1)), params)
        todayPrayerTimes = PrayerTimes(coordinates, DateComponents.from(Date()), params)
        tomorrowPrayerTimes = PrayerTimes(coordinates, DateComponents.from(getNDaysBeforeOrAfter(1)), params)
    }

    private fun initViews() {
        binding.apply {
            goYesterdayBtn.setOnClickListener {
                if (dateIndex > 0) {
                    dateIndex--
                    prepareData()
                }
            }

            goTomorrowBtn.setOnClickListener {
                if (dateIndex < 2) {
                    dateIndex++
                    prepareData()
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            val popUpMenu = PopupMenu(requireContext(), it)
            popUpMenu.menuInflater.inflate(R.menu.prayers_page_popup_menu, popUpMenu.menu)
            popUpMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.prayers_page_popup_menu_option_1 -> {

                        MadhabAndCalculationSelectionBsFragment
                            .builder()
                            .setMadhabOptions(
                                madhabTitles,
                                object : OnMadhabMethodClickedListener {
                                    override fun onMadhabMethodClicked(madhab: Madhab) {
                                        val sharedPreferences = context?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        val editor = sharedPreferences?.edit()
                                        editor?.putString(MADHAB_METHOD_KEY.value, madhab.name)
                                        editor?.apply()

                                        removeAllAlarms()
                                        prepareData()
                                        (parentFragmentManager.findFragmentByTag("bs") as BottomSheetDialogFragment).dismiss()
                                    }
                                },
                            )
                            .build()
                            .show(parentFragmentManager, "bs")

                        true
                    }

                    R.id.prayers_page_popup_menu_option_2 -> {

                        MadhabAndCalculationSelectionBsFragment
                            .builder()
                            .setCalculationOptions(
                                calculationTitles,
                                object : OnCalculationMethodClickedListener {
                                    override fun onCalculationMethodClicked(calculationMethod: CalculationMethod) {
                                        val sharedPreferences = context?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        val editor = sharedPreferences?.edit()
                                        editor?.putString(CALCULATION_METHOD_KEY.value, calculationMethod.name)
                                        editor?.apply()

                                        removeAllAlarms()
                                        prepareData()
                                        (parentFragmentManager.findFragmentByTag("bs") as BottomSheetDialogFragment).dismiss()
                                    }
                                },
                            )
                            .build()
                            .show(parentFragmentManager, "bs")
                        true
                    }

                    else -> false
                }
            }
            popUpMenu.show()
        }
    }

    private fun removeAllAlarms() {
        removeAlarmIfSet(todayPrayerTimes.fajr)
        removeAlarmIfSet(todayPrayerTimes.sunrise)
        removeAlarmIfSet(todayPrayerTimes.dhuhr)
        removeAlarmIfSet(todayPrayerTimes.asr)
        removeAlarmIfSet(todayPrayerTimes.maghrib)
        removeAlarmIfSet(todayPrayerTimes.isha)
        removeAlarmIfSet(tomorrowPrayerTimes.fajr)
        removeAlarmIfSet(tomorrowPrayerTimes.sunrise)
        removeAlarmIfSet(tomorrowPrayerTimes.dhuhr)
        removeAlarmIfSet(tomorrowPrayerTimes.asr)
        removeAlarmIfSet(tomorrowPrayerTimes.maghrib)
        removeAlarmIfSet(tomorrowPrayerTimes.isha)
    }

    private fun removeAlarmIfSet(prayer: Date) {
        if (getNotificationPreference(prayer) != PrayersConstants.NotificationTypes.MUTED) {
            val notificationId = timeToNotificationId(prayer.time)
            removeFromSharedPref(notificationId)
            NotificationUtils.cancelNotification(requireContext(), notificationId)
        }
    }

    fun saveToSharedPref(
        notificationId: Int,
        notificationType: String
    ) {
        val sharedPreferences = requireContext().getSharedPreferences("PrayersPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        val key = "${ALARM_SET_AT.value}$notificationId"
        editor?.putString(key, notificationType)
        editor?.apply()
    }

    fun removeFromSharedPref(notificationId: Int) {
        val editor = requireContext().getSharedPreferences("PrayersPreferences", Context.MODE_PRIVATE).edit()
        editor.remove("${ALARM_SET_AT.value}$notificationId")
        editor.apply()
    }

    private fun changePrayerDayLabelText() {
        binding.prayerDayLabel.text = when (dateIndex) {
            0 -> getString(R.string.kahf_yesterday)
            1 -> getString(R.string.kahf_today)
            2 -> getString(R.string.kahf_tomorrow)
            else -> ""
        }
    }

    private fun preparePrayerTimesView() {
        binding.apply {
            changePrayerDayLabelText()
            arrangeDateInformation()
            addressInfo.text = fullAddress
            dateInfoContainer.visibility = View.VISIBLE

            prayersLinearLayout.removeAllViews()
            prayerModelViews?.forEach {
                prayersLinearLayout.addView(it)
            }
        }
    }

    private fun getNextPrayerTimeText(now: Date): CharSequence {
        var nextPrayer = todayPrayerTimes.nextPrayer()
        val remainingSecs = when (nextPrayer) {
            Prayer.NONE -> {
                val calendar = Calendar.getInstance()
                calendar.time = getNDaysBeforeOrAfter(1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                nextPrayer = tomorrowPrayerTimes.nextPrayer(calendar.time)
                tomorrowPrayerTimes.timeForPrayer(nextPrayer).time.minus(now.time).div(60000L)
            }

            else -> todayPrayerTimes.timeForPrayer(nextPrayer).time.minus(now.time).div(60000L)
        }
        val remainingHours = remainingSecs.div(60L)
        val remainingMinutes = ceil(remainingSecs.mod(60L).toDouble()).toInt()
        val convertedName = convertToTitleText(nextPrayer.name)
        return when (remainingHours > 0) {
            true -> {
                when (remainingMinutes > 0) {
                    true -> "$remainingHours ${getString(R.string.kahf_hours)} $remainingMinutes ${getString(R.string.kahf_minutes_until)} $convertedName"
                    else -> "$remainingHours ${getString(R.string.kahf_hours_until)} $convertedName"
                }
            }

            else -> {
                when (remainingMinutes > 0) {
                    true -> "$remainingMinutes ${getString(R.string.kahf_minutes_until)} $convertedName"
                    else -> "$remainingSecs ${getString(R.string.kahf_secs_until)} $convertedName"
                }
            }
        }
    }

    private fun arrangeDateInformation() {
        binding.apply {
            val calendar = Calendar.getInstance()
            calendar.time = getDate()
            val prayerDateText =
                "$cityName | ${calendar.get(Calendar.DAY_OF_MONTH)} ${monthNames[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"
            prayerDate.text = prayerDateText
        }
    }

    private fun getDate(): Date {
        return when (dateIndex) {
            0 -> getNDaysBeforeOrAfter(-1)
            1 -> Date()
            2 -> getNDaysBeforeOrAfter(1)
            else -> Date()
        }
    }

    fun timeToNotificationId(value: Long): Int {
        return ((value.div(1000L)).toInt() % Integer.MAX_VALUE)
    }

    private fun getNotificationPreference(prayerTime: Date): String {
        return sharedPreferences?.getString("${ALARM_SET_AT.value}${timeToNotificationId(prayerTime.time)}", PrayersConstants.NotificationTypes.MUTED)
            ?: PrayersConstants.NotificationTypes.MUTED
    }

    private fun getTimeString(calendar: Calendar) = dateFormat.format(calendar.time)

    /**
     * @param n send negative to go back, send positive to go ahead
     * */
    private fun getNDaysBeforeOrAfter(n: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, n)
        return calendar.time
    }

    private fun convertToTitleText(input: String): String {
        val words = input.split("_").map {
            it.lowercase().replaceFirstChar { lowercased ->
                if (lowercased.isLowerCase()) lowercased.titlecase()
                else it
            }
        }
        return when (words.joinToString(" ")) {
            "Fajr" -> getString(R.string.kahf_fajr)
            "Sunrise" -> getString(R.string.kahf_sunrise)
            "Dhuhr" -> getString(R.string.kahf_dhuhr)
            "Asr" -> getString(R.string.kahf_asr)
            "Maghrib" -> getString(R.string.kahf_magrib)
            "Isha" -> getString(R.string.kahf_isha)
            "None" -> getString(R.string.kahf_isha)
            else -> words.joinToString(" ")
        }
    }

    class ItemOffsetDecoration(private val context: Context) : RecyclerView.ItemDecoration() {

        private val spacing: Int = dpToPx(10) // Convert dp to pixels

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            val itemPosition = parent.getChildAdapterPosition(view)

            // Add top spacing for the first item
            if (itemPosition == 0) {
                outRect.top = spacing * 2
                outRect.bottom = spacing / 2
            }

            // Add bottom spacing for the last item
            if (itemPosition == parent.adapter?.itemCount?.minus(1)) {
                outRect.top = spacing / 2
                outRect.bottom = spacing * 2
            }

            // Add spacing between items
            if (itemPosition > 0 && itemPosition < (parent.adapter?.itemCount?.minus(1) ?: 0)) {
                outRect.top = spacing / 2
                outRect.bottom = spacing / 2
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
}

enum class SharedPrefKey(val value: String) {
    ALARM_SET_AT("ALARM_SET_AT_"),
    CALCULATION_METHOD_KEY("CALCULATION_METHOD_KEY"),
    MADHAB_METHOD_KEY("MADHAB_METHOD_KEY")
}
