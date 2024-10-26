package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.jupiterdetail

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class JupiterDetailViewModel @Inject constructor (

) : ViewModel() {
    private val _uiState = MutableStateFlow<JupiterDetailUIState>(JupiterDetailUIState.Loading)
    val uiState: StateFlow<JupiterDetailUIState> = _uiState.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)

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

                // Calculate the amount of time it took light to reach the Earth from Jupiter.
                // The distance to Jupiter (AU) divided by the speed of light (AU/day) = time in days.
                val lightTravelDays = jv.length() / C_AUDAY

                // The jupiterMoons function calculates positions of Jupiter's moons without
                // correcting for light travel time. Correct for light travel by backdating
                // by the given amount of light travel time.
                val backdate = time.addDays(-lightTravelDays)

                val jm = jupiterMoons(backdate)

                // Tricky: the `+` operator for adding `Vector` will throw an exception
                // if the vectors do not have matching times. We work around this
                // by using `withTime` to clone each moon's position vector to have
                // a different time. This is a manual override to work around a safety check.
                _uiState.value = JupiterDetailUIState.Success(
                    time = dateUtc,
                    lightTravelDays = lightTravelDays,
                    jupiterPos = jv.toEquatorial(),
                    ioPos = (jv + jm.io.position().withTime(jv.t)).toEquatorial(),
                    europaPos = (jv + jm.europa.position().withTime(jv.t)).toEquatorial(),
                    ganymedePos = (jv + jm.ganymede.position().withTime(jv.t)).toEquatorial(),
                    callistoPos = (jv + jm.callisto.position().withTime(jv.t)).toEquatorial(),
                )
            }
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
}

sealed class JupiterDetailUIState {
    object Loading : JupiterDetailUIState()
    data class Success(
        val time: LocalDateTime,
        val lightTravelDays: Double,
        val jupiterPos: Equatorial,
        val ioPos: Equatorial,
        val europaPos: Equatorial,
        val ganymedePos: Equatorial,
        val callistoPos: Equatorial) : JupiterDetailUIState()
    data class Error(val message: String) : JupiterDetailUIState()
}