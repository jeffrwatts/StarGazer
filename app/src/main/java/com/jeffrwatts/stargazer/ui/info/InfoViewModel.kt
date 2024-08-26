package com.jeffrwatts.stargazer.ui.info

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor (
    private val locationRepository: LocationRepository
) : ViewModel() {

    companion object {
        val POLARIS_RA = Utils.hmsToDegrees(2, 41, 39.0)
    }

    private val _uiState = MutableStateFlow<InfoUiState>(InfoUiState.Loading)
    val uiState: StateFlow<InfoUiState> = _uiState.asStateFlow()

    private val _timeOffset = MutableStateFlow(0L)

    init {
        val timerFlow = flow {
            while (true) {
                emit(Unit)
                delay(1000L) // Emit every second
            }
        }

        viewModelScope.launch {
            combine(
                locationRepository.locationFlow,
                _timeOffset,
                timerFlow
            ) { location, timeOffset, _ ->
                try {
                    val date = LocalDateTime.now().plusHours(timeOffset)
                    val julianDate = Utils.calculateJulianDateFromLocal(date)
                    var lhaPolaris = 0.0
                    location?.let {loc->
                        val lst = Utils.calculateLocalSiderealTime(loc.longitude, julianDate)
                        lhaPolaris = Utils.calculateLocalHourAngle(lst, POLARIS_RA)
                    }
                    _uiState.value = InfoUiState.Success(date, location, lhaPolaris)
                } catch (e: Exception) {
                    _uiState.value = InfoUiState.Error(e.message ?: "Unknown error")
                }
            }.collect()
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

sealed class InfoUiState {
    object Loading : InfoUiState()
    data class Success(val localDateTime: LocalDateTime, val location: Location?, val lhaPolaris: Double) : InfoUiState()
    data class Error(val message: String) : InfoUiState()
}


