package com.sergey.nes.recorder.ui.home

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergey.nes.recorder.R
import com.sergey.nes.recorder.app.MainActivityInterface
import com.sergey.nes.recorder.app.Config.RECORDING_DURATION
import com.sergey.nes.recorder.app.toDateTimeString
import com.sergey.nes.recorder.ui.components.ItemView
import com.sergey.nes.recorder.ui.components.RecordButton
import com.sergey.nes.recorder.ui.components.SelectedItemView
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.ui.theme.normalSpace
import com.sergey.nes.recorder.whispertflite.asr.WAVRecorder
import kotlinx.coroutines.launch

class MainActivityForPreview : MainActivityInterface {
    override fun micPermissions(): Boolean {
        return false
    }

    override fun shareFileViaEmail(path: String, date: String) {
        TODO("Not yet implemented")
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    StoryRecTheme {
        HomeScreenView(
            MainActivityForPreview(),
            HomeVewModel(HomeRepository(), null),
            null, null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewList() {
    StoryRecTheme {
        val listState = rememberLazyListState()
        val list = listOf(RecordingItem(), RecordingItem())
        Column {
            LazyListForRecordings(
                composableParams = ComposableParams(
                    listState = listState
                ),
                params = Params(
                    recordings = list
                )
            ) {

            }
        }

    }
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

data class Params(
    val recordings: List<RecordingItem>,
    val selectedIndex: Int = -1,
    val isPlaying: Boolean = false,
    val transcribing: Boolean = false
)

data class ComposableParams(
    val listState: LazyListState,
    val micPermission: Boolean = false,
    val audioLength: Int = 0,
    val audioPlayback: Float = 0f,
)

@Composable
fun HomeScreenView(
    activity: MainActivityInterface,
    viewModel: HomeVewModel,
    audioPlayer: AudioPlayer?,
    audioRecorder: WAVRecorder?
) {
    fun handleShareIntent() {
        viewModel.handleIntent(HomeScreenIntent.OnShare)
    }

    fun handleOnDialogConfirm() {
        viewModel.handleIntent(HomeScreenIntent.OnDialogConfirm)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onPlayCompletionListener = MediaPlayer.OnCompletionListener {
        viewModel.handleIntent(HomeScreenIntent.OnPlayCompleted(audioPlayer))
    }
    val uiState = viewModel.uiState.collectAsState().value

    var micPermission by remember { mutableStateOf(false) }
    micPermission = activity.micPermissions()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        micPermission = it
    }

    val audioLength = audioPlayer?.audioLength?.value ?: 0
    val audioPlayback = audioPlayer?.audioPlayback?.value ?: 0f

    LaunchedEffect("Init") {
        viewModel.handleIntent(HomeScreenIntent.OnLoad(0))
        audioPlayer?.setCompletionListener(onPlayCompletionListener)
    }

    // animated scroll for auto play
    LaunchedEffect(viewModel.selectedIndex()) {
        viewModel.selectedIndex()?.let {
            if (viewModel.isNextAvailable()) {
                // autoplay block
                coroutineScope.launch {
                    listState.animateScrollToItem(it)
                }
            } else {
                // set current audio to see the length in UI
                viewModel.currentItem()?.let {
                    audioPlayer?.setCurrentFile(it)
                }
            }
        }
    }

    Box {
        when (uiState) {
            HomeVewModel.UiState.Initial -> {
                HomeViewLoading()
            }

            is HomeVewModel.UiState.Loading -> {
                HomeViewLoading()
            }

            is HomeVewModel.UiState.Content -> {
                val showDialog = uiState.showDialog
                val error = uiState.error

                HomeViewContent(
                    composableParams = ComposableParams(
                        listState = listState,
                        micPermission = micPermission,
                        audioLength = audioLength,
                        audioPlayback = audioPlayback,
                    ),
                    params = uiState.extractParams(),
                    onUiAction = { action ->
                        when (action) {
                            UiAction.OnShare -> {
                                activity.shareFileViaEmail(
                                    viewModel.currentItem()!!.id,
                                    System.currentTimeMillis().toDateTimeString()
                                )
                            }//handleShareIntent()
                            UiAction.ActionDelete -> handleOnDialogConfirm()
                            UiAction.OnPlayClicked -> {
                                audioPlayer?.let {
                                    it.pausePlay()
                                    viewModel.handleIntent(HomeScreenIntent.OnPlayClicked(it.isPlaying()))
                                }
                            }

                            is UiAction.OnRecordingStarted -> {
                                if (!action.value) {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    audioPlayer?.stop()
                                    audioRecorder?.start()
                                }
                            }

                            UiAction.OnRecordingStopped -> {
                                audioRecorder?.stop()?.let {
                                    viewModel.handleIntent(
                                        HomeScreenIntent.OnRecordingStopped(it)
                                    )
                                }
                            }

                            is UiAction.OnSelected -> {
                                viewModel.handleIntent(
                                    HomeScreenIntent.OnSelected(
                                        action.value,
                                        audioPlayer = audioPlayer
                                    )
                                )
                            }

                            is UiAction.OnSliderValueChange -> {
                                audioPlayer?.updateAudioPlayback(action.value)
                            }

                            UiAction.OnTranscribe -> {
                                viewModel.handleIntent(HomeScreenIntent.OnTranscribe)
                            }
                        }
                    }
                )
                if (showDialog) {
                    ConfirmDeletingDialog(onConfirm = {
                        viewModel.handleIntent(HomeScreenIntent.ActionDelete)
                    }) {
                        viewModel.handleIntent(HomeScreenIntent.OnDialogDismiss)
                    }
                }
                if (error.isNotEmpty()) {
                    ErrorMessageDialog(error) {
                        viewModel.handleIntent(HomeScreenIntent.OnErrorDialogDismiss)
                    }
                }
            }

            HomeVewModel.UiState.Error -> {
                HomeViewError()
            }
        }
    }
}

@Composable
fun ConfirmDeletingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .alpha(0.7f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onConfirm)
                { Text(text = stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss)
                { Text(text = stringResource(R.string.cancel)) }
            },
            title = { Text(text = stringResource(R.string.please_confirm_deleting)) },
            text = { Text(text = stringResource(R.string.are_you_sure_you_can_not_undo_this_action)) }
        )
    }
}

@Composable
fun ErrorMessageDialog(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .alpha(0.7f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss)
                { Text(text = stringResource(R.string.yes)) }
            },

            title = { Text(text = "Error") },
            text = { Text(text = message) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewError() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text(text = "Error...")
            }
        },
        bottomBar = { })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewLoading() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text(text = "Loading...")
            }
        },
        bottomBar = { })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewContent(
    params: Params,
    composableParams: ComposableParams,
    onUiAction: (UiAction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (!composableParams.micPermission) {
                    MicPermissionMessage()
                }
                if (params.recordings.isEmpty()) {
                    EmptyListMessage()
                } else {
                    LazyListForRecordings(composableParams, params, onUiAction)
                }
            }
        },
        bottomBar = { BottomBar(composableParams, onUiAction) })
}

@Composable
fun LazyListForRecordings(
    composableParams: ComposableParams,
    params: Params,
    onUiAction: (UiAction) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val filteredItems =
        params.recordings.filter {
            it.transcription.contains(
                query.text,
                ignoreCase = true
            )
        }
    TextField(
        value = query,
        textStyle = TextStyle(
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        colors = OutlinedTextFieldDefaults.colors(),
        singleLine = true,
        onValueChange = { newQuery -> query = newQuery },
        label = { Text(stringResource(R.string.search)) },
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(
                MaterialTheme.colorScheme.background,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = normalSpace)
    )
    Spacer(modifier = Modifier.height(normalSpace))
    LazyColumn(state = composableParams.listState, modifier = Modifier.fillMaxHeight()) {
        items(filteredItems.size) { index ->
            val item = filteredItems[index]
            val title = item.dateTime.toDateTimeString()
            val transcription = item.transcription
            if (params.selectedIndex == index) {
                SelectedItemView(
                    title = title,
                    transcription = transcription,
                    speaking = params.isPlaying,
                    audioLength = composableParams.audioLength,
                    audioPlayback = composableParams.audioPlayback,
                    actionPlay = { onUiAction(UiAction.OnPlayClicked) },
                    actionDelete = { onUiAction(UiAction.ActionDelete) },
                    onSliderValueChange = {
                        onUiAction(
                            UiAction.OnSliderValueChange(
                                it
                            )
                        )
                    },
                    transcribing = params.transcribing,
                    onTranscribe = { onUiAction(UiAction.OnTranscribe) },
                    onShare = { onUiAction(UiAction.OnShare) }
                )
            } else {
                ItemView(title = title, transcription = transcription, onSelect = {
                    onUiAction(UiAction.OnSelected(index))
                })
            }
        }
    }
}

@Composable
fun BottomBar(composableParams: ComposableParams, onUiAction: (UiAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(normalSpace)
    ) {
        RecordButton(
            maxDuration = RECORDING_DURATION,
            micPermission = composableParams.micPermission,
            onRecordingStarted = { onUiAction(UiAction.OnRecordingStarted(it)) },
            onRecordingStopped = { onUiAction(UiAction.OnRecordingStopped) }
        )
    }
}

@Composable
fun EmptyListMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(normalSpace)
            .fillMaxWidth()
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.there_are_no_recordings_yet),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun MicPermissionMessage() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
    ) {
        Text(
            text = stringResource(R.string.allow_access_to_the_microphone),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(normalSpace)
        )
    }
}