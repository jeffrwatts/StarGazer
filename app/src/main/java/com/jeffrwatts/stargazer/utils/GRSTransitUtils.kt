package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils

import com.jeffrwatts.stargazer.utils.Utils
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

object GRSTransitUtils {
    const val GRS_LONGITUDE_REF_JAN_2025 = 68.0 // Degrees
    const val GRS_DATE_REF_JAN_2025 = 2460676.5 // Jan 1, 2025 0:0:0 UTC
    const val GRS_DRIFT = 1.75 // Degrees per month.

    enum class JovianSystem (val baseCentralMeridian: Double, val rotationRate: Double) {
        SYSTEM_I (156.84, 877.8169147),
        SYSTEM_II (181.62, 870.1869147),
        SYSTEM_III (138.41, 870.4535567)
    }

    fun calculateCentralMeridian(
        observationTime:
        Double, jovianSystem: JovianSystem
    ): Double {
        val jupiterMean = (observationTime - 2455636.938) * 360.0 / 4332.89709

        val equationOfCenter = 5.55 * sin(Math.toRadians(jupiterMean))

        val angle = (observationTime - 2451870.628) * 360.0 / 398.884 - equationOfCenter

        val correction = (11 * sin(Math.toRadians(angle)) +
                5 * cos(Math.toRadians(angle)) -
                1.25 * cos(Math.toRadians(jupiterMean)) - equationOfCenter)

        val centralMeridian = jovianSystem.baseCentralMeridian + jovianSystem.rotationRate * observationTime + correction

        return centralMeridian % 360
    }

    fun estimateGRSLongitude(
        observationTime: Double,
        referenceDate: Double = GRS_DATE_REF_JAN_2025,
        referenceLongitude: Double = GRS_LONGITUDE_REF_JAN_2025,
        driftPerMonth: Double = GRS_DRIFT
    ): Double {
        val elapsedDays = observationTime - referenceDate

        val elapsedMonths = elapsedDays / 30.4375  // Average days per month

        val currentLongitude = referenceLongitude + (driftPerMonth * elapsedMonths)

        return currentLongitude % 360
    }

    fun predictGRSTransits(
        startTime: Double
    ) : Pair<Double, Double> {
        // Great Red Spot is within System II band.
        val currentCentralMeridian = calculateCentralMeridian(startTime, JovianSystem.SYSTEM_II)
        val currentGRSLongitude = estimateGRSLongitude(startTime)

        // Degrees past the last transit
        val degreesPastLastTransit = (currentCentralMeridian - currentGRSLongitude) % 360

        // Calculate last transit time
        val lastTransit = startTime - (degreesPastLastTransit / JovianSystem.SYSTEM_II.rotationRate)

        // Degrees to the next transit
        val degreesToNextTransit = (currentGRSLongitude + 360 - currentCentralMeridian) % 360

        // Calculate the next transit time
        val nextTransit = startTime + (degreesToNextTransit / JovianSystem.SYSTEM_II.rotationRate)

        return Pair(lastTransit, nextTransit)
    }
}