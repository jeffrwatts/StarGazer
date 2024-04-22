package com.jeffrwatts.stargazer.ui.deepskyobjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepSkyObjectsViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _selectedFilter = MutableStateFlow<Boolean>(true) // true will be Recommended, false will be All
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<DeepSkyObjectsUiState>(DeepSkyObjectsUiState.Loading)
    val uiState: StateFlow<DeepSkyObjectsUiState> = _uiState

    init {
        fetchObjects()
    }

    fun setRecommendedFilter(recommended: Boolean) {
        _selectedFilter.value = recommended
        fetchObjects()
    }

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    val typesToQuery =
                        listOf(ObjectType.CLUSTER, ObjectType.NEBULA, ObjectType.GALAXY)

                    celestialObjRepository.getAllCelestialObjsByType(typesToQuery, location, date)
                        .distinctUntilChanged() // To avoid redundant UI updates
                        .catch { e ->
                            _uiState.value =
                                DeepSkyObjectsUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            var listFiltered = celestialObjPosList

                            if (selectedFilter.value) {
                                listFiltered = listFiltered.filter { it.celestialObj.recommended }
                            }

                            listFiltered = listFiltered.sortedWith(compareByDescending { it.observable })

                            _uiState.value = DeepSkyObjectsUiState.Success(listFiltered)
                        }
                }
            }
        }
    }
}

sealed class DeepSkyObjectsUiState {
    object Loading : DeepSkyObjectsUiState()
    data class Success(val data: List<CelestialObjPos>) : DeepSkyObjectsUiState()
    data class Error(val message: String) : DeepSkyObjectsUiState()
}