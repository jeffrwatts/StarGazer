package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.solarsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.solarsystem.PlanetObjPos
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.solarsystem.SolarSystemRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SolarSystemViewModel @Inject constructor(
    private val solarSystemRepository: SolarSystemRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private var collectionJob: Job? = null
    private val _uiState = MutableStateFlow<SolarSystemUiState>(SolarSystemUiState.Loading)
    val uiState: StateFlow<SolarSystemUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        collectionJob?.cancel() // Cancel the previous collection job if it exists
        collectionJob = viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location?.let { location ->
                    val date = Utils.calculateJulianDateNow()

                    solarSystemRepository.getAllPlanets(location, date)
                        .catch { e ->
                            _uiState.value = SolarSystemUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { planetObjPosList ->
                            val listFiltered = planetObjPosList
                                .sortedWith(compareByDescending { it.observable })
                            _uiState.value = SolarSystemUiState.Success(listFiltered)
                        }
                }
            }
        }
    }
}

sealed class SolarSystemUiState {
    object Loading : SolarSystemUiState()
    data class Success(val data: List<PlanetObjPos>) : SolarSystemUiState()
    data class Error(val message: String) : SolarSystemUiState()
}