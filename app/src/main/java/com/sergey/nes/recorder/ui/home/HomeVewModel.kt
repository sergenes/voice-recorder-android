package com.sergey.nes.recorder.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeVewModel(
    private val repository: HomeRepository,
) : ViewModel() {

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun onDialogConfirm() {
        _showDialog.value = true
    }

    fun onDialogDismiss() {
        _showDialog.value = false
    }

    private val _isPlaying: MutableState<Boolean> = mutableStateOf(false)
    val isPlaying: State<Boolean> get() = _isPlaying

    data class DataSourceState(
        val recordings: List<RecordingItem> = emptyList(),
        val selectedIndex: Int = -1
    )

    private val _dataSource = MutableStateFlow(DataSourceState())
    val dataSource: StateFlow<DataSourceState>
        get() = _dataSource

    fun onLoad(ready: (DataSourceState) -> Unit, onError: (String) -> Unit) =
        viewModelScope.launch {
            repository.getRecordings().collect { result ->
                result.fold(
                    onSuccess = { recordings ->
                        _dataSource.value = DataSourceState(recordings)
                        launch(Dispatchers.Main) {
                            ready(_dataSource.value)
                        }
                    },
                    onFailure = { throwable ->
                        throwable.localizedMessage?.let {
                            onError(it)
                        } ?: run {
                            onError("Unknown Error")
                        }
                    }
                )
            }
        }

    fun deleteRecording(onError: (String) -> Unit) = viewModelScope.launch {
        val index = _dataSource.value.selectedIndex
        if (index in 0.._dataSource.value.recordings.lastIndex) {
            val item = _dataSource.value.recordings[index]
            repository.deleteRecording(item.id.toString()).collect { result ->
                result.fold(
                    onSuccess = {
                        onLoad(ready = {
                            _dataSource.value = _dataSource.value.copy(selectedIndex = -1)
                        }, onError = {

                        })
                    },
                    onFailure = { throwable ->
                        throwable.localizedMessage?.let {
                            onError(it)
                        } ?: run {
                            onError("Unknown Error")
                        }
                    }
                )
            }
        }
    }

    fun selectRecording(index: Int, audioPlayer: AudioPlayer?) {
        _dataSource.value = _dataSource.value.copy(selectedIndex = index)
        if (index in 0.._dataSource.value.recordings.lastIndex) {
            audioPlayer?.setCurrentFile(_dataSource.value.recordings[index])
        }
    }

    fun onPlayCompleted(audioPlayer: AudioPlayer?) {
        audioPlayer?.stop()
        _isPlaying.value = false
        val nextIndex = _dataSource.value.selectedIndex + 1
        if (_dataSource.value.recordings.lastIndex >= nextIndex) {
            // play next in list
            _isPlaying.value = true
            selectRecording(nextIndex, audioPlayer)
            audioPlayer?.pausePlay()
        } else {
            selectRecording(_dataSource.value.selectedIndex, audioPlayer)
        }
    }

    fun updatePlaying(value: Boolean) {
        _isPlaying.value = value
    }
}