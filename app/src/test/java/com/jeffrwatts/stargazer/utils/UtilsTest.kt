package com.jeffrwatts.stargazer.utils

import org.junit.Assert.*

import org.junit.Test
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
    fun calculatePosition() {
        val dateTime = LocalDateTime.of(2000, 1, 1, 12, 0, 0)
        val julianDate = Utils.calculateJulianDateUtc(dateTime)

        val (alt1, azm1, timeUntilMeridian1) = Utils.calculateAltAzm(SIRIUS_RA, SIRIUS_DEC, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(47.10283005268597, alt1, 0.01)
        assertEquals(213.61455455893736, azm1, 0.01)
        assertEquals(22.45489503459259, timeUntilMeridian1, 0.01)

        val (ra1, dec1) = Utils.calculateRAandDEC(alt1, azm1, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(SIRIUS_RA, ra1, 0.01)
        assertEquals(SIRIUS_DEC, dec1, 0.01)

        val (alt2, azm2, timeUntilMeridian2) = Utils.calculateAltAzm(SIRIUS_RA, SIRIUS_DEC, TOKYO_LATITUDE, TOKYO_LONGITUDE, julianDate)
        assertEquals(24.70721174963697, alt2, 0.01)
        assertEquals(136.08356306211235, azm2, 0.01)
        assertEquals(2.742322442, timeUntilMeridian2, 0.01)

        val (ra2, dec2) = Utils.calculateRAandDEC(alt2, azm2, TOKYO_LATITUDE, TOKYO_LONGITUDE, julianDate)
        assertEquals(SIRIUS_RA, ra2, 0.01)
        assertEquals(SIRIUS_DEC, dec2, 0.01)

        val (alt3, azm3, timeUntilMeridian3) = Utils.calculateAltAzm(SIRIUS_RA, SIRIUS_DEC, CAPE_TOWN_LATITUDE, CAPE_TOWN_LONGITUDE, julianDate)
        assertEquals(-36.13503221187687, alt3, 0.01)
        assertEquals(203.02330984433365, azm3, 0.01)
        assertEquals(13.283375775333328, timeUntilMeridian3, 0.01)

        val (ra3, dec3) = Utils.calculateRAandDEC(alt3, azm3, CAPE_TOWN_LATITUDE, CAPE_TOWN_LONGITUDE, julianDate)
        assertEquals(SIRIUS_RA, ra3, 0.01)
        assertEquals(SIRIUS_DEC, dec3, 0.01)

        val (alt4, azm4, timeUntilMeridian4) = Utils.calculateAltAzm(SIRIUS_RA, SIRIUS_DEC, SYDNEY_LATITUDE, SYDNEY_LONGITUDE, julianDate)
        assertEquals(58.38462821975049, alt4, 0.01)
        assertEquals(64.56698046413477, azm4, 0.01)
        assertEquals(1.9744824419999987, timeUntilMeridian4, 0.01)

        val (ra4, dec4) = Utils.calculateRAandDEC(alt4, azm4, SYDNEY_LATITUDE, SYDNEY_LONGITUDE, julianDate)
        assertEquals(SIRIUS_RA, ra4, 0.01)
        assertEquals(SIRIUS_DEC, dec4, 0.01)

        val (alt5, azm5, timeUntilMeridian5) = Utils.calculateAltAzm(VEGA_RA, VEGA_DEC, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(-26.972065357589596, alt5, 0.01)
        assertEquals(21.89390027930739, azm5, 0.01)
        assertEquals(10.318066923481487, timeUntilMeridian5, 0.01)

        val (ra5, dec5) = Utils.calculateRAandDEC(alt5, azm5, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(VEGA_RA, ra5, 0.01)
        assertEquals(VEGA_DEC, dec5, 0.01)

        val (alt6, azm6, timeUntilMeridian6) =  Utils.calculateAltAzm(VEGA_RA, VEGA_DEC, TOKYO_LATITUDE, TOKYO_LONGITUDE, julianDate)
        assertEquals(-7.241411123082607, alt6, 0.01)
        assertEquals(330.30939585559577, azm6, 0.01)
        assertEquals(14.60549433088889, timeUntilMeridian6, 0.01)

        val (ra6, dec6) = Utils.calculateRAandDEC(alt6, azm6, TOKYO_LATITUDE, TOKYO_LONGITUDE, julianDate)
        assertEquals(VEGA_RA, ra6, 0.01)
        assertEquals(VEGA_DEC, dec6, 0.01)

        val (alt7, azm7, timeUntilMeridian7) =  Utils.calculateAltAzm(VEGA_RA, VEGA_DEC, CAPE_TOWN_LATITUDE, CAPE_TOWN_LONGITUDE, julianDate)
        assertEquals(15.56400321720133, alt7, 0.01)
        assertEquals(13.837953743577943, azm7, 0.01)
        assertEquals(1.1465476642222219, timeUntilMeridian7, 0.01)

        val (ra7, dec7) = Utils.calculateRAandDEC(alt7, azm7, CAPE_TOWN_LATITUDE, CAPE_TOWN_LONGITUDE, julianDate)
        assertEquals(VEGA_RA, ra7, 0.01)
        assertEquals(VEGA_DEC, dec7, 0.01)

        val (alt8, azm8, timeUntilMeridian8) =  Utils.calculateAltAzm(VEGA_RA, VEGA_DEC, SYDNEY_LATITUDE, SYDNEY_LONGITUDE, julianDate)
        assertEquals(-67.3514373136623, alt8, 0.01)
        assertEquals(290.5200542205871, azm8, 0.01)
        assertEquals(13.837654330888897, timeUntilMeridian8, 0.01)

        val (ra8, dec8) = Utils.calculateRAandDEC(alt8, azm8, SYDNEY_LATITUDE, SYDNEY_LONGITUDE, julianDate)
        assertEquals(VEGA_RA, ra8, 0.01)
        assertEquals(VEGA_DEC, dec8, 0.01)
    }

    @Test
    fun PolarisTests() {
        val dateTime = LocalDateTime.of(2023, 12, 16, 12, 0, 0)
        val julianDate = Utils.calculateJulianDateUtc(dateTime)

        val (alt, azm, timeUntilMeridian) =  Utils.calculateAltAzm(POLARIS_RA, POLARIS_DEC, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(19.908625619869397, alt, 0.01)
        assertEquals(359.27202352564825, azm, 0.01)
        assertEquals(19.43561954788745, timeUntilMeridian, 0.01)

        val (ra, dec) = Utils.calculateRAandDEC(alt, azm, KONA_LATITUDE, KONA_LONGITUDE, julianDate)
        assertEquals(POLARIS_RA, ra, 0.01)
        assertEquals(POLARIS_DEC, dec, 0.01)
    }
}