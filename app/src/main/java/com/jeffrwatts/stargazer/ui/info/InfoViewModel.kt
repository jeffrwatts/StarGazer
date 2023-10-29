package com.jeffrwatts.stargazer.ui.info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class InfoViewModel : ViewModel() {
    private val _state = MutableStateFlow(InfoUiState("", "", "", "", 0.0, 0.0))
    val state: StateFlow<InfoUiState> = _state

    var offset = 0.0

    companion object {
        const val LATITUDE = 19.639994  // Example: Kona's latitude
        const val LONGITUDE = -155.996926 // Example: Kona's longitude

        val POLARIS_RA = Utils.hmsToDegrees(2, 41, 39.0)
        val POLARIS_DEC = Utils.dmsToDegrees(89, 15, 51.0)

        val CELESTIAL_NORTH_POLE_RA = 0.0
        val CELESTIAL_NORTH_POLE_DEC = 90.0
    }
    init {
        updateDateTime()
        updateLocationAndPolarCoords()
    }

    // Updates the time every second
    private fun updateDateTime() {
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

    // Updates the polar coordinates and location every minute
    private fun updateLocationAndPolarCoords() {
        viewModelScope.launch {
            while (true) {
                // Placeholder logic for lat, lon, polarisX, and polarisY
                val newLatitude = Utils.decimalToDMS(LATITUDE, "N", "S")
                val newLongitude = Utils.decimalToDMS(LONGITUDE, "E", "W")
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
        val polarisY = altPolaris-altCNP

        return Pair(polarisX, polarisY)
    }
}

data class InfoUiState(
    val currentTime: String,
    val currentDate: String,
    val latitude: String,
    val longitude: String,
    val polarisX: Double,
    val polarisY: Double
)
