package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.solarsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.planetaryposition.SolarSystemRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SolarSystemViewModel @Inject constructor(
    private val solarSystemRepository: SolarSystemRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SolarSystemUiState>(SolarSystemUiState.Loading)
    val uiState: StateFlow<SolarSystemUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location?.let {
                    val date = Utils.calculateJulianDateNow()

                    solarSystemRepository.getAllPlanets(it, date)
                        .catch { e ->
                            _uiState.value = SolarSystemUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val listFiltered = celestialObjPosList
                                .sortedWith(compareByDescending<CelestialObjPos> { it.observable })
                            _uiState.value = SolarSystemUiState.Success(listFiltered)
                        }
                }
            }
        }
    }
}

sealed class SolarSystemUiState {
    object Loading : SolarSystemUiState()
    data class Success(val data: List<CelestialObjPos>) : SolarSystemUiState()
    data class Error(val message: String) : SolarSystemUiState()
}