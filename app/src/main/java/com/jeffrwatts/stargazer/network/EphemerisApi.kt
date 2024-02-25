package com.jeffrwatts.stargazer.network

import com.jeffrwatts.stargazer.data.solarsystem.PlanetPosJson
import retrofit2.http.GET
import retrofit2.http.Query

interface EphemerisApi {
    @GET("get_ephemeris")
    suspend fun getEphemeris(@Query("start") start:Double,
                            @Query("length") length: Double) : List<PlanetPosJson>
}