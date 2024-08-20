package com.jeffrwatts.stargazer.data.celestialobject

import android.location.Location
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.julianDateToAstronomyTime
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon

data class CelestialObjPos(
    val celestialObjWithImage: CelestialObjWithImage,
    val alt: Double,
    val azm: Double,
    val timeUntilMeridian: Double,
    val observable: Boolean
) {
    companion object {
        private const val MIN_ALTITUDE = 15

        fun fromCelestialObj(obj: CelestialObj, julianDate: Double, location: Location, image: CelestialObjImage?=null): CelestialObjPos {
            val time = julianDateToAstronomyTime(julianDate)
            val observer = Observer(location.latitude, location.longitude, location.altitude)

            if (obj.type == ObjectType.PLANET) {
                mapBody(obj.objectId)?.let { body->
                    val radec: Equatorial = equator(body, time, observer, EquatorEpoch.J2000, Aberration.Corrected)
                    obj.ra= radec.ra
                    obj.dec=radec.dec
                }
            }

            val altazm: Topocentric = horizon(time, observer, obj.ra, obj.dec, Refraction.Normal)
            val lst = Utils.calculateLocalSiderealTime(location.longitude,julianDate)
            val timeUntilMeridian = Utils.calculateTimeToMeridian(obj.ra, lst)
            return CelestialObjPos(CelestialObjWithImage(obj, image), altazm.altitude, altazm.azimuth, timeUntilMeridian, (altazm.altitude> MIN_ALTITUDE))
        }

        fun fromCelestialObjWithImage(obj: CelestialObjWithImage, julianDate: Double, location: Location): CelestialObjPos {
            return fromCelestialObj(obj.celestialObj, julianDate, location, obj.image)
        }
    }
}