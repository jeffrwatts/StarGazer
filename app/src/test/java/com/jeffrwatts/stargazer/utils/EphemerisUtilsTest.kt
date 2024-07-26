package com.jeffrwatts.stargazer.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.EphemerisUtils
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.mapPlanet
import org.junit.Assert
import org.junit.Test
import java.io.File
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
        val threshold = 5.0/60.0 // 5 minute accuracy threshold.

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
                Assert.assertEquals(0.0, angularDifference(ra, data.ra), threshold)
                Assert.assertEquals(dec, data.dec, threshold)
            }
        }
    }
}