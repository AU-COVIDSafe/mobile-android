package com.atlassian.mobilekit.module.feedback;

import android.content.SharedPreferences;

class FeedbackSettings {

    private static final String KEY_ENABLE_DIALOG_SHOWN = "enable_dialog_shown";

    private final SharedPreferences store;

    FeedbackSettings(SharedPreferences store) {
        this.store = store;
    }

    final void setEnableDialogDisplayed() {
        SharedPreferences.Editor editor = store.edit();
        editor.putBoolean(KEY_ENABLE_DIALOG_SHOWN, true);
        editor.apply();
    }
}
