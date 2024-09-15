package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview.ScalingOption
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview.SkyViewRepository
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview.SkyViewRequestParams
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjWithImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FieldOfViewViewModel @Inject constructor(
    private val skyViewRepository: SkyViewRepository,
    private val celestialObjRepository: CelestialObjRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FieldOfViewUiState>(FieldOfViewUiState.Loading)
    val uiState: StateFlow<FieldOfViewUiState> = _uiState.asStateFlow()

    // MutableState for size, scaling, and rotation
    var size = MutableStateFlow(2.0)
    var scaling = MutableStateFlow(ScalingOption.LINEAR)
    var rotation = MutableStateFlow(0)

    // Cached celestial object
    private var celestialObjWithImage: CelestialObjWithImage? = null

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                celestialObjWithImage = celestialObjRepository.getCelestialObj(sightId).first()
                refreshImage()
            } catch (e: Exception) {
                _uiState.value = FieldOfViewUiState.Error("Error loading data")
            }
        }
    }

    // Refresh image whenever size, scaling, or rotation changes
    fun refreshImage() {
        viewModelScope.launch {
            celestialObjWithImage?.let { obj ->
                _uiState.value = FieldOfViewUiState.Loading
                try {
                    val imageRequestParams = SkyViewRequestParams.Builder()
                        .size(size.value)
                        .scaling(scaling.value)
                        .rotation(rotation.value)
                        .build()
                    val image = skyViewRepository.fetchSkyViewImage(obj, imageRequestParams).first()

                    // Convert ResponseBody to ByteArray
                    val imageData = image.bytes()
                    _uiState.value = FieldOfViewUiState.Success(obj, imageData)
                } catch (e: Exception) {
                    _uiState.value = FieldOfViewUiState.Error("Error refreshing image")
                }
            }
        }
    }

    // Methods to update size, scaling, and rotation
    fun updateSize(newSize: Double) {
        size.value = newSize
        refreshImage()
    }

    fun updateScaling(newScaling: ScalingOption) {
        scaling.value = newScaling
        refreshImage()
    }

    fun updateRotation(newRotation: Int) {
        rotation.value = newRotation
        refreshImage()
    }
}

sealed class FieldOfViewUiState {
    object Loading : FieldOfViewUiState()
    data class Success(val celestialObjWithImage: CelestialObjWithImage, val imageData: ByteArray) : FieldOfViewUiState()
    data class Error(val message: String) : FieldOfViewUiState()
}
