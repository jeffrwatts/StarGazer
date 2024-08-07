package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.variablestar

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.ASTRONOMICAL_TWILIGHT_ANGLE
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.EphemerisUtils
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
import java.time.LocalTime
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
                    val nowAdjusted = LocalDateTime.now().plusDays(timeOffset)
                    val currentTime = nowAdjusted.format(DATE_TIME_FORMATTER)
                    location?.let { loc->
                        val (start, end, isNight) = getRange(nowAdjusted, loc)
                        val nightStart = Utils.julianDateToLocalTime(start).format(DATE_TIME_FORMATTER)
                        val nightEnd = Utils.julianDateToLocalTime(end).format(DATE_TIME_FORMATTER)

                        val variableStarEventList = variableStarObjs.mapNotNull { varStarObj ->
                            getEventTime(varStarObj, start, end).takeIf { it != 0.0 }?.let { eventTimeJulianDate ->
                                val variableStarObjPos = VariableStarObjPos.fromVariableStarObj(varStarObj, eventTimeJulianDate, loc.latitude, loc.longitude)
                                val eventTime = Utils.julianDateToLocalTime(eventTimeJulianDate).format(DATE_TIME_FORMATTER)
                                val utcTime = Utils.julianDateToUTC(eventTimeJulianDate).format(DATE_TIME_FORMATTER)
                                VariableStarEvent(variableStarObjPos, eventTime, utcTime)
                            }
                        }

                        _uiState.value = VariableStarPlannerUiState.Success(currentTime, nightStart, nightEnd, isNight, variableStarEventList)
                    } ?: run {
                        _uiState.value = VariableStarPlannerUiState.Success(currentTime, "", "", false, emptyList())
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

    private fun getRange(now: LocalDateTime, location: Location): Triple<Double, Double, Boolean> {
        val nowJulian = Utils.calculateJulianDateFromLocal(now)
        val tomorrow: LocalDateTime = now.plusDays(1).with(LocalTime.MIDNIGHT)
        val yesterday: LocalDateTime = now.plusDays(-1).with(LocalTime.MIDNIGHT)

        val todayNightEnd = EphemerisUtils.calculateRiseSetUtc(now.year, now.monthValue, now.dayOfMonth,
            location.latitude, location.longitude, true, ASTRONOMICAL_TWILIGHT_ANGLE)

        val tomorrowNightEnd = EphemerisUtils.calculateRiseSetUtc(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth,
            location.latitude, location.longitude, true, ASTRONOMICAL_TWILIGHT_ANGLE)

        val yesterdayNightStart = EphemerisUtils.calculateRiseSetUtc(yesterday.year, yesterday.monthValue, yesterday.dayOfMonth,
            location.latitude, location.longitude, false, ASTRONOMICAL_TWILIGHT_ANGLE)

        val todayNightStart = EphemerisUtils.calculateRiseSetUtc(now.year, now.monthValue, now.dayOfMonth,
            location.latitude, location.longitude, false, ASTRONOMICAL_TWILIGHT_ANGLE)

        val nightStart: Double
        val nightEnd: Double

        if (nowJulian > todayNightEnd) {
            nightStart = todayNightStart
            nightEnd = tomorrowNightEnd
        } else {
            nightStart = yesterdayNightStart
            nightEnd = todayNightEnd
        }

        val isNight = (nowJulian > nightStart) && (nowJulian < nightEnd)

        return Triple(nightStart, nightEnd, isNight)
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
        val currentTime: String,
        val nightStart: String,
        val nightEnd: String,
        val isNight: Boolean,
        val variableStarEvents: List<VariableStarEvent>
    ) : VariableStarPlannerUiState()
    data class Error(val message: String) : VariableStarPlannerUiState()
}