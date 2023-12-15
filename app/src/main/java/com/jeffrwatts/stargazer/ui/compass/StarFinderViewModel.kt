package com.jeffrwatts.stargazer.ui.compass


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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
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
        val declination = location?.let { calculateDeclination(it) } ?: 0f
        val adjustedAzimuth = normalizeAzimuth(orientationData.azimuth + declination)
        StarFinderUIState(
            orientationData.copy(azimuth = adjustedAzimuth),
            declination,
            location != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StarFinderUIState(OrientationData(0.0, 0.0, SensorManager.SENSOR_STATUS_UNRELIABLE), 0f, false))

    val _foundObjects = MutableStateFlow<List<CelestialObj>> (emptyList())
    val foundObjects = _foundObjects.asStateFlow()

    private val _searchCompleted = MutableStateFlow(false)
    val searchCompleted: StateFlow<Boolean> = _searchCompleted.asStateFlow()

    fun findObjects(alt: Double, azimuth: Double) {
        viewModelScope.launch {
            val location = locationRepository.locationFlow.value?: return@launch

            val timeNow = Utils.calculateJulianDateNow()
            val (ra, dec) = Utils.calculateRAandDEC(alt, azimuth, location.latitude, location.longitude, timeNow)

            val threshold = 5.0
            val objs = celestialObjRepository.getCelestialObjsByRaDec(ra, dec, threshold).firstOrNull()
            _foundObjects.value = objs ?: emptyList()
            _searchCompleted.value = true
        }
    }

    fun clearFoundObjects() {
        _foundObjects.value = emptyList()
        _searchCompleted.value = false
    }

    private fun normalizeAzimuth(azimuth: Double): Double {
        var normalizedAzimuth = azimuth % 360
        if (normalizedAzimuth < 0) normalizedAzimuth += 360
        return normalizedAzimuth
    }

    private fun calculateDeclination(location: Location): Float {
        val geomagneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        return geomagneticField.declination
    }
}

data class StarFinderUIState(
    val orientationData: OrientationData,
    val magDeclination: Float,
    val isMagDeclinationValid: Boolean
)
