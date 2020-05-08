package com.atlassian.mobilekit.module.feedback.model;


public final class FeedbackConfig {

    private final String host;
    private final String apiKey;
    private final String projectKey;
    private final String[] components;

    public FeedbackConfig(String host, String apiKey, String projectKey, String[] components) {
        this.host = host;
        this.apiKey = apiKey;
        this.projectKey = projectKey;
        this.components = components;
    }

    public String getHost() {
        return host;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String[] getComponents() {
        return components;
    }
}
