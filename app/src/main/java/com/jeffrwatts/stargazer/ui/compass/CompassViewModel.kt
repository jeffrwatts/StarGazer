package com.jeffrwatts.stargazer.ui.compass

import androidx.lifecycle.ViewModel
import com.jeffrwatts.stargazer.data.compass.CompassData
import com.jeffrwatts.stargazer.data.compass.CompassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor (
    private val compassRepository: CompassRepository
) : ViewModel() {
    val compassDirection: StateFlow<CompassData> = compassRepository.compassData
    private var sensorsStarted = false

    fun setupSensors() {
        if (!sensorsStarted) {
            compassRepository.setupSensors()
            sensorsStarted = true
        }
    }

}