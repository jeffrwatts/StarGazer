package com.jeffrwatts.stargazer.ui.updatescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.jeffrwatts.stargazer.workers.UpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor (
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateUiState(lastUpdated = getLastUpdate(),
        isDownloading = false)
    )
    val state: StateFlow<UpdateUiState> = _state

    private val _statusMessages = MutableStateFlow(listOf<String>())
    val statusMessages: StateFlow<List<String>> = _statusMessages

    fun triggerImageUpdate() {
        val updateRequest = OneTimeWorkRequestBuilder<UpdateWorker>().build()
        workManager.enqueue(updateRequest)

        // Observe the work status and update the state
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(updateRequest.id).asFlow().collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        workInfo.progress.getString("Status")?.let { message->
                            updateStatus(message)
                        }
                        _state.value = _state.value.copy(isDownloading = true)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        workInfo.outputData.getString("Status")?.let { message->
                            updateStatus(message)
                        }
                        _state.value = _state.value.copy(
                            isDownloading = false,
                            lastUpdated = getLastUpdate()
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        workInfo.outputData.getString("Status")?.let { message->
                            updateStatus(message)
                        }
                        _state.value = _state.value.copy(isDownloading = false)
                    }
                    else -> _state.value = _state.value.copy(isDownloading = false)
                }
            }
        }
    }

    private fun updateStatus(message: String) {
        _statusMessages.value = _statusMessages.value + message
    }

    fun clearStatus() {
        _statusMessages.value = emptyList()
    }

    private fun getLastUpdate(): String {
        // UPDATE: pull this from the last download time.
        val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

data class UpdateUiState(
    val lastUpdated: String,
    val isDownloading: Boolean = false
)