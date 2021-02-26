package au.gov.health.covidsafe.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.databinding.FragmentHomeBinding
import au.gov.health.covidsafe.extensions.*
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.notifications.NotificationBuilder
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.base.BaseFragment
import au.gov.health.covidsafe.ui.onboarding.OnboardingActivity
import au.gov.health.covidsafe.utils.AnimationUtils.slideAnimation
import au.gov.health.covidsafe.utils.NetworkConnectionCheck
import au.gov.health.covidsafe.utils.SlideDirection
import au.gov.health.covidsafe.utils.SlideType
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home_case_statistics.*
import kotlinx.android.synthetic.main.fragment_home_external_links.*
import kotlinx.android.synthetic.main.fragment_home_header.*
import kotlinx.android.synthetic.main.view_covid_share_tile.*
import kotlinx.android.synthetic.main.view_help_topics_tile.*
import kotlinx.android.synthetic.main.view_home_setup_complete.*
import kotlinx.android.synthetic.main.view_home_setup_incomplete.*
import kotlinx.android.synthetic.main.view_national_case_statistics.national_case_layout
import kotlinx.android.synthetic.main.view_state_case_statistics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "HomeFragment"

private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
private const val FOURTEEN_DAYS_IN_MILLIS = 14 * ONE_DAY_IN_MILLIS

class HomeFragment : BaseFragment(), EasyPermissions.PermissionCallbacks, NetworkConnectionCheck.NetworkConnectionListener {

    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels()

    private var mIsBroadcastListenerRegistered = false

    private var counter: Int = 0
    private var jwtExpired: Boolean = false

    private var checkIsInternetConnected = false
    private var isAppWithLatestVersion = false
    lateinit var staticsLayout: ConstraintLayout
    private lateinit var stateListAdapter: StateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeObservers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentHomeBinding.inflate(layoutInflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = homeFragmentViewModel
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        staticsLayout = national_case_layout
        initialisePrivacyPolicyMessageAfterUpdate()
        initializeSettingsNavigation()
        setAppVersionNumber()
        initializeDebugTestActivity()
        initializeNoNetworkError()
        initializeRefreshButton()
        initializePullToRefresh()
        initialiseReRegistration()
        setupHyperlink()
        removeRegistrationData()

        NetworkConnectionCheck.addNetworkChangedListener(requireContext(), this)
    }

    private fun removeRegistrationData() {
        Preference.putName(requireContext(), "")
        Preference.putAge(requireContext(), "")
        Preference.putPostCode(requireContext(), "")
    }

    private fun setupHyperlink() {
        txt_update_description.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initializeNoNetworkError() {
        no_network_error_text_view.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
        }
    }

    private fun initializeRefreshButton() {
        refresh_button.setOnClickListener {
            initiateFetchingCaseNumbers()
        }
    }

    private fun initializePullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            initiateFetchingCaseNumbers()
        }
    }

    private fun initialiseReRegistration() {
        registration_layout.setOnClickListener {
            Preference.putIsOnBoarded(requireContext(), false)
            Preference.putIsReRegister(requireContext(), true)
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }
    }

    private fun initiateFetchingCaseNumbers() {
        lifecycleScope.launch {
            homeFragmentViewModel.fetchGetCaseStatistics(lifecycle)
        }
    }

    private fun initializeObservers() {
        (activity as HomeActivity?)?.run {
            isJWTCorrupted.observe(this@HomeFragment, isJwtExpired)
            isJWTExpired.observe(this@HomeFragment, isJwtExpired)
            isAppUpdateAvailableLiveData.observe(this@HomeFragment, latestAppAvailable)
            isWindowFocusChangeLiveData.observe(this@HomeFragment, refreshUiObserver)
        }

        homeFragmentViewModel.turnCaseNumber.observe(this, Observer { turnOn ->
            showAndHideCaseNumber(turnOn)
        })

        homeFragmentViewModel.caseStatisticsLiveData.observe(this, Observer { caseNumber ->
            if (caseNumber == null) {
                national_case_layout.visibility = GONE
            }
        })

        homeFragmentViewModel.collectionMessageVisible.observe(this, Observer { visible ->
            visible?.let {
                if (visible) {
                    enableParentViewWhenNotificationIsShowing(false)
                } else {
                    enableParentViewWhenNotificationIsShowing(true)
                }
            }
        })

        homeFragmentViewModel.visibleSelectStateLayout.observe(this, Observer { visible ->
            visible?.let {
                select_state_layout.visibility = if (visible) VISIBLE else GONE
            }
        })

        homeFragmentViewModel.hotSpotLink.observe(this, Observer { link ->
            link?.let {
                val title = homeFragmentViewModel.hotSpotTitle.value
                txt_hotspot.text = LinkBuilder.getHotSpot(title, link)
                txt_hotspot.movementMethod = LinkMovementMethod.getInstance()
            }
        })

        homeFragmentViewModel.reIssueFail.observe(this, Observer {
            it?.let {
                if (it) {
                    permissions_card_subtitle.visibility = GONE
                    registration_layout.visibility = VISIBLE
                }
            }
        })

        homeFragmentViewModel.reIssueSuccess.observe(this, Observer {
            it?.let {
                if (it) {
                    jwtExpired = false
                    refreshSetupCompleteOrIncompleteUi()

                }
            }
        })
    }

    private val latestAppAvailable = Observer<Boolean> {
        isAppWithLatestVersion = it
        refreshSetupCompleteOrIncompleteUi()
        showCovidThanksMessage()
    }

    private val refreshUiObserver = Observer<Boolean> {
        refreshSetupCompleteOrIncompleteUi()

        if (it) {
            initiateFetchingCaseNumbers()
        }
    }

    private val isJwtExpired = Observer<Boolean> { expired ->
        jwtExpired = expired
    }

    override fun onResume() {
        super.onResume()

        // disable the app update reminder for now
        app_update_reminder.visibility = GONE
        getRefreshToken()
        initializePermissionViewButtonClickListeners()

        initializeUploadTestDataNavigation()

        initializeAppShareNavigation()
        initializeHelpTopicsNavigation()
        initializeChangeLanguageNavigation()

        registerBroadcastListener()

        refreshSetupCompleteOrIncompleteUi()

        updateHealthOfficialTile()
        initializeBluetoothPairingInfo()
        loadStateAndListener()
    }

    private fun getRefreshToken() {
        if (Preference.getEncryptRefreshToken(this.requireContext()).isNullOrEmpty()) {
             homeFragmentViewModel.getRefreshToken(lifecycle)
        }
    }

    private fun loadStateAndListener() {
        stateListAdapter = StateAdapter(this.requireContext())
        select_state.adapter = stateListAdapter
        val layoutManager = LinearLayoutManager(this.requireContext())
        select_state.layoutManager = layoutManager

        stateListAdapter.setRecords(createStateList(), 0)

        stateListAdapter.setOnStateListClickListener(object : StateAdapter.OnStateListClickListener{
            override fun onStateClick(state: String) {
                homeFragmentViewModel.setSelectedState(state)
            }
        })
    }

    private fun initialisePrivacyPolicyMessageAfterUpdate() {
        homeFragmentViewModel.getCollectionMessage()
        txt_collection_message.text = LinkBuilder.getCollectionMassageLearnMore(requireContext())
        txt_collection_message.movementMethod = LinkMovementMethod.getInstance()

        txt_location_permission.text = LinkBuilder.getLocationPermission(requireContext())
        txt_location_permission.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initializeSettingsNavigation() {
        home_header_settings.setOnClickListener {
            navigateToSettingsFragment()
        }
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
    }

    private fun setAppVersionNumber() {
        home_version_number.text = context?.getAppVersionNumberDetails()
    }

    private fun initializePermissionViewButtonClickListeners() {
        bluetooth_card_view.setOnClickListener { requestBlueToothPermissionThenNextPermission() }
        location_card_view.setOnClickListener { askForLocationPermission() }
        battery_card_view.setOnClickListener { excludeFromBatteryOptimization() }
        txt_proceed.setOnClickListener {
            homeFragmentViewModel.collectionMessageVisible.value = false
            askForLocationPermission()
        }
        btn_proceed.setOnClickListener {
            layout_herald_upgrade.slideAnimation(SlideDirection.UP, SlideType.HIDE, 200)
            homeFragmentViewModel.heraldUpgradeMessage.value = false
        }
    }

    private fun initializeUploadTestDataNavigation() {
        home_been_tested_button.setOnClickListener {
            navigateTo(R.id.action_home_to_selfIsolate)
        }
    }

    private fun registerBroadcastListener() {
        if (!mIsBroadcastListenerRegistered) {
            registerBroadcast()
        }
    }

    private fun initializeAppShareNavigation() {
        app_share.setOnClickListener {
            context?.shareThisApp()
        }
    }

    private fun initializeHelpTopicsNavigation() {
        help_topics_link.setOnClickListener {
            HelpFragment.anchor = null
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())
        }
    }

    private fun initializeChangeLanguageNavigation() {
        change_language_link.setOnClickListener {
            HelpFragment.anchor = "#other-languages"
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())
        }

        home_header_picture_setup_complete.setOnClickListener {
            counter++
            if (counter >= 2) {
                counter = 0
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPeekActivity())
            }
        }
    }

    private fun initializeBluetoothPairingInfo() {
        home_header_no_bluetooth_pairing.text = LinkBuilder.getNoBluetoothPairingContent(requireContext())
        home_header_no_bluetooth_pairing.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initializeDebugTestActivity() {
        if (BuildConfig.ENABLE_DEBUG_SCREEN) {
            header_background.setOnClickListener {
                counter++
                if (counter >= 2) {
                    counter = 0
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPeekActivity())
                }
            }
            home_header_picture_setup_complete.setOnClickListener {
                counter++
                if (counter >= 2) {
                    counter = 0
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPeekActivity())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        unregisterAllClickListener()
        unregisterBroadcastListener()
    }

    private fun unregisterAllClickListener() {
        bluetooth_card_view.setOnClickListener(null)
        location_card_view.setOnClickListener(null)
        battery_card_view.setOnClickListener(null)
        home_been_tested_button.setOnClickListener(null)
        app_share.setOnClickListener(null)
        help_topics_link.setOnClickListener(null)
    }

    private fun unregisterBroadcastListener() {
        activity?.let { activity ->
            if (mIsBroadcastListenerRegistered) {
                activity.unregisterReceiver(mBroadcastListener)
                mIsBroadcastListenerRegistered = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        NetworkConnectionCheck.removeNetworkChangedListener(this)
        home_root.removeAllViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterObservers()
    }

    private fun unregisterObservers() {
        (activity as HomeActivity?)?.run {
            isAppUpdateAvailableLiveData.removeObserver(latestAppAvailable)
            isWindowFocusChangeLiveData.removeObserver(refreshUiObserver)
        }
    }

    private fun isDataUploadedInPast14Days(context: Context): Boolean {
        val isUploaded = Preference.isDataUploaded(context)
        CentralLog.d(TAG, "isDataUploadedInPast14Days : $isUploaded")
        if (!isUploaded) {
            return false
        }

        val millisSinceDataUploaded = System.currentTimeMillis() - Preference.getDataUploadedDateMs(context)
        return (millisSinceDataUploaded < FOURTEEN_DAYS_IN_MILLIS)
    }

    private fun getDataUploadDateHtmlString(context: Context): String {
        val dataUploadedDateMillis = Preference.getDataUploadedDateMs(context)
        val format = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
        val dateString = format.format(Date(dataUploadedDateMillis))
        return "<b>$dateString</b>"
    }

    @SuppressLint("SetTextI18n")
    fun refreshSetupCompleteOrIncompleteUi() {
        lifecycleScope.launch(Dispatchers.Main) {
            CentralLog.d(TAG, "refreshSetupCompleteOrIncompleteUi")
            context?.let {
                val isAllPermissionsEnabled = it.allPermissionsEnabled() && !jwtExpired
                if (!isAllPermissionsEnabled) {
                    NotificationBuilder.clearPossibleIssueNotificationCheck()
                    updateJwtExpiredHeader()
                }
                val isDataUploadedInPast14Days = isDataUploadedInPast14Days(it)
                updateSetupCompleteStatus(isAllPermissionsEnabled)
                updateSetupCompleteHeaderTitle1(it, isAllPermissionsEnabled, isDataUploadedInPast14Days)
                updateSetupCompleteHeaderTitle2(isAllPermissionsEnabled)
                updateHealthOfficialTile(isDataUploadedInPast14Days)
            }
        }
    }

    private fun updateSetupCompleteHeaderTitle1(context: Context, isAllPermissionsEnabled: Boolean, isDataUploadedInPast14Days: Boolean) {
        showLastDataUploadedInfo(context, isDataUploadedInPast14Days)

        home_header_setup_complete_header_line_1.run {
            text = getString(getCovidActiveStatusMessage(isAllPermissionsEnabled))
            setHeading()
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }

    private fun updateSetupCompleteHeaderTitle2(isAllPermissionsEnabled: Boolean) {
        if (isAllPermissionsEnabled) {
            val isAppPerformanceRequired = !checkIsInternetConnected || !isAppWithLatestVersion

            home_header_setup_complete_header_line_2.run {
                setTextColor(ContextCompat.getColor(TracerApp.AppContext, if (isAppPerformanceRequired) R.color.error_red else R.color.slate_black_2))
                typeface = if (isAppPerformanceRequired) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                text = fromHtml(getString(if (isAppPerformanceRequired) R.string.improve else R.string.home_header_active_no_action_required))
            }

            active_status_layout.setOnClickListener {
                if (isAppPerformanceRequired) {
                    navigateToSettingsFragment()
                }
            }
        }
    }

    private fun updateJwtExpiredHeader() {
        if (jwtExpired) {
            homeFragmentViewModel.getReissueAuth(lifecycle)
        } else {
            permissions_card_subtitle.visibility = VISIBLE
            registration_layout.visibility = GONE
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun showLastDataUploadedInfo(context: Context, isDataUploadedInPast14Days: Boolean) {
        if (isDataUploadedInPast14Days) {
            data_last_uploaded_layout.visibility = VISIBLE
            val appendUploadInfo = getString(R.string.home_header_uploaded_on_date, getDataUploadDateHtmlString(context))
            home_header_no_last_updated_text_view.text = fromHtml(appendUploadInfo)
        } else {
            data_last_uploaded_layout.visibility = GONE
        }
    }

    private fun updateHealthOfficialTile(isDataUploadedInPast14Days: Boolean) {
        home_been_tested_button.visibility = if (isDataUploadedInPast14Days) GONE else VISIBLE
    }

    private fun updateSetupCompleteStatus(isAllPermissionsEnabled: Boolean) {
        if (isAllPermissionsEnabled) {
            home_setup_incomplete_permissions_layout.visibility = GONE
            home_setup_complete_layout.visibility = VISIBLE
        } else {
            home_setup_incomplete_permissions_layout.visibility = VISIBLE
            home_setup_complete_layout.visibility = GONE
            updateBlueToothStatus()
            updateBatteryOptimizationStatus()
            updateLocationStatus()
        }
    }

    private fun getCovidActiveStatusMessage(isAllPermissionsEnabled: Boolean): Int {
        return if (isAllPermissionsEnabled) {
            if (isShowThanksCovidMsg(isAllPermissionsEnabled))
                R.string.home_header_active_title_thanks
            else R.string.home_header_active_title
        } else {
            R.string.home_header_inactive_title
        }
    }

    private fun registerBroadcast() {
        activity?.let { activity ->
            var f = IntentFilter()
            activity.registerReceiver(mBroadcastListener, f)
            // bluetooth on/off
            f = IntentFilter()
            f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            activity.registerReceiver(mBroadcastListener, f)
            mIsBroadcastListenerRegistered = true
        }
    }

    private fun updateBlueToothStatus() {
        requireContext().isBlueToothEnabled()?.let {
            if (!it && !jwtExpired) {
                bluetooth_card_view_layout.visibility = VISIBLE
                bluetooth_card_view.render(formatBlueToothTitle(it), it)
            } else {
                bluetooth_card_view_layout.visibility = GONE
            }
        } ?: run {
            bluetooth_card_view_layout.visibility = GONE
        }
    }

    private fun updateBatteryOptimizationStatus() {
        requireContext().isBatteryOptimizationDisabled()?.let {
            if (!it && !jwtExpired) {
                battery_card_view_layout.visibility = VISIBLE
                battery_card_view.render(
                        formatNonBatteryOptimizationTitle(!it),
                        it,
                        getString(R.string.battery_optimisation_prompt)
                )
            } else {
                battery_card_view_layout.visibility = GONE
            }
        } ?: run {
            battery_card_view_layout.visibility = GONE
        }
    }

    private fun updateLocationStatus() {
        requireContext().isLocationPermissionAllowed()?.let {
            val locationWorking = it && requireContext().isLocationEnabledOnDevice()
            val locationOffPrompts = getString(R.string.home_set_location_why)
            if (!locationWorking && !jwtExpired) {
                location_card_view_layout.visibility = VISIBLE
                location_card_view.render(formatLocationTitle(locationWorking), locationWorking, locationOffPrompts)
            } else {
                location_card_view_layout.visibility = GONE
            }
        } ?: run {
            location_card_view_layout.visibility = GONE
        }
    }

    private fun updateHealthOfficialTile() {
        home_been_tested_button.run {
            setTitleTextTypeFace(Typeface.DEFAULT_BOLD)
            setContentTextTypeFace(Typeface.DEFAULT_BOLD)
        }
    }

    private fun formatBlueToothTitle(on: Boolean): String {
        return resources.getString(R.string.home_bluetooth_permission, getPermissionEnabledTitle(on))
    }

    private fun formatLocationTitle(on: Boolean): String {
        return resources.getString(R.string.home_location_permission, getPermissionEnabledTitle(on))
    }

    private fun formatNonBatteryOptimizationTitle(on: Boolean): String {
        return resources.getString(R.string.home_non_battery_optimization_permission, getEnabledOrDisabledString(on))
    }

    private fun getPermissionEnabledTitle(on: Boolean): String {
        return resources.getString(if (on) R.string.home_permission_on else R.string.home_permission_off)
    }

    private fun getEnabledOrDisabledString(isEnabled: Boolean): String {
        return resources.getString(if (isEnabled) R.string.enabled else R.string.disabled)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == LOCATION && EasyPermissions.somePermissionPermanentlyDenied(this, listOf(Manifest.permission.ACCESS_FINE_LOCATION))) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == LOCATION) {
            checkBLESupport()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun isShowThanksCovidMsg(isAllPermissionsEnabled: Boolean): Boolean {
        return isAllPermissionsEnabled && checkIsInternetConnected && isAppWithLatestVersion && NotificationBuilder.isShowPossibleIssueNotification()
    }

    private fun showCovidThanksMessage() {
        activity?.runOnUiThread {
            context?.let {
                home_header_setup_complete_header_line_1.text = it.getString(getCovidActiveStatusMessage(it.allPermissionsEnabled()))
            }
        }
    }

    private fun showAndHideCaseNumber(turnOn: Boolean) {
        val isV2Available = homeFragmentViewModel.isV2Available.value
        //When we open the app, should show the previous setting (hide/show) and doesn't need animation
        if (homeFragmentViewModel.getTurningCaseAfterOpenPage()) {
            if (turnOn) {
                if (isV2Available != null && isV2Available) {
                    state_layout.visibility = VISIBLE
                } else {
                    national_case_layout.visibility = VISIBLE
                }
            } else {
                if (isV2Available != null && isV2Available) {
                    state_layout.visibility = GONE
                } else {
                    national_case_layout.visibility = GONE
                }
            }
        } else {
            if (turnOn) {
                if (isV2Available != null && isV2Available) {
                    state_layout.slideAnimation(SlideDirection.DOWN, SlideType.SHOW)
                } else {
                    national_case_layout.slideAnimation(SlideDirection.DOWN, SlideType.SHOW)
                }
            } else {
                if (isV2Available != null && isV2Available) {
                    state_layout.slideAnimation(SlideDirection.UP, SlideType.HIDE)
                } else {
                    national_case_layout.slideAnimation(SlideDirection.UP, SlideType.HIDE)
                }
                national_case_layout.slideAnimation(SlideDirection.UP, SlideType.HIDE)
            }
        }
    }

    override fun onNetworkStatusChanged(isAvailable: Boolean) {
        CentralLog.d(TAG, "onNetworkStatusChanged: $checkIsInternetConnected $isAvailable")
        checkIsInternetConnected = isAvailable
        refreshSetupCompleteOrIncompleteUi()

        initiateFetchingCaseNumbers()
    }

    private val mBroadcastListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_OFF -> {
                        bluetooth_card_view.render(formatBlueToothTitle(false), false)
                        refreshSetupCompleteOrIncompleteUi()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        bluetooth_card_view.render(formatBlueToothTitle(false), false)
                        refreshSetupCompleteOrIncompleteUi()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        refreshSetupCompleteOrIncompleteUi()
                    }
                }
            }
        }
    }

    private fun enableParentViewWhenNotificationIsShowing(enableParent: Boolean) {
        txt_hide_number.isEnabled = enableParent
        txt_show_number.isEnabled = enableParent
        bluetooth_card_view.isEnabled = enableParent
        location_card_view.isEnabled = enableParent
        battery_card_view.isEnabled = enableParent
    }

    fun createStateList(): ArrayList<String>{
        val list = ArrayList<String>()
        list.add("Australia")
        list.add("Australian Capital Territory")
        list.add("New South Wales")
        list.add("Northern Territory")
        list.add("Queensland")
        list.add("South Australia")
        list.add("Tasmania")
        list.add("Victoria")
        list.add("Western Australia")
        return list
    }
}
