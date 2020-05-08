package com.atlassian.mobilekit.module.feedback.model;


import android.text.TextUtils;

import com.atlassian.mobilekit.module.core.utils.StringUtils;

import java.util.List;

import androidx.annotation.Keep;

@Keep
public final class CreateIssueRequest {

    @Keep
    public static class Builder {

        private String type;
        private String summary;
        private String description;
        private boolean isCrash;
        private String udid;
        private String uuid;

        private String appName;
        private String appId;
        private String appVersion;

        private String systemVersion;
        private String systemName;
        private String deviceName;
        private String model;

        private String language;
        private List<String> components;

        public Builder() {

        }

        public Builder issueType(String type) {
            this.type = type;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder appendToDescription(String moreInfo) {
            if (!TextUtils.isEmpty(description)) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtils.EOL).append(StringUtils.EOL).append(moreInfo);
                description += sb.toString();
            }
            return this;
        }

        public Builder isCrash(boolean crash) {
            isCrash = crash;
            return this;
        }

        public Builder udid(String udid) {
            this.udid = udid;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder appVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        public Builder systemVersion(String systemVersion) {
            this.systemVersion = systemVersion;
            return this;
        }

        public Builder systemName(String systemName) {
            this.systemName = systemName;
            return this;
        }

        public Builder deviceName(String devName) {
            this.deviceName = devName;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder components(List<String> components) {
            this.components = components;
            return this;
        }

        public CreateIssueRequest build() {
            return new CreateIssueRequest(this);
        }
    }


    private static final int MAX_SUMMARY_LENGTH = 240;
    private final String type;
    private final String summary;
    private final String description;
    private final boolean isCrash;
    private final String udid;
    private final String uuid;

    private final String appName;
    private final String appId;
    private final String appVersion;

    private final String systemVersion;
    private final String systemName;

    // This is actually DeviceName.
    // *** But this declaration cannot be changed since the Server API expects it to be 'devName'
    private final String devName;

    private final String model;

    private final String language;

    private final List<String> components;

    public CreateIssueRequest(Builder builder) {
        this.type = builder.type;
        this.summary = StringUtils.ellipsize(builder.summary, MAX_SUMMARY_LENGTH);
        this.description = builder.description;
        this.isCrash = builder.isCrash;
        this.udid = builder.udid;
        this.uuid = builder.uuid;
        this.appName = builder.appName;
        this.appId = builder.appId;
        this.appVersion = builder.appVersion;
        this.systemVersion = builder.systemVersion;
        this.systemName = builder.systemName;
        this.devName = builder.deviceName;
        this.model = builder.model;
        this.language = builder.language;
        this.components = builder.components;
    }

    public String getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCrash() {
        return isCrash;
    }

    public String getUdid() {
        return udid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getDeviceName() {
        return devName;
    }

    public String getModel() {
        return model;
    }

    public String getLanguage() {
        return language;
    }

    public List<String> getComponents() {
        return components;
    }
}
