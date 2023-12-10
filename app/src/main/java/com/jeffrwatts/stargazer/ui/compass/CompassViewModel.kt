package com.jeffrwatts.stargazer.ui.compass


import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.orientation.OrientationData
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor(
    private val orientationRepository: OrientationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val orientationDataFlow = orientationRepository.orientationData
    private val locationFlow = locationRepository.locationFlow

    val uiState: StateFlow<CompassUIState> = combine(
        orientationDataFlow,
        locationFlow
    ) { orientationData, location ->
        val declination = location?.let { calculateDeclination(it) } ?: 0f
        val adjustedAzimuth = normalizeAzimuth(orientationData.azimuth + declination)
        CompassUIState(
            orientationData.copy(azimuth = adjustedAzimuth),
            declination,
            location != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompassUIState(OrientationData(0.0, 0.0, SensorManager.SENSOR_STATUS_UNRELIABLE), 0f, false))

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

data class CompassUIState(
    val orientationData: OrientationData,
    val magDeclination: Float,
    val isMagDeclinationValid: Boolean
)
