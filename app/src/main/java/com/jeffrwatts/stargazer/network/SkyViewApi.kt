package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network

import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.ResponseBody

interface SkyViewApi {
    @GET("runquery.pl")
    suspend fun getSkyViewImage(
        @Query("Position") position: String,
        @Query("Size") size: Double,
        @Query("Pixels") pixels: Int,
        @Query("Return") returnType: String = "PNG",
        @Query("Scaling") scaling: String = "linear",
        @Query("coordinates") coordinates: String = "J2000",
        @Query("Survey") survey: String = "DSS"
    ): ResponseBody
}

