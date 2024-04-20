package com.sergey.nes.recorder.ui.home

import androidx.compose.foundation.lazy.LazyListState
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.ui.ContentUIState
import com.sergey.nes.recorder.ui.UiLCEState


data class HomeUiState(
    val recordings: List<RecordingItem>,
    val selectedIndex: Int,
    val isPlaying: Boolean,
    val isTranscribing: Boolean,
    val showDialog: Boolean,
    val error: String
) : ContentUIState<HomeUiState, HomeUiState.Params> {

    override fun extractParams(): Params {
        return Params(
            recordings = this.recordings,
            selectedIndex = this.selectedIndex,
            isPlaying = this.isPlaying,
            transcribing = this.isTranscribing,
            showDialog = this.showDialog,
            error = this.error
        )
    }

    override fun contentValue(): UiLCEState.Content<HomeUiState> {
        return UiLCEState.Content(this)
    }

    data class Params(
        val recordings: List<RecordingItem>,
        val selectedIndex: Int = -1,
        val isPlaying: Boolean = false,
        val transcribing: Boolean = false,
        val showDialog: Boolean = false,
        val error: String = ""
    )
}

data class ComposableParams(
    val listState: LazyListState,
    val micPermission: Boolean = false,
    val audioLength: Int = 0,
    val audioPlayback: Float = 0f,
)