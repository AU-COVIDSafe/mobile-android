package com.atlassian.mobilekit.module.core;


import android.os.Handler;
import android.os.Looper;

public class AndroidUiNotifier implements UiNotifier {

    private final Handler uiHandler;

    public AndroidUiNotifier() {
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void post(Runnable runnable) {
        uiHandler.post(runnable);
    }

    @Override
    public void postDelayed(Runnable runnable, int delay) {
        uiHandler.postDelayed(runnable, delay);
    }
}
