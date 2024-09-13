package com.jeffrwatts.stargazer.ui.celestialobjdetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CelestialObjDetailViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<CelestialObjDetailUiState>(CelestialObjDetailUiState.Loading)
    val uiState: StateFlow<CelestialObjDetailUiState> = _uiState.asStateFlow()

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                combine(
                    celestialObjRepository.getCelestialObj(sightId),
                    locationRepository.locationFlow
                ) { celestialObjWithImage, location ->
                    location?.let { loc->
                        val jdNow = Utils.calculateJulianDateFromLocal(LocalDateTime.now())
                        val (start, stop, _) = Utils.getNight(jdNow, loc)
                        val altitudes = calculateAltitudes(celestialObjWithImage, loc, start, stop)
                        val currentTimeIndex = findClosestIndex(jdNow, altitudes)
                        val xAxisLabels= getXAxisLabels(start, stop)
                        _uiState.value = CelestialObjDetailUiState.Success(celestialObjWithImage, currentTimeIndex, altitudes, xAxisLabels)
                    }?:run{
                        _uiState.value = CelestialObjDetailUiState.Success(celestialObjWithImage, -1, emptyList(), emptyList())
                    }
                }.collect()
            } catch (e: Exception) {
                _uiState.value = CelestialObjDetailUiState.Error("Error loading data")
            }
        }
    }

    private fun calculateAltitudes(obj: CelestialObjWithImage, location: Location, startTime: Double, stopTime:Double): List<Pair<Double, Double>> {
        val altitudeData = mutableListOf<Pair<Double, Double>>()
        val incrementMinutes = 10/24.0/60.0 // 10 minutes

        var timeIx = startTime

        while (timeIx < stopTime) {
            val celestialObjPos = CelestialObjPos.fromCelestialObjWithImage(obj, timeIx, location)
            altitudeData.add(Pair(timeIx, celestialObjPos.alt))
            timeIx += incrementMinutes
        }
        return altitudeData
    }

    fun findClosestIndex(currentJulianTime: Double, altitudeData: List<Pair<Double, Double>>): Int {
        if (altitudeData.isEmpty() || currentJulianTime < altitudeData.first().first || currentJulianTime>altitudeData.last().first) {
            return -1 // Return -1 if the list is empty or current time is before the first entry
        }

        return altitudeData.indices.minByOrNull { index ->
            kotlin.math.abs(altitudeData[index].first - currentJulianTime)
        } ?: -1 // Find the index with the closest Julian time
    }


    private fun getXAxisLabels(
        startJulianDate: Double,
        endJulianDate: Double
    ): List<String> {
        val numLabels = 5
        val totalDuration = endJulianDate - startJulianDate
        val step = totalDuration / (numLabels - 1) // Divide into equal parts

        return List(numLabels) { index ->
            val julianDate = startJulianDate + index * step
            val localDateTime = Utils.julianDateToLocalTime(julianDate)

            localDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        }
    }
}

sealed class CelestialObjDetailUiState {
    object Loading : CelestialObjDetailUiState()
    data class Success(val celestialObjWithImage: CelestialObjWithImage,
                       val currentTimeIndex: Int,
                       val altitudes: List<Pair<Double, Double>>,
                       val xAxisLabels: List<String>) : CelestialObjDetailUiState()
    data class Error(val message: String) : CelestialObjDetailUiState()
}
