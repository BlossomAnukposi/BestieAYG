package com.bayg.data.remote.model

import com.google.gson.annotations.SerializedName

data class CbsResponse(
    @SerializedName("value")
    val value: List<CbsDataPoint>
)

data class CbsDataPoint(
    @SerializedName("Perioden")
    val year: String,
    @SerializedName("SociaalNetwerk_31")
    val socialNetworkPercent: Double?
)
