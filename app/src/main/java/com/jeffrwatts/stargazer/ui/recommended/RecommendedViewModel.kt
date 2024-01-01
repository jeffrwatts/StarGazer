package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.recommended

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendedViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendedUiState>(RecommendedUiState.Loading)
    val uiState: StateFlow<RecommendedUiState> = _uiState

    init {
        fetchObjects()
    }
    fun fetchObjects() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    val typesToQuery = listOf(ObjectType.CLUSTER, ObjectType.PLANET, ObjectType.NEBULA, ObjectType.GALAXY)

                    celestialObjRepository.getAllCelestialObjsByType(typesToQuery, location, date)
                        .distinctUntilChanged() // To avoid redundant UI updates
                        .catch { e ->
                            _uiState.value = RecommendedUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val listFiltered = celestialObjPosList
                                .filter { it.celestialObj.recommended }
                                .sortedWith(compareByDescending<CelestialObjPos> { it.observable })
                            _uiState.value = RecommendedUiState.Success(listFiltered)
                        }
                }
            }
        }
    }
}

sealed class RecommendedUiState {
    object Loading : RecommendedUiState()
    data class Success(val data: List<CelestialObjPos>) : RecommendedUiState()
    data class Error(val message: String) : RecommendedUiState()
}