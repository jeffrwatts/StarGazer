package com.jeffrwatts.stargazer.utils


import android.location.Location
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

const val EPS = 1e-6

fun Double.mod2pi() = (this % (2 * PI)).let { if (it < 0) it + 2 * PI else it }
fun Double.mod360() = (this % 360).let { if (it < 0) it + 360 else it }

const val SUNRISE_SUNSET_ANGLE = 0.833
const val CIVIL_TWILIGHT_ANGLE = 6.0
const val NAUTICAL_TWILIGHT_ANGLE = 12.0
const val ASTRONOMICAL_TWILIGHT_ANGLE = 18.0

fun julianDateToAstronomyTime(julianDate: Double): Time {
    // Step 1: Define the Julian Date for the reference epoch (January 1, 2000, at 12:00 UTC)
    val referenceJulianDate = 2451545.0

    // Step 2: Calculate UT1/UTC days since the reference epoch
    val ut = julianDate - referenceJulianDate

    // Step 3: Create and return the Time object using the calculated `ut` value
    return Time(ut)
}

object Utils {
    data class AltitudeEntry(val time: LocalDateTime, val alt: Double)

    fun dmsToDegrees(degrees: Int, minutes: Int, seconds: Double): Double {
        val sign = if (degrees >= 0) 1 else -1
        return sign * (abs(degrees) + (minutes / 60.0) + (seconds / 3600.0))
    }

    fun hmsToDegrees(hours: Int, minutes: Int, seconds: Double): Double {
        val totalHours = hours + (minutes / 60.0) + (seconds / 3600.0)
        return totalHours * 15.0
    }

    fun decimalToDMS(decimal: Double, dirPos: String, dirNeg: String): String {
        val degrees = decimal.toInt()
        val minutes = ((decimal - degrees) * 60).toInt()
        val seconds = (((decimal - degrees) * 60 - minutes) * 60)
        val direction = if (decimal >= 0) dirPos else dirNeg
        return "%02d°%02d'%05.2f\"%s".format(abs(degrees), abs(minutes), abs(seconds), direction)
    }

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

        // Calculate Greenwich Mean Sidereal Time (GMST)
        var gst = 280.46061837 + 360.98564736629 * (julianDate - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        gst = (gst + 360) % 360
        val lst = (gst + longitude + 360) % 360
        return lst
    }

    fun calculateLocalHourAngle(lst: Double, ra: Double): Double {
        var ha = lst - ra
        if (ha < 0) ha += 360.0
        return ha
    }

    fun calculateTimeToMeridian(ra: Double, lst: Double): Double {
        var hourAngle = ra - lst

        // Normalize the hour angle to be within 0 to 360 degrees
        hourAngle = (hourAngle + 360) % 360

        // If the hour angle is greater than 180 degrees, it means the object will cross the meridian in the past, adjust it
        if (hourAngle > 180) {
            hourAngle -= 360.0
        }

        // Convert hour angle to time, considering the Earth rotates at 15 degrees per hour
        val timeUntilMeridian = if (hourAngle < 0) (hourAngle + 360) / 15 else hourAngle / 15

        return timeUntilMeridian
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

    fun calculateRiseSetUtc(year: Int, month: Int, day: Int, location:Location, rise: Boolean, angle: Double):Double {
        val julianDate = calculateJulianDateUtc(LocalDateTime.of(year, month, day, 0, 0, 0))
        val date = julianDateToAstronomyTime(julianDate)
        val observer = Observer(location.latitude, location.longitude, location.altitude)
        val radec: Equatorial = equator(Body.Sun, date, observer, EquatorEpoch.J2000, Aberration.Corrected)
        val eot = calculateEquationOfTime(julianDate)
        var hourAngle = calculateTwilightHourAngle(location.latitude, radec.dec, angle)
        if (!rise) hourAngle*=-1.0
        val delta = location.longitude + Math.toDegrees(hourAngle)
        val timeMinUtc = 720.0 - (4.0 * delta) - eot
        return julianDate + timeMinUtc / 1440.0
    }
}