package com.sergey.nes.recorder.models

import java.util.UUID

data class RecordingItem(
    val id: UUID = UUID.randomUUID(),
    val dateTime: String = "Unknown"
)