package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.solarsystem
import com.jeffrwatts.stargazer.utils.Utils

data class PlanetObjPos(
    val planetObj: PlanetObj,
    val alt: Double,
    val azm: Double,
    val timeUntilMeridian: Double,
    val observable: Boolean
) {
    companion object {
        private const val MIN_ALTITUDE = 15

        fun fromPlanetObj(obj: PlanetObj, julianDate: Double, lat: Double, lon: Double): PlanetObjPos {
            val (alt, azm, timeUntilMeridian) = Utils.calculateAltAzm(obj.ra, obj.dec, lat, lon, julianDate)

            return PlanetObjPos(obj, alt, azm, timeUntilMeridian, observable = (alt> MIN_ALTITUDE))
        }
    }
}