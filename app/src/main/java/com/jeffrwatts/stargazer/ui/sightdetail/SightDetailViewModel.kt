package com.jeffrwatts.stargazer.ui.sightdetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SightDetailViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SightDetailUiState>(SightDetailUiState.Loading)
    val uiState: StateFlow<SightDetailUiState> = _uiState.asStateFlow()

    fun fetchSightDetail(sightId: Int) {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    celestialObjRepository.getCelestialObj(sightId, location, date).collect { celestialObjPos ->
                        celestialObjPos?.let {
                            _uiState.value = SightDetailUiState.Success(it.celestialObj, getAltitudeEntries(it.celestialObj, location))
                        } ?: run {
                            _uiState.value = SightDetailUiState.Error("Object not found.")
                        }
                    }
                }
            }
        }
    }

    private fun calculateJulianDate(localTime:LocalDateTime):Double {
        val zonedDateTime = localTime.atZone(ZoneId.systemDefault())
        val utcZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
        val utcNow = utcZonedDateTime.toLocalDateTime()
        return Utils.calculateJulianDate(utcNow)
    }

    private fun getAltitudeEntries(celestialObj: CelestialObj, location: Location): List<AltitudeEntry> {
        val altitudeData = mutableListOf<AltitudeEntry>()

        // Set up the start time to be 2 hours before at 0 min and 0 sec.
        var timeIx = LocalDateTime.now()
            .minusHours(2)
            .withMinute(0)
            .withSecond(0)

        val endTime = timeIx.plusHours(24)

        while (timeIx.isBefore(endTime)) {
            val julianDate = calculateJulianDate(timeIx)
            val (alt, _, _) = Utils.calculateAltAzm(
                celestialObj.ra,
                celestialObj.dec,
                location.latitude,
                location.longitude,
                julianDate
            )
            altitudeData.add(AltitudeEntry(timeIx, alt))
            timeIx = timeIx.plusMinutes(10)
        }

        return altitudeData
    }

}

data class AltitudeEntry(val time: LocalDateTime, val alt: Double)

sealed class SightDetailUiState {
    object Loading : SightDetailUiState()
    data class Success(val data: CelestialObj, val altitudes: List<AltitudeEntry>) : SightDetailUiState()
    data class Error(val message: String) : SightDetailUiState()
}
