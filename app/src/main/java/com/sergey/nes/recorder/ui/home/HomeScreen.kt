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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergey.nes.recorder.R
import com.sergey.nes.recorder.app.Config
import com.sergey.nes.recorder.app.MainActivityInterface
import com.sergey.nes.recorder.app.Config.RECORDING_DURATION
import com.sergey.nes.recorder.ui.components.ItemView
import com.sergey.nes.recorder.ui.components.RecordButton
import com.sergey.nes.recorder.ui.components.SelectedItemView
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.ui.theme.normalSpace
import com.sergey.nes.recorder.whispertflite.asr.WAVRecorder
import kotlinx.coroutines.launch
import java.util.Date

class MainActivityForPreview : MainActivityInterface {
    override fun micPermissions(): Boolean {
        return false
    }

    override fun showErrorMessage(message: String) {

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StoryRecTheme {
        HomeScreenView(
            MainActivityForPreview(),
            HomeVewModel(HomeRepository(), null),
            null,
            null
        )
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
        viewModel.onPlayCompleted(audioPlayer)
    }
    var micPermission by remember { mutableStateOf(false) }
    micPermission = activity.micPermissions()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        micPermission = it
    }

    val dataSourceState = viewModel.dataSource.collectAsState().value
    val recordings = dataSourceState.recordings
    val selectedIndex = dataSourceState.selectedIndex
    val isPlaying = viewModel.isPlaying
    val audioLength = audioPlayer?.audioLength?.value ?: 0
    val audioPlayback = audioPlayer?.audioPlayback?.value ?: 0f
    val showDialog = viewModel.showDialog.collectAsState().value
    val transcribing = viewModel.transcribing.collectAsState().value

    LaunchedEffect("Init") {
        viewModel.onLoad(ready = {
            if (it.recordings.isNotEmpty() && it.selectedIndex < 0) {
                viewModel.selectRecording(0, audioPlayer)
            }
        }, onError = {
            activity.showErrorMessage(it)
        })
        audioPlayer?.setCompletionListener(onPlayCompletionListener)
    }

    // animated scroll for auto play
    LaunchedEffect(selectedIndex) {
        if (viewModel.isPlaying.value && selectedIndex in 0..recordings.lastIndex) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Box {
        HomeViewContent(
            recordings = recordings,
            listState = listState,
            selectedIndex = selectedIndex,
            micPermission = micPermission,
            isPlaying = isPlaying.value,
            audioLength = audioLength,
            audioPlayback = audioPlayback,
            onSelected = {
                viewModel.selectRecording(it, audioPlayer)
                viewModel.updatePlaying(false)
            },
            onPlayClicked = {
                audioPlayer?.let {
                    it.pausePlay()
                    viewModel.updatePlaying(it.isPlaying())
                }
            },
            actionDelete = {
                viewModel.onDialogConfirm()
            },
            onRecordingStarted = { micPermission ->
                if (!micPermission) {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    audioPlayer?.stop()
                    viewModel.updatePlaying(false)
                    audioRecorder?.start()
                }
            },
            onRecordingStopped = {
                audioRecorder?.stop()?.let {
                    viewModel.saveInfo(it, ready = { dataSource ->
                        if (dataSource.recordings.isNotEmpty()) {
                            viewModel.selectRecording(0, audioPlayer)
                            viewModel.updatePlaying(false)
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    }, onError = { error ->
                        activity.showErrorMessage(error)
                    })
                }
            },
            onSliderValueChange = {
                audioPlayer?.updateAudioPlayback(it)
            },
            transcribing = transcribing,
            onTranscribe = {
                viewModel.transcribe {
                    activity.showErrorMessage(it)
                }
            },
            onShare = {

            }
        )
        if (showDialog) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .alpha(0.7f)
            ) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.onDialogDismiss()
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.onDialogDismiss()
                            viewModel.deleteRecording {
                                activity.showErrorMessage(it)
                            }
                        })
                        { Text(text = stringResource(R.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.onDialogDismiss()
                        })
                        { Text(text = stringResource(R.string.cancel)) }
                    },
                    title = { Text(text = stringResource(R.string.please_confirm_deleting)) },
                    text = { Text(text = stringResource(R.string.are_you_sure_you_can_not_undo_this_action)) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewContent(
    recordings: List<RecordingItem>,
    listState: LazyListState,
    selectedIndex: Int,
    micPermission: Boolean,
    isPlaying: Boolean,
    audioLength: Int = 0,
    audioPlayback: Float = 0f,
    onSelected: (Int) -> Unit,
    onPlayClicked: () -> Unit,
    actionDelete: () -> Unit,
    onRecordingStarted: (Boolean) -> Unit,
    onRecordingStopped: () -> Unit,
    onSliderValueChange: (Float) -> Unit = {},
    transcribing: Boolean = false,
    onTranscribe: () -> Unit = {},
    onShare: () -> Unit = {}
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
                if (!micPermission) {
                    MicPermissionMessage()
                }
                if (recordings.isEmpty()) {
                    EmptyListMessage()
                } else {
                    var query by remember { mutableStateOf(TextFieldValue("")) }
                    val filteredItems =
                        recordings.filter { it.transcription.contains(query.text, ignoreCase = true) }
                    TextField(
                        value = query,
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(),
                        singleLine = true,
                        onValueChange = { newQuery -> query = newQuery },
                        label = { Text("Search") },
                        modifier = Modifier.semantics {
                            this.contentDescription = "Search through the conversation history." }
                            .fillMaxWidth(0.9f)
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = normalSpace)
                    )
                    Spacer(modifier = Modifier.height(normalSpace))
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        items(filteredItems.size) { index ->
                            val item = filteredItems[index]
                            val title = Config.DATE_TIME_FORMAT.format(
                                Date(item.dateTime)
                            )
                            val transcription = item.transcription
                            if (selectedIndex == index) {
                                SelectedItemView(
                                    title = title,
                                    transcription = transcription,
                                    speaking = isPlaying,
                                    audioLength = audioLength,
                                    audioPlayback = audioPlayback,
                                    actionPlay = onPlayClicked,
                                    actionDelete = actionDelete,
                                    onSliderValueChange = onSliderValueChange,
                                    transcribing = transcribing,
                                    onTranscribe = onTranscribe,
                                    onShare = onShare
                                )
                            } else {
                                ItemView(title = title, transcription = transcription, onSelect = {
                                    onSelected(index)
                                })
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(normalSpace)
            ) {
                RecordButton(
                    maxDuration = RECORDING_DURATION,
                    micPermission = micPermission,
                    onRecordingStarted = onRecordingStarted,
                    onRecordingStopped = onRecordingStopped
                )
            }
        })
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