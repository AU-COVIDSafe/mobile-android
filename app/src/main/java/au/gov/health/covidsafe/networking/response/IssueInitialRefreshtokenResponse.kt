package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep

@Keep
data class IssueInitialRefreshtokenResponse(val token: String, val refreshToken: String)