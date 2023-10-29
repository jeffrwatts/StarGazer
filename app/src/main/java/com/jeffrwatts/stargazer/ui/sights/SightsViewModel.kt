package com.jeffrwatts.stargazer.ui.sights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SightsViewModel(private val repository: CelestialObjRepository) : ViewModel() {
    companion object {
        const val LATITUDE = 19.639994  // Example: Kona's latitude
        const val LONGITUDE = -155.996926 // Example: Kona's longitude
    }

    private val _uiState = MutableStateFlow<SightsUiState>(SightsUiState.Loading)
    val uiState: StateFlow<SightsUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            _uiState.value = SightsUiState.Loading

            runCatching {
                val jdNow = Utils.calculateJulianDateNow();

                val objects = repository.getAllStream().first()
                    .filterNot { it.type == ObjectType.STAR }
                objects.map { obj ->
                    CelestialObjPos.fromCelestialObj(obj, julianDate = jdNow, lat = LATITUDE, lon = LONGITUDE)
                }.sortedByDescending { it.alt }
            }.onSuccess { _uiState.value = SightsUiState.Success(it) }
                .onFailure { _uiState.value = SightsUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun updateObservationStatus(celestialObj: CelestialObj, newObservationStatus: ObservationStatus) {
        viewModelScope.launch {
            val updatedItem = celestialObj.copy(observationStatus = newObservationStatus)
            repository.update(updatedItem)
        }
    }
}

sealed class SightsUiState {
    object Loading : SightsUiState()
    data class Success(val data: List<CelestialObjPos>) : SightsUiState()
    data class Error(val message: String) : SightsUiState()
}

