package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils

import kotlin.math.*

const val EPS = 1e-6

fun Double.mod2pi() = (this % (2 * PI)).let { if (it < 0) it + 2 * PI else it }
fun Double.mod360() = (this % 360).let { if (it < 0) it + 360 else it }

sealed class Planet(
    private val a: Double, private val da: Double,
    private val e: Double, private val de: Double,
    private val i: Double, private val di: Double,
    private val O: Double, private val dO: Double,
    private val w: Double, private val dw: Double,
    private val L: Double, private val dL: Double
) {

    data object Mercury : Planet(
        0.38709893, 0.00000066,
        0.20563069, 0.00002527,
        7.00487, -23.51,
        48.33167, -446.30,
        77.45645, 573.57,
        252.25084, 538101628.29
    )

    data object Venus : Planet(
        0.72333199, 0.00000092,
        0.00677323, -0.00004938,
        3.39471, -2.86,
        76.68069, -996.89,
        131.53298, -108.80,
        181.97973, 210664136.06
    )

    data object Earth : Planet(
        1.00000011, -0.00000005,
        0.01671022, -0.00003804,
        0.00005, -46.94,
        -11.26064, -18228.25,
        102.94719, 1198.28,
        100.46435, 129597740.63
    )

    data object Mars : Planet(
        1.52366231, -0.00007221,
        0.09341233, 0.00011902,
        1.85061, -25.47,
        49.57854, -1020.19,
        336.04084, 1560.78,
        355.45332, 68905103.78
    )

    data object Jupiter : Planet(
        5.20336301, 0.00060737,
        0.04839266, -0.00012880,
        1.30530, -4.15,
        100.55615, 1217.17,
        14.75385, 839.93,
        34.40438, 10925078.35
    )

    data object Saturn : Planet(
        9.53707032, -0.00301530,
        0.05415060, -0.00036762,
        2.48446, 6.11,
        113.71504, -1591.05,
        92.43194, -1948.89,
        49.94432, 4401052.95
    )

    data object Uranus : Planet(
        19.19126393, 0.00152025,
        0.04716771, -0.00019150,
        0.76986, -2.09,
        74.22988, -1681.40,
        170.96424, 1312.56,
        313.23218, 1542547.79
    )

    data object Neptune : Planet(
        30.06896348, -0.00125196,
        0.00858587, 0.00002510,
        1.76917, -3.64,
        131.72169, -151.25,
        44.97135, -844.43,
        304.88003, 786449.21
    )

    data object Pluto : Planet(
        39.48168677, -0.00076912,
        0.24880766, 0.00006465,
        17.14175, 11.07,
        110.30347, -37.33,
        224.06676, -132.25,
        238.92881, 522747.90
    )

    data object Sun : Planet(
        1.00000011, -0.00000005,
        0.01671022, -0.00003804,
        0.00005, -46.94,
        -11.26064, -18228.25,
        102.94719, 1198.28,
        100.46435, 129597740.63
    )

    fun meanElements(d: Double): OrbitalElements {
        val cy = (d - 2451545.0) / 36525.0
        return OrbitalElements(
            a = a + cy * da,
            e = e + cy * de,
            i = Math.toRadians(i + cy * di / 3600),
            O = Math.toRadians(O + cy * dO / 3600),
            w = Math.toRadians(w + cy * dw / 3600),
            L = Math.toRadians(L + cy * dL / 3600).mod2pi()
        )
    }
}

data class OrbitalElements(
    var a: Double,
    var e: Double,
    var i: Double,
    var O: Double,
    var w: Double,
    var L: Double
)

fun mapPlanet(name: String): Planet? {
    return when (name) {
        "Mercury" -> Planet.Mercury
        "Venus" -> Planet.Venus
        "Sun" -> Planet.Sun
        "Mars" -> Planet.Mars
        "Jupiter" -> Planet.Jupiter
        "Saturn" -> Planet.Saturn
        "Uranus" -> Planet.Uranus
        "Neptune" -> Planet.Neptune
        "Pluto" -> Planet.Pluto
        else -> null
    }
}

object EphemerisUtils {

    fun calculatePlanetPosition(date: Double, planet: Planet): Triple<Double, Double, Double> {
        val earthElements = Planet.Earth.meanElements(date)
        val planetElements = planet.meanElements(date)

        val mEarth = meanAnomaly(earthElements)
        val vEarth = trueAnomaly(mEarth, earthElements.e)
        val rEarth = heliocentricRadius(earthElements.a, earthElements.e, vEarth)

        val (xEarth, yEarth, zEarth) = heliocentricCoordinatesEarth(rEarth, vEarth, earthElements.w)

        val mPlanet = meanAnomaly(planetElements)
        val vPlanet = trueAnomaly(mPlanet, planetElements.e)
        val rPlanet = heliocentricRadius(planetElements.a, planetElements.e, vPlanet)

        val (xPlanet, yPlanet, zPlanet) = heliocentricCoordinates(
            rPlanet,
            planetElements.O,
            vPlanet,
            planetElements.w,
            planetElements.i
        )

        val (xg, yg, zg) = if (planet is Planet.Sun) {
            Triple(0.0 - xEarth, 0.0 - yEarth, 0.0 - zEarth)
        } else {
            Triple(xPlanet - xEarth, yPlanet - yEarth, zPlanet - zEarth)
        }

        val ecl = Math.toRadians(23.439281)
        val xeq = xg
        val yeq = yg * cos(ecl) - zg * sin(ecl)
        val zeq = yg * sin(ecl) + zg * cos(ecl)

        val ra = Math.toDegrees(atan2(yeq, xeq)).mod360()
        val dec = Math.toDegrees(atan(zeq / sqrt(xeq * xeq + yeq * yeq)))
        val rvec = sqrt(xeq * xeq + yeq * yeq + zeq * zeq)

        return Triple(ra, dec, rvec)
    }

    private fun meanAnomaly(elements: OrbitalElements): Double {
        return (elements.L - elements.w).mod2pi()
    }

    private fun trueAnomaly(M: Double, e: Double): Double {
        var E = M + e * sin(M) * (1.0 + e * cos(M))

        while (true) {
            val E1 = E
            E = E1 - (E1 - e * sin(E1) - M) / (1 - e * cos(E1))
            if (abs(E - E1) <= EPS) break
        }

        var V = 2 * atan(sqrt((1 + e) / (1 - e)) * tan(0.5 * E))

        if (V < 0) V += 2 * PI

        return V
    }

    private fun heliocentricRadius(a: Double, e: Double, v: Double): Double {
        return (a * (1 - e * e)) / (1 + e * cos(v))
    }

    private fun heliocentricCoordinates(r: Double, O: Double, v: Double, w: Double, i: Double): Triple<Double, Double, Double> {
        val x = r * (cos(O) * cos(v + w - O) - sin(O) * sin(v + w - O) * cos(i))
        val y = r * (sin(O) * cos(v + w - O) + cos(O) * sin(v + w - O) * cos(i))
        val z = r * (sin(v + w - O) * sin(i))
        return Triple(x, y, z)
    }

    private fun heliocentricCoordinatesEarth(r: Double, v: Double, w: Double): Triple<Double, Double, Double> {
        val x = r * cos(v + w)
        val y = r * sin(v + w)
        val z = 0.0
        return Triple(x, y, z)
    }
}
