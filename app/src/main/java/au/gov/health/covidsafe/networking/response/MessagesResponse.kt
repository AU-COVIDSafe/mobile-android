package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep

@Keep
data class MessagesResponse(val messages: List<Message>?, val message: String?, val forceappupgrade: Boolean?, val errorBodyMessage:String?)

@Keep
data class Message(val title: String, val body: String, val destination: String)

@Keep
data class ErrorMessage(val message: String?)
