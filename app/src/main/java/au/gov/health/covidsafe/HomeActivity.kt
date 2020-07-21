package au.gov.health.covidsafe

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.scheduler.GetMessagesScheduler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId

private const val TAG = "HomeActivity"

class HomeActivity : FragmentActivity() {

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
        setContentView(R.layout.activity_home)

        Utils.startBluetoothMonitoringService(this)

        // messages API related
        getInstanceID()
//        GetMessagesScheduler.getMessages()
        GetMessagesScheduler.scheduleGetMessagesJob()
    }
}