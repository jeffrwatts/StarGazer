package com.jeffrwatts.stargazer.ui.celestialobjdetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import com.jeffrwatts.stargazer.data.celestialobject.MOON
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.mapBody
import com.jeffrwatts.stargazer.data.location.LocationRepository
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
                        val altitudeData = calculateAltitudes(celestialObjWithImage, loc, start, stop)
                        val currentTimeIndex = Utils.findClosestIndex(jdNow, altitudeData)
                        val xAxisLabels= Utils.getXAxisLabels(start, stop)

                        var moonAltitudeData = emptyList<Pair<Double, Double>>()

                        if (celestialObjWithImage.celestialObj.objectId != MOON) {
                            moonAltitudeData = Utils.calculatePlanetAltitudes(Body.Moon, loc, start, stop)
                        }

                        _uiState.value = CelestialObjDetailUiState.Success(celestialObjWithImage, currentTimeIndex, altitudeData, moonAltitudeData, xAxisLabels)
                    }?:run{
                        _uiState.value = CelestialObjDetailUiState.Success(celestialObjWithImage, -1, emptyList(), emptyList(), emptyList())
                    }
                }.collect()
            } catch (e: Exception) {
                _uiState.value = CelestialObjDetailUiState.Error("Error loading data")
            }
        }
    }

    private fun calculateAltitudes(obj: CelestialObjWithImage, location: Location, startTime: Double, stopTime:Double): List<Pair<Double, Double>> {
        var altitudes = emptyList<Pair<Double, Double>>()

        if (obj.celestialObj.type == ObjectType.PLANET) {
            mapBody(obj.celestialObj.objectId)?.let { body->
                altitudes = Utils.calculatePlanetAltitudes(body, location, startTime, stopTime)
            }
        } else {
            altitudes = Utils.calculateDSOAltitudes(obj.celestialObj.ra, obj.celestialObj.dec, location, startTime, stopTime)
        }

        return altitudes
    }
}

sealed class CelestialObjDetailUiState {
    object Loading : CelestialObjDetailUiState()
    data class Success(val celestialObjWithImage: CelestialObjWithImage,
                       val currentTimeIndex: Int,
                       val altitudeData: List<Pair<Double, Double>>,
                       val moonAltitudeData: List<Pair<Double, Double>>,
                       val xAxisLabels: List<String>) : CelestialObjDetailUiState()
    data class Error(val message: String) : CelestialObjDetailUiState()
}
