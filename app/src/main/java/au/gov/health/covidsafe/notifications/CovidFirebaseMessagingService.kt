package au.gov.health.covidsafe.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.ui.utils.Utils.gotoPlayStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TAG = "CovidFirebaseMessagingService"


class CovidFirebaseMessagingService : FirebaseMessagingService(), CoroutineScope {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        CentralLog.d(TAG, "onMessageReceived() received message from ${remoteMessage.from}")

        // log notification payload.
        remoteMessage.notification?.let {
            CentralLog.d(TAG, "onMessageReceived() notification = ${it.title} ${it.body} ${it.clickAction}")

            if (it.clickAction == getString(R.string.notification_click_action_upgrade_app)) {
                gotoPlayStore(applicationContext)
            } else {
                createPushNotification(it)
            }
        }

        // log data payload.
        remoteMessage.data.isNotEmpty().let {
            CentralLog.d(TAG, "onMessageReceived() data = " + remoteMessage.data)
        }
    }

    private fun createPushNotification(notification: RemoteMessage.Notification) {
        launch(Dispatchers.Main) {
            val intent = Intent(this@CovidFirebaseMessagingService, HomeActivity::class.java)
            intent.action = notification.clickAction
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this@CovidFirebaseMessagingService, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channelId = getString(R.string.default_notification_channel_id)

            val notificationBuilder = NotificationCompat.Builder(this@CovidFirebaseMessagingService, channelId)
                    .setContentTitle(notification.title)
                    .setContentText(notification.body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentIntent(pendingIntent)
                    .setSound(defaultSoundUri)
                    .setColor(ContextCompat.getColor(this@CovidFirebaseMessagingService, R.color.notification_tint))

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        channelId,
                        getString(R.string.default_notification_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    /**
     * Called when InstanceID token is updated.
     */
    override fun onNewToken(token: String) {
        CentralLog.d(TAG, "onNewToken() InstanceID = $token")
        Preference.putFirebaseInstanceID(TracerApp.AppContext, token)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()
}