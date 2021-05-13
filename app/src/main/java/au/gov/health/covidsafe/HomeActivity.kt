package au.gov.health.covidsafe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.Message
import au.gov.health.covidsafe.networking.response.MessagesResponse
import au.gov.health.covidsafe.notifications.NotificationBuilder
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.scheduler.GetMessagesScheduler
import au.gov.health.covidsafe.sensor.SensorDelegate
import au.gov.health.covidsafe.sensor.ble.BLEDevice
import au.gov.health.covidsafe.sensor.datatype.*
import au.gov.health.covidsafe.ui.devicename.DeviceNameChangePromptActivity
import au.gov.health.covidsafe.ui.home.HomeFragment
import au.gov.health.covidsafe.ui.restriction.RestrictionFragment
import au.gov.health.covidsafe.ui.settings.SettingsFragment
import au.gov.health.covidsafe.ui.utils.Utils
import au.gov.health.covidsafe.utils.NetworkConnectionCheck
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_home.*


private const val TAG = "HomeActivity"
private const val UNAUTHORIZED = "Unauthorized"
private const val UNAUTHENTICATED = "unauthenticated"
private const val CLOUDFRONT = "CloudFront"

class HomeActivity : FragmentActivity(), NetworkConnectionCheck.NetworkConnectionListener, SensorDelegate {

    var isAppUpdateAvailableLiveData = MutableLiveData<Boolean>()
    var appUpdateAvailableMessageResponseLiveData = MutableLiveData<MessagesResponse>()
    var isWindowFocusChangeLiveData = MutableLiveData<Boolean>()
    var isJWTCorrupted = MutableLiveData<Boolean>()
    var isJWTExpired = MutableLiveData<Boolean>()
    var cloudFrontIssue = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CentralLog.d(TAG, "onCreate() intent.action = ${intent.action}")
        NotificationBuilder.clearPossibleIssueNotificationCheck()
        NotificationBuilder.handlePushNotification(this, intent.action)

        setContentView(R.layout.activity_home)

        Utils.startBluetoothMonitoringService(this)

        //Get Firebase Token
        getInstanceID()
        onClickListener()

        NetworkConnectionCheck.addNetworkChangedListener(this, this)
    }

    private fun onClickListener() {
        navigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    val homeFragment = HomeFragment()
                    openFragment(homeFragment, "home")
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_restriction -> {
                    val restrictionFragment = RestrictionFragment()
                    openFragment(restrictionFragment, "restriction")
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    val settingsFragment = SettingsFragment()
                    openFragment(settingsFragment, "setting")
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
            return@setOnNavigationItemSelectedListener false
        }
    }

    private fun openFragment(fragment: Fragment, tag: String) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.home_nav_host)?.tag
        if (tag != currentFragment) {
            supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(tag)
                    .replace(R.id.home_nav_host, fragment, tag)
                    .commit()
        }
    }

    override fun onResume() {
        super.onResume()

        checkAndShowDeviceNameChangePrompt()
        checkAndUpdateHealthStatus()
    }

    private fun checkAndShowDeviceNameChangePrompt() {
        if (!Preference.isDeviceNameChangePromptDisplayed(this)) {
            Intent(this, DeviceNameChangePromptActivity::class.java).also {
                startActivity(it)
            }
            Preference.setDeviceNameChangePromptDisplayed(this)
        }
    }

    private fun checkAndUpdateHealthStatus() {
        if (!Preference.getAuthenticate(this)) {
            isJWTExpired.postValue(true)
        }
        GetMessagesScheduler.scheduleGetMessagesJob {
            if (it.errorBodyMessage.equals(UNAUTHORIZED) || it.errorBodyMessage.equals(UNAUTHENTICATED)) {
                isJWTCorrupted.postValue(true)
                isJWTExpired.postValue(true)
            } else if (it.errorBodyMessage.equals(CLOUDFRONT)) {
                isJWTExpired.postValue(true)
                cloudFrontIssue.postValue(true)
            } else {
                Log.d("LEE", "Authenticate")
                isJWTCorrupted.postValue(false)
                isJWTExpired.postValue(false)
            }

            val isAppWithLatestVersion = it.messages.isNullOrEmpty()
            isAppUpdateAvailableLiveData.postValue(isAppWithLatestVersion)
            CentralLog.d(TAG, "isAppWithLatestVersion: $it")

            if (!isAppWithLatestVersion) {
                val title = getString(R.string.update_available_title)
                val body = getString(R.string.update_available_message_android)

                appUpdateAvailableMessageResponseLiveData.postValue(MessagesResponse(
                        listOf(
                                Message(
                                        title,
                                        body,
                                        "https://play.google.com/store/apps/details?id=au.gov.health.covidsafe")
                        )
                , it.message, it.forceappupgrade, it.errorBodyMessage))
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        CentralLog.d(TAG, "onNewIntent = ${intent?.action})")
        NotificationBuilder.handlePushNotification(this, intent?.action)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        CentralLog.d(TAG, "onWindowFocusChanged(hasFocus = $hasFocus)")
        isWindowFocusChangeLiveData.postValue(hasFocus)

        super.onWindowFocusChanged(hasFocus)
    }

    /**
     * Provides notification support to inform users to update to the latest version of the app.
     * This feature will also allow for troubleshooting of the app in the future and allow for
     * targeted notifications
     */
    private fun getInstanceID() {
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        CentralLog.e(TAG, "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token

                    // Log and toast
                    CentralLog.d(TAG, "getInstance() InstanceID = $token")

                    token?.let {
                        Preference.putFirebaseInstanceID(TracerApp.AppContext, it)
                    }
                })
    }

    private var previousInternetConnection = true
    override fun onNetworkStatusChanged(isAvailable: Boolean) {
        if (!previousInternetConnection && isAvailable) {
            checkAndUpdateHealthStatus()
        }
        previousInternetConnection = isAvailable
    }

    override fun sensor(sensor: SensorType?, didDetect: TargetIdentifier?) {
    }

    override fun sensor(sensor: SensorType?, didRead: PayloadData?, fromTarget: TargetIdentifier?) {
    }

    override fun sensor(sensor: SensorType?, didShare: MutableList<PayloadData>?, fromTarget: TargetIdentifier?) {
    }

    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?) {
    }

    override fun sensor(sensor: SensorType?, didVisit: Location?) {
    }

    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?, withPayload: PayloadData?, device: BLEDevice) {
    }
    override fun sensor(sensor: SensorType?, didMeasure: Proximity?, fromTarget: TargetIdentifier?, withPayload: PayloadData?) {
    }

    override fun sensor(sensor: SensorType?, didUpdateState: SensorState?) {
    }

    override fun sensor(sensor: SensorType?, didRead: PayloadData?, fromTarget: TargetIdentifier?, atProximity: Proximity?, withTxPower: Int, device: BLEDevice) {
    }

}