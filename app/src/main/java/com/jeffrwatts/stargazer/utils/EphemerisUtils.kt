package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils

import com.jeffrwatts.stargazer.data.celestialobject.EARTH
import com.jeffrwatts.stargazer.data.celestialobject.JUPITER
import com.jeffrwatts.stargazer.data.celestialobject.MARS
import com.jeffrwatts.stargazer.data.celestialobject.MERCURY
import com.jeffrwatts.stargazer.data.celestialobject.NEPTUNE
import com.jeffrwatts.stargazer.data.celestialobject.PLUTO
import com.jeffrwatts.stargazer.data.celestialobject.SATURN
import com.jeffrwatts.stargazer.data.celestialobject.SUN
import com.jeffrwatts.stargazer.data.celestialobject.URANUS
import com.jeffrwatts.stargazer.data.celestialobject.VENUS
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.julianDateToAstronomyTime
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.equator
import java.time.LocalDateTime
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
        SUN -> Planet.Sun
        MERCURY -> Planet.Mercury
        VENUS -> Planet.Venus
        EARTH -> Planet.Earth
        MARS -> Planet.Mars
        JUPITER -> Planet.Jupiter
        SATURN -> Planet.Saturn
        URANUS -> Planet.Uranus
        NEPTUNE -> Planet.Neptune
        PLUTO -> Planet.Pluto
        else -> null
    }
}

const val SUNRISE_SUNSET_ANGLE = 0.833
const val CIVIL_TWILIGHT_ANGLE = 6.0
const val NAUTICAL_TWILIGHT_ANGLE = 12.0
const val ASTRONOMICAL_TWILIGHT_ANGLE = 18.0

object EphemerisUtils {

    fun calculatePlanetPosition2(jd: Double, body: Body): Triple<Double, Double, Double> {
        //val zonedDateTime = julianDateToZonedDateTime(jd)
        //val localTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
        //val time = localTime.toAstronomyTime()

        val time = julianDateToAstronomyTime(jd)
        val observer = Observer(0.0, 0.0, 0.0)
        val equ_2000: Equatorial = equator(body, time, observer, EquatorEpoch.J2000, Aberration.None)
        return Triple(equ_2000.ra*15.0, equ_2000.dec, equ_2000.dist)
    }

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

    fun calculateMoonPosition(julianDate: Double): Triple<Double, Double, Double> {
        val d = julianDate - 2451545.0 // Julian date for January 1, 2000

        val L = Math.toRadians(218.316 + 13.176396 * d)
        val M = Math.toRadians(134.963 + 13.064993 * d)
        val F = Math.toRadians(93.272 + 13.229350 * d)

        val l = L + Math.toRadians(6.289) * sin(M)
        val b = Math.toRadians(5.128) * sin(F)
        val dist = 385001 - 20905 * cos(M)

        // Calculate right ascension
        val E = Math.toRadians(23.4397) // Obliquity of the Earth
        var ra = Math.toDegrees(atan2(sin(l) * cos(E) - tan(b) * sin(E), cos(l)))
        // Calculate declination
        val dec = Math.toDegrees(asin(sin(b) * cos(E) + cos(b) * sin(E) * sin(l)))

        // Normalize RA to be between 0 and 360 degrees
        if (ra < 0) {
            ra += 360.0
        }

        return Triple(ra, dec, dist)
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

    fun calculateEquationOfTime(date: Double): Double {
        val cy = date / 36525  // Centuries since J2000.0
        val epsilon = Math.toRadians(23.4392911 - 0.0130042 * cy)  // Obliquity of the ecliptic

        val L0 = Math.toRadians(280.46646 + 36000.76983 * cy).mod2pi()  // Mean longitude of the Sun
        val M = Math.toRadians(357.52911 + 35999.05029 * cy).mod2pi()  // Mean anomaly of the Sun
        val e = 0.016708634 - 0.000042037 * cy  // Eccentricity of Earth's orbit

        val y = tan(epsilon / 2).pow(2)

        val EoTRad = (y * sin(2 * L0) - 2 * e * sin(M) + 4 * e * y * sin(M) * cos(2 * L0) -
                0.5 * y.pow(2) * sin(4 * L0) - 1.25 * e.pow(2) * sin(2 * M))

        return 4 * Math.toDegrees(EoTRad)  // Convert to minutes
    }

    fun calculateTwilightHourAngle(latitude: Double, dec: Double, angle: Double): Double {
        val latitudeRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(dec)
        val angleRad = Math.toRadians(90 + angle)

        // Calculate the hour angle argument
        var HAarg = (cos(angleRad) / (cos(latitudeRad) * cos(decRad)) - tan(latitudeRad) * tan(decRad))
        // Clamp HAarg to the range [-1, 1] to avoid domain errors in acos
        HAarg = HAarg.coerceIn(-1.0, 1.0)

        // Calculate the hour angle
        return acos(HAarg)
    }

    fun calculateRiseSetUtc(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, rise: Boolean, angle: Double):Double {
        val date = Utils.calculateJulianDateUtc(LocalDateTime.of(year, month, day, 0, 0, 0))
        val (_, dec, _) = calculatePlanetPosition(date, Planet.Sun)
        val eot = calculateEquationOfTime(date)
        var hourAngle = calculateTwilightHourAngle(latitude, dec, angle)
        if (!rise) hourAngle*=-1.0
        val delta = longitude + Math.toDegrees(hourAngle)
        val timeMinUtc = 720.0 - (4.0 * delta) - eot
        return date + timeMinUtc / 1440.0
    }
}
