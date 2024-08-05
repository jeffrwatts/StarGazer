package com.jeffrwatts.stargazer.ui.variablestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class VariableStarViewModel @Inject constructor(
    private val variableStarObjRepository: VariableStarObjRepository,
    private val locationRepository: LocationRepository
):ViewModel() {
    private val _uiState = MutableStateFlow<VariableStarUiState>(VariableStarUiState.Loading)
    val uiState: StateFlow<VariableStarUiState> = _uiState

    private val _timeOffset = MutableStateFlow(0L)
    private val _pullToRefresh = MutableSharedFlow<Unit>(replay = 1)

    init {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit) // Emit an initial value to start the process
            combine(
                variableStarObjRepository.getAllVariableStarObjs(),
                locationRepository.locationFlow,
                _timeOffset,
                _pullToRefresh
            ) { variableStarObjs, location, timeOffset, _ ->
                try {
                    val date = LocalDateTime.now().plusHours(timeOffset)
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let {loc->
                        val variableStarObjList = variableStarObjs
                            .map { variableStarObj->

                                VariableStarObjPos.fromVariableStarObj(variableStarObj, julianDate, loc.latitude, loc.longitude)
                            }
                            .sortedWith(compareByDescending { it.observable })
                        _uiState.value = VariableStarUiState.Success(variableStarObjList, true, date.format(DATE_TIME_FORMATTER))
                    }?: run {
                        _uiState.value = VariableStarUiState.Success(emptyList(), false, date.format(DATE_TIME_FORMATTER))
                    }

                } catch (e: Exception) {
                    _uiState.value = VariableStarUiState.Error(e.message ?: "Unknown error")
                }
            }.collect()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit)
        }
    }

    fun incrementOffset() {
        viewModelScope.launch {
            _timeOffset.update { it + 1 }
        }
    }

    fun decrementOffset() {
        viewModelScope.launch {
            _timeOffset.update { it - 1 }
        }
    }

    fun resetOffset() {
        viewModelScope.launch {
            _timeOffset.emit(0L)
        }
    }
}


sealed class VariableStarUiState {
    object Loading : VariableStarUiState()
    data class Success(val data: List<VariableStarObjPos>, val locationAvailable: Boolean, val currentTime: String) : VariableStarUiState()
    data class Error(val message: String) : VariableStarUiState()
}