package com.atlassian.mobilekit.module.feedback;

import java.util.Map;
import androidx.annotation.Nullable;

/**
 * This is used to provide more information which will be used when creating the feedback JIRA issue.
 */
public interface FeedbackDataProvider {

    /**
     * This string will be appended to the description of Feedback JIRA issue.
     * It may contain standard wiki markup that is accepted by the JIRA Instance in description field.
     * Encoding supported: UTF-8
     * @return
     */
    String getAdditionalDescription();

    /**
     * See {@link JiraIssueType} for available options.
     * If this returns null, then the library will default to {@link JiraIssueType#TASK}
     * An admin must pre-configure the JIRA Project to accept this type.
     * If not, resultant issue will be of default type as per the project.
     * @return
     */
    JiraIssueType getIssueType();

    /**
     * This data will be passed to the feedback client to create custom fields in the Jira issue
     * @return map of Jira field name and respective values
     */
    @Nullable
    Map<String, Object> getCustomFieldsData();
}
