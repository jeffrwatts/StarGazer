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
    private val _title = MutableStateFlow<String>("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _imageData = MutableStateFlow<ByteArray?>(null)
    val imageData: StateFlow<ByteArray?> = _imageData.asStateFlow()

    private val _telescopes = MutableStateFlow<List<Telescope>>(emptyList())
    val telescopes: StateFlow<List<Telescope>> = _telescopes.asStateFlow()

    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()

    private val _opticalElements = MutableStateFlow<List<OpticalElement>>(emptyList())
    val opticalElements: StateFlow<List<OpticalElement>> = _opticalElements.asStateFlow()

    // Selected equipment
    private val _selectedTelescope = MutableStateFlow<Telescope?>(null)
    val selectedTelescope: StateFlow<Telescope?> = _selectedTelescope.asStateFlow()

    private val _selectedCamera = MutableStateFlow<Camera?>(null)
    val selectedCamera: StateFlow<Camera?> = _selectedCamera.asStateFlow()

    private val _selectedOpticalElement = MutableStateFlow<OpticalElement?>(null)
    val selectedOpticalElement: StateFlow<OpticalElement?> = _selectedOpticalElement.asStateFlow()

    // FOV State
    private val _fieldOfView = MutableStateFlow<Pair<Double, Double>?>(null)
    val fieldOfView: StateFlow<Pair<Double, Double>?> = _fieldOfView.asStateFlow()

    // MutableState for size, scaling, and rotation
    var size = MutableStateFlow(1.0)
    var scaling = MutableStateFlow(ScalingOption.LINEAR)
    var rotation = MutableStateFlow(0)

    // Cached celestial object
    private var celestialObjWithImage: CelestialObjWithImage? = null

    fun initDetail(sightId: Int, imageSize: Int) {
        viewModelScope.launch {
            try {
                val telescopes = equipmentRepository.getAllTelescopes()
                _selectedTelescope.value = telescopes.first()
                _telescopes.value = telescopes

                val cameras = equipmentRepository.getAllCameras()
                _selectedCamera.value = cameras.first()
                _cameras.value = cameras

                val opticalElements = equipmentRepository.getAllOpticalElements()
                _selectedOpticalElement.value = opticalElements.first()
                _opticalElements.value = opticalElements

                // Set celestial object and fetch image here
                celestialObjWithImage = celestialObjRepository.getCelestialObj(sightId).first()

                celestialObjWithImage?.let {
                    _title.value = it.celestialObj.displayName
                }

                refreshImage(imageSize) // Fetch image immediately
            } catch (e: Exception) {
                _imageData.value = null // Reset image data in case of error
            }
        }
    }

    // Refresh image with the specified image size
    fun refreshImage(imageSize: Int) {
        viewModelScope.launch {
            celestialObjWithImage?.let { obj ->
                _imageData.value = null // Clear previous image data
                try {
                    val imageRequestParams = SkyViewRequestParams.Builder()
                        .pixels(imageSize)
                        .size(size.value)
                        .scaling(scaling.value)
                        .rotation(rotation.value)
                        .build()
                    val image = skyViewRepository.fetchSkyViewImage(obj, imageRequestParams).first()

                    // Update image data
                    _imageData.value = image.bytes()

                    // Calculate the field of view based on the selected telescope and camera
                    calculateFieldOfView()
                } catch (e: Exception) {
                    _imageData.value = null // Reset image data in case of error
                }
            }
        }
    }

    // Calculate the field of view based on the selected telescope, camera, and optical element
    private fun calculateFieldOfView() {
        val telescope = _selectedTelescope.value
        val camera = _selectedCamera.value
        val opticalElement = _selectedOpticalElement.value

        if (telescope != null && camera != null && opticalElement != null) {
            val effectiveFocalLength = telescope.focalLength * opticalElement.factor
            val fieldOfViewWidth = (camera.sensorWidth / effectiveFocalLength) * 57.2958 // Convert to degrees
            val fieldOfViewHeight = (camera.sensorHeight / effectiveFocalLength) * 57.2958 // Convert to degrees

            _fieldOfView.value = Pair(fieldOfViewWidth, fieldOfViewHeight)
        } else {
            _fieldOfView.value = null
        }
    }

    // Methods to update selected equipment
    fun updateSelectedTelescope(telescope: Telescope) {
        _selectedTelescope.value = telescope
        calculateFieldOfView()
    }

    fun updateSelectedCamera(camera: Camera) {
        _selectedCamera.value = camera
        calculateFieldOfView()
    }

    fun updateSelectedOpticalElement(opticalElement: OpticalElement) {
        _selectedOpticalElement.value = opticalElement
        calculateFieldOfView()
    }

    // Methods to update size, scaling, and rotation
    fun updateSize(newSize: Double, imageSize: Int) {
        size.value = newSize
        refreshImage(imageSize) // Pass the fixed image size to refresh the image
    }

    fun updateScaling(newScaling: ScalingOption, imageSize: Int) {
        scaling.value = newScaling
        refreshImage(imageSize) // Pass the fixed image size to refresh the image
    }

    fun updateRotation(newRotation: Int, imageSize: Int) {
        rotation.value = newRotation
        refreshImage(imageSize) // Pass the fixed image size to refresh the image
    }
}
