package com.jeffrwatts.stargazer.ui.deepskydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeepSkyDetailViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SightDetailUiState>(SightDetailUiState.Loading)
    val uiState: StateFlow<SightDetailUiState> = _uiState.asStateFlow()

    fun fetchSightDetail(sightId: Int) {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    celestialObjRepository.getCelestialObj(sightId, location, date)
                        .collect { celestialObjPos ->

                            val timeStart = LocalDateTime.now()
                                .minusHours(2)
                                .withMinute(0)
                                .withSecond(0)

                            val altitudeEntries = Utils.getAltitudeEntries(
                                celestialObjPos.celestialObjWithImage.celestialObj.ra,
                                celestialObjPos.celestialObjWithImage.celestialObj.dec,
                                location,
                                timeStart,
                                24,
                                10
                            )

                            _uiState.value = SightDetailUiState.Success(celestialObjPos.celestialObjWithImage, altitudeEntries)
                        }
                }
            }
        }
    }
}

sealed class SightDetailUiState {
    object Loading : SightDetailUiState()
    data class Success(val data: CelestialObjWithImage, val altitudes: List<Utils.AltitudeEntry>) : SightDetailUiState()
    data class Error(val message: String) : SightDetailUiState()
}
