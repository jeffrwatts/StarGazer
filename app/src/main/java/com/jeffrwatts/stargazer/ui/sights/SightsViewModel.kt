package com.jeffrwatts.stargazer.ui.sights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SightsViewModel (
    savedStateHandle: SavedStateHandle,
    private val celestialObjRepository: CelestialObjRepository
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }

    private val _sightsUiState = MutableStateFlow<SightsUiState>(SightsUiState.Loading)
    val sightsUiState: StateFlow<SightsUiState> = _sightsUiState

    init {
        // Initially load items without force refresh
        refreshItems(forceRefresh = false)
    }

    fun refreshItems(forceRefresh: Boolean) {
        // Emit a loading state when a refresh begins
        _sightsUiState.value = SightsUiState.Loading

        // Hardcoded values
        val latitude = 19.639994
        val longitude = -155.996926
        val utcNow = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime();

        val julianDate = Utils.calculateJulianDate(utcNow)

        viewModelScope.launch {
            try {
                val itemsFlow = celestialObjRepository.getAllStream(forceRefresh)
                itemsFlow.collect {items->
                    val enhancedItems = items.map { item ->
                        // Calculate Alt and Azm for the item using the Utils
                        val (alt, azm) = Utils.calculateAltAzm(item.ra, item.dec, latitude, longitude, julianDate)
                        // Return an EnhancedItem object
                        EnhancedCelestialObj(item, alt, azm)
                    }
                    _sightsUiState.value = SightsUiState.Success(enhancedItems)                }
            } catch (e: Exception) {
                // Emit an error state if refreshing fails
                _sightsUiState.value = SightsUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
    fun updateObservationStatus(enhancedCelestialObj: EnhancedCelestialObj, newObservationStatus: ObservationStatus) {
        viewModelScope.launch {
            val updatedItem = enhancedCelestialObj.celestialObj.copy(observationStatus = newObservationStatus)
            celestialObjRepository.update(updatedItem)
        }
    }
}

data class EnhancedCelestialObj(
    val celestialObj: CelestialObj,
    val alt: Double,
    val azm: Double
)

sealed class SightsUiState {
    object Loading : SightsUiState()
    data class Success(val enhancedItemList: List<EnhancedCelestialObj>) : SightsUiState()
    data class Error(val message: String) : SightsUiState()
}
