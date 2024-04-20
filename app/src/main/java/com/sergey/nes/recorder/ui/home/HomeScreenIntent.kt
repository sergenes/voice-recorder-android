package com.sergey.nes.recorder.ui.home

import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer

sealed class HomeScreenIntent {
    data class OnLoad(val index: Int) : HomeScreenIntent()
    data class OnSelected(val index: Int, val audioPlayer: AudioPlayer?) : HomeScreenIntent()
    data class OnPlayCompleted(val audioPlayer: AudioPlayer?) : HomeScreenIntent()
    data class OnPlayClicked(val value: Boolean) : HomeScreenIntent()
    data object OnDialogConfirm : HomeScreenIntent()
    data object OnDialogDismiss : HomeScreenIntent()
    data object OnErrorDialogDismiss : HomeScreenIntent()
    data object ActionDelete : HomeScreenIntent()
    data class OnRecordingStopped(val value: RecordingItem) : HomeScreenIntent()
    data object OnTranscribe : HomeScreenIntent()
    data object OnShare : HomeScreenIntent()
}

sealed interface UiAction {
    data class OnSelected(val value: Int) : UiAction
    data class OnRecordingStarted(val value: Boolean) : UiAction
    data class OnSliderValueChange(val value: Float) : UiAction
    data object OnPlayClicked : UiAction
    data object ActionDelete : UiAction
    data object OnRecordingStopped : UiAction
    data object OnTranscribe : UiAction
    data object OnShare : UiAction
}
