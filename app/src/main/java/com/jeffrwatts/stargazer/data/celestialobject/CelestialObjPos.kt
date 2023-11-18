package com.jeffrwatts.stargazer.data.celestialobject

import com.jeffrwatts.stargazer.utils.Utils
import java.time.LocalDateTime

data class CelestialObjPos(
    val celestialObj: CelestialObj,
    val alt: Double,
    val azm: Double,
    val lha: Double,
    val observable: Boolean,
    val polarAlignCandidate: Boolean
) {
    companion object {
        val MIN_ALTITUDE = 15

        fun fromCelestialObj(obj: CelestialObj, julianDate: Double, lat: Double, lon: Double): CelestialObjPos {
            val (alt, azm, lha) = Utils.calculatePosition(obj.ra, obj.dec, lat, lon, julianDate)
            var polarAlignCandidate = false

            if (obj.type == ObjectType.STAR) {
                polarAlignCandidate = Utils.isGoodForPolarAlignment(alt, obj.dec, lha)
            }

            return CelestialObjPos(obj, alt, azm, lha, observable = (alt> MIN_ALTITUDE), polarAlignCandidate)
        }
    }
}