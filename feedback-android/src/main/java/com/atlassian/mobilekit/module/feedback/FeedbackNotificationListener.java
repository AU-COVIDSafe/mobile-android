package com.atlassian.mobilekit.module.feedback;

/**
 * This listener is notified when Feedback success/error prompts are displayed to the user. <br/>
 * These api are guaranteed to be invoked on the Main thread.
 */
public interface FeedbackNotificationListener {

    void onNotificationStarted();

    void onNotificationDismissed();
}
