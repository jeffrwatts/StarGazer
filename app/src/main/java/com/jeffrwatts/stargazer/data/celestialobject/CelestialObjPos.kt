package com.jeffrwatts.stargazer.data.celestialobject

import com.jeffrwatts.stargazer.utils.Utils
import java.time.LocalDateTime

data class CelestialObjPos(
    val celestialObj: CelestialObj,
    val alt: Double,
    val azm: Double,
    val lha: Double,
    val polarAlignCandidate: Boolean
) {
    companion object {
        fun fromCelestialObj(obj: CelestialObj, datetime: LocalDateTime, lat: Double, lon: Double): CelestialObjPos {
            val julianDate = Utils.calculateJulianDate(datetime)
            val (alt, azm, lha) = Utils.calculatePosition(obj.ra, obj.dec, lat, lon, julianDate)
            var polarAlignCandidate = false

            if (obj.type == ObjectType.STAR) {
                polarAlignCandidate = Utils.isGoodForPolarAlignment(alt, obj.dec, lha)
            }

            return CelestialObjPos(obj, alt, azm, lha, polarAlignCandidate)
        }
    }
}