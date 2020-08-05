package au.gov.health.covidsafe.services

import au.gov.health.covidsafe.*
import au.gov.health.covidsafe.Utils.gotoPlayStore
import au.gov.health.covidsafe.logging.CentralLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "CovidFirebaseMessagingService"

class CovidFirebaseMessagingService : FirebaseMessagingService() {

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

            if (it.clickAction == "au.gov.health.covidsafe.UPGRADE_APP"){
                gotoPlayStore(applicationContext)
            }
        }

        // log data payload.
        remoteMessage.data.isNotEmpty().let {
            CentralLog.d(TAG, "onMessageReceived() data = " + remoteMessage.data)
        }
    }

    /**
     * Called when InstanceID token is updated.
     */
    override fun onNewToken(token: String) {
        CentralLog.d(TAG, "onNewToken() InstanceID = $token")

        Preference.putFirebaseInstanceID(TracerApp.AppContext, token)
    }
}