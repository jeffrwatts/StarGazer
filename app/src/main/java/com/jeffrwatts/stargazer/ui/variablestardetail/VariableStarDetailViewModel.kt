package com.jeffrwatts.stargazer.ui.variablestardetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.cosinekitty.astronomy.Body
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

    fun initDetail(sightId: Int, observationTime: LocalDateTime) {
        viewModelScope.launch {
            try {
                combine(
                    variableStarObjRepository.getVariableStarObj(sightId),
                    locationRepository.locationFlow
                ) { variableStarObj, location ->
                    location?.let { loc->
                        val jdNow = Utils.calculateJulianDateFromLocal(observationTime)
                        val (start, stop, _) = Utils.getNight(jdNow, loc)
                        val altitudeData = Utils.calculateDSOAltitudes(variableStarObj.ra, variableStarObj.dec, location, start, stop)
                        val currentTimeIndex = Utils.findClosestIndex(jdNow, altitudeData)
                        val xAxisLabels= Utils.getXAxisLabels(start, stop)
                        val moonAltitudeData = Utils.calculatePlanetAltitudes(Body.Moon, loc, start, stop)

                        _uiState.value = VariableStarDetailUiState.Success(variableStarObj, currentTimeIndex, altitudeData, moonAltitudeData, xAxisLabels)
                    }?:run{
                        _uiState.value = VariableStarDetailUiState.Success(variableStarObj, -1, emptyList(), emptyList(),  emptyList())
                    }
                }.collect()
            } catch (e: Exception) {
                _uiState.value = VariableStarDetailUiState.Error("Error loading data")
            }
        }
    }
}



sealed class VariableStarDetailUiState {
    object Loading : VariableStarDetailUiState()
    data class Success(val variableStarObj: VariableStarObj,
                       val currentTimeIndex: Int,
                       val altitudeData: List<Pair<Double, Double>>,
                       val moonAltitudeData: List<Pair<Double, Double>>,
                       val xAxisLabels: List<String>) : VariableStarDetailUiState()
    data class Error(val message: String) : VariableStarDetailUiState()
}