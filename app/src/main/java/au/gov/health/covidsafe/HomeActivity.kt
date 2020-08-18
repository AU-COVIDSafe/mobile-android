package au.gov.health.covidsafe

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import au.gov.health.covidsafe.Utils.checkInternetConnectionToGoogle
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.Message
import au.gov.health.covidsafe.networking.response.MessagesResponse
import au.gov.health.covidsafe.scheduler.GetMessagesScheduler
import au.gov.health.covidsafe.ui.home.HomeFragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId

private const val TAG = "HomeActivity"

class HomeActivity : FragmentActivity() {
    private fun checkInternetConnection() {
        checkInternetConnectionToGoogle {
            HomeFragment.instanceWeakRef?.get()?.updateConnectionTile(it)
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CentralLog.d(TAG, "onCreate() intent.action = ${intent.action}")

        setContentView(R.layout.activity_home)

        Utils.startBluetoothMonitoringService(this)

        // messages API related
        getInstanceID()
    }

    override fun onResume() {
        super.onResume()

        CentralLog.d(TAG, "onResume() intent.action = ${intent.action}")

        if (intent.action == "au.gov.health.covidsafe.UPGRADE_APP") {
            Utils.gotoPlayStore(this)
        }

        if (!Preference.isDeviceNameChangePromptDisplayed(this)) {
            Intent(this, DeviceNameChangePromptActivity::class.java).also {
                startActivity(it)
            }

            Preference.setDeviceNameChangePromptDisplayed(this)
        }

        checkInternetConnection()

        GetMessagesScheduler.scheduleGetMessagesJob {
//            HomeFragment.instanceWeakRef?.get()?.updateMessageTiles(it)

//            HomeFragment.instanceWeakRef?.get()?.updateMessageTiles(MessagesResponse(
//                    listOf(
//                            Message(
//                                    "Update available",
//                                    "We’ve been making improvements to COVIDSafe. Update via Google Play Store.",
//                                    "market://details?id=au.gov.health.covidsafe"),
//                            Message(
//                                    "Update available",
//                                    "We’ve been making improvements to COVIDSafe. Update via Google Play Store.",
//                                    "https://play.google.com/store/apps/details?id=au.gov.health.covidsafe")
//                    )
//            ))

            if (!it.messages.isNullOrEmpty()) {
                val title = getString(R.string.update_available_title)
                val body = getString(R.string.update_available_message_android)

                HomeFragment.instanceWeakRef?.get()?.updateMessageTiles(MessagesResponse(
                        listOf(
                                Message(
                                        title,
                                        body,
                                        "https://play.google.com/store/apps/details?id=au.gov.health.covidsafe")
                        )
                ))
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        CentralLog.d(TAG, "onWindowFocusChanged(hasFocus = $hasFocus)")

        HomeFragment.instanceWeakRef?.get()?.refreshSetupCompleteOrIncompleteUi()
        checkInternetConnection()

        super.onWindowFocusChanged(hasFocus)
    }
}