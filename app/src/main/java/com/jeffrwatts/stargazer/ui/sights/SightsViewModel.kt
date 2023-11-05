package com.jeffrwatts.stargazer.ui.sights
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.*
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SightsViewModel(
    private val repository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<ObservationStatus?>(null) // null will represent 'Show All'
    val filterStatus: StateFlow<ObservationStatus?> = _filterStatus

    private val _uiState = MutableStateFlow<SightsUiState>(SightsUiState.Loading)
    val uiState: StateFlow<SightsUiState> = _uiState

    init {
        //locationRepository.startLocationUpdates(viewModelScope)
        fetchObjects()
    }

    fun setObservationStatusFilter(status: ObservationStatus?) {
        _filterStatus.value = status
        fetchObjects()
    }
    fun fetchObjects() {
        viewModelScope.launch {
            repository.getAllStream()
                .combine(locationRepository.locationFlow) { objects, location ->
                    // Check if location is not null and proceed
                    if (location != null) {
                        val jdNow = Utils.calculateJulianDateNow()
                        objects.filterNot { it.type == ObjectType.STAR }
                            .filter { filterStatus.value == null || it.observationStatus == filterStatus.value }
                            .map { obj ->
                                CelestialObjPos.fromCelestialObj(obj, julianDate = jdNow, lat = location.latitude, lon = location.longitude)
                            }.sortedWith(
                                compareByDescending<CelestialObjPos> { it.alt >= 20 }
                                    .thenBy { it.celestialObj.observationStatus.priority }
                            )
                    } else {
                        // Emit a Loading state if the location is not yet available
                        _uiState.value = SightsUiState.Loading
                        return@combine emptyList<CelestialObjPos>()
                    }
                }
                .distinctUntilChanged() // To avoid redundant UI updates
                .catch { e ->
                    _uiState.value = SightsUiState.Error(e.message ?: "Unknown error")
                }
                .collect { celestialObjPosList ->
                    // If location is still not available (null), keep the Loading state
                    if (celestialObjPosList.isNotEmpty() || locationRepository.locationFlow.value != null) {
                        _uiState.value = SightsUiState.Success(celestialObjPosList)
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

    override fun onCleared() {
        super.onCleared()
        //locationRepository.stopLocationUpdates()
    }
}

sealed class SightsUiState {
    object Loading : SightsUiState()
    data class Success(val data: List<CelestialObjPos>) : SightsUiState()
    data class Error(val message: String) : SightsUiState()
}
