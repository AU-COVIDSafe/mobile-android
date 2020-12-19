package com.atlassian.mobilekit.module.feedback;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.atlassian.mobilekit.module.core.ActivityTracker;
import com.atlassian.mobilekit.module.core.AndroidUiNotifier;
import com.atlassian.mobilekit.module.core.Command;
import com.atlassian.mobilekit.module.core.DeviceInfo;
import com.atlassian.mobilekit.module.core.JobQueue;
import com.atlassian.mobilekit.module.core.UiInfo;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.feedback.commands.LoadFeedbackConfigCommand;
import com.atlassian.mobilekit.module.feedback.commands.Result;
import com.atlassian.mobilekit.module.feedback.network.JmcRestClient;

public final class FeedbackModule {

    private static final String NAMESPACE = "com.atlassian.mobilekit.module.feedback";
    private static final String STORE_NAME = NAMESPACE + ".preferences";

    private static FeedbackClient feedbackClient = null;
    private static JobQueue jobQueue = null;
    private static UiInfo activityTracker = null;

    private static UiNotifier androidUiNotifier = new AndroidUiNotifier();

    private FeedbackModule() {
        throw new AssertionError("Instances of this class are not allowed.");
    }

    /**
     * Initializes using Application object
     *
     * @param application
     */
    public static void init(@NonNull Application application) {
        // Build a Feedback Client here and _start_ its initialization.
        // Initialization will happen asynchronously in a background thread.
        if (feedbackClient == null) {
            jobQueue = new JobQueue();
            activityTracker = new ActivityTracker(application);

            final SharedPreferences store = application.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);

            feedbackClient = new FeedbackClient(
                    new JmcRestClient(),
                    new DeviceInfo(application.getApplicationContext()),
                    jobQueue,
                    androidUiNotifier,
                    activityTracker,
                    new FeedbackSettings(store));
        }

        Command loadConfigCommand = new LoadFeedbackConfigCommand(
                application.getApplicationContext(),
                feedbackClient, androidUiNotifier);

        jobQueue.enqueue(loadConfigCommand);
    }

    /**
     * Displays a screen to prompt user for feedback
     */
    public static void showFeedbackScreen() {
        feedbackClient.showFeedback();
    }

    
    static void notificationStarted() {
        feedbackClient.notificationStarted();
    }

    static void notificationDismissed() {
        feedbackClient.notificationDismissed();
    }

    static int getNotificationViewId() {
        return feedbackClient.getNotificationViewId();
    }

    static void sendFeedback(@NonNull String message, @NonNull String email) {
        feedbackClient.sendFeedback(message, email);
    }

    static void setEnableDialogDisplayed() {
        feedbackClient.setEnableDialogDisplayed();
    }

    static void registerSendFeedbackListener(SendFeedbackListener listener) {
        feedbackClient.registerSendFeedbackListener(listener);
    }

    static void unregisterSendFeedbackListener(SendFeedbackListener listener) {
        feedbackClient.unregisterSendFeedbackListener(listener);
    }

    static void notifySendCompleted(Result result) {
        feedbackClient.notifySendCompleted(result);
    }
}
