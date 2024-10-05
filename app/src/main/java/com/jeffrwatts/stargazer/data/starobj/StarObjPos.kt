package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj

import android.location.Location
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.julianDateToAstronomyTime
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.horizon

data class StarObjPos(
    val starObj: StarObj,
    val alt: Double,
    val azm: Double,
    val timeUntilMeridian: Double,
    val observable: Boolean
) {
    companion object {
        private const val MIN_ALTITUDE = 15

        fun fromStarObj(obj: StarObj, julianDate: Double, location: Location): StarObjPos {
            val time = julianDateToAstronomyTime(julianDate)
            val observer = Observer(location.latitude, location.longitude, location.altitude)

            val altazm: Topocentric = horizon(time, observer, obj.ra, obj.dec, Refraction.Normal)
            val lst = Utils.calculateLocalSiderealTime(location.longitude,julianDate)
            val timeUntilMeridian = Utils.calculateTimeToMeridian(obj.ra, lst)
            return StarObjPos(obj, altazm.altitude, altazm.azimuth, timeUntilMeridian, (altazm.altitude> MIN_ALTITUDE))
        }
    }
}