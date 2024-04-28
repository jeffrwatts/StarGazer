package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network

import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjJson
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageJson
import com.jeffrwatts.stargazer.data.solarsystem.EphemerisEntryJson
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjJson
import retrofit2.http.GET
import retrofit2.http.Query

interface StarGazerApi {

    @GET("get_dso")
    suspend fun getDso() : List<CelestialObjJson>

    @GET("get_variable_stars")
    suspend fun getVariableStars() : List<VariableStarObjJson>

    @GET("get_ephemeris")
    suspend fun getEphemeris(@Query("start") start:Double,
                             @Query("length") length: Double) : List<EphemerisEntryJson>

    @GET("get_images")
    suspend fun getImages() : List<CelestialObjImageJson>
}