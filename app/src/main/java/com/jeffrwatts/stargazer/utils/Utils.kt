package com.jeffrwatts.stargazer.utils


import android.location.Location
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchAltitude
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

fun Double.mod2pi() = (this % (2 * PI)).let { if (it < 0) it + 2 * PI else it }
fun Double.mod360() = (this % 360).let { if (it < 0) it + 360 else it }

const val SUNRISE_SUNSET_ANGLE = 0.833
const val CIVIL_TWILIGHT_ANGLE = 6.0
const val NAUTICAL_TWILIGHT_ANGLE = 12.0
const val ASTRONOMICAL_TWILIGHT_ANGLE = 18.0

fun julianDateToAstronomyTime(julianDate: Double): Time {
    val referenceJulianDate = 2451545.0
    val ut = julianDate - referenceJulianDate
    return Time(ut)
}

fun astronomyTimeToJulianDate(time: Time): Double {
    val referenceJulianDate = 2451545.0
    return time.ut + referenceJulianDate
}

object Utils {
    private const val JULIAN_MINUTE = 1.0/24.0/60.0

    fun julianDateToUTC(julianDate: Double): LocalDateTime {
        // Julian date to seconds since epoch
        val secondsSinceEpoch = ((julianDate - 2440587.5) * 86400).toLong()
        // Convert to LocalDateTime in system default timezone
        return LocalDateTime.ofEpochSecond(secondsSinceEpoch, 0, ZoneOffset.UTC)
    }

    fun julianDateToLocalTime(julianDate: Double): LocalDateTime {
        val utcDateTime = julianDateToUTC(julianDate)
        return utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun calculateJulianDateFromLocal(localTime:LocalDateTime):Double {
        val zonedDateTime = localTime.atZone(ZoneId.systemDefault())
        val utcZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
        val utcNow = utcZonedDateTime.toLocalDateTime()
        return calculateJulianDateUtc(utcNow)
    }

    fun calculateJulianDateUtc(date: LocalDateTime): Double {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val hour = date.hour
        val minute = date.minute
        val second = date.second

        // In the Julian calendar, January and February are the 13th and 14th month of the previous year
        val adjustedYear = if (month > 2) year else year - 1
        val adjustedMonth = if (month > 2) month else month + 12

        val a = (adjustedYear / 100)
        val b = 2 - a + (a/4)
        val d = day + (hour + minute / 60.0 + second / 3600.0) / 24.0

        return (365.25*(adjustedYear+4716)).toInt() + (30.6001*(adjustedMonth+1)).toInt() + d + b - 1524.5
    }

    fun calculateLocalSiderealTime(longitude: Double, julianDate: Double): Double {
        val t = (julianDate - 2451545.0) / 36525.0

        // Calculate Greenwich Mean Sidereal Time (GMST) in degrees
        var gst = 280.46061837 + 360.98564736629 * (julianDate - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        gst = (gst + 360) % 360
        val lstDegrees = (gst + longitude + 360) % 360

        // Convert LST from degrees to hours
        val lstHours = lstDegrees / 15.0
        return lstHours
    }

    fun calculateLocalHourAngle(lst: Double, ra: Double): Double {
        // Both lst and ra should be in hours
        var ha = lst - ra
        if (ha < 0) ha += 24.0  // Adjust within the 0-24 hour range
        return ha
    }

    fun calculateTimeToMeridian(ra: Double, lst: Double): Double {
        // Both ra and lst are in hours
        var hourAngle = ra - lst

        // Normalize the hour angle to be within 0 to 24 hours
        hourAngle = (hourAngle + 24) % 24

        // The time until meridian crossing is the hour angle itself now
        return hourAngle
    }

    fun calculateEquationOfTime(date: Double): Double {
        val cy = date / 36525  // Centuries since J2000.0
        val epsilon = Math.toRadians(23.4392911 - 0.0130042 * cy)  // Obliquity of the ecliptic

        val L0 = Math.toRadians(280.46646 + 36000.76983 * cy).mod2pi()  // Mean longitude of the Sun
        val M = Math.toRadians(357.52911 + 35999.05029 * cy).mod2pi()  // Mean anomaly of the Sun
        val e = 0.016708634 - 0.000042037 * cy  // Eccentricity of Earth's orbit

        val y = tan(epsilon / 2).pow(2)

        val EoTRad = (y * sin(2 * L0) - 2 * e * sin(M) + 4 * e * y * sin(M) * cos(2 * L0) -
                0.5 * y.pow(2) * sin(4 * L0) - 1.25 * e.pow(2) * sin(2 * M))

        return 4 * Math.toDegrees(EoTRad)  // Convert to minutes
    }

    fun calculateTwilightHourAngle(latitude: Double, dec: Double, angle: Double): Double {
        val latitudeRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(dec)
        val angleRad = Math.toRadians(90 + angle)

        // Calculate the hour angle argument
        var HAarg = (cos(angleRad) / (cos(latitudeRad) * cos(decRad)) - tan(latitudeRad) * tan(decRad))
        // Clamp HAarg to the range [-1, 1] to avoid domain errors in acos
        HAarg = HAarg.coerceIn(-1.0, 1.0)

        // Calculate the hour angle
        return acos(HAarg)
    }

    fun getNight(julianDate: Double, location: Location): Triple<Double, Double, Boolean> {
        val astronomicalTwilightAngle = -18.0
        val time = julianDateToAstronomyTime(julianDate)

        val observer = Observer(location.latitude, location.longitude, location.altitude)
        val nextRise = searchAltitude(Body.Sun, observer, Direction.Rise, time, 1.0, astronomicalTwilightAngle)
        val nextSet = searchAltitude(Body.Sun, observer, Direction.Set, time, 1.0, astronomicalTwilightAngle)

        var nightStart = 0.0
        var nightEnd = 0.0
        var isNight = false

        if (nextRise != null && nextSet != null) {
            val (timeToRise, timeToSet) = Pair(nextRise.tt - time.tt, nextSet.tt - time.tt)
            isNight = timeToRise < timeToSet
            nightEnd = astronomyTimeToJulianDate(nextRise)

            nightStart = if (isNight) {
                searchAltitude(Body.Sun, observer, Direction.Set, time, -1.0, -18.0)?.let { astronomyTimeToJulianDate(it) } ?: 0.0
            } else {
                astronomyTimeToJulianDate(nextSet)
            }
        }

        return Triple(nightStart, nightEnd, isNight)
    }

    fun calculatePlanetAltitudes(body: Body, location: Location, startTime: Double, stopTime:Double): List<Pair<Double, Double>> {
        val altitudeData = mutableListOf<Pair<Double, Double>>()
        val incrementMinutes = 10 * JULIAN_MINUTE // 10 minutes
        val observer = Observer(location.latitude, location.longitude, location.altitude)

        var timeIx = startTime

        while (timeIx < stopTime) {
            val time = julianDateToAstronomyTime(timeIx)
            val radec: Equatorial = equator(body, time, observer, EquatorEpoch.J2000, Aberration.Corrected)
            val altazm: Topocentric = horizon(time, observer, radec.ra, radec.dec, Refraction.Normal)
            altitudeData.add(Pair(timeIx, altazm.altitude))
            timeIx += incrementMinutes
        }
        return altitudeData
    }

    fun calculateDSOAltitudes(ra:Double, dec:Double, location: Location, startTime: Double, stopTime:Double): List<Pair<Double, Double>> {
        val altitudeData = mutableListOf<Pair<Double, Double>>()
        val incrementMinutes = 10 * JULIAN_MINUTE // 10 minutes
        val observer = Observer(location.latitude, location.longitude, location.altitude)

        var timeIx = startTime

        while (timeIx < stopTime) {
            val time = julianDateToAstronomyTime(timeIx)
            val altazm: Topocentric = horizon(time, observer, ra, dec, Refraction.Normal)
            altitudeData.add(Pair(timeIx, altazm.altitude))
            timeIx += incrementMinutes
        }
        return altitudeData
    }

    fun findClosestIndex(currentJulianTime: Double, altitudeData: List<Pair<Double, Double>>): Int {
        if (altitudeData.isEmpty() || currentJulianTime < altitudeData.first().first || currentJulianTime>altitudeData.last().first) {
            return -1 // Return -1 if the list is empty or current time is before the first entry
        }

        return altitudeData.indices.minByOrNull { index ->
            kotlin.math.abs(altitudeData[index].first - currentJulianTime)
        } ?: -1 // Find the index with the closest Julian time
    }

    fun getXAxisLabels(startTime: Double, stopTime: Double): List<String> {
        val numLabels = 5
        val totalDuration = stopTime - startTime
        val step = totalDuration / (numLabels - 1) // Divide into equal parts

        return List(numLabels) { index ->
            val julianDate = startTime + index * step
            val localDateTime = julianDateToLocalTime(julianDate)

            localDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        }
    }

}