package com.jeffrwatts.stargazer.ui.sights
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.*
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SightsViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private var locationUpdating = false

    private val _selectedFilter = MutableStateFlow<ObservationStatus?>(null) // null will represent 'Show All'
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<SightsUiState>(SightsUiState.Loading)
    val uiState: StateFlow<SightsUiState> = _uiState

    fun setObservationStatusFilter(status: ObservationStatus?) {
        _selectedFilter.value = status
        fetchObjects()
    }

    fun startLocationUpdates() {
        if (!locationUpdating) {
            locationRepository.startLocationUpdates(viewModelScope)
            locationUpdating = true
            fetchObjects()
        }
    }

    fun fetchObjects() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect{location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    val typesToQuery = listOf(ObjectType.PLANET, ObjectType.CLUSTER, ObjectType.NEBULA, ObjectType.GALAXY)

                    celestialObjRepository.getAllCelestialObjsByType(typesToQuery, location, date)
                        .distinctUntilChanged() // To avoid redundant UI updates
                        .catch { e ->
                            _uiState.value = SightsUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val sorted = celestialObjPosList
                                .filter { selectedFilter.value == null || it.celestialObj.observationStatus == selectedFilter.value }
                                .sortedWith(compareByDescending<CelestialObjPos> { it.observable }
                                    .thenBy { it.celestialObj.observationStatus.priority } )
                            _uiState.value = SightsUiState.Success(sorted)
                        }
                }
            }
        }
    }

    fun updateObservationStatus(celestialObj: CelestialObj, newObservationStatus: ObservationStatus) {
        viewModelScope.launch {
            val updatedItem = celestialObj.copy(observationStatus = newObservationStatus)
            celestialObjRepository.update(updatedItem)
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
