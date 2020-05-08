package com.atlassian.mobilekit.module.core;

import androidx.appcompat.app.AppCompatActivity;


public class FeedbackBaseActivity extends AppCompatActivity {

    private boolean isPaused;
    private long pausedAt;

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        pausedAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        pausedAt = 0;
    }

    protected boolean isPaused() {
        return isPaused;
    }

    protected long getPausedDuration() {
        return System.currentTimeMillis() - pausedAt;
    }
}
