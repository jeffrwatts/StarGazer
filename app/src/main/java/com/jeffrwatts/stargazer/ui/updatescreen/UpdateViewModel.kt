package com.jeffrwatts.stargazer.ui.updatescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.jeffrwatts.stargazer.workers.UpdateDSOVariableWorker
import com.jeffrwatts.stargazer.workers.UpdateEphemerisWorker
import com.jeffrwatts.stargazer.workers.UpdateImageWorker
import com.jeffrwatts.stargazer.workers.UpdateType
import com.jeffrwatts.stargazer.workers.toUpdateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor (
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateUiState(
            lastUpdated = getLastUpdate(),
            isImageUpdating = false,
            isEphemerisUpdating = false,
            isDSOVariableUpdating = false
        )
    )
    val state: StateFlow<UpdateUiState> = _state

    private val _statusMessages = MutableStateFlow(listOf<String>())
    val statusMessages: StateFlow<List<String>> = _statusMessages

    init {
        pruneAndMonitorExistingJobs()
    }

    fun triggerImageUpdate() {
        val updateRequest = OneTimeWorkRequestBuilder<UpdateImageWorker>()
            .addTag(UpdateType.IMAGE.name)
            .build()
        workManager.enqueue(updateRequest)
        monitorJobStatus(updateRequest.id)
    }

    fun triggerEphemerisUpdate() {
        val updateRequest = OneTimeWorkRequestBuilder<UpdateEphemerisWorker>()
            .addTag(UpdateType.EPHEMERIS.name)
            .build()
        workManager.enqueue(updateRequest)
        monitorJobStatus(updateRequest.id)
    }

    fun triggerDSOVariableUpdate() {
        val updateRequest = OneTimeWorkRequestBuilder<UpdateDSOVariableWorker>()
            .addTag(UpdateType.DSO_VARIABLE.name)
            .build()
        workManager.enqueue(updateRequest)
        monitorJobStatus(updateRequest.id)
    }

    private fun pruneAndMonitorExistingJobs() {
        viewModelScope.launch {
            //workManager.pruneWork()  // Reconsider whether to prune or not

            listOf(UpdateType.IMAGE, UpdateType.EPHEMERIS, UpdateType.DSO_VARIABLE).forEach { type ->
                val workInfos = workManager.getWorkInfosByTag(type.name).await()
                workInfos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .forEach { monitorJobStatus(it.id) }
            }
        }
    }


    private fun monitorJobStatus(workId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(workId).asFlow().collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> updateUI(workInfo.progress, true)
                    WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED -> {
                        updateUI(workInfo.outputData, false)
                    }
                    else -> { } // No action needed for other states
                }
            }
        }
    }

    private fun updateUI(data: Data, downloading: Boolean) {
        data.getString("UpdateType")?.toUpdateType().let { updateType ->
            val status = data.getString("Status") ?: ""
            _statusMessages.value = _statusMessages.value + status
            _state.value = when (updateType) {
                UpdateType.IMAGE -> _state.value.copy(isImageUpdating = downloading)
                UpdateType.EPHEMERIS -> _state.value.copy(isEphemerisUpdating = downloading)
                UpdateType.DSO_VARIABLE -> _state.value.copy(isDSOVariableUpdating = downloading)
                else -> _state.value
            }
        }
    }

    fun clearStatus() {
        _statusMessages.value = emptyList()
    }

    private fun getLastUpdate(): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

data class UpdateUiState(
    val lastUpdated: String,
    val isImageUpdating: Boolean = false,
    val isEphemerisUpdating: Boolean = false,
    val isDSOVariableUpdating: Boolean = false
)
