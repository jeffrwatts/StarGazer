package com.jeffrwatts.stargazer.ui.skytonight

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.location.LocationRepository
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
    private val _filterType = MutableStateFlow(FilterType.RECOMMENDED)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

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
                _filterType,
                _timeOffset,
                _pullToRefresh
            ) { celestialObjs, location, filter, timeOffset, _ ->
                try {
                    val date = if (timeOffset != 0L) {
                        LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                    } else {
                        LocalDateTime.now()
                    }
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let {loc->
                        val celestialObjPosList = celestialObjs
                            .map {celestialObj->
                                CelestialObjPos.fromCelestialObjWithImage(celestialObj, julianDate, location)
                            }
                            .filter {
                                when (filter) {
                                    FilterType.RECOMMENDED -> it.celestialObjWithImage.celestialObj.recommended
                                    FilterType.NEAR_MERIDIAN -> {
                                        it.timeUntilMeridian< 3.0|| it.timeUntilMeridian>23.0 || it.celestialObjWithImage.celestialObj.type== ObjectType.PLANET
                                    }
                                    else -> true
                                }
                            }
                            .sortedWith(compareByDescending { it.observable })

                        val (nightStart, nightEnd, isNight) = Utils.getNight(julianDate, loc)
                        Log.d("TEST", "IsNight=$isNight - Night Start: ${Utils.julianDateToLocalTime(nightStart)}; Night End: ${Utils.julianDateToLocalTime(nightEnd)} ")

                        val illuminationInfo = illumination(Body.Moon, julianDateToAstronomyTime(julianDate))
                        val moonIllumination = (illuminationInfo.phaseFraction * 100.0).toInt()
                        _uiState.value = SkyTonightUiState.Success(celestialObjPosList, moonIllumination, true, date)
                    }?: run {
                        _uiState.value = SkyTonightUiState.Success(emptyList(), moonIllumination = 0, false, date)
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

    fun setFilter(filterType: FilterType) {
        viewModelScope.launch {
            _filterType.emit(filterType)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit)
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

enum class FilterType {
    RECOMMENDED, NEAR_MERIDIAN, ALL
}

sealed class SkyTonightUiState {
    object Loading : SkyTonightUiState()
    data class Success(val data: List<CelestialObjPos>,
                       val moonIllumination: Int,
                       val locationAvailable: Boolean,
                       val currentTime: LocalDateTime) : SkyTonightUiState()
    data class Error(val message: String) : SkyTonightUiState()
}