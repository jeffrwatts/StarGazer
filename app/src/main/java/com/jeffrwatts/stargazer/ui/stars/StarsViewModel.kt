package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.stars

import android.util.Log
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjPos
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.celestialobject.JUPITER
import com.jeffrwatts.stargazer.data.celestialobject.MARS
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.SATURN
import com.jeffrwatts.stargazer.data.celestialobject.VENUS
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.AppConstants
import com.jeffrwatts.stargazer.utils.Utils
import com.jeffrwatts.stargazer.utils.isWithinRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class StarsViewModel @Inject constructor(
    private val starsRepository: StarObjRepository,
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _filter = MutableStateFlow(StarFilter.ALL)
    val filter: StateFlow<StarFilter> = _filter

    private val _timeOffset = MutableStateFlow(0L)
    private val _pullToRefresh = MutableSharedFlow<Unit>(replay = 1)

    private val _uiState = MutableStateFlow<StarsUiState>(StarsUiState.Loading)
    val uiState: StateFlow<StarsUiState> = _uiState.asStateFlow()

    companion object {
        const val AZM_RANGE = 40.0
        const val ALT_LOW = 40.0
        const val ALT_HIGH = 70.0
        const val MAGNITUDE_LOW = 1.0
        const val MAGNITUDE_HIGH = 5.0
    }

    init {
        viewModelScope.launch {
            _pullToRefresh.emit(Unit) // Emit an initial value to start the process
            combine(
                starsRepository.getStars(),
                locationRepository.locationFlow,
                planetPositionFlow(),
                _timeOffset,
                _pullToRefresh
            ) { starObjs, location, planetFilter, timeOffset, _ ->
                try {
                    val date = if (timeOffset != 0L) {
                        LocalDateTime.now().plusHours(timeOffset).withMinute(0)
                    } else {
                        LocalDateTime.now()
                    }

                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    location?.let {loc->
                        val planetAzm = planetFilter?.let { planet ->
                            CelestialObjPos.fromCelestialObjWithImage(planet, julianDate, loc).azm
                        }

                        val starObjPosList = starObjs
                            .map {starObj->
                                StarObjPos.fromStarObj(starObj, julianDate, location)
                            }
                            .filter {
                                val isInAzm = planetAzm?.let { azm -> isWithinRange(azm, it.azm, AZM_RANGE) } ?: true
                                val isInMag = it.starObj.magnitude in MAGNITUDE_LOW .. MAGNITUDE_HIGH
                                isInAzm && it.alt >= ALT_LOW && isInMag}
                            .sortedBy { it.starObj.magnitude }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun planetPositionFlow(): Flow<CelestialObjWithImage?> {
        return _filter.flatMapLatest { filter ->
            when (filter) {
                StarFilter.NEAR_VENUS -> celestialObjRepository.getCelestialObjByObjectId(VENUS)
                StarFilter.NEAR_MARS -> celestialObjRepository.getCelestialObjByObjectId(MARS)
                StarFilter.NEAR_JUPITER -> celestialObjRepository.getCelestialObjByObjectId(JUPITER)
                StarFilter.NEAR_SATURN -> celestialObjRepository.getCelestialObjByObjectId(SATURN)
                StarFilter.ALL -> flowOf(null)
            }
        }
    }

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun setFilter(filter: StarFilter) {
        viewModelScope.launch {
            _filter.value = filter
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

enum class StarFilter {
    NEAR_VENUS,
    NEAR_MARS,
    NEAR_JUPITER,
    NEAR_SATURN,
    ALL
}

sealed class StarsUiState {
    object Loading : StarsUiState()
    data class Success(val data: List<StarObjPos>,
                       val locationAvailable: Boolean,
                       val currentTime: String) : StarsUiState()
    data class Error(val message: String) : StarsUiState()
}