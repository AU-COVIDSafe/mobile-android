package com.atlassian.mobilekit.module.feedback;

public enum JiraIssueType {

    BUG("Bug"),
    EPIC("Epic"),
    IMPROVEMENT("Improvement"),
    STORY("Story"),
    SUPPORT("Support"),
    TASK("Task");

    private String type;

    JiraIssueType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
