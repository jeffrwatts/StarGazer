package com.jeffrwatts.stargazer.utils

import java.time.LocalDateTime
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

    fun decimalToHMS(decimal: Double): String {
        val hours = (decimal / 15).toInt()
        val minutes = ((decimal / 15 - hours) * 60).toInt()
        val seconds = (((decimal / 15 - hours) * 60 - minutes) * 60)
        return "%02dh %02dm %.2fs".format(hours, minutes, seconds)
    }

    fun decimalToDMS(decimal: Double): String {
        val degrees = decimal.toInt()
        val minutes = ((decimal - degrees) * 60).toInt()
        val seconds = (((decimal - degrees) * 60 - minutes) * 60)
        val direction = if (decimal >= 0) "N" else "S"
        return "%02d°%02d'%05.2f\"%s".format(abs(degrees), abs(minutes), abs(seconds), direction)
    }

    fun calculateJulianDate(date: LocalDateTime): Double {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val hour = date.hour
        val minute = date.minute
        val second = date.second

        // In the Julian calendar, January and February are the 13th and 14th month of the previous year
        val adjustedYear = if (month > 2) year else year - 1
        val adjustedMonth = if (month > 2) month else month + 12

        val a = (adjustedYear / 100).toInt()
        val b = 2 - a + (a/4).toInt()
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

    fun calculateHourAngle(lst: Double, ra: Double): Double {
        var ha = lst - ra
        if (ha < 0) ha += 360.0
        return ha
    }

    fun calculateAltAzm(ra: Double, dec: Double, latitude: Double, longitude: Double, julianDate: Double) : Pair<Double, Double> {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(dec)
        val lst = calculateLocalSiderealTime(longitude, julianDate)
        val haRad = Math.toRadians(calculateHourAngle(lst, ra))

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

        return Pair(alt, azm)
    }

    fun isGoodForPolarAlignment(alt: Double, azm: Double, dec: Double): Boolean {
        return azm in 160.0..200.0 && dec in -20.0..20.0 && alt < 80.0
    }
}