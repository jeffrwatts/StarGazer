package com.jeffrwatts.stargazer.ui.sightdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SightDetailViewModel(
    private val repository: CelestialObjRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SightDetailUiState>(SightDetailUiState.Loading)
    val uiState: StateFlow<SightDetailUiState> = _uiState.asStateFlow()

    fun fetchSightDetail(sightId: Int) {
        viewModelScope.launch {
            repository.getStream(sightId).collect { celestialObj ->
                celestialObj?.let {
                    _uiState.value = SightDetailUiState.Success(it)
                } ?: run {
                    _uiState.value = SightDetailUiState.Error("Object not found.")
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