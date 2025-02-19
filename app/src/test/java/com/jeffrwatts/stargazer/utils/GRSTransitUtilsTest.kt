package com.jeffrwatts.stargazer.utils

import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.GRSTransitUtils
import org.junit.Assert.*

import org.junit.Test
import java.time.LocalDateTime

class GRSTransitUtilsTest {

    @Test
    fun calculateCentralMeridian() {
        val observationTime = LocalDateTime.of(2025, 1, 12, 0, 0, 0)
        val observationTimeJulian = Utils.calculateJulianDateUtc(observationTime)

        val centralMeridianSysII = GRSTransitUtils.calculateCentralMeridian(observationTimeJulian, GRSTransitUtils.JovianSystem.SYSTEM_II)
        assertEquals(centralMeridianSysII, 210.0, 0.01)
    }

    @Test
    fun estimateGRSLongitude () {
        val observationTime = LocalDateTime.of(2025, 1, 12, 0, 0, 0)
        val observationTimeJulian = Utils.calculateJulianDateUtc(observationTime)

        val grsLongitude = GRSTransitUtils.estimateGRSLongitude(observationTimeJulian)
        assertEquals(grsLongitude, 68.63, 0.01)
    }

    @Test
    fun predictGRSTransits () {
        val observationTime = LocalDateTime.of(2025, 1, 12, 0, 0, 0)
        val observationTimeJulian = Utils.calculateJulianDateUtc(observationTime)

        val (lastTime, nextTime) = GRSTransitUtils.predictGRSTransits(observationTimeJulian)

        assertEquals(lastTime, 2460687.337, 0.01)
        assertEquals(nextTime, 2460687.751, 0.01)
    }
}