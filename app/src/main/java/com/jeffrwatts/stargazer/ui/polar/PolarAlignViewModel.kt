package com.jeffrwatts.stargazer.ui.polar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PolarAlignViewModel(
    private val repository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    companion object {
        const val LATITUDE = 19.639994  // Example: Kona's latitude
        const val LONGITUDE = -155.996926 // Example: Kona's longitude
    }

    private val _uiState = MutableStateFlow<PolarAlignUiState>(PolarAlignUiState.Loading)
    val uiState: StateFlow<PolarAlignUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            _uiState.value = PolarAlignUiState.Loading

            runCatching {
                val jdNow = Utils.calculateJulianDateNow()
                val objects = repository.getAllByTypeStream(ObjectType.STAR).first()
                objects.map { obj ->
                    CelestialObjPos.fromCelestialObj(obj, julianDate = jdNow, lat = LATITUDE, lon = LONGITUDE)
                }.sortedByDescending { it.polarAlignCandidate }  // Sort by polarAlignCandidate
            }.onSuccess { _uiState.value = PolarAlignUiState.Success(it) }
                .onFailure { _uiState.value = PolarAlignUiState.Error(it.message ?: "Unknown error") }
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
