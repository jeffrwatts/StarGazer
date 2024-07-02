package com.jeffrwatts.stargazer.ui.variablestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.timeoffset.TimeOffsetRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class VariableStarViewModel @Inject constructor(
    private val variableStarObjRepository: VariableStarObjRepository,
    private val locationRepository: LocationRepository,
    private val timeOffsetRepository: TimeOffsetRepository
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
                    val currentTime = LocalDateTime.now().plusHours(timeOffsetRepository.getTimeOffset())
                    val date = Utils.calculateJulianDateFromLocal(currentTime)

                    variableStarObjRepository.getAllVariableStarObjs(location, date)
                        .distinctUntilChanged() // To avoid redundant UI updates
                        .catch { e ->
                            _uiState.value = VariableStarUiState.Error(e.message ?: "Unknown error")
                        }
                        .collect { celestialObjPosList ->
                            val listSorted = celestialObjPosList.sortedByDescending { it.alt }
                            _uiState.value = VariableStarUiState.Success(listSorted, currentTime.format(DATE_TIME_FORMATTER))
                        }
                }
            }
        }
    }

    fun incrementTime() {
        timeOffsetRepository.incrementTime()
        fetchObjects()
    }

    fun decrementTime() {
        timeOffsetRepository.decrementTime()
        fetchObjects()
    }

    fun resetTime() {
        timeOffsetRepository.resetTime()
        fetchObjects()
    }
}


sealed class VariableStarUiState {
    object Loading : VariableStarUiState()
    data class Success(val data: List<VariableStarObjPos>, val currentTime: String) : VariableStarUiState()
    data class Error(val message: String) : VariableStarUiState()
}