package au.gov.health.covidsafe.networking.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CaseStatisticResponse(@SerializedName("updated_date") val updatedDate: String?,
                                 @SerializedName("stats_published_status") val statsPublishedStatus: String?,
                                 val national: CaseDetailsData?,
                                 val act: CaseDetailsData?,
                                 val nsw: CaseDetailsData?,
                                 val nt: CaseDetailsData?,
                                 val qld: CaseDetailsData?,
                                 val sa: CaseDetailsData?,
                                 val tas: CaseDetailsData?,
                                 val vic: CaseDetailsData?,
                                 val wa: CaseDetailsData?)

@Keep
data class CaseDetailsData(
        @SerializedName("total_cases") var totalCases: Int?,
        @SerializedName("active_cases") var activeCases: Int?,
        @SerializedName("new_cases") var newCases: Int?,
        @SerializedName("recovered_cases") var recoveredCases: Int?,
        var deaths: Int?)