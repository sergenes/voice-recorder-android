package com.sergey.nes.recorder.app

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
object Config {
    // max recording duration in seconds
    const val RECORDING_DURATION = 60
    const val RECORDINGS_FOLDER = "Records"
    const val FILE_EXTENSION = "mp3"
    val DATE_TIME_FORMAT = SimpleDateFormat("c, M/dd/yyyy hh:mm a")
}