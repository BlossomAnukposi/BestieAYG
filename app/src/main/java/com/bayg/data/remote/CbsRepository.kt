package com.bayg.data.remote

import com.bayg.data.remote.model.CbsDataPoint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class CbsRepository {

    private val httpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://opendata.cbs.nl/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CbsApiService::class.java)

    suspend fun getSocialNetworkUsage(): Result<CbsDataPoint> {
        return try {
            val response = api.getSocialNetworkStats()
            val dataPoint = response.value.firstOrNull()
                ?: return Result.failure(Exception("No CBS data available"))
            Result.success(dataPoint)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
