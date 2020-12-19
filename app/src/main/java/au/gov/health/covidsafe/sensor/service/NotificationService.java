package au.gov.health.covidsafe.sensor.service;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import au.gov.health.covidsafe.R;
import au.gov.health.covidsafe.sensor.datatype.Triple;
import au.gov.health.covidsafe.sensor.datatype.Tuple;

/// Notification service for enabling foreground service (notification must be displayed to show app is running in the background).
public class NotificationService {
    private static NotificationService shared;
    private static Application application;
    private final Context context;
    private final static String notificationChannelName = "NotificationChannel";
    private final int notificationChannelId = notificationChannelName.hashCode();
    private Triple<String, String, Notification> notificationContent = new Triple<>(null, null, null);

    private NotificationService(final Application application) {
        this.application = application;
        this.context = application.getApplicationContext();
        createNotificationChannel();
    }

    /// Get shared global instance of notification service
    public final static NotificationService shared(final Application application) {
        if (shared == null) {
            shared = new NotificationService(application);
        }
        return shared;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(notificationChannelName, notificationChannelName, importance);
            channel.setDescription(notificationChannelName);
            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Tuple<Integer, Notification> notification(final String title, final String body) {
        if (title != null && body != null) {
            final String existingTitle = notificationContent.a;
            final String existingBody = notificationContent.b;
            if (!title.equals(existingTitle) || !body.equals(existingBody)) {
                createNotificationChannel();
                final Intent intent = new Intent(context, application.getClass());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelName)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                final Notification notification = builder.build();
                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(notificationChannelId, notification);
                notificationContent = new Triple<>(title, body, notification);
                return new Tuple<>(notificationChannelId, notification);
            }
        } else {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.deleteNotificationChannel(notificationChannelName);
            notificationContent = new Triple<>(null, null, null);
        }
        return new Tuple<>(notificationChannelId, null);
    }
}
