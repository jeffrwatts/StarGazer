package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
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
class VariableStarViewModel @Inject constructor(
    private val variableStarObjRepository: VariableStarObjRepository,
    private val locationRepository: LocationRepository
):ViewModel() {
    private val _uiState = MutableStateFlow<VariableStarUiState>(VariableStarUiState.Loading)
    val uiState: StateFlow<VariableStarUiState> = _uiState

    init {
        fetchObjects()
    }

    fun fetchObjects() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()

                    variableStarObjRepository.getAllVariableStarObjs(location, date)
                        .distinctUntilChanged() // To avoid redundant UI updates
                        .catch { e ->
                            _uiState.value = VariableStarUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val listSorted = celestialObjPosList.sortedByDescending { it.alt }
                            _uiState.value = VariableStarUiState.Success(listSorted)
                        }
                }
            }
        }
    }
}


sealed class VariableStarUiState {
    object Loading : VariableStarUiState()
    data class Success(val data: List<VariableStarObjPos>) : VariableStarUiState()
    data class Error(val message: String) : VariableStarUiState()
}