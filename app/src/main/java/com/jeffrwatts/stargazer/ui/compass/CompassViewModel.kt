package com.jeffrwatts.stargazer.ui.compass


import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.compass.CompassData
import com.jeffrwatts.stargazer.data.compass.CompassRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor(
    private val compassRepository: CompassRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val compassDataFlow = compassRepository.compassData
    private val locationFlow = locationRepository.locationFlow

    val uiState: StateFlow<CompassUIState> = combine(compassDataFlow, locationFlow) { compassData, location ->
        val declination = location?.let { calculateDeclination(it) } ?: 0f
        val isDeclinationValid = location != null
        CompassUIState(compassData, declination, isDeclinationValid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompassUIState(CompassData(0.0, SensorManager.SENSOR_STATUS_UNRELIABLE), 0f, false))

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
    val compassData: CompassData,
    val magDeclination: Float,
    val isMagDeclinationValid: Boolean
)
