package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestardetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class VariableStarDetailViewModel @Inject constructor(
    private val variableStarObjRepository: VariableStarObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<VariableStarDetailUiState>(VariableStarDetailUiState.Loading)
    val uiState: StateFlow<VariableStarDetailUiState> = _uiState.asStateFlow()

    fun fetchVariableStar(variableStarId: Int) {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location->
                location?.let {
                    val date = Utils.calculateJulianDateNow()
                    variableStarObjRepository.getVariableStarObj(variableStarId, location, date).collect { variableStarObjPos ->

                        // Set up the start time to be 2 hours before at 0 min and 0 sec.
                        val timeStart = LocalDateTime.now()
                            .minusHours(2)
                            .withMinute(0)
                            .withSecond(0)

                        val altitudeEntries = Utils.getAltitudeEntries(
                            variableStarObjPos.variableStarObj.ra,
                            variableStarObjPos.variableStarObj.dec,
                            location,
                            timeStart,
                            24,
                            10)

                        _uiState.value = VariableStarDetailUiState.Success(variableStarObjPos, altitudeEntries)
                    }
                }
            }
        }
    }
}



sealed class VariableStarDetailUiState {
    object Loading : VariableStarDetailUiState()
    data class Success(val data: VariableStarObjPos, val altitudes: List<Utils.AltitudeEntry>) : VariableStarDetailUiState()
    data class Error(val message: String) : VariableStarDetailUiState()
}