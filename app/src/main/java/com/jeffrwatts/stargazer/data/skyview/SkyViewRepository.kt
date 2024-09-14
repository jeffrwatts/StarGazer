package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview

import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.SkyViewApi
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject

class SkyViewRepository @Inject constructor(private val skyViewApi: SkyViewApi) {
    fun fetchSkyViewImage(celestialObjWithImage: CelestialObjWithImage, size: Double = 2.0, pixels: Int = 500): Flow<ResponseBody> {
        return flow {
            // Note: can not use on planets yet... need a solution where planet image is loaded from resources.
            val position = "${celestialObjWithImage.celestialObj.ra*15.0},${celestialObjWithImage.celestialObj.dec}"
            val responseBody = skyViewApi.getSkyViewImage(position, size, pixels)
            emit(responseBody)
        }
    }
}
