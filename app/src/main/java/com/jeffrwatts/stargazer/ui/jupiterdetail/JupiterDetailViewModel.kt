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
import io.github.cosinekitty.astronomy.JUPITER_EQUATORIAL_RADIUS_KM
import io.github.cosinekitty.astronomy.Time
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
import java.lang.Math.toDegrees
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.atan

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
                val jupiterAngularRadius = calculateAngularRadius(JUPITER_EQUATORIAL_RADIUS_KM, jv.toEquatorial().dist)

                // Calculate the amount of time it took light to reach the Earth from Jupiter.
                // The distance to Jupiter (AU) divided by the speed of light (AU/day) = time in days.
                val lightTravelDays = jv.length() / C_AUDAY

                // The jupiterMoons function calculates positions of Jupiter's moons without
                // correcting for light travel time. Correct for light travel by backdating
                // by the given amount of light travel time.
                val backdate = time.addDays(-lightTravelDays)

                val jm = jupiterMoons(backdate)

                val jovianMoonEvents = predictAllJovianMoonEvents( dateUtc.withHour(0).withMinute(0).withSecond(0), 24.0)

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

    private fun calculateAngularRadius(radiusKm: Double, distanceAU: Double): Double {
        val radiusAU = radiusKm / 1.496e8  // Convert radius from km to AU
        return toDegrees(atan(radiusAU / distanceAU))
    }

    private fun predictAllJovianMoonEvents(startTime: LocalDateTime, durationHours: Double): List<JovianMoonEvent> {
        val moons = listOf("Io", "Europa", "Ganymede", "Callisto")
        val allEvents = mutableListOf<JovianMoonEvent>()

        for (moon in moons) {
            val moonEvents = predictJovianMoonEvents(moon, startTime, durationHours)
            allEvents.addAll(moonEvents)
        }

        // Optional: Sort events by time, if needed
        allEvents.sortBy { it.time }

        return allEvents
    }


    private fun predictJovianMoonEvents(moon: String, startTime: LocalDateTime, durationHours: Double): List<JovianMoonEvent> {
        val events = mutableListOf<JovianMoonEvent>()
        var inCrossingEvent: Boolean? = null  // Track initial state as unknown
        var inShadowEvent: Boolean? = null    // Track initial state as unknown for shadow events

        var julianIndex = Utils.calculateJulianDateUtc(startTime)
        val julianEnd = julianIndex + (durationHours / 24.0)
        val julianStep = 1.0 / 1440.0

        while (julianIndex <= julianEnd) {
            val time = julianDateToAstronomyTime(julianIndex)
            val jupiterVec = geoVector(Body.Jupiter, time, Aberration.None)
            val jupiterAngularRadius = calculateAngularRadius(JUPITER_EQUATORIAL_RADIUS_KM, jupiterVec.length())

            // Account for light travel time
            val backdate = time.addDays(-jupiterVec.length() / C_AUDAY)
            val moonVec = calculateMoonPosition(moon, jupiterVec, backdate)

            // Calculate angular separation between Jupiter and the moon
            val angularSeparation = jupiterVec.angleWith(moonVec)

            // Update event state (either transit or occultation)
            inCrossingEvent = updateCrossingEventState(
                inCrossingEvent,
                angularSeparation,
                jupiterAngularRadius,
                moonVec.length(),
                jupiterVec.length(),
                julianIndex,
                events,
                moon
            )

            // Calculate the shadow position of the moon on Jupiter
            val sunVec = geoVector(Body.Sun, time, Aberration.None)
            val shadowPosition = calculateMoonShadowPosition(moonVec, sunVec, jupiterVec)

            // Calculate the angular separation between Jupiter and the shadow position
            val shadowAngularSeparation = jupiterVec.angleWith(shadowPosition)

            // Update shadow event state
            inShadowEvent = updateShadowEventState(
                inShadowEvent,
                shadowAngularSeparation,
                jupiterAngularRadius,
                julianIndex,
                events,
                moon,
                moonVec.length(),
                jupiterVec.length()
            )

            julianIndex += julianStep
        }

        return events
    }

    /**
     * Calculates the position of the specified moon relative to Jupiter.
     */
    private fun calculateMoonPosition(moon: String, jupiterVec: Vector, time: Time): Vector {
        val moonPositions = jupiterMoons(time)
        return when (moon) {
            "Io" -> jupiterVec + moonPositions.io.position().withTime(jupiterVec.t)
            "Europa" -> jupiterVec + moonPositions.europa.position().withTime(jupiterVec.t)
            "Ganymede" -> jupiterVec + moonPositions.ganymede.position().withTime(jupiterVec.t)
            "Callisto" -> jupiterVec + moonPositions.callisto.position().withTime(jupiterVec.t)
            else -> throw IllegalArgumentException("Unknown moon: $moon")
        }
    }

    /**
     * Updates the transit state and logs entry or exit events as needed.
     */
    private fun updateCrossingEventState(
        inCrossingEvent: Boolean?,
        angularSeparation: Double,
        jupiterAngularRadius: Double,
        moonDistance: Double,
        jupiterDistance: Double,
        julianIndex: Double,
        events: MutableList<JovianMoonEvent>,
        moon: String
    ): Boolean {
        return when {
            inCrossingEvent == null -> {
                // Initial state check
                if (angularSeparation < jupiterAngularRadius) {
                    true
                } else {
                    false
                }
            }
            !inCrossingEvent && angularSeparation < jupiterAngularRadius -> {
                // Moon enters a new event (either transit or occultation)
                val eventType = if (moonDistance < jupiterDistance) {
                    EventType.MOON_ENTERS_JUPITER_TRANSIT
                } else {
                    EventType.MOON_ENTERS_JUPITER_OCCLUSION
                }
                events.add(JovianMoonEvent(eventType, Utils.julianDateToUTC(julianIndex), moon))
                true
            }
            inCrossingEvent && angularSeparation >= jupiterAngularRadius -> {
                // Moon exits the current event
                val exitEventType = if (moonDistance < jupiterDistance) {
                    EventType.MOON_EXITS_JUPITER_TRANSIT
                } else {
                    EventType.MOON_EXITS_JUPITER_OCCLUSION
                }
                events.add(JovianMoonEvent(exitEventType, Utils.julianDateToUTC(julianIndex), moon))
                false
            }
            else -> inCrossingEvent
        }
    }

    private fun updateShadowEventState(
        inShadowEvent: Boolean?,
        shadowAngularSeparation: Double,
        jupiterAngularRadius: Double,
        julianIndex: Double,
        events: MutableList<JovianMoonEvent>,
        moon: String,
        moonDistance: Double,   // Pass moon's distance as an additional parameter
        jupiterDistance: Double // Pass Jupiter's distance as an additional parameter
    ): Boolean {
        return when {
            inShadowEvent == null -> {
                // Initial state check: only start if moon is closer to Earth than Jupiter
                shadowAngularSeparation < jupiterAngularRadius && moonDistance < jupiterDistance
            }
            !inShadowEvent && shadowAngularSeparation < jupiterAngularRadius && moonDistance < jupiterDistance -> {
                // Shadow starts crossing Jupiter
                events.add(JovianMoonEvent(EventType.MOON_SHADOW_BEGINS_JUPITER_DISK, Utils.julianDateToUTC(julianIndex), moon))
                true
            }
            inShadowEvent && (shadowAngularSeparation >= jupiterAngularRadius || moonDistance >= jupiterDistance) -> {
                // Shadow leaves Jupiter or moon moves behind Jupiter
                events.add(JovianMoonEvent(EventType.MOON_SHADOW_LEAVES_JUPITER_DISK, Utils.julianDateToUTC(julianIndex), moon))
                false
            }
            else -> inShadowEvent
        }
    }



    private fun calculateMoonShadowPosition(moonVec: Vector, sunVec: Vector, jupiterVec: Vector): Vector {
        // Step 1: Convert sunVec and moonVec to be relative to Jupiter
        val moonRelativeToJupiter = moonVec - jupiterVec
        val sunRelativeToJupiter = sunVec - jupiterVec

        // Step 2: Calculate the direction from the Sun to the Moon (relative to Jupiter)
        val shadowDirection = moonRelativeToJupiter - sunRelativeToJupiter

        // Step 3: Normalize the shadow direction vector
        val shadowDirectionNormalized = shadowDirection / shadowDirection.length()

        // Step 4: Scale the normalized direction to the length of the moon's vector (relative to Jupiter)
        val scaledShadowDirection = Vector(
            shadowDirectionNormalized.x * moonRelativeToJupiter.length(),
            shadowDirectionNormalized.y * moonRelativeToJupiter.length(),
            shadowDirectionNormalized.z * moonRelativeToJupiter.length(),
            moonVec.t
        )

        // Step 5: Calculate the shadow position relative to Jupiter
        val shadowPositionRelativeToJupiter = Vector(
            moonRelativeToJupiter.x + scaledShadowDirection.x,
            moonRelativeToJupiter.y + scaledShadowDirection.y,
            moonRelativeToJupiter.z + scaledShadowDirection.z,
            moonVec.t
        )

        // Step 6: Convert the shadow position back to Earth-centered coordinates
        val shadowPositionRelativeToEarth = shadowPositionRelativeToJupiter + jupiterVec

        return shadowPositionRelativeToEarth
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
    MOON_ENTERS_JUPITER_TRANSIT,    // Moon begins transit across Jupiter
    MOON_EXITS_JUPITER_TRANSIT,     // Moon ends transit across Jupiter
    MOON_ENTERS_JUPITER_OCCLUSION,  // Moon begins occlusion behind Jupiter
    MOON_EXITS_JUPITER_OCCLUSION,    // Moon ends occlusion behind Jupiter
    MOON_SHADOW_BEGINS_JUPITER_DISK,    // Moon shadow starts crossing Jupiter
    MOON_SHADOW_LEAVES_JUPITER_DISK,     // Moon shadow leaves Jupiter disk
    MOON_ENTERS_ECLIPSE_OF_JUPITER,     // Moon enters eclipse
    MOON_EXITS_ECLIPSE_OF_JUPITER       // Moon exits eclipse
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