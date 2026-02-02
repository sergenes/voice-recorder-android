package com.sergey.nes.recorder.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.ui.UiLCEState
import com.sergey.nes.recorder.whispertflite.asr.IWhisperListener
import com.sergey.nes.recorder.whispertflite.asr.Whisper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

class Container<STATE, SIDE_EFFECT>(
    private val scope: CoroutineScope,
    initialState: STATE
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = _state

    private val _sideEffect = Channel<SIDE_EFFECT>(Channel.BUFFERED)
    val sideEffect: Flow<SIDE_EFFECT> = _sideEffect.receiveAsFlow()

    fun intent(transform: suspend Container<STATE, SIDE_EFFECT>.() -> Unit) {
        scope.launch(SINGLE_THREAD) {
            this@Container.transform()
        }
    }

    suspend fun reduce(reducer: STATE.() -> STATE) {
        withContext(SINGLE_THREAD) {
            _state.value = _state.value.reducer()
        }
    }

    suspend fun postSideEffect(event: SIDE_EFFECT) {
        _sideEffect.send(event)
    }

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        private val SINGLE_THREAD = newSingleThreadContext("mvi")
    }
}

class HomeVewModel(
    private val micPermission: Boolean = false,
    private val repository: HomeRepository,
    private val whisper: Whisper? = null,
    private val navController: NavController? = null
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

    fun currentItem(): RecordingItem? = uiState.contentState()?.let {
        val index = it.selectedIndex
        if (index in 0..it.recordings.lastIndex) {
            return it.recordings[index]
        } else return null
    } ?: run {
        return null
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

    fun handleIntent(intent: HomeScreenIntent) {
        when (intent) {
            is HomeScreenIntent.OnLoad -> handleOnLoad(intent.index)
            is HomeScreenIntent.OnSelected -> handleOnSelected(intent.index, intent.audioPlayer)
            is HomeScreenIntent.OnPlayCompleted -> handleOnPlayCompleted(intent.audioPlayer)
            is HomeScreenIntent.OnPlayClicked -> handleOnPlayClicked(intent.value)
            is HomeScreenIntent.OnRecordingStopped -> handleOnRecordingStopped(intent.value)
            HomeScreenIntent.ActionDelete -> handleActionDelete()
            HomeScreenIntent.OnTranscribe -> handleOnTranscribe()
            HomeScreenIntent.OnDialogConfirm -> onDialogConfirm()
            HomeScreenIntent.OnDialogDismiss -> onDialogDismiss()
            HomeScreenIntent.OnErrorDialogDismiss -> onErrorDialogDismiss()
            HomeScreenIntent.OnShare -> handleOnShare()
            HomeScreenIntent.OnSettings -> handleOnSettings()
            is HomeScreenIntent.OnPermissionUpdate -> handlePermissionUpdate(intent.value)
        }
    }

    private fun handlePermissionUpdate(value: Boolean) = uiState.contentState()?.let {
        _uiState.value = it.copy(micPermission = value).contentValue()
    }

    private fun handleOnSettings() {
        navController?.navigate("settings")
    }

    private fun handleOnLoad(index: Int) {
        onLoad(index)
    }

    private fun handleOnSelected(index: Int, audioPlayer: AudioPlayer?) {
        selectRecording(index, audioPlayer)
        updatePlaying(false)
    }

    private fun handleOnPlayCompleted(audioPlayer: AudioPlayer?) {
        onPlayCompleted(audioPlayer)
    }

    private fun handleOnPlayClicked(value: Boolean) {
        updatePlaying(value)
    }

    private fun handleActionDelete() {
        deleteRecording()
        onDialogDismiss()
    }

    private fun handleOnRecordingStopped(value: RecordingItem) {
        saveInfo(value)
    }

    private fun handleOnTranscribe() {
        transcribe()
    }

    private fun handleOnShare() {
        deleteTranscriptForTest()
    }

    private val _uiState = MutableStateFlow<UiLCEState<HomeUiState>>(UiLCEState.Initial)
    val uiState: StateFlow<UiLCEState<HomeUiState>> = _uiState

    private fun StateFlow<UiLCEState<HomeUiState>>.contentState(): HomeUiState? =
        (this.value as? UiLCEState.Content<HomeUiState>)?.data

    private fun onLoad(index2Select: Int) = viewModelScope.launch {
        _uiState.value = UiLCEState.Loading
        repository.getRecordings().collect { result ->
            result.fold(
                onSuccess = { recordings ->
                    val selectedIndex =
                        if (index2Select in 0..recordings.lastIndex) index2Select else -1
                    _uiState.value = UiLCEState.Content(
                        HomeUiState(
                            recordings = recordings,
                            selectedIndex = selectedIndex,
                            isPlaying = false,
                            isTranscribing = false,
                            showDialog = false,
                            micPermission = micPermission,
                            error = ""
                        )
                    )
                },
                onFailure = {
                    _uiState.value = UiLCEState.Error
                }
            )
        }
    }

    private fun resolveCurrentItem(resolved: (RecordingItem, Int, HomeUiState) -> Unit) =
        uiState.contentState()?.let {
            val index = it.selectedIndex
            if (index in 0..it.recordings.lastIndex) {
                val item = it.recordings[index]
                resolved(item, index, it)
            }
        }


    private fun transcribe() = resolveCurrentItem { item, _, state ->
        if (item.transcription.isEmpty()) {
            if (whisper?.isInProgress == true) {
                onSoftError("In the process")
            } else {
                whisper?.setFilePath(item)
                whisper?.setAction(Whisper.ACTION_TRANSCRIBE)
                whisper?.start()
                _uiState.value = state.copy(isTranscribing = true).contentValue()
            }
        } else {
            onSoftError("Already transcribed")
        }
    }


    private fun saveTranscription(transcription: String, audioFileId: String?) =
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

    private fun saveInfo(recording: RecordingItem) = viewModelScope.launch {
        repository.saveRecordingInfo(recording).collect { result ->
            result.fold(
                onSuccess = {
                    onLoad(0)
                },
                onFailure = { throwable ->
                    val message = throwable.localizedMessage ?: "Unknown Error"
                    uiState.contentState()?.let {
                        _uiState.value = UiLCEState.Content(it.copy(error = message))
                    }
                }
            )
        }
    }

    private fun deleteRecording() = resolveCurrentItem { item, index, _ ->
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


    private fun selectRecording(index: Int, audioPlayer: AudioPlayer?) =
        uiState.contentState()?.let {
            _uiState.value = UiLCEState.Content(it.copy(selectedIndex = index))
            if (index in 0..it.recordings.lastIndex) {
                val item = it.recordings[index]
                audioPlayer?.setCurrentFile(item)
            }
        }

    private fun onSoftError(value: String) = uiState.contentState()?.let { state ->
        _uiState.value =
            state.copy(isPlaying = false, isTranscribing = false, error = value).contentValue()
    }


    private fun onPlayCompleted(audioPlayer: AudioPlayer?) {
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

    private fun updatePlaying(value: Boolean) = uiState.contentState()?.let {
        _uiState.value = it.copy(isPlaying = value).contentValue()
    }


    private fun onDialogConfirm() = uiState.contentState()?.let {
        _uiState.value = it.copy(showDialog = true).contentValue()
    }

    private fun onDialogDismiss() = uiState.contentState()?.let {
        _uiState.value = it.copy(showDialog = false).contentValue()
    }

    private fun onErrorDialogDismiss() = uiState.contentState()?.let {
        _uiState.value = it.copy(error = "").contentValue()
    }

    private fun deleteTranscriptForTest() = uiState.contentState()?.let {
        val index = it.selectedIndex
        val recording = it.recordings[index]
        saveTranscription("", audioFileId = recording.id)
    }
}