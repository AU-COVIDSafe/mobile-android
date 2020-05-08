package com.atlassian.mobilekit.module.feedback;

import com.atlassian.mobilekit.module.feedback.commands.Result;

/**
 * This listener is notified when Feedback sending completes successfully or with an error
 */
public interface SendFeedbackListener {

    void onSendCompleted(Result result);
}
