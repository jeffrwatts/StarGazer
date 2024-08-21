package com.jeffrwatts.stargazer.ui.celestialobjdetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CelestialObjDetailViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<CelestialObjDetailUiState>(CelestialObjDetailUiState.Loading)
    val uiState: StateFlow<CelestialObjDetailUiState> = _uiState.asStateFlow()

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                combine(
                    celestialObjRepository.getCelestialObj(sightId),
                    locationRepository.locationFlow
                ) { celestialObjWithImage, location ->
                    var altitudes = emptyList<Utils.AltitudeEntry>()
                    location?.let { loc->
                        altitudes = generateAltitudes(celestialObjWithImage, loc)
                    }
                    _uiState.value = CelestialObjDetailUiState.Success(celestialObjWithImage, altitudes)
                }.collect()
            } catch (e: Exception) {
                _uiState.value = CelestialObjDetailUiState.Error("Error loading data")
            }
        }
    }

    private fun generateAltitudes(obj: CelestialObjWithImage, location: Location): List<Utils.AltitudeEntry> {
        val timeStart = LocalDateTime.now().minusHours(2).withMinute(0).withSecond(0)
        val durationHours = 24L
        val incrementMinutes = 10L
        val altitudeData = mutableListOf<Utils.AltitudeEntry>()

        // Set up the start time to be 2 hours before at 0 min and 0 sec.
        var timeIx = timeStart
        val endTime = timeIx.plusHours(durationHours)

        while (timeIx.isBefore(endTime)) {
            val julianDate = Utils.calculateJulianDateFromLocal(timeIx)
            val celestialObjPos = CelestialObjPos.fromCelestialObjWithImage(obj, julianDate, location)
            altitudeData.add(Utils.AltitudeEntry(timeIx, celestialObjPos.alt))
            timeIx = timeIx.plusMinutes(incrementMinutes)
        }

        return altitudeData
    }
}

sealed class CelestialObjDetailUiState {
    object Loading : CelestialObjDetailUiState()
    data class Success(val data: CelestialObjWithImage, val altitudes: List<Utils.AltitudeEntry>) : CelestialObjDetailUiState()
    data class Error(val message: String) : CelestialObjDetailUiState()
}
