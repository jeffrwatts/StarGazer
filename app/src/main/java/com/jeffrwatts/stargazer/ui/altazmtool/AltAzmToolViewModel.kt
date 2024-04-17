package com.jeffrwatts.stargazer.ui.altazmtool


import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AltAzmToolViewModel @Inject constructor(
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
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StarFinderUIState(0.0, 0.0, 0.0, 0.0, SensorManager.SENSOR_STATUS_UNRELIABLE)
    )

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
    val accuracy: Int,
)
