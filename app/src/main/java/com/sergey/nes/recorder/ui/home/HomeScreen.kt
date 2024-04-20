package com.sergey.nes.recorder.ui.home

import android.Manifest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sergey.nes.recorder.ui.UiLCEState
import com.sergey.nes.recorder.ui.resolve
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.ui.theme.normalSpace
import com.sergey.nes.recorder.whispertflite.asr.WAVRecorder
import kotlinx.coroutines.launch


@Preview(showBackground = true)
@Composable
fun Preview() {
    StoryRecTheme {
        val listState = rememberLazyListState()
        val list = listOf(RecordingItem(), RecordingItem())

        val activity: MainActivityInterface = object : MainActivityInterface {

            override fun shareFileViaEmail(path: String, date: String) {
                TODO("Not yet implemented")
            }

        }

        val audioLength = 0
        val audioPlayback = 0f

        HomeViewContent(
            listState = listState,
            composableParams = AudioPlaybackParams(
                audioLength = audioLength,
                audioPlayback = audioPlayback,
            ),
            params = HomeUiState.Params(
                recordings = list,
                selectedIndex = 0,
                isPlaying = false,
                transcribing = false,
                showDialog = false,
                error = ""
            ),
            onUiAction = { _ ->

            }
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
                listState = listState,
                composableParams = AudioPlaybackParams(),
                params = HomeUiState.Params(
                    recordings = list
                )
            ) {

            }
        }

    }
}

@Composable
fun HomeScreenView(
    activity: MainActivityInterface,
    viewModel: HomeVewModel,
    audioPlayer: AudioPlayer?,
    audioRecorder: WAVRecorder?
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onPlayCompletionListener = MediaPlayer.OnCompletionListener {
        viewModel.handleIntent(HomeScreenIntent.OnPlayCompleted(audioPlayer))
    }
    val uiState = viewModel.uiState.collectAsState().value

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.handleIntent(HomeScreenIntent.OnPermissionUpdate(it))
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
            UiLCEState.Initial -> {
                HomeViewLoading()
            }

            is UiLCEState.Loading -> {
                HomeViewLoading()
            }

            is UiLCEState.Content<HomeUiState> -> {
                val showDialog = uiState.resolve().showDialog
                val error = uiState.resolve().error

                HomeViewContent(
                    listState = listState,
                    composableParams = AudioPlaybackParams(
                        audioLength = audioLength,
                        audioPlayback = audioPlayback,
                    ),
                    params = uiState.resolve().extractParams(),
                    onUiAction = { action ->
                        onUiActionHandler(
                            onPermissionRequest = {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            action = action,
                            activity,
                            viewModel,
                            audioPlayer,
                            audioRecorder
                        )
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

            UiLCEState.Error -> {
                HomeViewError()
            }
        }
    }
}

fun onUiActionHandler(
    onPermissionRequest: () -> Unit,
    action: UiAction,
    activity: MainActivityInterface,
    viewModel: HomeVewModel,
    audioPlayer: AudioPlayer?,
    audioRecorder: WAVRecorder?
) = run {
    when (action) {
        UiAction.OnShare -> {
            activity.shareFileViaEmail(
                viewModel.currentItem()!!.id,
                System.currentTimeMillis().toDateTimeString()
            )
        }//handleShareIntent()
        UiAction.ActionDelete -> {
            viewModel.handleIntent(HomeScreenIntent.OnDialogConfirm)
        }

        UiAction.OnPlayClicked -> {
            audioPlayer?.let {
                it.pausePlay()
                viewModel.handleIntent(HomeScreenIntent.OnPlayClicked(it.isPlaying()))
            }
        }

        is UiAction.OnRecordingStarted -> {
            if (!action.value) {
                onPermissionRequest()
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

        UiAction.OnSettings -> {
            viewModel.handleIntent(HomeScreenIntent.OnSettings)
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
    listState: LazyListState,
    params: HomeUiState.Params,
    composableParams: AudioPlaybackParams,
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
                },
                actions = {
                    IconButton(onClick = { onUiAction(UiAction.OnSettings) }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (!params.micPermission) {
                    MicPermissionMessage()
                }
                if (params.recordings.isEmpty()) {
                    EmptyListMessage()
                } else {
                    LazyListForRecordings(listState, composableParams, params, onUiAction)
                }
            }
        },
        bottomBar = { BottomBar(params, onUiAction) })
}

@Composable
fun LazyListForRecordings(
    listState: LazyListState,
    composableParams: AudioPlaybackParams,
    params: HomeUiState.Params,
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
    LazyColumn(state = listState, modifier = Modifier.fillMaxHeight()) {
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
fun BottomBar(params: HomeUiState.Params, onUiAction: (UiAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(normalSpace)
    ) {
        RecordButton(
            maxDuration = RECORDING_DURATION,
            micPermission = params.micPermission,
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