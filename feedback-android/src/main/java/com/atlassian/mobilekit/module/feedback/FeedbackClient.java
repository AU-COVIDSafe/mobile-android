package com.atlassian.mobilekit.module.feedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.atlassian.mobilekit.module.core.Command;
import com.atlassian.mobilekit.module.core.DeviceInfo;
import com.atlassian.mobilekit.module.core.JobQueue;
import com.atlassian.mobilekit.module.core.Receiver;
import com.atlassian.mobilekit.module.core.UiInfo;
import com.atlassian.mobilekit.module.core.UiInfoListener;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.feedback.commands.Result;
import com.atlassian.mobilekit.module.feedback.commands.SendFeedbackCommand;
import com.atlassian.mobilekit.module.feedback.model.CreateIssueRequest;
import com.atlassian.mobilekit.module.feedback.model.FeedbackConfig;
import com.atlassian.mobilekit.module.feedback.network.BaseApiParams;
import com.atlassian.mobilekit.module.feedback.network.JmcRestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Commands supported by this client
 * Show Feedback
 * Send Feedback
 */
class FeedbackClient implements Receiver<FeedbackConfig>, UiInfoListener {

    private static final String LOG_TAG = FeedbackClient.class.getSimpleName();
    private static final String PROTOCOL_HTTPS = "https://";

    private static final Set<Class<?>> IGNORE_ACTIVITIES = new HashSet<>();

    static {
        IGNORE_ACTIVITIES.add(FeedbackActivity.class);
    }

    private final JmcRestClient restClient;
    private final JobQueue jobQueue;
    private final DeviceInfo deviceInfo;
    private final Map<String, String> baseQueryMap = new HashMap<>();
    private final UiNotifier uiNotifier;
    private final UiInfo uiInfo;
    private final FeedbackSettings settings;
    private final AtomicInteger notificationViewId = new AtomicInteger(0);
    private final CopyOnWriteArraySet<FeedbackNotificationListener> notificationListeners = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<SendFeedbackListener> sendFeedbackListeners = new CopyOnWriteArraySet<>();
    private FeedbackDataProvider feedbackDataProvider;

    private FeedbackConfig feedbackConfig;

    FeedbackClient(@NonNull JmcRestClient restClient,
                   @NonNull DeviceInfo deviceInfo,
                   @NonNull JobQueue jobQueue,
                   @NonNull UiNotifier uiNotifier,
                   @NonNull UiInfo uiInfo,
                   @NonNull FeedbackSettings settings) {

        this.restClient = restClient;
        this.jobQueue = jobQueue;
        this.deviceInfo = deviceInfo;
        this.uiNotifier = uiNotifier;
        this.uiInfo = uiInfo;
        this.settings = settings;

        init();
    }

    private void init() {
        uiInfo.registerListener(this);
    }

    @Override
    public void receive(FeedbackConfig data) {
        feedbackConfig = data;
        restClient.init(PROTOCOL_HTTPS, data.getHost());

        baseQueryMap.put(BaseApiParams.API_KEY, data.getApiKey());
        baseQueryMap.put(BaseApiParams.PROJECT, data.getProjectKey());
    }

    final void sendFeedback(String message, String email) {

        final CreateIssueRequest.Builder requestBuilder =
                new CreateIssueRequest.Builder()
                        .summary(message)
                        .description(message)
                        .isCrash(false)
                        .udid(deviceInfo.getUdid())
                        .uuid(deviceInfo.getUuid())
                        .appName(deviceInfo.getAppName())
                        .appId(deviceInfo.getAppPkgName())
                        .systemName(deviceInfo.getSystemName())
                        .deviceName(deviceInfo.getDeviceName())
                        .language(deviceInfo.getLanguage())
                        .components(Arrays.asList(feedbackConfig.getComponents()));

        setFeedbackDataProvider(new FeedbackDataProvider() {
            @Override
            public String getAdditionalDescription() {
                return null;
            }

            @Override
            public JiraIssueType getIssueType() {
                return JiraIssueType.SUPPORT;
            }

            @Override
            public Map<String, Object> getCustomFieldsData() {
                HashMap<String, Object> map = new HashMap<>();
                map.put("E-mail", email);
                map.put("OS version", deviceInfo.getSystemVersion());
                map.put("App version", deviceInfo.getAppVersionName());
                map.put("Phone model", deviceInfo.getModel());
                return map;
            }
        });

        final Command cmd = new SendFeedbackCommand(
                baseQueryMap, requestBuilder, feedbackDataProvider,
                restClient,
                new SnackbarReceiver(uiInfo, uiNotifier, message, email),
                uiNotifier);

        jobQueue.enqueue(cmd);
    }

    @Override
    public void onAppVisible() {

    }

    @Override
    public void onAppNotVisible() {
    }

    final void showFeedback() {

        final Activity curActivity = uiInfo.getCurrentActivity();
        if (curActivity == null) {
            Log.e(LOG_TAG, "No usable current activity. Abort Feedback.");
            return;
        } else if (IGNORE_ACTIVITIES.contains(curActivity.getClass())) {
            Log.e(LOG_TAG, "User is already in Feedback flow. Abort.");
            return;
        }

        final Context appCtx = curActivity.getApplicationContext();

        uiNotifier.post(new Runnable() {
            @Override
            public void run() {
                launchFeedbackScreen(curActivity, appCtx);
            }
        });
    }

    private void launchFeedbackScreen(Activity activity, Context appCtx) {
        final Context useCtx = activity.isFinishing() || activity.isChangingConfigurations()
                ? appCtx : activity;

        Intent intent = FeedbackActivity.getIntent(useCtx);
        useCtx.startActivity(intent);
    }

    private void setFeedbackDataProvider(FeedbackDataProvider feedbackDataProvider) {
        this.feedbackDataProvider = feedbackDataProvider;
    }

    final int getNotificationViewId() {
        return notificationViewId.get();
    }

    final void registerSendFeedbackListener(SendFeedbackListener listener) {
        sendFeedbackListeners.add(listener);
    }

    final void unregisterSendFeedbackListener(SendFeedbackListener listener) {
        sendFeedbackListeners.remove(listener);
    }

    final void notifySendCompleted(Result result) {
        for (SendFeedbackListener listener : sendFeedbackListeners) {
            listener.onSendCompleted(result);
        }
    }

    final void notificationStarted() {
        for (FeedbackNotificationListener fnl : notificationListeners) {
            fnl.onNotificationStarted();
        }
    }

    final void notificationDismissed() {
        for (FeedbackNotificationListener fnl : notificationListeners) {
            fnl.onNotificationDismissed();
        }
    }

    final void setEnableDialogDisplayed() {
        settings.setEnableDialogDisplayed();
    }
}
