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
    fun calculateRiseSet() {
        // REVIEW - need more test cases (different locations, more angles, more times)
        // REVIEW - validation approach exposes internals of the function under test so not that elegant.
        val year = 2024
        val month = 8
        val day = 4
        val date = Utils.calculateJulianDateUtc(LocalDateTime.of(2024, 8, 4, 0, 0, 0))
        val minInDay = 60.0*24.0
        val delta = 1.0/minInDay // one minute accuracy

        val location = Mockito.mock(Location::class.java)

        // Define the behavior of the mock
        Mockito.`when`(location.latitude).thenReturn(LATITUDE_KONA)
        Mockito.`when`(location.longitude).thenReturn(LONGITUDE_KONA)
        Mockito.`when`(location.altitude).thenReturn(0.0)
        Mockito.`when`(location.provider).thenReturn(LocationManager.GPS_PROVIDER)

        val timeUtcRise = Utils.calculateRiseSetUtc(year, month, day, location, true, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedRise = date + 882.0006343882211/minInDay
        assertEquals(timeUtcRise, expectedRise, delta)

        val timeUtcSet = Utils.calculateRiseSetUtc(2024, 8, 4, location, false, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedSet = date + 1818.216207974997/minInDay
        assertEquals(timeUtcSet, expectedSet, delta)
    }
}