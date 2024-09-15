package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview

import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.SkyViewApi
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject

enum class ScalingOption(val value: String) {
    LOG("log"),
    SQRT("sqrt"),
    LINEAR("linear"),
    HISTEQ("histeq"),
    LOGLOG("loglog")
}

class SkyViewRequestParams private constructor(
    val size: Double,
    val pixels: Int,
    val rotation: Int,
    val scaling: ScalingOption
) {
    // Builder class
    class Builder {
        // Default values
        private var size: Double = 3.0
        private var pixels: Int = 300
        private var rotation: Int = 0
        private var scaling: ScalingOption = ScalingOption.LINEAR // Default scaling

        // Builder methods for optional parameters
        fun size(size: Double) = apply { this.size = size }
        fun pixels(pixels: Int) = apply { this.pixels = pixels }
        fun rotation(rotation: Int) = apply { this.rotation = rotation }
        fun scaling(scaling: ScalingOption) = apply { this.scaling = scaling }

        // Build method
        fun build() = SkyViewRequestParams(size, pixels, rotation, scaling)
    }
}


class SkyViewRepository @Inject constructor(private val skyViewApi: SkyViewApi) {

    fun fetchSkyViewImage(
        celestialObjWithImage: CelestialObjWithImage,
        requestParams: SkyViewRequestParams
    ): Flow<ResponseBody> {
        return flow {
            // Convert RA from hours to degrees
            val position = "${celestialObjWithImage.celestialObj.ra * 15.0},${celestialObjWithImage.celestialObj.dec}"

            // Use the requestParams object to extract the necessary parameters
            val responseBody = skyViewApi.getSkyViewImage(
                position = position,
                size = requestParams.size,
                pixels = requestParams.pixels,
                rotation = requestParams.rotation,
                scaling = requestParams.scaling.value // Pass the enum's string value
            )
            emit(responseBody)
        }
    }
}



