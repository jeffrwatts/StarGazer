package com.jeffrwatts.stargazer.ui.polar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.ui.sights.SightsUiState
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PolarAlignViewModel(
    private val repository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PolarAlignUiState>(PolarAlignUiState.Loading)
    val uiState: StateFlow<PolarAlignUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            repository.getAllByTypeStream(ObjectType.STAR)
                .combine(locationRepository.locationFlow) { objects, location ->
                    // Check if location is not null and proceed
                    if (location != null) {
                        val jdNow = Utils.calculateJulianDateNow()
                        objects.map { obj ->
                                CelestialObjPos.fromCelestialObj(obj, julianDate = jdNow, lat = location.latitude, lon = location.longitude)
                            }.sortedByDescending { it.polarAlignCandidate }
                    } else {
                        // Emit a Loading state if the location is not yet available
                        _uiState.value = PolarAlignUiState.Loading
                        return@combine emptyList<CelestialObjPos>()
                    }
                }
                .distinctUntilChanged() // To avoid redundant UI updates
                .catch { e ->
                    _uiState.value = PolarAlignUiState.Error(e.message ?: "Unknown error")
                }
                .collect { celestialObjPosList ->
                    // If location is still not available (null), keep the Loading state
                    if (celestialObjPosList.isNotEmpty() || locationRepository.locationFlow.value != null) {
                        _uiState.value = PolarAlignUiState.Success(celestialObjPosList)
                    }
                }
        }
    }

    fun updateObservationStatus(celestialObj: CelestialObj, newObservationStatus: ObservationStatus) {
        viewModelScope.launch {
            val updatedItem = celestialObj.copy(observationStatus = newObservationStatus)
            repository.update(updatedItem)
        }
    }
}

sealed class PolarAlignUiState {
    object Loading : PolarAlignUiState()
    data class Success(val data: List<CelestialObjPos>) : PolarAlignUiState()
    data class Error(val message: String) : PolarAlignUiState()
}
