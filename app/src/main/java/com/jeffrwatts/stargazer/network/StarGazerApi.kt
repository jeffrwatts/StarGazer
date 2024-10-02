package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network

import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjJson
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjJson
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageJson
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjJson
import retrofit2.http.GET

interface StarGazerApi {

    @GET("get_dso")
    suspend fun getDso() : List<CelestialObjJson>

    @GET("get_stars")
    suspend fun getStars() : List<StarObjJson>

    @GET("get_variable_stars")
    suspend fun getVariableStars() : List<VariableStarObjJson>

    @GET("get_images")
    suspend fun getImages() : List<CelestialObjImageJson>
}