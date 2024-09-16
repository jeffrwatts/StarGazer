package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import javax.inject.Inject

class EquipmentRepository @Inject constructor(
    private val telescopeDao: TelescopeDao,
    private val cameraDao: CameraDao,
    private val opticalElementDao: OpticalElementDao
) {
    // Hard code my equipment for now.
    private val predefinedTelescopes = listOf(
        Telescope(displayName = "Celestron C8", focalLength = 2032.0, aperture = 203.2)
    )

    private val predefinedCameras = listOf(
        Camera(displayName = "ZWO ASI294MC Pro", sensorWidth = 19.1, sensorHeight = 13.0, pixelSize = 4.63, resolutionWidth = 4144, resolutionHeight = 2822),
        Camera(displayName = "ZWO ASI678MC", sensorWidth = 7.7, sensorHeight = 4.3, pixelSize = 2.0, resolutionWidth = 3840, resolutionHeight = 2160)
    )

    private val predefinedOpticalElements = listOf(
        OpticalElement(displayName = "None", factor = 1.0),
        OpticalElement(displayName = "0.63x Reducer", factor = 0.63),
        OpticalElement(displayName = "2.0x Barlow", factor = 2.0)
    )

    // Modified get methods
    suspend fun getAllTelescopes(): List<Telescope> {
        ensureDataInitialized()
        return telescopeDao.getAllTelescopes()
    }

    suspend fun getAllCameras(): List<Camera> {
        ensureDataInitialized()
        return cameraDao.getAllCameras()
    }

    suspend fun getAllOpticalElements(): List<OpticalElement> {
        ensureDataInitialized()
        return opticalElementDao.getAllOpticalElements()
    }

    private suspend fun ensureDataInitialized() {
        if (telescopeDao.getAllTelescopes().isEmpty()) {
            insertDefaultTelescopes()
        }
        if (cameraDao.getAllCameras().isEmpty()) {
            insertDefaultCameras()
        }
        if (opticalElementDao.getAllOpticalElements().isEmpty()) {
            insertDefaultOpticalElements()
        }
    }

    // Insert predefined telescopes
    private suspend fun insertDefaultTelescopes() {
        predefinedTelescopes.forEach { telescope ->
            telescopeDao.insert(
                Telescope(
                    displayName = telescope.displayName,
                    focalLength = telescope.focalLength,
                    aperture = telescope.aperture
                )
            )
        }
    }

    // Insert predefined cameras
    private suspend fun insertDefaultCameras() {
        predefinedCameras.forEach { camera ->
            cameraDao.insert(
                Camera(
                    displayName = camera.displayName,
                    sensorWidth = camera.sensorWidth,
                    sensorHeight = camera.sensorHeight,
                    pixelSize = camera.pixelSize,
                    resolutionWidth = camera.resolutionWidth,
                    resolutionHeight = camera.resolutionHeight
                )
            )
        }
    }

    // Insert predefined optical elements
    private suspend fun insertDefaultOpticalElements() {
        predefinedOpticalElements.forEach { opticalElement ->
            opticalElementDao.insert(
                OpticalElement(
                    displayName = opticalElement.displayName,
                    factor = opticalElement.factor
                )
            )
        }
    }
}