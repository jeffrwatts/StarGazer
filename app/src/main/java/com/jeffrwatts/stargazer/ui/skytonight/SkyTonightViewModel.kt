package com.jeffrwatts.stargazer.ui.skytonight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.julianDateToAstronomyTime
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.illumination
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SkyTonightViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _recommendedFilter = MutableStateFlow(true)
    val recommendedFilter: StateFlow<Boolean> = _recommendedFilter.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)
    private val _pullToRefresh = MutableSharedFlow<Unit>(replay = 1)

    private val _uiState = MutableStateFlow<SkyTonightUiState>(SkyTonightUiState.Loading)
    val uiState: StateFlow<SkyTonightUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit) // Emit an initial value to start the process
            combine(
                celestialObjRepository.getAllCelestialObjects(),
                locationRepository.locationFlow,
                _recommendedFilter,
                _timeOffset,
                _pullToRefresh
            ) { celestialObjs, location, recommended, timeOffset, _ ->
                try {
                    val date = LocalDateTime.now().plusHours(timeOffset)
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let {loc->
                        val celestialObjPosList = celestialObjs
                            .filter { !recommended || it.celestialObj.recommended }
                            .map {celestialObj->
                                CelestialObjPos.fromCelestialObjWithImage(celestialObj, julianDate, location)
                            }
                            .sortedWith(compareByDescending { it.observable })

                        val illuminationInfo = illumination(Body.Moon, julianDateToAstronomyTime(julianDate))
                        val moonIllumination = (illuminationInfo.phaseFraction * 100.0).toInt()
                        _uiState.value = SkyTonightUiState.Success(celestialObjPosList, moonIllumination, true, date.format(DATE_TIME_FORMATTER))
                    }?: run {
                        _uiState.value = SkyTonightUiState.Success(emptyList(), moonIllumination = 0, false, date.format(DATE_TIME_FORMATTER))
                    }

                } catch (e: Exception) {
                    _uiState.value = SkyTonightUiState.Error(e.message ?: "Unknown error")
                }
            }.collect()
        }
    }

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun setRecommendedFilter(recommended: Boolean) {
        viewModelScope.launch {
            _recommendedFilter.emit(recommended)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit)
        }
    }

    fun incrementOffset() {
        viewModelScope.launch {
            _timeOffset.update { it + 1 }
        }
    }

    fun decrementOffset() {
        viewModelScope.launch {
            _timeOffset.update { it - 1 }
        }
    }

    fun resetOffset() {
        viewModelScope.launch {
            _timeOffset.emit(0L)
        }
    }
}

sealed class SkyTonightUiState {
    object Loading : SkyTonightUiState()
    data class Success(val data: List<CelestialObjPos>,
                       val moonIllumination: Int,
                       val locationAvailable: Boolean,
                       val currentTime: String) : SkyTonightUiState()
    data class Error(val message: String) : SkyTonightUiState()
}