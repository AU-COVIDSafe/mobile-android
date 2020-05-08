package com.atlassian.mobilekit.module.feedback.network;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class JmcRestClient {

    private JmcApi jmcApi = null;

    public JmcRestClient() {

    }

    public void init(String protocol, String host) {

        final String baseUrl = new StringBuilder()
                .append(protocol)
                .append(host)
                .append("/")
                .toString();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jmcApi = retrofit.create(JmcApi.class);
    }

    public JmcApi getJmcApi() {
        return jmcApi;
    }
}
