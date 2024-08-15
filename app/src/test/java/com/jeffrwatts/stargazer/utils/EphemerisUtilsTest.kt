package com.jeffrwatts.stargazer.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.ASTRONOMICAL_TWILIGHT_ANGLE
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.EphemerisUtils
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.SUNRISE_SUNSET_ANGLE
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.mapPlanet
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.min

data class EphemerisTestData(
    val julianDate: Double,
    val body: String,
    val ra: Double,
    val dec: Double,
    val distance: Double
)
class EphemerisUtilsTest {

    private fun angularDifference(ra1: Double, ra2: Double): Double {
        val diff = abs(ra1 - ra2)
        return min(diff, 360 - diff)
    }

    @Test
    fun calculatePlanetPosition() {
        val angle_threshold = 5.0/60.0 // 5 minute accuracy threshold.
        val dist_threshold = 0.05 // 0.05 AU accuracy threshold

        val jsonFile = File(javaClass.classLoader?.getResource("ephemeris_test.json")!!.file).readText()
        val gson = Gson()
        val ephemerisTestType = object : TypeToken<List<EphemerisTestData>>() {}.type
        val testData: List<EphemerisTestData> = gson.fromJson(jsonFile, ephemerisTestType)

        // Iterate through the test data and verify the calculations
        for (data in testData) {
            val planet = mapPlanet(data.body)
            planet?.let {
                print(data.julianDate)
                val (ra, dec, dist) = EphemerisUtils.calculatePlanetPosition(data.julianDate, it)
                Assert.assertEquals(0.0, angularDifference(ra, data.ra), angle_threshold)
                Assert.assertEquals(dec, data.dec, angle_threshold)
                Assert.assertEquals(dist, data.distance, dist_threshold)
            }
        }
    }

    @Test
    fun calculateMoonPosition() {
        // REVIEW Need more tests.
        val date1 = 2460526.5
        val (ra1, dec1, _) = EphemerisUtils.calculateMoonPosition(date1)
        Assert.assertEquals(ra1, 131.38353369982516, 0.1)
        Assert.assertEquals(dec1, 22.924318942896388, 0.1)

        val date2 = 2460536.5
        val (ra2, dec2, _) = EphemerisUtils.calculateMoonPosition(date2)
        Assert.assertEquals(ra2, 245.42028577312414, 0.1)
        Assert.assertEquals(dec2, -26.229606553934158, 0.1)
    }

    @Test
    fun calculateEquationOfTime() {
        // REVIEW - Need more tests and check on the large delta needed for this to pass.
        val date = 2460526.5
        val eot = EphemerisUtils.calculateEquationOfTime(date)
        Assert.assertEquals(eot, -6.120717181609098, 0.6)
    }

    @Test
    fun calculateTwilightHourAngle() {
        // REVIEW - Need more test cases
        val latitude= 19.639994
        val dec = 17.25457627315962
        val hourAngleSunrise = EphemerisUtils.calculateTwilightHourAngle(latitude, dec, SUNRISE_SUNSET_ANGLE)
        Assert.assertEquals(hourAngleSunrise, 1.6981463545913114, 0.01)

        val hourAngleAstronomical = EphemerisUtils.calculateTwilightHourAngle(latitude, dec, ASTRONOMICAL_TWILIGHT_ANGLE)
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

        val timeUtcRise = EphemerisUtils.calculateRiseSetUtc(year, month, day, latitude, longitude, true, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedRise = date + 882.0006343882211/minInDay
        Assert.assertEquals(timeUtcRise, expectedRise, delta)

        val timeUtcSet = EphemerisUtils.calculateRiseSetUtc(2024, 8, 4, latitude, longitude, false, ASTRONOMICAL_TWILIGHT_ANGLE)
        val expectedSet = date + 1818.216207974997/minInDay
        Assert.assertEquals(timeUtcSet, expectedSet, delta)
    }
}