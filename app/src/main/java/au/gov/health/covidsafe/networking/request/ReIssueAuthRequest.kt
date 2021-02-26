package au.gov.health.covidsafe.networking.request

import androidx.annotation.Keep

@Keep
data class ReIssueAuthRequest(val subject: String?, val refresh: String?)