package au.gov.health.covidsafe.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import au.gov.health.covidsafe.HomeActivity
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.services.BluetoothMonitoringService.Companion.DAILY_UPLOAD_NOTIFICATION_CODE
import au.gov.health.covidsafe.services.BluetoothMonitoringService.Companion.PENDING_ACTIVITY
import au.gov.health.covidsafe.services.BluetoothMonitoringService.Companion.PENDING_WIZARD_REQ_CODE

class NotificationTemplates {

    companion object {

        fun getRunningNotification(context: Context, channel: String): Notification {

            val intent = Intent(context, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val activityPendingIntent = PendingIntent.getActivity(
                    context,
                    PENDING_ACTIVITY,
                    intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                    .setContentTitle(context.getText(R.string.service_ok_title))
                    .setContentText(context.getText(R.string.service_ok_body))
                    .setTicker(context.getText(R.string.service_ok_body))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(context.getText(R.string.service_ok_body)))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentIntent(activityPendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setSound(null)
                    .setVibrate(null)
                    .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun lackingThingsNotification(context: Context, contentTextResId: Int, channel: String): Notification {
            return lackingThingsNotification(context, context.getString(contentTextResId), channel)
        }

        fun lackingThingsNotification(context: Context, contentText: String, channel: String): Notification {
            val intent = Intent(context, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("page", 3)

            val activityPendingIntent = PendingIntent.getActivity(
                    context, PENDING_WIZARD_REQ_CODE,
                    intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                    .setContentTitle(context.getText(R.string.service_not_ok_title))
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.ic_notification_warning)
                    .setTicker(contentText)
                    .addAction(
                            R.drawable.ic_notification_setting,
                            context.getText(R.string.service_not_ok_action),
                            activityPendingIntent
                    )
                    .setContentIntent(activityPendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setSound(null)
                    .setVibrate(null)
                    .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }
    }

}
