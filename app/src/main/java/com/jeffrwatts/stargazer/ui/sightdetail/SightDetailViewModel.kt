package com.jeffrwatts.stargazer.ui.sightdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SightDetailViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SightDetailUiState>(SightDetailUiState.Loading)
    val uiState: StateFlow<SightDetailUiState> = _uiState.asStateFlow()

    fun fetchSightDetail(sightId: Int) {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    celestialObjRepository.getCelestialObj(sightId, location, date).collect { celestialObjPos ->
                        celestialObjPos?.let {
                            _uiState.value = SightDetailUiState.Success(it.celestialObj)
                        } ?: run {
                            _uiState.value = SightDetailUiState.Error("Object not found.")
                        }
                    }
                }
            }
        }
    }
}


sealed class SightDetailUiState {
    object Loading : SightDetailUiState()
    data class Success(val data: CelestialObj) : SightDetailUiState()
    data class Error(val message: String) : SightDetailUiState()
}