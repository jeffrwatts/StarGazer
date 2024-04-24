package com.jeffrwatts.stargazer.data.celestialobject

import com.jeffrwatts.stargazer.utils.Utils

data class CelestialObjPos(
    val celestialObjWithImage: CelestialObjWithImage,
    val alt: Double,
    val azm: Double,
    val timeUntilMeridian: Double,
    val observable: Boolean
) {
    companion object {
        private const val MIN_ALTITUDE = 15

        fun fromCelestialObjWithImage(obj: CelestialObjWithImage, julianDate: Double, lat: Double, lon: Double): CelestialObjPos {
            val (alt, azm, timeUntilMeridian) = Utils.calculateAltAzm(obj.celestialObj.ra, obj.celestialObj.dec, lat, lon, julianDate)

            return CelestialObjPos(obj, alt, azm, timeUntilMeridian, observable = (alt> MIN_ALTITUDE))
        }
    }
}