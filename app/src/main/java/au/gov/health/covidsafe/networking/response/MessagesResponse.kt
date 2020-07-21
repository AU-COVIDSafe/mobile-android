package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep

@Keep
data class MessagesResponse(val message: String, val forceappupgrade: Boolean)