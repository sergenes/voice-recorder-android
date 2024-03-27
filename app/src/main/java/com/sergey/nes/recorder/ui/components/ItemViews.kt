package com.sergey.nes.recorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sergey.nes.recorder.app.formatSecondsToHMS
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.ui.theme.noSpace
import com.sergey.nes.recorder.ui.theme.normalRadius
import com.sergey.nes.recorder.ui.theme.normalSpace
import com.sergey.nes.recorder.ui.theme.smallSpace

@Preview
@Composable
fun SelectedMessageView_Preview() {
    StoryRecTheme {
        SelectedItemView(
            title = "3/3/2024 11:24 AM",
            transcription = "Hello hello, I'm doing pretty good. How are you? Hello hello, I'm doing pretty good. How are you?",
        )
    }
}

@Preview
@Composable
fun RegularMessageView_Preview() {
    StoryRecTheme {
        ItemView(
            title = "3/3/2024 11:24 AM",
            transcription = "Hello hello, I'm doing pretty good. How are you? Hello hello, I'm doing pretty good. How are you?",
            onSelect = {})
    }
}

@Composable
fun SelectedItemView(
    modifier: Modifier = Modifier,
    title: String,
    transcription: String = "",
    speaking: Boolean = false,
    audioLength: Int = 0,
    audioPlayback: Float = 0f,
    actionPlay: () -> Unit = {},
    actionDelete: () -> Unit = {},
    onSliderValueChange: (Float) -> Unit = {},
    transcribing: Boolean = false,
    onTranscribe: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(normalRadius),
        modifier = modifier.padding(normalSpace, noSpace, normalSpace, normalSpace)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.weight(1f)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                actionPlay()
                            }) {
                            if (speaking) {
                                Icon(
                                    Icons.Filled.Stop,
                                    contentDescription = "stop",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.scale(1f)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "play",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.scale(1f)
                                )
                            }
                        }
                        Column {
                            Spacer(Modifier.height(normalSpace))
                            val currentPosition = audioLength * audioPlayback
                            Slider(
                                value = audioPlayback,
                                onValueChange = onSliderValueChange,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = normalSpace)
                            )
                            Row {
                                Text(
                                    text = currentPosition.formatSecondsToHMS(),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = audioLength.formatSecondsToHMS(),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(Modifier.width(normalSpace))
                            }
                        }
                        Spacer(Modifier.width(normalSpace))

                    }
                }
                Spacer(Modifier.height(smallSpace))
                Divider(color = MaterialTheme.colorScheme.onSurface)
                Column(
                    modifier = Modifier
                        .padding(start = normalSpace, top = smallSpace, end = normalSpace)
                ) {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (transcription.isNotEmpty()) {
                        Spacer(Modifier.height(smallSpace))
                        Text(
                            transcription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(smallSpace))

                    }
                }
                Divider(color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = normalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transcription.isEmpty()) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                enabled = !transcribing,
                                onClick = onTranscribe
                            ) {
                                Icon(
                                    Icons.Filled.Receipt,
                                    contentDescription = "delete",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (transcribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 6.dp
                                )
                            }
                        }
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "delete",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = actionDelete
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "delete",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemView(
    title: String,
    transcription: String = "",
    onSelect: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.primaryContainer
    val padding: Modifier = Modifier.padding(normalSpace, noSpace, normalSpace, normalSpace)
    Card(
        shape = RoundedCornerShape(normalRadius),
        modifier = padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onSelect)
                .fillMaxWidth()
                .background(color = surface)
        ) {
            Column(Modifier.weight(1f)) {
                Box(modifier = Modifier.padding(normalSpace)) {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (transcription.isNotEmpty()) {
                            Spacer(Modifier.height(smallSpace))
                            Divider(color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(smallSpace))
                            Text(
                                transcription,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                }
            }
        }
    }
}