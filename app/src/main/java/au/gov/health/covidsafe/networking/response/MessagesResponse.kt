package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep

@Keep
data class MessagesResponse(val messages: List<Message>?)

@Keep
data class Message(val title: String, val body: String, val destination: String)