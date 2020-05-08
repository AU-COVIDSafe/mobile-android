package com.atlassian.mobilekit.module.feedback;

import com.google.android.material.snackbar.Snackbar;


public class SnackbarCallback extends Snackbar.Callback {

    // Handle multiple onDismissed calls
    // Refer: https://code.google.com/p/android/issues/detail?id=214547
    private boolean isDismissed = false;

    @Override
    public void onDismissed(Snackbar snackbar, int event) {
        super.onDismissed(snackbar, event);
        if (isDismissed) {
            return;
        }
        isDismissed = true;
        FeedbackModule.notificationDismissed();
    }
}
