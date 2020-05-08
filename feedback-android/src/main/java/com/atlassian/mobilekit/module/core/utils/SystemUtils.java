package com.atlassian.mobilekit.module.core.utils;

import android.view.View;
import android.view.inputmethod.InputMethodManager;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Utils to interact with Android system APIs.
 */
public class SystemUtils {

    /**
     * Hides soft keyboard
     */
    public static void hideSoftKeyboard(View target) {
        InputMethodManager inputMethodManager = (InputMethodManager) target.getContext().getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
    }
}
