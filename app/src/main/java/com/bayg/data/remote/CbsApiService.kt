package com.bayg.data.remote

import com.bayg.data.remote.model.CbsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CbsApiService {

    @GET("ODataApi/odata/83429NED/TypedDataSet")
    suspend fun getSocialNetworkStats(
        @Query("\$filter") filter: String =
            "KenmerkenPersonen eq 'T009002' and Marges eq 'MW00000'",
        @Query("\$orderby") orderBy: String = "Perioden desc",
        @Query("\$top") top: Int = 1
    ): CbsResponse
}
