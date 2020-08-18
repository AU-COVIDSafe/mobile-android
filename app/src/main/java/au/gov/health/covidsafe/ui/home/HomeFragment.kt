package au.gov.health.covidsafe.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.navigation.fragment.findNavController
import au.gov.health.covidsafe.*
import au.gov.health.covidsafe.extensions.*
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.MessagesResponse
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.BaseFragment
import au.gov.health.covidsafe.ui.home.view.ExternalLinkCard
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home_external_links.*
import kotlinx.android.synthetic.main.fragment_home_setup_complete_header.*
import kotlinx.android.synthetic.main.fragment_home_setup_incomplete_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "HomeFragment"

private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
private const val FOURTEEN_DAYS_IN_MILLIS = 14 * ONE_DAY_IN_MILLIS

class HomeFragment : BaseFragment(), EasyPermissions.PermissionCallbacks {

    companion object {
        var instanceWeakRef: WeakReference<HomeFragment>? = null
    }

    private lateinit var presenter: HomePresenter

    private var mIsBroadcastListenerRegistered = false

    private var counter: Int = 0

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
                        bluetooth_card_view.render(formatBlueToothTitle(true), true)
                        refreshSetupCompleteOrIncompleteUi()
                    }
                }
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        presenter = HomePresenter(this)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.home_header_help.setOnClickListener {
            HelpFragment.anchor = null
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())

        }
        if (BuildConfig.ENABLE_DEBUG_SCREEN) {
            view.header_background.setOnClickListener {
                counter++
                if (counter >= 2) {
                    counter = 0
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPeekActivity())
                }
            }
        }
        home_version_number.text = getString(R.string.home_version_number, BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()

        instanceWeakRef = WeakReference(this)

        // disable the app update reminder for now
        app_update_reminder.visibility = GONE

        bluetooth_card_view.setOnClickListener { requestBlueToothPermissionThenNextPermission() }
        location_card_view.setOnClickListener { askForLocationPermission() }
        battery_card_view.setOnClickListener { excludeFromBatteryOptimization() }

        home_been_tested_button.setOnClickListener {
            navigateTo(R.id.action_home_to_selfIsolate)
        }
        home_setup_complete_share.setOnClickListener {
            shareThisApp()
        }
        home_setup_complete_news.setOnClickListener {
            goToNewsWebsite()
        }
        home_setup_complete_app.setOnClickListener {
            goToCovidApp()
        }

        help_topics_link.setOnClickListener {
            HelpFragment.anchor = null
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())
        }

        change_language_link.setOnClickListener {
            HelpFragment.anchor = "#other-languages"
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())
        }

        if (!mIsBroadcastListenerRegistered) {
            registerBroadcast()
        }
        refreshSetupCompleteOrIncompleteUi()

        home_header_no_bluetooth_pairing.text = LinkBuilder.getNoBluetoothPairingContent(requireContext())
        home_header_no_bluetooth_pairing.movementMethod = LinkMovementMethod.getInstance()

        updateNotificationStatusTile()
    }

    override fun onPause() {
        super.onPause()

        instanceWeakRef = null

        bluetooth_card_view.setOnClickListener(null)
        location_card_view.setOnClickListener(null)
        battery_card_view.setOnClickListener(null)
        home_been_tested_button.setOnClickListener(null)
        home_setup_complete_share.setOnClickListener(null)
        home_setup_complete_news.setOnClickListener(null)
        home_setup_complete_app.setOnClickListener(null)
        help_topics_link.setOnClickListener(null)

        activity?.let { activity ->
            if (mIsBroadcastListenerRegistered) {
                activity.unregisterReceiver(mBroadcastListener)
                mIsBroadcastListenerRegistered = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        home_root.removeAllViews()
    }

    private fun isDataUploadedInPast14Days(context: Context): Boolean {
        val isUploaded = Preference.isDataUploaded(context)

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
        context?.let {
            val isAllPermissionsEnabled = allPermissionsEnabled()
            val isDataUploadedInPast14Days = isDataUploadedInPast14Days(it)

            val line1 = it.getString(
                    if (isAllPermissionsEnabled) {
                        R.string.home_header_active_title
                    } else {
                        R.string.home_header_inactive_title
                    }
            )

            val line2 = if (isDataUploadedInPast14Days) {
                it.getString(R.string.home_header_uploaded_on_date, getDataUploadDateHtmlString(it)) + "<br/>"
            } else {
                ""
            }

            val line3 = it.getString(
                    if (isAllPermissionsEnabled) {
                        R.string.home_header_active_no_action_required
                    } else {
                        R.string.home_header_inactive_check_your_permissions
                    }
            )

            home_header_setup_complete_header_line_1.text = line1

            home_header_setup_complete_header_line_1.setHeading()
            home_header_setup_complete_header_line_1.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

            val line2and3 = "$line2$line3"

            home_header_setup_complete_header_line_2.text =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        Html.fromHtml(line2and3, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        Html.fromHtml(line2and3)
                    }

            if (isAllPermissionsEnabled) {
                home_header_top_left_icon.visibility = GONE
                home_header_picture_setup_complete.layoutParams.let { layoutParams ->
                    val size = resources.getDimensionPixelSize(R.dimen.covidsafe_lottie_size)
                    layoutParams.height = size
                    layoutParams.width = size
                }
                home_header_picture_setup_complete.setAnimation("spinner_home.json")
                home_header_picture_setup_complete.resumeAnimation()

                content_setup_incomplete_group.visibility = GONE
                ContextCompat.getColor(it, R.color.lighter_green).let { bgColor ->
                    header_background.setBackgroundColor(bgColor)
                    header_background_overlap.setBackgroundColor(bgColor)
                }
            } else {
                home_header_top_left_icon.visibility = VISIBLE
                home_header_picture_setup_complete.layoutParams.let { layoutParams ->
                    val size = resources.getDimensionPixelSize(R.dimen.keyline_8)
                    layoutParams.height = size
                    layoutParams.width = size
                }
                home_header_picture_setup_complete.setImageResource(R.drawable.ic_red_cross_no_circle)

                content_setup_incomplete_group.visibility = VISIBLE
                updateBlueToothStatus()
                updateBatteryOptimizationStatus()
                updateLocationStatus()
                ContextCompat.getColor(it, R.color.grey).let { bgColor ->
                    header_background.setBackgroundColor(bgColor)
                    header_background_overlap.setBackgroundColor(bgColor)
                }
            }

            home_been_tested_button.visibility = if (isDataUploadedInPast14Days) GONE else VISIBLE
        }
    }

    private fun allPermissionsEnabled(): Boolean {
        val context = requireContext()

        val bluetoothEnabled = context.isBlueToothEnabled() ?: false
        val nonBatteryOptimizationAllowed = context.isBatteryOptimizationDisabled() ?: true
        val locationStatusAllowed = context.isLocationPermissionAllowed() ?: true

        return bluetoothEnabled &&
                nonBatteryOptimizationAllowed &&
                locationStatusAllowed &&
                context.isLocationEnabledOnDevice()
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

    private fun shareThisApp() {
        val newIntent = Intent(Intent.ACTION_SEND)
        newIntent.type = "text/plain"
        newIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_this_app_content))
        startActivity(Intent.createChooser(newIntent, null))
    }

    private fun updateBlueToothStatus() {
        requireContext().isBlueToothEnabled()?.let {
            bluetooth_card_view.visibility = VISIBLE
            bluetooth_card_view.render(formatBlueToothTitle(it), it)
        } ?: run {
            bluetooth_card_view.visibility = GONE
        }
    }

    private fun updateBatteryOptimizationStatus() {
        requireContext().isBatteryOptimizationDisabled()?.let {
            battery_card_view.visibility = VISIBLE
            battery_card_view.render(
                    formatNonBatteryOptimizationTitle(!it),
                    it,
                    getString(R.string.battery_optimisation_prompt)
            )
        } ?: run {
            battery_card_view.visibility = GONE
        }
    }

    private fun updateLocationStatus() {
        requireContext().isLocationPermissionAllowed()?.let {
            val locationWorking = it && requireContext().isLocationEnabledOnDevice()
            val locationOffPrompts = getString(R.string.home_set_location_why)

            location_card_view.visibility = VISIBLE
            location_card_view.render(formatLocationTitle(locationWorking), locationWorking, locationOffPrompts)
        } ?: run {
            location_card_view.visibility = GONE
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

    private fun formatPushNotificationTitle(on: Boolean): String {
        return resources.getString(R.string.home_push_notification_permission, getPermissionEnabledTitle(on))
    }

    private fun getPermissionEnabledTitle(on: Boolean): String {
        return resources.getString(if (on) R.string.home_permission_on else R.string.home_permission_off)
    }

    private fun getEnabledOrDisabledString(isEnabled: Boolean): String {
        return resources.getString(if (isEnabled) R.string.enabled else R.string.disabled)
    }

    private fun goToNewsWebsite() {
        val url = getString(R.string.home_set_complete_external_link_news_url)
        try {
            Intent(Intent.ACTION_VIEW).run {
                data = Uri.parse(url)
                startActivity(this)
            }
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(activity, WebViewActivity::class.java)
            intent.putExtra(WebViewActivity.URL_ARG, url)
            startActivity(intent)
        }
    }

    private fun goToCovidApp() {
        val url = getString(R.string.home_set_complete_external_link_app_url)
        try {
            Intent(Intent.ACTION_VIEW).run {
                data = Uri.parse(url)
                startActivity(this)
            }
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(activity, WebViewActivity::class.java)
            intent.putExtra(WebViewActivity.URL_ARG, url)
            startActivity(intent)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == LOCATION && EasyPermissions.somePermissionPermanentlyDenied(this, listOf(Manifest.permission.ACCESS_COARSE_LOCATION))) {
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

    fun updateConnectionTile(isInternetConnected: Boolean) {
        // called on IO thread; run the UI logic on UI thread
        GlobalScope.launch(Dispatchers.Main) {
            CentralLog.d(TAG, "updateConnectionTile() isInternetConnected = $isInternetConnected")

            var visibility = if (isInternetConnected) GONE else VISIBLE

            // don't display the tile when there's permission not enabled
            if (!allPermissionsEnabled()) {
                visibility = GONE
            }

            improve_performance_card.visibility = visibility
            internet_connection_tile.visibility = visibility

            if (visibility == VISIBLE) {
                internet_connection_tile.setOnClickListener {
                    // startActivity(Intent(ACTION_DATA_ROAMING_SETTINGS))
                    // startActivity(Intent(ACTION_WIFI_SETTINGS))

                    startActivity(Intent(requireContext(), InternetConnectionIssuesActivity::class.java))
                }
            }
        }
    }

    fun updateMessageTiles(messagesResponse: MessagesResponse) {
        GlobalScope.launch(Dispatchers.Main) {
            improve_performance_card_linear_layout.children.forEach {
                if (it != internet_connection_tile && it != improve_performance_title) {
                    improve_performance_card_linear_layout.removeView(it)
                }
            }

            if (!messagesResponse.messages.isNullOrEmpty()) {
                improve_performance_card.visibility = VISIBLE

                messagesResponse.messages.forEach { message ->
                    ExternalLinkCard(requireContext(), null, 0).also {
                        it.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { layoutParams ->
                            layoutParams.setMargins(0, resources.getDimensionPixelSize(R.dimen.divider_height), 0, 0)
                        }

                        it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

                        it.setMessage(message)
                        it.setErrorTextColor()

                        improve_performance_card_linear_layout.addView(it)
                    }
                }
            } else {
                improve_performance_card.visibility = GONE
            }
        }
    }

    private fun updateNotificationStatusTile() {
        var title = ""
        var body = ""

        if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        ) {
            notification_status_link.setTopRightIcon(R.drawable.ic_green_tick)
            title = getString(R.string.home_set_complete_external_link_notifications_title_iOS)
            body = getString(R.string.NotificationsEnabledBlurb)
        } else {
            notification_status_link.setTopRightIcon(R.drawable.ic_red_cross)
            title = getString(R.string.home_set_complete_external_link_notifications_title_iOS_off)
            body = getString(R.string.NotificationsEnabledBlurb)
        }

        notification_status_link.setTitleBodyAndClickCallback(title, body) {
            openAppNotificationSettings()
        }

        notification_status_link.setColorForContentWithAction()
    }

    private fun openAppNotificationSettings() {
        val context = requireContext()

        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = Uri.parse("package:" + context.packageName)
                }
            }
        }

        context.startActivity(intent)
    }

}
