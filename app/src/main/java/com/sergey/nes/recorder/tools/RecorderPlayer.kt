package com.sergey.nes.recorder.tools

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.net.toUri
import com.sergey.nes.recorder.app.Config.FILE_EXTENSION
import com.sergey.nes.recorder.app.Config.RECORDINGS_FOLDER
import com.sergey.nes.recorder.models.RecordingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream

fun State<Int>?.zeroIfNull(): Int = this?.value ?: 0

fun State<Float>?.zeroIfNull(): Float = this?.value ?: 0f

class AudioPlayer(
    private val context: Context,
) {
    private val _audioLength: MutableState<Int> = mutableIntStateOf(0)
    val audioLength: State<Int> get() = _audioLength

    private val _audioPlayback: MutableState<Float> = mutableFloatStateOf(0f)
    val audioPlayback: State<Float> get() = _audioPlayback

    private var timer: Job? = null
    private fun CoroutineScope.launchPeriodicAsync(
        repeatMillis: Long,
        action: () -> Unit
    ) = async {
        if (repeatMillis > 0) {
            while (isActive) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }

    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var completionListener: MediaPlayer.OnCompletionListener? = null

    fun setCompletionListener(listener: MediaPlayer.OnCompletionListener) {
        this.completionListener = listener
    }

    fun setCurrentFile(item: RecordingItem) {
        val fileName = "${item.id}.$FILE_EXTENSION"
        val audioFile = File(
            context.getExternalFilesDir(RECORDINGS_FOLDER),
            fileName
        )
        setFile(audioFile)
    }

    private fun setFile(file: File) {
            stop()
            currentFile = file
            MediaPlayer.create(context, file.toUri()).apply {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setVolume(1f, 1f)
                setOnCompletionListener(completionListener)
                setOnPreparedListener {
                    _audioLength.value = (it.duration / 1000) + 1
                }
                player = this
            }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        player?.stop()
        player?.release()
        player?.setOnCompletionListener(null)
        player = null
        currentFile = null
        _audioPlayback.value = 0f
    }

    fun pausePlay() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.start()
            if (timer == null) {
                timer = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(300) {
                    player?.let {
                        _audioPlayback.value = it.currentPosition.toFloat() / it.duration
                    }
                }
            }
        }
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    fun updateAudioPlayback(value: Float) {
        _audioPlayback.value = value
        player?.let {
            it.seekTo((value * it.duration).toInt())
        }
    }
}

@Deprecated("I had to use WAVRecorder, as I wasn't able to record in PCM WAV format.")
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentItem: RecordingItem? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start() {
        currentItem = RecordingItem()
        val fileName = "${currentItem!!.id}.$FILE_EXTENSION"
        val outputFile = File(
            context.getExternalFilesDir(RECORDINGS_FOLDER),
            fileName
        )

        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)

            prepare()
            start()
            recorder = this
        }
    }

    fun stop(): RecordingItem? {
        recorder?.stop()
        recorder?.reset()
        recorder = null

        return currentItem
    }
}