package com.jeffrwatts.stargazer.ui.photoplanner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.*
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoPlannerViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _selectedFilter = MutableStateFlow<PhotoStatus?>(null) // null will represent 'Show All'
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<PhotoPlannerUiState>(PhotoPlannerUiState.Loading)
    val uiState: StateFlow<PhotoPlannerUiState> = _uiState

    fun setPhotoStatusFilter(status: PhotoStatus?) {
        _selectedFilter.value = status
        fetchObjects()
    }

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
        fetchObjects()
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
                            _uiState.value = PhotoPlannerUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val sorted = celestialObjPosList
                                .filter { selectedFilter.value == null || it.celestialObj.photoStatus == selectedFilter.value }
                                .sortedWith(compareByDescending<CelestialObjPos> { it.observable }
                                    .thenBy { it.celestialObj.photoStatus.priority } )
                            _uiState.value = PhotoPlannerUiState.Success(sorted)
                        }
                }
            }
        }
    }

    fun updatePhotoStatus(celestialObj: CelestialObj, newPhotoStatus: PhotoStatus) {
        viewModelScope.launch {
            val updatedItem = celestialObj.copy(photoStatus = newPhotoStatus)
            celestialObjRepository.update(updatedItem)
        }
    }
}

sealed class PhotoPlannerUiState {
    object Loading : PhotoPlannerUiState()
    data class Success(val data: List<CelestialObjPos>) : PhotoPlannerUiState()
    data class Error(val message: String) : PhotoPlannerUiState()
}
