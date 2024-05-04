package com.jeffrwatts.stargazer.utils

import android.location.Location
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

object Utils {
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

    private fun julianDateToUTC(julianDate: Double): LocalDateTime {
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

    fun calculateJulianDateNow():Double {
        val currentTime = LocalDateTime.now()
        val zonedDateTime = currentTime.atZone(ZoneId.systemDefault())
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

    fun calculateAltAzm(ra: Double, dec: Double, latitude: Double, longitude: Double, julianDate: Double) : Triple<Double, Double, Double> {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(dec)
        val lst = calculateLocalSiderealTime(longitude, julianDate)
        val lha = calculateLocalHourAngle(lst, ra)
        val haRad = Math.toRadians(lha)

        val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
        val altRad = asin(sinAlt)
        val alt = Math.toDegrees(altRad)

        val numerator = sin(decRad) - sin(altRad) * sin(latRad)
        val denominator = cos(altRad) * cos(latRad)
        val azmRad = acos(numerator/denominator)
        var azm = Math.toDegrees(azmRad)

        if (sin(haRad) >=0) {
            azm = 360.0 - azm
        }

        // Time until meridian crossing
        var hourAngle = ra - lst
        // Normalize the hour angle to be within 0 to 360 degrees
        hourAngle = (hourAngle + 360) % 360

        // If the hour angle is greater than 180 degrees, it means the object will cross the meridian in the past, adjust it
        if (hourAngle > 180) {
            hourAngle -= 360.0
        }

        // Convert hour angle to time, considering the Earth rotates at 15 degrees per hour
        val timeUntilMeridian = if (hourAngle < 0) (hourAngle + 360) / 15 else hourAngle / 15


        return Triple(alt, azm, timeUntilMeridian)
    }

    fun calculateRAandDEC(alt: Double, azm: Double, latitude: Double, longitude: Double, julianDate: Double): Pair<Double, Double> {
        val altRad = Math.toRadians(alt)
        val azmRad = Math.toRadians(azm)
        val latRad = Math.toRadians(latitude)

        // Calculate the declination
        val sinDec = sin(altRad) * sin(latRad) + cos(altRad) * cos(latRad) * cos(azmRad)
        val decRad = asin(sinDec)
        val dec = Math.toDegrees(decRad)

        // Calculate the hour angle
        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        // Threshold to handle precision issues near zenith or horizon
        val adjustedCosH = when {
            cosH > 1.0 -> 1.0
            cosH < -1.0 -> -1.0
            else -> cosH
        }

        var lhaRad = Math.acos(adjustedCosH)
        lhaRad = if (sin(azmRad) > 0) 2 * Math.PI - lhaRad else lhaRad
        val lha = Math.toDegrees(lhaRad)

        // Calculate the Local Sidereal Time
        val lst = calculateLocalSiderealTime(longitude, julianDate)

        // Convert LST to RA
        var ra = lst - lha
        if (ra < 0) ra += 360

        return Pair(ra, dec)
    }

    data class AltitudeEntry(val time: LocalDateTime, val alt: Double)

    fun getAltitudeEntries(
        ra: Double,
        dec: Double,
        location: Location,
        timeStart: LocalDateTime,
        durationHours: Long,
        incrementMinutes: Long): List<AltitudeEntry> {
        val altitudeData = mutableListOf<AltitudeEntry>()

        // Set up the start time to be 2 hours before at 0 min and 0 sec.
        var timeIx = timeStart
        val endTime = timeIx.plusHours(durationHours)

        while (timeIx.isBefore(endTime)) {
            val julianDate = calculateJulianDateFromLocal(timeIx)
            val (alt, _, _) = calculateAltAzm(
                ra,
                dec,
                location.latitude,
                location.longitude,
                julianDate
            )
            altitudeData.add(AltitudeEntry(timeIx, alt))
            timeIx = timeIx.plusMinutes(incrementMinutes)
        }

        return altitudeData
    }
}