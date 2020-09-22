package au.gov.health.covidsafe.notifications

import android.content.Context
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.ui.utils.Utils

class NotificationBuilder {

    companion object {

        private var isPushNotificationErrorCheck = false

        fun handlePushNotification(context: Context, action: String?) {
            action?.let {
                when (it) {
                    context.getString(R.string.notification_click_action_upgrade_app) -> {
                        Utils.gotoPlayStore(context)
                    }
                    context.getString(R.string.notification_click_action_no_check_in),
                    context.getString(R.string.notification_click_action_check_possible_encountered_error),
                    context.getString(R.string.notification_click_action_check_possible_issue) -> {
                        isPushNotificationErrorCheck = true
                    }
                }
            }
        }

        fun isShowPossibleIssueNotification() = isPushNotificationErrorCheck

        fun clearPossibleIssueNotificationCheck() {
            isPushNotificationErrorCheck = false
        }
    }

}