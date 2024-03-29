package com.sergey.nes.recorder.app

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@SuppressLint("SimpleDateFormat")
fun Long?.toDateTimeString(): String {
    this?.let {
       return SimpleDateFormat("M/dd/yyyy hh:mm a").format(
            Date(this)
        )
    }?: run {
        return "Unknown"
    }
}

@SuppressLint("SimpleDateFormat")
fun Float?.formatSecondsToHMS(): String {
    this?.let {
        val seconds = it * 1000L
        val d = Date(seconds.toLong())
        return if (it < 3600) {
            val df = SimpleDateFormat("mm:ss")
            df.timeZone = TimeZone.getTimeZone("GMT")
            df.format(d)
        } else {
            val df = SimpleDateFormat("HH:mm:ss")
            df.timeZone = TimeZone.getTimeZone("GMT")
            df.format(d)
        }
    }?: run {
        return "00:00"
    }
}

@SuppressLint("SimpleDateFormat")
fun Int?.formatSecondsToHMS(): String {
    this?.let {
        val seconds = it * 1000L
        val d = Date(seconds)
        return if (it < 3600) {
            val df = SimpleDateFormat("mm:ss")
            df.timeZone = TimeZone.getTimeZone("GMT")
            df.format(d)
        } else {
            val df = SimpleDateFormat("HH:mm:ss")
            df.timeZone = TimeZone.getTimeZone("GMT")
            df.format(d)
        }
    }?: run {
        return "00:00"
    }
}