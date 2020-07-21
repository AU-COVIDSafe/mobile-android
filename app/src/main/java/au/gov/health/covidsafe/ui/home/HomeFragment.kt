package au.gov.health.covidsafe.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.WebViewActivity
import au.gov.health.covidsafe.extensions.*
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home_external_links.*
import kotlinx.android.synthetic.main.fragment_home_setup_complete_header.*
import kotlinx.android.synthetic.main.fragment_home_setup_incomplete_content.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
private const val FOURTEEN_DAYS_IN_MILLIS = 14 * ONE_DAY_IN_MILLIS

class HomeFragment : BaseFragment(), EasyPermissions.PermissionCallbacks {

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

        // display app update reminder
//        if (System.currentTimeMillis()
//                -
//                Preference.getAppUpdateReminderDismissedTime(requireContext())
//                > ONE_DAY_IN_MILLIS
//        ) {
//            app_update_reminder.visibility = VISIBLE
//        }

        // disable the app update reminder for now
        app_update_reminder.visibility = GONE

        bluetooth_card_view.setOnClickListener { requestBlueToothPermissionThenNextPermission() }
        location_card_view.setOnClickListener { askForLocationPermission() }
        battery_card_view.setOnClickListener { excludeFromBatteryOptimization() }
        push_card_view.setOnClickListener { gotoPushNotificationSettings() }

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
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToHelpFragment())
        }

        go_to_play_store.setOnClickListener {
            app_update_reminder.visibility = GONE
        }

        remind_me_later.setOnClickListener {
            Preference.putAppUpdateReminderDismissedTime(requireContext())
            app_update_reminder.visibility = GONE
        }

        if (!mIsBroadcastListenerRegistered) {
            registerBroadcast()
        }
        refreshSetupCompleteOrIncompleteUi()

        home_header_no_bluetooth_pairing.text = LinkBuilder.getNoBluetoothPairingContent(requireContext())
        home_header_no_bluetooth_pairing.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onPause() {
        super.onPause()
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
    private fun refreshSetupCompleteOrIncompleteUi() {
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
                home_header_picture_setup_complete.setImageResource(R.drawable.ic_red_cross)

                content_setup_incomplete_group.visibility = VISIBLE
                updateBlueToothStatus()
                updatePushNotificationStatus()
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
        val bluetoothEnabled = isBlueToothEnabled() ?: false
        val pushNotificationEnabled = isPushNotificationEnabled() ?: true
        val nonBatteryOptimizationAllowed = isBatteryOptimizationDisabled() ?: true
        val locationStatusAllowed = isLocationPermissionAllowed() ?: true

        return bluetoothEnabled &&
                pushNotificationEnabled &&
                nonBatteryOptimizationAllowed &&
                locationStatusAllowed &&
                isLocationEnabledOnDevice()
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
        isBlueToothEnabled()?.let {
            bluetooth_card_view.visibility = VISIBLE
            bluetooth_card_view.render(formatBlueToothTitle(it), it)
        } ?: run {
            bluetooth_card_view.visibility = GONE
        }
    }

    private fun updatePushNotificationStatus() {
        isPushNotificationEnabled()?.let {
            push_card_view.visibility = VISIBLE
            push_card_view.render(
                    formatPushNotificationTitle(it),
                    it,
                    getString(R.string.home_app_permission_push_notification_prompt)
            )
        } ?: run {
            push_card_view.visibility = GONE
        }
    }

    private fun updateBatteryOptimizationStatus() {
        isBatteryOptimizationDisabled()?.let {
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
        isLocationPermissionAllowed()?.let {
            val locationWorking = it && isLocationEnabledOnDevice()

            location_card_view.visibility = VISIBLE
            location_card_view.render(formatLocationTitle(locationWorking), locationWorking)
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

}
