package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.stars

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjPos
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.AppConstants
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class StarsViewModel @Inject constructor(
    private val starsRepository: StarObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _recommendedFilter = MutableStateFlow(true)
    val recommendedFilter: StateFlow<Boolean> = _recommendedFilter.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)
    private val _pullToRefresh = MutableSharedFlow<Unit>(replay = 1)

    private val _uiState = MutableStateFlow<StarsUiState>(StarsUiState.Loading)
    val uiState: StateFlow<StarsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit) // Emit an initial value to start the process
            combine(
                starsRepository.getStars(),
                locationRepository.locationFlow,
                _recommendedFilter,
                _timeOffset,
                _pullToRefresh
            ) { starObjs, location, recommended, timeOffset, _ ->
                try {
                    val date = if (timeOffset != 0L) {
                        LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                    } else {
                        LocalDateTime.now()
                    }
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let {loc->
                        val starObjPosList = starObjs
                            .map {starObj->
                                StarObjPos.fromStarObj(starObj, julianDate, location)
                            }
                            .filter { it.alt in 40.0..70.0 && it.starObj.magnitude in 1.5..3.0 }
                            .sortedBy { it.starObj.magnitude }

                        val (nightStart, nightEnd, isNight) = Utils.getNight(julianDate, loc)
                        Log.d("TEST", "IsNight=$isNight - Night Start: ${Utils.julianDateToLocalTime(nightStart)}; Night End: ${Utils.julianDateToLocalTime(nightEnd)} ")

                        _uiState.value = StarsUiState.Success(starObjPosList, true, date.format(
                            AppConstants.DATE_TIME_FORMATTER
                        ))
                    }?: run {
                        _uiState.value = StarsUiState.Success(emptyList(), false, date.format(
                            AppConstants.DATE_TIME_FORMATTER
                        ))
                    }

                } catch (e: Exception) {
                    _uiState.value = StarsUiState.Error(e.message ?: "Unknown error")
                }
            }.collect()
        }
    }

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun setRecommendedFilter(recommended: Boolean) {
        viewModelScope.launch {
            _recommendedFilter.emit(recommended)
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
}

sealed class StarsUiState {
    object Loading : StarsUiState()
    data class Success(val data: List<StarObjPos>,
                       val locationAvailable: Boolean,
                       val currentTime: String) : StarsUiState()
    data class Error(val message: String) : StarsUiState()
}