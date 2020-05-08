package com.atlassian.mobilekit.module.feedback.commands;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.atlassian.mobilekit.module.core.Receiver;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.feedback.model.FeedbackConfig;
import com.atlassian.mobilekit.module.feedback.R;

import java.net.URI;
import java.net.URISyntaxException;

public final class LoadFeedbackConfigCommand extends AbstractCommand<FeedbackConfig> {

    private static final String LOG_TAG = LoadFeedbackConfigCommand.class.getSimpleName();
    private final Context context;

    public LoadFeedbackConfigCommand(Context ctx,
                                     Receiver<FeedbackConfig> receiver,
                                     UiNotifier uiNotifier) {
        super(receiver, uiNotifier);
        context = ctx;
    }


    @Override
    public void run() {

        FeedbackConfig config = new FeedbackConfig(
                context.getString(R.string.mp_feedback_host),
                context.getString(R.string.mp_feedback_apikey),
                context.getString(R.string.mp_feedback_projectkey),
                context.getResources().getStringArray(R.array.mp_feedback_components)
        );

        String errMsg = errorCheck(config);
        if (errMsg != null) {
            // This will crash the app, so that developers can correct their code.
            throw new IllegalStateException(errMsg);
        }

        updateReceiver(config);
    }

    private String errorCheck(FeedbackConfig config) {

        StringBuilder errMsg = new StringBuilder();

        if (TextUtils.isEmpty(config.getHost())) {
            errMsg.append(getConfigEmptyErrMsg(R.string.mp_feedback_host));
        } else if (!isValidHost(config.getHost())) {
            errMsg.append(getConfigInvalidErrMsg(R.string.mp_feedback_host));
        }

        if (TextUtils.isEmpty(config.getApiKey())) {
            errMsg.append(getConfigEmptyErrMsg(R.string.mp_feedback_apikey));
        }

        if (TextUtils.isEmpty(config.getProjectKey())) {
            errMsg.append(getConfigEmptyErrMsg(R.string.mp_feedback_projectkey));
        }

        if (errMsg.length() > 0) {
            errMsg.append(context.getString(R.string.mk_fb_config_err_help));
            return errMsg.toString();
        }

        return null;
    }

    private String getConfigEmptyErrMsg(int resId) {
        return context.getString(R.string.mk_fb_no_config_property,
                context.getResources().getResourceEntryName(resId));
    }

    private String getConfigInvalidErrMsg(int resId) {
        return context.getString(R.string.mk_fb_invalid_config_property,
                context.getResources().getResourceEntryName(resId));
    }

    private boolean isValidHost(String input) {

        if (TextUtils.isEmpty(input)) {
            return false;
        }

        if (input.indexOf("/") >= 0) {
            return false;
        }

        try {
            URI uri = new URI("scheme://" + input);
            String host = uri.getHost();
            int port = uri.getPort();

            if (TextUtils.isEmpty(host)) {
                return false;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(host);

            if (port != -1) {
                sb.append(":").append(port);
            }

            return input.equals(sb.toString());

        } catch (URISyntaxException use) {
            Log.e(LOG_TAG, "URI Validation Failed.", use);
        }

        return false;
    }

}
