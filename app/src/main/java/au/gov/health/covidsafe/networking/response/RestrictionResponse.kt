package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep
import au.gov.health.covidsafe.status.persistence.StatusRecord
import com.google.gson.annotations.SerializedName

@Keep
data class RestrictionResponse(@SerializedName("state") val state: String?,
                               @SerializedName("activities") val activities: ArrayList<Activities>?)

@Keep
data class Activities(
        @SerializedName("activity") val activity: String?,
        @SerializedName("activity-title") val activitiyTitle: String?,
        @SerializedName("content-date-title") val contentDateTitle: String?,
        @SerializedName("subheadings") val subheadings: ArrayList<Subheadings>,
        @SerializedName("content") val content: String?)
@Keep
data class Subheadings(
        @SerializedName("title") val title: String?,
        @SerializedName("content") val content: String?)