package com.jeffrwatts.stargazer.ui.starfinder


import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.orientation.OrientationData
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StarFinderViewModel @Inject constructor(
    private val celestialObjRepository: CelestialObjRepository,
    private val orientationRepository: OrientationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {


    val uiState: StateFlow<StarFinderUIState> = combine(
        orientationRepository.orientationData,
        locationRepository.locationFlow
    ) { orientationData, location ->
        val magDeclination = location?.let { calculateDeclination(it) } ?: 0.0
        val trueAzimuth = calculateTrueAzimuth(orientationData.azimuth, magDeclination)
        StarFinderUIState(orientationData.altitude, orientationData.azimuth, trueAzimuth, magDeclination, orientationData.accuracy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        StarFinderUIState(0.0, 0.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_UNRELIABLE))

    val _foundObjects = MutableStateFlow<List<CelestialObj>> (emptyList())
    val foundObjects = _foundObjects.asStateFlow()

    private val _searchCompleted = MutableStateFlow(false)
    val searchCompleted: StateFlow<Boolean> = _searchCompleted.asStateFlow()

    fun findObjects(altitude: Double, azimuth: Double) {
        viewModelScope.launch {
            Log.d("TAG", "altitude:${altitude}, azimuth:${azimuth}")

            val location = locationRepository.locationFlow.value?: return@launch

            val timeNow = Utils.calculateJulianDateNow()
            val (ra, dec) = Utils.calculateRAandDEC(altitude, azimuth, location.latitude, location.longitude, timeNow)

            val objs = celestialObjRepository.getCelestialObjsByRaDec(ra, dec, timeNow).firstOrNull()
            _foundObjects.value = objs ?: emptyList()
            _searchCompleted.value = true
        }
    }

    fun clearFoundObjects() {
        _foundObjects.value = emptyList()
        _searchCompleted.value = false
    }

    private var searchJob: Job? = null

    fun startContinuousSearching() {
        searchJob = viewModelScope.launch {
            while (isActive) {
                val orientationData = orientationRepository.orientationData.firstOrNull() ?: continue
                val location = locationRepository.locationFlow.firstOrNull() ?: continue

                findObjects(orientationData.altitude, orientationData.azimuth)
                delay(1000) // Delay for 5 seconds before searching again
            }
        }
    }

    fun stopContinuousSearching() {
        searchJob?.cancel()
    }

    private fun calculateTrueAzimuth(azimuth: Double, magDeclination: Double): Double {
        var trueAzimuth = (azimuth + magDeclination) % 360
        if (trueAzimuth < 0) {
            trueAzimuth += 360
        }
        return trueAzimuth
    }

    private fun calculateDeclination(location: Location): Double {
        val geomagneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        return geomagneticField.declination.toDouble()
    }
}

data class StarFinderUIState(
    val altitude: Double,
    val azimuth: Double,
    val trueAzimuth: Double,
    val magDeclination: Double,
    val accuracy: Int
)
