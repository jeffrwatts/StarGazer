package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.ui.fieldofview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.skyview.SkyViewRepository
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

    fun initDetail(sightId: Int) {
        viewModelScope.launch {
            try {
                val celestialObjWithImage = celestialObjRepository.getCelestialObj(sightId).first()
                val image = skyViewRepository.fetchSkyViewImage(celestialObjWithImage).first()

                // Convert ResponseBody to ByteArray
                val imageData = image.bytes() // This is safe to call since `image` is already a ByteArray in memory

                _uiState.value = FieldOfViewUiState.Success(celestialObjWithImage, imageData)
            } catch (e: Exception) {
                _uiState.value = FieldOfViewUiState.Error("Error loading data")
            }
        }
    }
}


sealed class FieldOfViewUiState {
    object Loading : FieldOfViewUiState()
    data class Success(val celestialObjWithImage: CelestialObjWithImage, val imageData: ByteArray) : FieldOfViewUiState()
    data class Error(val message: String) : FieldOfViewUiState()
}
