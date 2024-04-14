package com.jeffrwatts.stargazer.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor (
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InfoUiState(currentTime = "",
        currentDate = "",
        latitude = "",
        longitude = "",
        accuracy = "",
        altitude = "",
        polarisX = 0.0,
        polarisY = 0.0))
    val state: StateFlow<InfoUiState> = _state

    companion object {
        val POLARIS_RA = Utils.hmsToDegrees(2, 41, 39.0)
        val POLARIS_DEC = Utils.dmsToDegrees(89, 15, 51.0)

        val CELESTIAL_NORTH_POLE_RA = 0.0
        val CELESTIAL_NORTH_POLE_DEC = 90.0
    }
    init {
        observeDateTimeUpdates()
        observeLocationUpdates()
    }

    // Updates the time every second
    private fun observeDateTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                _state.value = _state.value.copy(
                    currentTime = getCurrentTime(),
                    currentDate = getCurrentDate()
                )
                delay(1000) // Delay for a second
            }
        }
    }

    // Observes location updates
    private fun observeLocationUpdates() {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location?.let {
                    // Update the UI state with the new location data
                    val (polarisX, polarisY) = updatePolarisCoords(it.latitude, it.longitude)

                    _state.update { currentState ->
                        currentState.copy(
                            latitude = Utils.decimalToDMS(it.latitude, "N", "S"),
                            longitude = Utils.decimalToDMS(it.longitude, "E", "W"),
                            accuracy = String.format("%.1f feet", it.accuracy * 3.28084),
                            altitude = String.format("%.1f feet", it.altitude * 3.28084),
                            polarisX = polarisX,
                            polarisY = polarisY
                        )
                    }
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun updatePolarisCoords(lat: Double, lon: Double): Pair<Double, Double> {
        val jdNow = Utils.calculateJulianDateNow()

        val (altPolaris, azmPolaris, _) = Utils.calculateAltAzm(POLARIS_RA, POLARIS_DEC, lat, lon, jdNow)
        val (altCNP, _, _) = Utils.calculateAltAzm(CELESTIAL_NORTH_POLE_RA, CELESTIAL_NORTH_POLE_DEC, lat, lon, jdNow)

        // Adjust polaris azimuth to be zero based.
        val polarisX = if (azmPolaris<180.0) azmPolaris else azmPolaris-360.0
        val polarisY = altCNP-altPolaris

        return Pair(polarisX, polarisY)
    }
}

data class InfoUiState(
    val currentTime: String,
    val currentDate: String,
    val latitude: String,
    val longitude: String,
    val accuracy: String,
    val altitude: String,
    val polarisX: Double,
    val polarisY: Double
)
