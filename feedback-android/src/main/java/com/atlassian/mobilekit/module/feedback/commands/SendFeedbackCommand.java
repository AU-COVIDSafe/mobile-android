package com.atlassian.mobilekit.module.feedback.commands;

import android.text.TextUtils;
import android.util.Log;

import com.atlassian.mobilekit.module.core.Receiver;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.feedback.FeedbackDataProvider;
import com.atlassian.mobilekit.module.feedback.JiraIssueType;
import com.atlassian.mobilekit.module.feedback.model.CreateIssueRequest;
import com.atlassian.mobilekit.module.feedback.model.CreateIssueResponse;
import com.atlassian.mobilekit.module.feedback.network.JmcRestClient;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public final class SendFeedbackCommand extends AbstractCommand<Result> {


    private static final String LOG_TAG = SendFeedbackCommand.class.getSimpleName();

    private final Map<String, String> queryMap;
    private final CreateIssueRequest.Builder requestBuilder;

    private final JmcRestClient restClient;
    private final FeedbackDataProvider feedbackDataProvider;

    public SendFeedbackCommand(Map<String, String> queryMap,
                               CreateIssueRequest.Builder requestBuilder,
                               FeedbackDataProvider feedbackDataProvider,
                               JmcRestClient restClient,
                               Receiver<Result> receiver,
                               UiNotifier uiNotifier) {

        super(receiver, uiNotifier);
        this.queryMap = queryMap;
        this.requestBuilder = requestBuilder;
        this.feedbackDataProvider = feedbackDataProvider;
        this.restClient = restClient;
    }

    @Override
    public void run() {

        JiraIssueType issueType = JiraIssueType.TASK;

        List<MultipartBody.Part> customFieldsPart = new ArrayList<>();
        if (feedbackDataProvider != null) {
            final String appendDesc = feedbackDataProvider.getAdditionalDescription();
            if (!TextUtils.isEmpty(appendDesc)) {
                requestBuilder.appendToDescription(appendDesc);
            }

            final JiraIssueType typeFromProvider = feedbackDataProvider.getIssueType();
            if (typeFromProvider != null) {
                issueType = typeFromProvider;
            }

            final Map<String, Object> customFieldsData = feedbackDataProvider.getCustomFieldsData();
            if(customFieldsData != null) {
                RequestBody customFieldRequestBody =
                        RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(customFieldsData));

                MultipartBody.Part customFieldPart =
                        MultipartBody.Part.createFormData("customfields", "customfields.json", customFieldRequestBody);
                customFieldsPart.add(customFieldPart);
            }

        }
        requestBuilder.issueType(issueType.toString());

        final CreateIssueRequest request = requestBuilder.build();

        Call<CreateIssueResponse> call = restClient.getJmcApi().createIssue(queryMap, request, Collections.emptyList(), customFieldsPart);
        try {
            Response<CreateIssueResponse> response = call.execute();
            Log.d(LOG_TAG, String.format("Response code %1$d\nmessage %2$s\nbody %3$s",
                    response.code(), response.message(), response.body()));

            if (response.isSuccessful()) {
                CreateIssueResponse body = response.body();
                if (body == null) {
                    Log.e(LOG_TAG, "Bad api response. Empty body.");
                } else if (TextUtils.isEmpty(body.getKey())) {
                    Log.e(LOG_TAG, "Bad api response. Missing Issue Key.");
                } else {
                    Log.d(LOG_TAG, String.format("New Issue Created %s", body.getKey()));
                    updateReceiver(Result.SUCCESS);
                    return;
                }
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG,"Failed to create new issue.", ioe);
        }

        updateReceiver(Result.FAIL);
    }

}
