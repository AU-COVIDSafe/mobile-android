package com.atlassian.mobilekit.module.feedback.network;


import com.atlassian.mobilekit.module.feedback.model.CreateIssueRequest;
import com.atlassian.mobilekit.module.feedback.model.CreateIssueResponse;

import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;

public interface JmcApi {

    @Multipart
    @POST("rest/jconnect/latest/issue/create")
    @Keep
    Call<CreateIssueResponse> createIssue(
            @QueryMap Map<String, String> params,
            @Part("issue") CreateIssueRequest request,
            @Part List<MultipartBody.Part> screenshotPart,
            @Part List<MultipartBody.Part> customFields);
}