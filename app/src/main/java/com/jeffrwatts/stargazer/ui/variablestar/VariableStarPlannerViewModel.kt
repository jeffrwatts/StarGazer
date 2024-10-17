package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjPos
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.utils.AppConstants.DATE_TIME_FORMATTER
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.floor

@HiltViewModel
class VariableStarPlannerViewModel @Inject constructor(
    private val variableStarObjRepository: VariableStarObjRepository,
    private val locationRepository: LocationRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<VariableStarPlannerUiState>(VariableStarPlannerUiState.Loading)
    val uiState: StateFlow<VariableStarPlannerUiState> = _uiState

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
                    val date = if (timeOffset != 0L) {
                        LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                    } else {
                        LocalDateTime.now()
                    }
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let { loc->
                        val (start, end, isNight) = Utils.getNight(julianDate, loc)

                        val nightStart = Utils.julianDateToLocalTime(start).format(DATE_TIME_FORMATTER)
                        val nightEnd = Utils.julianDateToLocalTime(end).format(DATE_TIME_FORMATTER)

                        val variableStarEventList = variableStarObjs.mapNotNull { varStarObj ->
                            getEventTime(varStarObj, start, end).takeIf { eventTime-> eventTime != 0.0 }?.let { eventTimeJulianDate ->
                                VariableStarObjPos.fromVariableStarObj(varStarObj, eventTimeJulianDate, location).takeIf { it.observable }?.let {
                                    val eventTime = Utils.julianDateToLocalTime(eventTimeJulianDate).format(DATE_TIME_FORMATTER)
                                    val utcTime = Utils.julianDateToUTC(eventTimeJulianDate).format(DATE_TIME_FORMATTER)
                                    VariableStarEvent(it, eventTime, utcTime)
                                }
                            }
                        }

                        _uiState.value = VariableStarPlannerUiState.Success(date, nightStart, nightEnd, isNight, variableStarEventList)
                    } ?: run {
                        _uiState.value = VariableStarPlannerUiState.Success(date, "", "", false, emptyList())
                    }
                } catch (e:Exception) {
                    _uiState.value = VariableStarPlannerUiState.Error(e.message ?: "Unknown error")
                }
            }.collect()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit)
        }
    }

    fun incrementOffset(incrementBy: Int) {
        viewModelScope.launch {
            _timeOffset.update { it + incrementBy }
        }
    }

    fun decrementOffset(decrementBy: Int) {
        viewModelScope.launch {
            _timeOffset.update { it - decrementBy }
        }
    }

    fun resetOffset() {
        viewModelScope.launch {
            _timeOffset.emit(0L)
        }
    }

    private fun getEventTime (variableStarObj: VariableStarObj, nightStart: Double, nightEnd: Double): Double {
        val periodsSinceEpochStart = (nightStart-variableStarObj.epoch) / variableStarObj.period

        // Find the closest eclipse time after nightStart
        val nextEclipseAfterStart = variableStarObj.epoch + (floor(periodsSinceEpochStart) + 1) * variableStarObj.period

        // Check if the next eclipse after nightStart is before nightEnd
        return if (nextEclipseAfterStart in nightStart..nightEnd) {
            nextEclipseAfterStart
        } else {
            0.0
        }
    }
}

data class VariableStarEvent (
    val variableStarObjPos: VariableStarObjPos,
    val eventTime: String,
    val UTCTime: String
)

sealed class VariableStarPlannerUiState {
    object Loading : VariableStarPlannerUiState()
    data class Success(
        val currentTime: LocalDateTime,
        val nightStart: String,
        val nightEnd: String,
        val isNight: Boolean,
        val variableStarEvents: List<VariableStarEvent>
    ) : VariableStarPlannerUiState()
    data class Error(val message: String) : VariableStarPlannerUiState()
}