package com.atlassian.mobilekit.module.feedback;


import android.app.Activity;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarBuilder {

    private SnackbarBuilder() {
        // intentionally empty
    }

    public static Snackbar build(Activity activity, int resId) {
        return Snackbar.make(getNotificationView(activity),
                applyColorSpan(activity.getString(resId)),
                Snackbar.LENGTH_LONG);
    }

    private static SpannableStringBuilder applyColorSpan(String txt) {
        // Force text color, otherwise it may show up using odd color in the app.
        final ForegroundColorSpan whiteSpan = new ForegroundColorSpan(Color.WHITE);
        final SpannableStringBuilder spanText = new SpannableStringBuilder(txt);
        spanText.setSpan(whiteSpan, 0, spanText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spanText;
    }

    private static View getNotificationView(Activity activity) {
        int id = FeedbackModule.getNotificationViewId();
        if (id == 0) {
            id = android.R.id.content;
        }

        View v = activity.findViewById(id);
        if (v == null) {
            v = activity.findViewById(android.R.id.content);
        }

        return v;
    }
}
