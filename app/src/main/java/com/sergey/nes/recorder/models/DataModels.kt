package com.sergey.nes.recorder.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RecordingItem(
    val id: String = UUID.randomUUID().toString(),
    val dateTime: Long = System.currentTimeMillis(),
    val transcription: String = "",
    val duration: Long = -1
)