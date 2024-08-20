package com.jeffrwatts.stargazer.ui.variablestardetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                combine(
                    variableStarObjRepository.getVariableStarObj(sightId),
                    locationRepository.locationFlow
                ) { variableStarObj, location ->
                    var altitudes = emptyList<Utils.AltitudeEntry>()
                    location?.let { loc->
                        altitudes = generateAltitudes(variableStarObj, loc)
                    }
                    _uiState.value = VariableStarDetailUiState.Success(variableStarObj, altitudes)
                }.collect()
            } catch (e: Exception) {
                _uiState.value = VariableStarDetailUiState.Error("Error loading data")
            }
        }
    }

    private fun generateAltitudes(variableStarObj: VariableStarObj, location: Location): List<Utils.AltitudeEntry> {
        val timeStart = LocalDateTime.now().minusHours(2).withMinute(0).withSecond(0)
        val durationHours = 24L
        val incrementMinutes = 10L
        val altitudeData = mutableListOf<Utils.AltitudeEntry>()

        // Set up the start time to be 2 hours before at 0 min and 0 sec.
        var timeIx = timeStart
        val endTime = timeIx.plusHours(durationHours)

        while (timeIx.isBefore(endTime)) {
            val julianDate = Utils.calculateJulianDateFromLocal(timeIx)
            val variableStarObjPos = VariableStarObjPos.fromVariableStarObj(variableStarObj, julianDate, location)
            altitudeData.add(Utils.AltitudeEntry(timeIx, variableStarObjPos.alt))
            timeIx = timeIx.plusMinutes(incrementMinutes)
        }

        return altitudeData
    }

}



sealed class VariableStarDetailUiState {
    object Loading : VariableStarDetailUiState()
    data class Success(val data: VariableStarObj, val altitudes: List<Utils.AltitudeEntry>) : VariableStarDetailUiState()
    data class Error(val message: String) : VariableStarDetailUiState()
}