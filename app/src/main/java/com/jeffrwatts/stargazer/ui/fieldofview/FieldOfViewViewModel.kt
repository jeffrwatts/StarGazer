package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Camera
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.EquipmentRepository
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.OpticalElement
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Telescope
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
    private val celestialObjRepository: CelestialObjRepository,
    private val equipmentRepository: EquipmentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<FieldOfViewUiState>(FieldOfViewUiState.Loading)
    val uiState: StateFlow<FieldOfViewUiState> = _uiState.asStateFlow()

    private val _telescopes = MutableStateFlow<List<Telescope>>(emptyList())
    val telescopes: StateFlow<List<Telescope>> = _telescopes.asStateFlow()

    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()

    private val _opticalElements = MutableStateFlow<List<OpticalElement>>(emptyList())
    val opticalElements: StateFlow<List<OpticalElement>> = _opticalElements.asStateFlow()

    // Selected equipment
    var selectedTelescope: Telescope? = null
    var selectedCamera: Camera? = null
    var selectedOpticalElement: OpticalElement? = null

    // FOV State
    private val _fieldOfView = MutableStateFlow<Pair<Double, Double>?>(null)
    val fieldOfView: StateFlow<Pair<Double, Double>?> = _fieldOfView.asStateFlow()


    // MutableState for size, scaling, and rotation
    var size = MutableStateFlow(2.0)
    var scaling = MutableStateFlow(ScalingOption.LINEAR)
    var rotation = MutableStateFlow(0)

    // Cached celestial object
    private var celestialObjWithImage: CelestialObjWithImage? = null

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                _telescopes.value = equipmentRepository.getAllTelescopes()
                _cameras.value = equipmentRepository.getAllCameras()
                _opticalElements.value = equipmentRepository.getAllOpticalElements()

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
