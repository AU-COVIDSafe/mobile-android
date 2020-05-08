package com.atlassian.mobilekit.module.feedback;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

import com.atlassian.mobilekit.module.core.UiInfo;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.core.UiReceiver;
import com.atlassian.mobilekit.module.feedback.commands.Result;
import com.google.android.material.snackbar.Snackbar;

public class SnackbarReceiver implements UiReceiver<Result> {

    private final UiInfo uiInfo;
    private final UiNotifier uiNotifier;
    private final String message;
    private final String email;

    SnackbarReceiver(@NonNull UiInfo uiInfo, @NonNull UiNotifier uiNotifier,
                     @NonNull String message, @NonNull String email) {
        this.uiInfo = uiInfo;
        this.uiNotifier = uiNotifier;
        this.message = message;
        this.email = email;
    }

    @Override
    public void receive(Result data) {
        switch (data) {
            case SUCCESS:
                showSuccessRunnable.run();
                break;

            case FAIL:
                showFailureRunnable.run();
                break;
        }
    }

    private static void showProgressBar(Activity activity) {
        if (activity instanceof ProgressDialogActions) {
            ((ProgressDialogActions) activity).showProgressDialog();
        }
    }

    private static void dismissProgressBar(Activity activity) {
        if (activity instanceof ProgressDialogActions) {
            ((ProgressDialogActions) activity).dismissProgressDialog();
        }
    }

    private static void doFinish(Activity activity) {
        if (activity instanceof FinishAction) {
            ((FinishAction) activity).doFinish();
        }
    }

    private final Runnable showFailureRunnable = new Runnable() {

        private int numOfRetries;

        @Override
        public void run() {
            final Activity activity = uiInfo.getCurrentActivity();

            if (!uiInfo.isAppVisible()) {
                FeedbackModule.notifySendCompleted(Result.FAIL);
                return;
            } else if (null == activity) {
                if (numOfRetries < 3) {
                    numOfRetries++;
                    uiNotifier.postDelayed(this, 200);
                } else {
                    FeedbackModule.notifySendCompleted(Result.FAIL);
                }
                return;
            }

            // If the code has reached here, then the activity is visible.
            FeedbackModule.notifySendCompleted(Result.FAIL);

            final Snackbar snackbar = SnackbarBuilder.build(activity, R.string.mk_fb_feedback_failed);
            final SnackbarCallback callback = new SnackbarCallback();
            snackbar.addCallback(callback);
            snackbar.setAction(R.string.mk_fb_retry, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FeedbackModule.sendFeedback(message, email);
                    // We remove the callback here so we don't release the screenshot
                    snackbar.removeCallback(callback);

                    showProgressBar(activity);

                    // We have to handle any notifications that the callback would have.
                    FeedbackModule.notificationDismissed();
                }
            });

            // Notify Listeners Early of intent
            FeedbackModule.notificationStarted();
            dismissProgressBar(activity);

            snackbar.show();
        }
    };

    private final Runnable showSuccessRunnable = new Runnable() {

        private int numOfRetries;

        @Override
        public void run() {
            final Activity activity = uiInfo.getCurrentActivity();

            if (!uiInfo.isAppVisible()) {
                FeedbackModule.notifySendCompleted(Result.SUCCESS);
                return;
            } else if (null == activity) {
                if (numOfRetries < 3) {
                    numOfRetries++;
                    uiNotifier.postDelayed(this, 200);
                } else {
                    FeedbackModule.notifySendCompleted(Result.SUCCESS);
                }
                return;
            }

            // If the code has reached here, then the activity is visible.
            FeedbackModule.notifySendCompleted(Result.SUCCESS);

            final Snackbar snackbar = SnackbarBuilder.build(activity, R.string.mk_fb_feedback_sent);
            snackbar.addCallback(new SnackbarCallback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);
                    doFinish(activity);
                }
            });

            // Notify Listeners Early of intent
            FeedbackModule.notificationStarted();
            dismissProgressBar(activity);

            snackbar.show();
        }
    };

}
