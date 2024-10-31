package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.jupiterdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.julianDateToAstronomyTime
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.C_AUDAY
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.jupiterMoons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

@HiltViewModel
class JupiterDetailViewModel @Inject constructor (

) : ViewModel() {
    private val _uiState = MutableStateFlow<JupiterDetailUIState>(JupiterDetailUIState.Loading)
    val uiState: StateFlow<JupiterDetailUIState> = _uiState.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)
    private var incrementJob: Job? = null

    init {
        viewModelScope.launch {
            _timeOffset.collect {timeOffset ->
                val date = if (timeOffset != 0L) {
                    LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                } else {
                    LocalDateTime.now()
                }
                val julianDate = Utils.calculateJulianDateFromLocal(date)
                val dateUtc = Utils.julianDateToUTC(julianDate)
                val time = julianDateToAstronomyTime(julianDate)

                // Call geoVector to calculate the geocentric position of Jupiter.
                // geoVector corrects for light travel time.
                // That means it returns a vector to where Jupiter appears to be
                // in the sky, when the light left Jupiter to travel toward the
                // Earth to arrive here at the specified time. This is different from
                // where Jupiter is at that time.
                val jv = geoVector(Body.Jupiter, time, Aberration.Corrected)
                val jupiterAngularRadius = calculateJupiterAngularRadius(jv.toEquatorial().dist)

                // Calculate the amount of time it took light to reach the Earth from Jupiter.
                // The distance to Jupiter (AU) divided by the speed of light (AU/day) = time in days.
                val lightTravelDays = jv.length() / C_AUDAY

                // The jupiterMoons function calculates positions of Jupiter's moons without
                // correcting for light travel time. Correct for light travel by backdating
                // by the given amount of light travel time.
                val backdate = time.addDays(-lightTravelDays)

                val jm = jupiterMoons(backdate)

                val jovianMoonEvents = calculateAllJovianMoonEvents( julianDate, 24.0, jupiterAngularRadius)

                jovianMoonEvents.forEach { event ->
                    // Log the event details
                    Log.d("MoonEventLogger", "Event: ${event.type}, Moon: ${event.moon}, Time (UTC): ${event.time}")
                }


                // Tricky: the `+` operator for adding `Vector` will throw an exception
                // if the vectors do not have matching times. We work around this
                // by using `withTime` to clone each moon's position vector to have
                // a different time. This is a manual override to work around a safety check.
                _uiState.value = JupiterDetailUIState.Success(
                    time = dateUtc,
                    jupiterAngularRadius = jupiterAngularRadius,
                    jupiterPos = jv.toEquatorial(),
                    ioPos = (jv + jm.io.position().withTime(jv.t)).toEquatorial(),
                    europaPos = (jv + jm.europa.position().withTime(jv.t)).toEquatorial(),
                    ganymedePos = (jv + jm.ganymede.position().withTime(jv.t)).toEquatorial(),
                    callistoPos = (jv + jm.callisto.position().withTime(jv.t)).toEquatorial(),
                    jovianMoonEvents
                )
            }
        }
    }

    private fun calculateJupiterAngularRadius(jupiterDist: Double): Double {
        val jupiterRadiusKm = 71492.0 // Jupiter's radius in kilometers
        val auToKm = 1.496e+8 // Conversion factor: 1 AU in kilometers
        return Math.toDegrees(Math.atan(jupiterRadiusKm / (jupiterDist * auToKm))) // Convert AU to km and calculate angular radius in degrees
    }

    private fun isMoonInEclipse(jupiterPos: Vector, moonPos: Vector, sunPos: Vector): Boolean {
        val jupiterToSun = jupiterPos.length() - sunPos.length()
        val jupiterToMoon = jupiterPos.length() - moonPos.length()
        return jupiterToMoon > jupiterToSun // Moon is behind Jupiter in line with the Sun
    }

    private fun angularSeparation(pos1: Equatorial, pos2: Equatorial): Double {
        // Convert RA from hours to degrees
        val ra1 = Math.toRadians(pos1.ra * 15)
        val ra2 = Math.toRadians(pos2.ra * 15)

        // Convert Dec from degrees to radians
        val dec1 = Math.toRadians(pos1.dec)
        val dec2 = Math.toRadians(pos2.dec)

        // Calculate the cosine of the angular separation using the spherical law of cosines
        val cosAngle = sin(dec1) * sin(dec2) + cos(dec1) * cos(dec2) * cos(abs(ra1 - ra2))

        // Clamp the result to [-1, 1] to ensure the acos calculation is safe
        val clampedCosAngle = cosAngle.coerceIn(-1.0, 1.0)

        // Return the angular separation in degrees
        return Math.toDegrees(acos(clampedCosAngle))
    }

    fun calculateAllJovianMoonEvents(
        currentDate: Double,
        durationHours: Double,
        jupiterAngularRadius: Double
    ): List<JovianMoonEvent> {
        val moons = listOf("Io", "Europa", "Ganymede", "Callisto")
        val allEvents = mutableListOf<JovianMoonEvent>()

        moons.forEach { moon ->
            val moonEvents = calculateMoonEvents(moon, currentDate, durationHours, jupiterAngularRadius)
            allEvents.addAll(moonEvents)
        }

        return allEvents
    }

    private fun calculateMoonEvents(
        moon: String,
        currentDate: Double,
        durationHours: Double,
        jupiterAngularRadius: Double
    ): List<JovianMoonEvent> {
        val events = mutableListOf<JovianMoonEvent>()

        // Convert duration from hours to days
        val durationDays = durationHours / 24.0
        val stepDays = 1.0 / 1440.0

        var dateIndex = currentDate
        val endJulianDate = currentDate + durationDays

        // Track transit and eclipse states
        var inTransit = false
        var inEclipse = false

        while (dateIndex <= endJulianDate) {
            val time = julianDateToAstronomyTime(dateIndex)

            // Update positions of Jupiter, the Sun, and the moon at each time step
            val jv = geoVector(Body.Jupiter, time, Aberration.Corrected)
            val sv = geoVector(Body.Sun, time, Aberration.Corrected)

            val lightTravelDays = jv.length() / C_AUDAY
            val backdate = time.addDays(-lightTravelDays)

            val jmPosition = jupiterMoons(backdate)

            // Get the moon’s position relative to Jupiter for this time
            val moonPosition = when (moon) {
                "Io" -> jv + jmPosition.io.position().withTime(jv.t)
                "Europa" -> jv + jmPosition.europa.position().withTime(jv.t)
                "Ganymede" -> jv + jmPosition.ganymede.position().withTime(jv.t)
                "Callisto" -> jv + jmPosition.callisto.position().withTime(jv.t)
                else -> throw IllegalArgumentException("Unknown moon: $moon")
            }

            // Check for Transit Events
            val separation = angularSeparation(jv.toEquatorial(), moonPosition.toEquatorial())
            if (separation <= jupiterAngularRadius && !inTransit) {
                events.add(JovianMoonEvent(EventType.MOON_ENTERS_JUPITER_TRANSIT, Utils.julianDateToUTC(dateIndex), moon))
                inTransit = true
            }
            if (separation > jupiterAngularRadius && inTransit) {
                events.add(JovianMoonEvent(EventType.MOON_EXITS_JUPITER_TRANSIT, Utils.julianDateToUTC(dateIndex), moon))
                inTransit = false
            }

            // Check for Eclipse Events
            if (isMoonInEclipse(jv, moonPosition, sv) && !inEclipse) {
                events.add(JovianMoonEvent(EventType.MOON_ENTERS_ECLIPSE, Utils.julianDateToUTC(dateIndex), moon))
                inEclipse = true
            }
            if (!isMoonInEclipse(jv, moonPosition, sv) && inEclipse) {
                events.add(JovianMoonEvent(EventType.MOON_EXITS_ECLIPSE, Utils.julianDateToUTC(dateIndex), moon))
                inEclipse = false
            }

            // Increment the Julian Date by the step interval
            dateIndex += stepDays
        }

        return events
    }



    fun incrementOffset(incrementBy: Int) {
        viewModelScope.launch {
            _timeOffset.update { it + incrementBy }
        }
    }

    fun decrementOffset(decrementBy: Int) {
        viewModelScope.launch {
            _timeOffset.update { it - decrementBy }
        }
    }

    fun resetOffset() {
        viewModelScope.launch {
            _timeOffset.emit(0L)
        }
    }

    // Function to start incrementing offset by 0.5 continuously
    fun startIncrementingOffset() {
        // Cancel any existing job to avoid overlapping increments
        incrementJob?.cancel()
        incrementJob = viewModelScope.launch {
            while (isActive) {
                _timeOffset.update { it + 1 }
                delay(100) // Adjust delay for desired speed
            }
        }
    }

    // Function to stop incrementing offset
    fun stopIncrementingOffset() {
        incrementJob?.cancel()
    }
}

enum class EventType {
    MOON_SHADOW_ENTERS_JUPITER,     // Moon’s shadow begins to cross Jupiter’s disk
    MOON_SHADOW_EXITS_JUPITER,      // Moon’s shadow leaves Jupiter’s disk
    MOON_ENTERS_JUPITER_TRANSIT,    // Moon begins transit across Jupiter
    MOON_EXITS_JUPITER_TRANSIT,     // Moon ends transit across Jupiter
    MOON_ENTERS_ECLIPSE,            // Moon enters Jupiter’s shadow (eclipse)
    MOON_EXITS_ECLIPSE              // Moon exits Jupiter’s shadow (eclipse)
}

data class JovianMoonEvent(
    val type: EventType,
    val time: LocalDateTime, // Time in UTC
    val moon: String         // Name of the moon (e.g., "Io", "Europa", "Ganymede", "Callisto")
)

sealed class JupiterDetailUIState {
    object Loading : JupiterDetailUIState()
    data class Success(
        val time: LocalDateTime,
        val jupiterAngularRadius: Double,
        val jupiterPos: Equatorial,
        val ioPos: Equatorial,
        val europaPos: Equatorial,
        val ganymedePos: Equatorial,
        val callistoPos: Equatorial,
        val jovianMoonEvents: List<JovianMoonEvent>) : JupiterDetailUIState()
    data class Error(val message: String) : JupiterDetailUIState()
}