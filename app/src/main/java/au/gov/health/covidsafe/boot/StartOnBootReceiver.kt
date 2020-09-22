package au.gov.health.covidsafe.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.gov.health.covidsafe.ui.utils.Utils
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.scheduler.GetMessagesScheduler

private const val TAG = "StartOnBootReceiver"

class StartOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            CentralLog.d(TAG, "boot completed received")

            try {
                CentralLog.d("StartOnBootReceiver", "Attempting to start service")
                Utils.scheduleStartMonitoringService(context, 500)
                checkAndUpdateHealthStatus()
            } catch (e: Throwable) {
                CentralLog.e(TAG, e.localizedMessage ?: "Error message is empty:")
                e.printStackTrace()
            }

        }
    }

    private fun checkAndUpdateHealthStatus() {
        GetMessagesScheduler.scheduleGetMessagesJob {
            CentralLog.d(TAG, "checkAndUpdateHealthStatus on app reboot completed ")
        }
    }
}
