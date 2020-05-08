package com.atlassian.mobilekit.module.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArraySet;

public class ActivityTracker implements Application.ActivityLifecycleCallbacks, UiInfo {

    private WeakReference<Activity> activityRef = new WeakReference<>(null);
    private boolean isAppVisible = false;
    private final CopyOnWriteArraySet<UiInfoListener> listeners = new CopyOnWriteArraySet<>();

    public ActivityTracker(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public Activity getCurrentActivity() {
        Activity currentActivity = activityRef.get();

        if (currentActivity != null
                && (currentActivity.isFinishing()
                || currentActivity.isChangingConfigurations())) {
            currentActivity = null;
            activityRef = new WeakReference<>(null);
        }

        return currentActivity;
    }

    @Override
    public boolean isAppVisible() {
        return isAppVisible;
    }

    @Override
    public void registerListener(UiInfoListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(UiInfoListener listener) {
        listeners.remove(listener);
    }

    private void notifyAppVisible() {
        isAppVisible = true;
        for (UiInfoListener listener : listeners) {
            listener.onAppVisible();
        }
    }

    private void notifyAppNotVisible() {
        isAppVisible = false;
        for (UiInfoListener listener : listeners) {
            listener.onAppNotVisible();
        }
    }

    @Override
    public void onActivityCreated(@NotNull Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(@NotNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NotNull Activity activity) {
        final boolean wasEmpty = (activityRef.get() == null);
        activityRef = new WeakReference<>(activity);

        if (wasEmpty) {
            notifyAppVisible();
        }
    }

    @Override
    public void onActivityPaused(@NotNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NotNull Activity activity) {
        if (activityRef.get() == activity) {
            activityRef = new WeakReference<>(null);
            notifyAppNotVisible();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NotNull Activity activity) {
        if (activityRef.get() == activity) {
            activityRef = new WeakReference<>(null);
        }
    }
}
