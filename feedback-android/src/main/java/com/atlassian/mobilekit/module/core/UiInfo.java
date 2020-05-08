package com.atlassian.mobilekit.module.core;


import android.app.Activity;

public interface UiInfo {

    Activity getCurrentActivity();

    boolean isAppVisible();

    void registerListener(UiInfoListener listener);

    void unregisterListener(UiInfoListener listener);

}
