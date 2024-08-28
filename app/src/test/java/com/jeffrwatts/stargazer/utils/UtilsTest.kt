package com.jeffrwatts.stargazer.utils

import android.location.Location
import android.location.LocationManager
import org.junit.Assert
import org.junit.Assert.*

import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class UtilsTest {
    companion object {
        const val N = 1
        const val S = -1
        const val E = 1
        const val W = -1

        // Use four positions are earth (NW, NE, SW, and SE) and two stars (N dec, and S dec) to fully test functions
        val KONA_LATITUDE = Utils.dmsToDegrees(19, 38, 24.0) * N
        val KONA_LONGITUDE = Utils.dmsToDegrees(155, 59, 48.8) * W

        val TOKYO_LATITUDE = Utils.dmsToDegrees(35, 41, 22.2) * N
        val TOKYO_LONGITUDE = Utils.dmsToDegrees(139, 41, 30.12) * E

        val CAPE_TOWN_LATITUDE = Utils.dmsToDegrees(33, 55, 29.64) * S
        val CAPE_TOWN_LONGITUDE = Utils.dmsToDegrees(18, 25, 26.76) * W

        val SYDNEY_LATITUDE = Utils.dmsToDegrees(33, 52, 7.68) * S
        val SYDNEY_LONGITUDE = Utils.dmsToDegrees(151, 12, 33.48) * E

        val SIRIUS_RA = Utils.hmsToDegrees(6, 45, 8.9172) // RA of Sirius: 6h 45m 8.9172s
        val SIRIUS_DEC = Utils.dmsToDegrees(16, 42, 58.0) * S // Declination of Sirius: -16° 42' 58"

        val VEGA_RA = Utils.hmsToDegrees(18, 36, 56.336) // RA of Vega: 18h 36m 56.336s
        val VEGA_DEC = Utils.dmsToDegrees(38, 47, 1.28) * N // Declination of Vega: 38° 47' 1.28"

        val POLARIS_RA = Utils.hmsToDegrees(2, 41, 39.0)
        val POLARIS_DEC = Utils.dmsToDegrees(89, 15, 51.0)

    }

    @Test
    fun dmsToDegrees() {
        // Test positive degrees (north) - Kona Lat
        var degrees = Utils.dmsToDegrees(19, 38, 24.0)
        assertEquals(19.64, degrees, 0.01)

        // Test negative degrees (south) Sydney Lat
        degrees = Utils.dmsToDegrees(-33, 52, 7.68)
        assertEquals(-33.8688, degrees, 0.01)
    }

    @Test
    fun hmsToDegrees() {
        // Use Sirius
        val degrees = Utils.hmsToDegrees(6, 45, 8.9172)
        assertEquals(101.287155, degrees, 0.01)
    }

    @Test
    fun decimalToHMS() {
    }

    @Test
    fun decimalToDMS() {
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
        val dateTime = LocalDateTime.of(2023, 6, 24, 12, 0)
        val julianDate = Utils.calculateJulianDateUtc(dateTime)

        // Kona (West)
        var lst = Utils.calculateLocalSiderealTime(KONA_LONGITUDE, julianDate)
        assertEquals(296.4077276355, lst, 0.1)

        // Sydney (East)
        lst = Utils.calculateLocalSiderealTime(SYDNEY_LONGITUDE, julianDate)
        assertEquals(243.5961056915, lst, 0.1)
    }

    @Test
    fun calculateLocalHourAngle() {
        var ha = Utils.calculateLocalHourAngle(90.0, SIRIUS_RA)
        assertEquals(348.712845, ha, 0.1)

        ha = Utils.calculateLocalHourAngle(110.0, SIRIUS_RA)
        assertEquals(8.712845, ha, 0.1)
    }
    @Test
    fun calculateEquationOfTime() {
        // REVIEW - Need more tests and check on the large delta needed for this to pass.
        val date = 2460526.5
        val eot = Utils.calculateEquationOfTime(date)
        Assert.assertEquals(eot, -6.120717181609098, 0.6)
    }

    @Test
    fun calculateTwilightHourAngle() {
        // REVIEW - Need more test cases
        val latitude= 19.639994
        val dec = 17.25457627315962
        val hourAngleSunrise = Utils.calculateTwilightHourAngle(latitude, dec, SUNRISE_SUNSET_ANGLE)
        Assert.assertEquals(hourAngleSunrise, 1.6981463545913114, 0.01)

        val hourAngleAstronomical = Utils.calculateTwilightHourAngle(latitude, dec, ASTRONOMICAL_TWILIGHT_ANGLE)
        Assert.assertEquals(hourAngleAstronomical, 2.0425055334420623, 0.01)
    }

    @Test
    fun calculateRiseSet() {
        // REVIEW - need more test cases (different locations, more angles, more times)
        // REVIEW - validation approach exposes internals of the function under test so not that elegant.
        val year = 2024
        val month = 8
        val day = 4
        val date = Utils.calculateJulianDateUtc(LocalDateTime.of(year, month, day, 0, 0, 0))
        val latitude = 19.639994
        val longitude = -155.996926
        val minInDay = 60.0*24.0
        val delta = 1.0/minInDay // one minute accuracy

        val location = Mockito.mock(Location::class.java)

        // Define the behavior of the mock
        Mockito.`when`(location.latitude).thenReturn(latitude)
        Mockito.`when`(location.longitude).thenReturn(longitude)
        Mockito.`when`(location.altitude).thenReturn(0.0)
        Mockito.`when`(location.provider).thenReturn(LocationManager.GPS_PROVIDER)

        val timeUtcRise = Utils.calculateRiseSetUtc(year, month, day, location, true, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedRise = date + 882.0006343882211/minInDay
        Assert.assertEquals(timeUtcRise, expectedRise, delta)

        val timeUtcSet = Utils.calculateRiseSetUtc(2024, 8, 4, location, false, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedSet = date + 1818.216207974997/minInDay
        Assert.assertEquals(timeUtcSet, expectedSet, delta)
    }
}