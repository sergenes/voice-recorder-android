package com.sergey.nes.recorder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import kotlinx.coroutines.launch


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StoryRecTheme {
        RecordButton(
            maxDuration = 15,
            micPermission = true,
            onRecordingStarted = {},
            onRecordingStopped = {})
    }
}

@Composable
fun CircularGauge(progress: Animatable<Float, AnimationVector1D>, diameter: Dp = 44.dp) {
    val animatedProgress = progress.value
    Canvas(modifier = Modifier.size(diameter + 10.dp)) {
        val strokeWidth = 16f
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)
        val startAngle = -90f // Starting from top (12 o'clock)
        drawArc(
            color = Color.Gray,
            startAngle = startAngle,
            sweepAngle = 360 * animatedProgress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun RecordButton(
    maxDuration: Int = 60,
    micPermission: Boolean,
    onRecordingStarted: (Boolean) -> Unit,
    onRecordingStopped: () -> Unit,
    diameter: Dp = 64.dp,
    scale: Float = 2f,
    tint: Color = Color.White,
) {
    var isRecording by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = maxDuration * 1000, // Convert seconds to milliseconds
                    easing = LinearEasing
                )
            ) {
                if (value >= 1f) {
                    isRecording = false
                    launch {
                        progress.snapTo(0f)
                    }
                    onRecordingStopped()
                }
            }
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(4.dp)
            .background(color = Color.DarkGray, shape = RoundedCornerShape(50))
    ) {
        CircularGauge(progress = progress, diameter = diameter)
        FloatingActionButton(
            shape = RoundedCornerShape(50),
            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(diameter)
                .height(diameter),
            onClick = {
                if (micPermission) {
                    isRecording = !isRecording
                    if (isRecording) {
                        onRecordingStarted(true)
                    } else {
                        onRecordingStopped()
                    }
                } else {
                    onRecordingStarted(false)
                }
            }) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = tint,
                modifier = Modifier.scale(scale)
            )
        }
    }
}