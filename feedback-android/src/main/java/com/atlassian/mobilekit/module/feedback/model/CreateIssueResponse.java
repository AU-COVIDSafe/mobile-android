package com.atlassian.mobilekit.module.feedback.model;


import java.util.List;

import androidx.annotation.Keep;

@Keep
public final class CreateIssueResponse {

    private String key;
    private String status;
    private String summary;
    private String description;
    private long dateUpdated;
    private long dateCreated;
    private boolean hasUpdates;
    private List<String> comments;


    public String getKey() {
        return key;
    }

    public String getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public long getDateUpdated() {
        return dateUpdated;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public boolean hasUpdates() {
        return hasUpdates;
    }

    public List<String> getComments() {
        return comments;
    }
}
