package com.sergey.nes.recorder.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.whispertflite.asr.IWhisperListener
import com.sergey.nes.recorder.whispertflite.asr.Whisper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeVewModel(
    private val repository: HomeRepository,
    private val whisper: Whisper? = null
) : ViewModel() {

    init {
        whisper?.setListener(object : IWhisperListener {
            override fun onUpdateReceived(message: String) {
                Log.d("MainActivity", "Update is received, Message: $message")
//                if (message == Whisper.MSG_PROCESSING) {
//                } else if (message == Whisper.MSG_FILE_NOT_FOUND) {
//                    // write code as per need to handled this error
//                }
            }

            override fun onResultReceived(result: String, audioFileId: String) {
                Log.d("MainActivity", "Result: $result")
                saveTranscription(transcription = result, audioFileId = audioFileId)
            }
        })
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun StateFlow<UiState>.contentState(): UiState.Content? =
        this.value as? UiState.Content

    sealed class UiState {
        data object Initial : UiState()
        data class Loading(val recordings: List<RecordingItem> = emptyList()) : UiState()
        data class Content(
            val recordings: List<RecordingItem>,
            val selectedIndex: Int,
            val isPlaying: Boolean,
            val isTranscribing: Boolean,
            val showDialog: Boolean,
            val error: String
        ) : UiState() {
            fun copy(
                recordings: List<RecordingItem>? = null,
                selectedIndex: Int? = null,
                isPlaying: Boolean? = null,
                transcribing: Boolean? = null,
                showDialog: Boolean? = null,
                error: String? = null
            ): Content = Content(
                recordings = recordings ?: this.recordings,
                selectedIndex = selectedIndex ?: this.selectedIndex,
                isPlaying = isPlaying ?: this.isPlaying,
                isTranscribing = transcribing ?: this.isTranscribing,
                showDialog = showDialog ?: this.showDialog,
                error = error ?: this.error
            )
        }

        data object Error : UiState()
    }

    fun onLoad(index2Select: Int) = viewModelScope.launch {
        _uiState.value = UiState.Loading()
        repository.getRecordings().collect { result ->
            result.fold(
                onSuccess = { recordings ->
                    val selectedIndex =
                        if (index2Select in 0..recordings.lastIndex) index2Select else -1
                    _uiState.value = UiState.Content(
                        recordings = recordings,
                        selectedIndex = selectedIndex,
                        isPlaying = false,
                        isTranscribing = false,
                        showDialog = false,
                        error = ""
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = UiState.Error
                }
            )
        }
    }


    fun currentItem(): RecordingItem? = uiState.contentState()?.let {
        val index = it.selectedIndex
        if (index in 0..it.recordings.lastIndex) {
            return it.recordings[index]
        } else return null
    } ?: run {
        return null
    }


    private fun resolveCurrentItem(resolved: (RecordingItem, Int, UiState.Content) -> Unit) =
        uiState.contentState()?.let {
            val index = it.selectedIndex
            if (index in 0..it.recordings.lastIndex) {
                val item = it.recordings[index]
                resolved(item, index, it)
            }
        }


    fun transcribe() = resolveCurrentItem { item, _, state ->
        if (item.transcription.isEmpty()) {
            if (whisper?.isInProgress == true) {
                onSoftError("In the process")
            } else {
                whisper?.setFilePath(item)
                whisper?.setAction(Whisper.ACTION_TRANSCRIBE)
                whisper?.start()
                _uiState.value = state.copy(isTranscribing = true)
            }
        } else {
            onSoftError("Already transcribed")
        }
    }


    fun saveTranscription(transcription: String, audioFileId: String?) =
        resolveCurrentItem { item, index, _ ->
            viewModelScope.launch {
                if (item.id == audioFileId) {
                    val save = item.copy(transcription = transcription.trim())
                    repository.saveRecordingInfo(save).collect { result ->
                        result.fold(
                            onSuccess = {
                                onLoad(index)
                            },
                            onFailure = { throwable ->
                                throwable.localizedMessage?.let { message ->
                                    onSoftError(message)
                                } ?: run {
                                    onSoftError("Unknown Error")
                                }
                            }
                        )
                    }
                }
            }
        }

    fun saveInfo(recording: RecordingItem) = viewModelScope.launch {
        repository.saveRecordingInfo(recording).collect { result ->
            result.fold(
                onSuccess = {
                    onLoad(0)
                },
                onFailure = { throwable ->
                    val message = throwable.localizedMessage ?: "Unknown Error"
                    uiState.contentState()?.let {
                        _uiState.value = it.copy(error = message)
                    }
                }
            )
        }
    }

    fun deleteRecording() = resolveCurrentItem { item, index, _ ->
        viewModelScope.launch {
            repository.deleteRecording(item.id).collect { result ->
                result.fold(
                    onSuccess = {
                        val select = if (index > 0) index - 1 else 0
                        onLoad(select)
                    },
                    onFailure = { throwable ->
                        throwable.localizedMessage?.let { message ->
                            onSoftError(message)
                        } ?: run {
                            onSoftError("Unknown Error")
                        }
                    }
                )
            }
        }
    }


    fun selectRecording(index: Int, audioPlayer: AudioPlayer?) =
        uiState.contentState()?.let {
            _uiState.value = it.copy(selectedIndex = index)
            if (index in 0..it.recordings.lastIndex) {
                val item = it.recordings[index]
                audioPlayer?.setCurrentFile(item)
            }
        }

    private fun onSoftError(value: String) = uiState.contentState()?.let { state ->
        _uiState.value = state.copy(error = value, transcribing = false, isPlaying = false)
    }


    fun onPlayCompleted(audioPlayer: AudioPlayer?) {
        audioPlayer?.stop()
        updatePlaying(false)
        uiState.contentState()?.let {
            val nextIndex = it.selectedIndex + 1
            if (it.recordings.lastIndex >= nextIndex) {
                // play next in list
                updatePlaying(true)
                selectRecording(nextIndex, audioPlayer)
                audioPlayer?.pausePlay()
            } else {
                selectRecording(it.selectedIndex, audioPlayer)
            }
        }
    }

    fun selectedIndex(): Int? {
        return uiState.contentState()?.selectedIndex
    }

    fun isNextAvailable(): Boolean {
        uiState.contentState()?.let {
            return it.isPlaying && it.selectedIndex in 0..it.recordings.lastIndex
        } ?: run {
            return false
        }
    }

    fun updatePlaying(value: Boolean) = uiState.contentState()?.let {
        _uiState.value = it.copy(isPlaying = value)
    }


    fun onDialogConfirm() = uiState.contentState()?.let {
        _uiState.value = it.copy(showDialog = true)
    }

    fun onDialogDismiss() = uiState.contentState()?.let {
        _uiState.value = it.copy(showDialog = false)
    }

    fun onErrorDialogDismiss() = uiState.contentState()?.let {
        _uiState.value = it.copy(error = "")
    }

    fun deleteTranscriptForTest() = uiState.contentState()?.let {
        val index = it.selectedIndex
        val recording = it.recordings[index]
        saveTranscription("", audioFileId = recording.id)
    }
}