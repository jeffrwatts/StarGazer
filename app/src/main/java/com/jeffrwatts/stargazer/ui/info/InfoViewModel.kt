package com.jeffrwatts.stargazer.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InfoViewModel (
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InfoUiState("", "", "", "", 0.0, 0.0))
    val state: StateFlow<InfoUiState> = _state

    val locationFlow = locationRepository.locationFlow

    companion object {
        const val LATITUDE = 19.639994  // Example: Kona's latitude
        const val LONGITUDE = -155.996926 // Example: Kona's longitude

        val POLARIS_RA = Utils.hmsToDegrees(2, 41, 39.0)
        val POLARIS_DEC = Utils.dmsToDegrees(89, 15, 51.0)

        val CELESTIAL_NORTH_POLE_RA = 0.0
        val CELESTIAL_NORTH_POLE_DEC = 90.0
    }
    init {
        observeDateTimeUpdates()
        observeLocationUpdates()
        updateLocationAndPolarCoords()
        locationRepository.startLocationUpdates(viewModelScope)
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
                    it.accuracy
                    // Update the UI state with the new location data
                    val newLatitude = Utils.decimalToDMS(it.latitude, "N", "S")
                    val newLongitude = Utils.decimalToDMS(it.longitude, "E", "W")

                    _state.update { currentState ->
                        currentState.copy(
                            latitude = newLatitude,
                            longitude = newLongitude,
                        )
                    }
                }
            }
        }
    }

    // Updates the polar coordinates and location every minute
    private fun updateLocationAndPolarCoords() {
        viewModelScope.launch {
            while (true) {
                // Placeholder logic for lat, lon, polarisX, and polarisY
                val lat = locationFlow.value?.latitude ?: 0.0
                val lon = locationFlow.value?.longitude ?: 0.0

                val newLatitude = Utils.decimalToDMS(lat, "N", "S")
                val newLongitude = Utils.decimalToDMS(lon, "E", "W")
                val (polarisX, polarisY) = updatePolarisCoords()

                _state.value = _state.value.copy(
                    latitude = newLatitude,
                    longitude = newLongitude,
                    polarisX = polarisX,
                    polarisY = polarisY
                )
                delay(60_000) // Delay for a minute
            }
        }
    }

    fun toggleHorizontalFlip() {
        _state.value = _state.value.copy(isHorizontalFlip = !_state.value.isHorizontalFlip)
    }

    fun toggleVerticalFlip() {
        _state.value = _state.value.copy(isVerticalFlip = !_state.value.isVerticalFlip)
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun updatePolarisCoords(): Pair<Double, Double> {
        val jdNow = Utils.calculateJulianDateNow()

        val (altPolaris, azmPolaris, _) = Utils.calculatePosition(POLARIS_RA, POLARIS_DEC, LATITUDE, LONGITUDE, jdNow)
        val (altCNP, _, _) = Utils.calculatePosition(CELESTIAL_NORTH_POLE_RA, CELESTIAL_NORTH_POLE_DEC, LATITUDE, LONGITUDE, jdNow)

        // Adjust polaris azimuth to be zero based.
        val polarisX = if (azmPolaris<180.0) azmPolaris else azmPolaris-360.0
        val polarisY = altCNP-altPolaris

        return Pair(polarisX, polarisY)
    }

    override fun onCleared() {
        locationRepository.stopLocationUpdates()
        super.onCleared()
    }
}

data class InfoUiState(
    val currentTime: String,
    val currentDate: String,
    val latitude: String,
    val longitude: String,
    val polarisX: Double,
    val polarisY: Double,
    val isHorizontalFlip: Boolean = true,
    val isVerticalFlip: Boolean = false
)
