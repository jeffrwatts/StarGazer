package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network

import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.celestialobjectimagejson.CelestialObjImageJson
import retrofit2.http.GET

interface ImageApi {
    @GET("get_images")
    suspend fun getImages() : List<CelestialObjImageJson>
}
