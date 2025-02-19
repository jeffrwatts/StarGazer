package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.jupiterdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.GRSTransitUtils
import com.jeffrwatts.stargazer.data.location.LocationRepository
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Math.toDegrees
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.atan

@HiltViewModel
class JupiterDetailViewModel @Inject constructor (
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<JupiterDetailUIState>(JupiterDetailUIState.Loading)
    val uiState: StateFlow<JupiterDetailUIState> = _uiState.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)
    private var incrementJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                _timeOffset,
                locationRepository.locationFlow
            )
            { timeOffset, location ->
                val date = if (timeOffset != 0L) {
                    LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                } else {
                    LocalDateTime.now()
                }
                val julianDate = Utils.calculateJulianDateFromLocal(date)
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

                var jovianEvents = emptyList<JovianEvent>()
                location?.let {loc->
                    val (nightStart, nightEnd, isNight) = Utils.getNight(julianDate, loc)
                    jovianEvents = predictAllJovianEvents(nightStart, nightEnd)
                }

                // Tricky: the `+` operator for adding `Vector` will throw an exception
                // if the vectors do not have matching times. We work around this
                // by using `withTime` to clone each moon's position vector to have
                // a different time. This is a manual override to work around a safety check.
                _uiState.value = JupiterDetailUIState.Success(
                    time = date,
                    jupiterAngularRadius = jupiterAngularRadius,
                    jupiterPos = jv.toEquatorial(),
                    ioPos = (jv + jm.io.position().withTime(jv.t)).toEquatorial(),
                    europaPos = (jv + jm.europa.position().withTime(jv.t)).toEquatorial(),
                    ganymedePos = (jv + jm.ganymede.position().withTime(jv.t)).toEquatorial(),
                    callistoPos = (jv + jm.callisto.position().withTime(jv.t)).toEquatorial(),
                    jovianEvents
                )
            }.collect()
        }
    }

    private fun calculateAngularRadius(radiusKm: Double, distanceAU: Double): Double {
        val radiusAU = radiusKm / 1.496e8  // Convert radius from km to AU
        return toDegrees(atan(radiusAU / distanceAU))
    }

    private fun predictAllJovianEvents(nightStart: Double, nightEnd: Double): List<JovianEvent> {
        val allEvents = mutableListOf<JovianEvent>()

        // Great Red Spot Transit.
        val grsStart = nightStart - 4.0/24.0 // expand time so that there is some context for the events.
        val (_, grsNextTransit) = GRSTransitUtils.predictGRSTransits(grsStart)
        if (grsNextTransit in grsStart..nightEnd) {
            val isNight = grsNextTransit in nightStart..nightEnd
            allEvents.add(JovianEvent(EventType.GRS_TRANSIT, grsNextTransit, "Great Red Spot", isNight))
        }

        // Iterate through moons
        val moons = listOf("Io", "Europa", "Ganymede", "Callisto")
        for (moon in moons) {
            val moonEvents = predictJovianMoonEvents(moon, nightStart, nightEnd)
            allEvents.addAll(moonEvents)
        }

        allEvents.sortBy { it.eventTime }

        return allEvents
    }

    private fun predictJovianMoonEvents(moon: String, nightStart: Double, nightEnd: Double): List<JovianEvent> {
        val events = mutableListOf<JovianEvent>()
        var inCrossingEvent: Boolean? = null  // Track initial state as unknown
        var inShadowEvent: Boolean? = null    // Track initial state as unknown for shadow events

        var julianIndex = nightStart - 4.0/24.0 // expand time so that there is some context for the events.
        val julianStep = 1.0 / 1440.0

        while (julianIndex <= nightEnd) {
            val time = julianDateToAstronomyTime(julianIndex)
            val sunVec = geoVector(Body.Sun, time, Aberration.None)
            val jupiterVec = geoVector(Body.Jupiter, time, Aberration.None)
            val jupiterAngularRadius = calculateAngularRadius(JUPITER_EQUATORIAL_RADIUS_KM, jupiterVec.length())

            // Account for light travel time
            val backdate = time.addDays(-jupiterVec.length() / C_AUDAY)
            val moonVec = calculateMoonPosition(moon, jupiterVec, backdate)

            // Calculate the start/end transit
            val (crossingEventType, newInCrossingEvent) = updateCrossingEventState(
                inCrossingEvent,
                jupiterVec,
                moonVec,
                jupiterAngularRadius
            )

            inCrossingEvent = newInCrossingEvent

            crossingEventType?.let {
                events.add(JovianEvent(it, julianIndex, moon, julianIndex in nightStart.. nightEnd))
            }

            // Calculate shadow start/end transit
            val (shadowEventType, newInShadowEvent) = updateShadowEventState(
                inShadowEvent,
                jupiterVec,
                moonVec,
                sunVec,
                jupiterAngularRadius
            )

            inShadowEvent = newInShadowEvent

            shadowEventType?.let {
                events.add(JovianEvent(it, julianIndex, moon, julianIndex in nightStart.. nightEnd))
            }

            julianIndex += julianStep
        }

        return events
    }

    /**
     * Calculates the position of the specified moon relative to Earth.
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

    /**
     * Updates the transit state and logs entry or exit events as needed.
     */
    private fun updateCrossingEventState(
        inCrossingEvent: Boolean?,
        jupiterVec: Vector,
        moonVec: Vector,
        jupiterAngularRadius: Double
    ): Pair<EventType?, Boolean> {
        val angularSeparation = jupiterVec.angleWith(moonVec)

        return when {
            inCrossingEvent == null -> {
                // Initial state check
                Pair(null, angularSeparation < jupiterAngularRadius)
            }
            !inCrossingEvent && angularSeparation < jupiterAngularRadius -> {
                // Moon enters a new event (either transit or occultation)
                val eventType = if (moonVec.length() < jupiterVec.length()) {
                    EventType.MOON_ENTERS_JUPITER_TRANSIT
                } else {
                    EventType.MOON_ENTERS_JUPITER_OCCLUSION
                }
                Pair(eventType, true)
            }
            inCrossingEvent && angularSeparation >= jupiterAngularRadius -> {
                // Moon exits the current event
                val exitEventType = if (moonVec.length() < jupiterVec.length()) {
                    EventType.MOON_EXITS_JUPITER_TRANSIT
                } else {
                    EventType.MOON_EXITS_JUPITER_OCCLUSION
                }
                Pair(exitEventType, false)
            }
            else -> Pair(null, inCrossingEvent)
        }
    }

    private fun updateShadowEventState(
        inShadowEvent: Boolean?,
        jupiterVec: Vector,
        moonVec: Vector,
        sunVec: Vector,
        jupiterAngularRadius: Double
    ): Pair<EventType?, Boolean> {
        val shadowPosition = calculateMoonShadowPosition(moonVec, sunVec, jupiterVec)
        val shadowAngularSeparation = jupiterVec.angleWith(shadowPosition)

        return when {
            inShadowEvent == null -> {
                // Initial state check: only start if moon is closer to Earth than Jupiter
                Pair(null, shadowAngularSeparation < jupiterAngularRadius && moonVec.length() < jupiterVec.length())
            }
            !inShadowEvent && shadowAngularSeparation < jupiterAngularRadius && moonVec.length() < jupiterVec.length() -> {
                // Shadow starts crossing Jupiter
                Pair(EventType.MOON_SHADOW_BEGINS_JUPITER_DISK, true)
            }
            inShadowEvent && (shadowAngularSeparation >= jupiterAngularRadius || moonVec.length() >= jupiterVec.length()) -> {
                // Shadow leaves Jupiter or moon moves behind Jupiter
                Pair(EventType.MOON_SHADOW_LEAVES_JUPITER_DISK, false)
            }
            else -> Pair(null, inShadowEvent)
        }
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
    GRS_TRANSIT,                    // Great Red Spot crosses central meridian.
    MOON_ENTERS_JUPITER_TRANSIT,    // Moon begins transit across Jupiter
    MOON_EXITS_JUPITER_TRANSIT,     // Moon ends transit across Jupiter
    MOON_ENTERS_JUPITER_OCCLUSION,  // Moon begins occlusion behind Jupiter
    MOON_EXITS_JUPITER_OCCLUSION,    // Moon ends occlusion behind Jupiter
    MOON_SHADOW_BEGINS_JUPITER_DISK,    // Moon shadow starts crossing Jupiter
    MOON_SHADOW_LEAVES_JUPITER_DISK,     // Moon shadow leaves Jupiter disk
    MOON_ENTERS_ECLIPSE_OF_JUPITER,     // Moon enters eclipse
    MOON_EXITS_ECLIPSE_OF_JUPITER       // Moon exits eclipse
}

data class JovianEvent(
    val eventType: EventType,
    val eventTime: Double,
    val eventObject: String,
    val isNight: Boolean
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
        val jovianEvents: List<JovianEvent>) : JupiterDetailUIState()
    data class Error(val message: String) : JupiterDetailUIState()
}