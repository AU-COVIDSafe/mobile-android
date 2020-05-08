package com.atlassian.mobilekit.module.core;


public interface UiNotifier {

    void post(Runnable runnable);

    void postDelayed(Runnable runnable, int delay);
}
