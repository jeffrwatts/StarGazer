package com.jeffrwatts.stargazer.utils

import android.location.Location
import android.location.LocationManager
import org.junit.Assert.*

import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class UtilsTest {

    companion object {
        const val LATITUDE_KONA = 19.6399
        const val LONGITUDE_KONA = -155.9962

        const val LONGITUDE_SYDNEY = 151.2093

        const val POLARIS_RA = 2.694167
    }

    @Test
    fun calculateJulianDate() {
        val dateTime = LocalDateTime.of(2000, 1, 1, 12, 0, 0)
        val jd = Utils.calculateJulianDateUtc(dateTime)

        // Compare with a known Julian Date for this date and time.
        // The absolute difference should be less than 0.1 because of rounding errors
        assertEquals(2451545.0, jd, 0.1)
    }

    @Test
    fun calculateLocalSiderealTime() {
        val dateTime = LocalDateTime.of(2024, 9, 2, 12, 0, 0)
        val jd = Utils.calculateJulianDateUtc(dateTime)

        val lstKona = Utils.calculateLocalSiderealTime(LONGITUDE_KONA, jd)
        assertEquals(lstKona, 0.40885730801344, 0.1)

        val lstSydney = Utils.calculateLocalSiderealTime(LONGITUDE_SYDNEY, jd)
        assertEquals(lstSydney, 20.889223974680103, 0.1)
    }

    @Test
    fun calculateLocalHourAngle() {
        val dateTime = LocalDateTime.of(2024, 9, 2, 12, 0, 0)
        val jd = Utils.calculateJulianDateUtc(dateTime)

        val lst = Utils.calculateLocalSiderealTime(LONGITUDE_KONA, jd)
        assertEquals(lst, 0.40885730801344, 0.1)

        val lha = Utils.calculateLocalHourAngle(lst, POLARIS_RA)
        assertEquals(lha, 21.714690308013438, 0.1)
    }

    @Test
    fun calculateEquationOfTime() {
        // REVIEW - Need more tests and check on the large delta needed for this to pass.
        val date = 2460526.5
        val eot = Utils.calculateEquationOfTime(date)
        assertEquals(eot, -6.120717181609098, 0.6)
    }

    @Test
    fun calculateTwilightHourAngle() {
        // REVIEW - Need more test cases
        val dec = 17.25457627315962
        val hourAngleSunrise = Utils.calculateTwilightHourAngle(LATITUDE_KONA, dec, SUNRISE_SUNSET_ANGLE)
        assertEquals(hourAngleSunrise, 1.6981463545913114, 0.01)

        val hourAngleAstronomical = Utils.calculateTwilightHourAngle(LATITUDE_KONA, dec, ASTRONOMICAL_TWILIGHT_ANGLE)
        assertEquals(hourAngleAstronomical, 2.0425055334420623, 0.01)
    }

    @Test
    fun getNight() {
        val location = Mockito.mock(Location::class.java)

        Mockito.`when`(location.latitude).thenReturn(LATITUDE_KONA)
        Mockito.`when`(location.longitude).thenReturn(LONGITUDE_KONA)
        Mockito.`when`(location.altitude).thenReturn(0.0)
        Mockito.`when`(location.provider).thenReturn(LocationManager.GPS_PROVIDER)

        val time1 = LocalDateTime.of(2024, 9, 8, 16, 0, 0)
        val julianDate1 = Utils.calculateJulianDateFromLocal(time1)
        val (start1, end1, isNight1) = Utils.getNight(julianDate1, location)

        assertEquals(start1, 2460562.74, 0.1)
        assertEquals(end1, 2460563.12, 0.1)
        assertEquals(isNight1, false)

        val time2 = LocalDateTime.of(2024, 9, 8, 22, 0, 0)
        val julianDate2 = Utils.calculateJulianDateFromLocal(time2)
        val (start2, end2, isNight2) = Utils.getNight(julianDate2, location)

        assertEquals(start2, 2460562.74, 0.1)
        assertEquals(end2, 2460563.12, 0.1)
        assertEquals(isNight2, true)

    }
}