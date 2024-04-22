package com.jeffrwatts.stargazer.network

import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageJson
import retrofit2.http.GET

interface ImageApi {
    @GET("get_images")
    suspend fun getImages() : List<CelestialObjImageJson>
}
