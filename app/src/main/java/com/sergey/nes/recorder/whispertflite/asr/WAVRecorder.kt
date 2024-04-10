package com.sergey.nes.recorder.whispertflite.asr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.sergey.nes.recorder.app.Config
import com.sergey.nes.recorder.models.RecordingItem
import com.sergey.nes.recorder.whispertflite.utils.WaveUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class WAVRecorder(private val context: Context) {
    private val inProgress = AtomicBoolean(false)
    private var wavFilePath: String? = null
    private var executorThread: Thread? = null
    private var recorderListener: IRecorderListener? = null
    private var currentItem: RecordingItem? = null
    fun setListener(listener: IRecorderListener?) {
        recorderListener = listener
    }

    private fun setFilePath(wavFile: String?) {
        wavFilePath = wavFile
    }

    fun start() {
        currentItem = RecordingItem()
        val fileName = "${currentItem!!.id}.${Config.FILE_EXTENSION}"
        val outputFile = File(
            context.getExternalFilesDir(Config.RECORDINGS_FOLDER),
            fileName
        )
        setFilePath(outputFile.absolutePath)
        if (inProgress.get()) {
            Log.d(TAG, "Recording is already in progress...")
            return
        }
        executorThread = Thread {
            inProgress.set(true)
            threadFunction()
            inProgress.set(false)
        }
        executorThread!!.start()
    }

    fun stop(): RecordingItem? {
        inProgress.set(false)
        return try {
            if (executorThread != null) {
                executorThread!!.join()
                executorThread = null
            }
            currentItem
        } catch (e: InterruptedException) {
            e.printStackTrace()
            null
        }


    }

    val isInProgress: Boolean
        get() = inProgress.get()

    private fun sendUpdate(message: String?) {
        if (recorderListener != null) recorderListener!!.onUpdateReceived(message)
    }

    private fun sendData(samples: FloatArray) {
        if (recorderListener != null) recorderListener!!.onDataReceived(samples)
    }

    private fun threadFunction() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "AudioRecord permission is not granted")
                return
            }
            sendUpdate(MSG_RECORDING)
            val channels = 1
            val bytesPerSample = 2
            val sampleRateInHz = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO // as per channels
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT // as per bytesPerSample
            val audioSource = MediaRecorder.AudioSource.MIC
            val bufferSize =
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
            val audioRecord =
                AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSize)
            audioRecord.startRecording()
            val bufferSize1Sec = sampleRateInHz * bytesPerSample * channels
            val bufferSize30Sec = bufferSize1Sec * Config.RECORDING_DURATION

            val buffer30Sec = ByteBuffer.allocateDirect(bufferSize30Sec)
            val bufferRealtime = ByteBuffer.allocateDirect(bufferSize1Sec * 5)
            var timer = 0
            var totalBytesRead = 0
            val audioData = ByteArray(bufferSize)
            while (inProgress.get() && totalBytesRead < bufferSize30Sec) {
                sendUpdate(MSG_RECORDING + timer + "s")
                val bytesRead = audioRecord.read(audioData, 0, bufferSize)
                if (bytesRead > 0) {
                    buffer30Sec.put(audioData, 0, bytesRead)
                    bufferRealtime.put(audioData, 0, bytesRead)
                } else {
                    Log.d(TAG, "AudioRecord error, bytes read: $bytesRead")
                    break
                }

                // Update timer after every second
                totalBytesRead += bytesRead
                val timer_tmp = totalBytesRead / bufferSize1Sec
                if (timer != timer_tmp) {
                    timer = timer_tmp

                    // Transcribe realtime buffer after every 3 seconds
                    if (timer % 3 == 0) {
                        // Flip the buffer for reading
                        bufferRealtime.flip()
                        bufferRealtime.order(ByteOrder.nativeOrder())

                        // Create a sample array to hold the converted data
                        val samples = FloatArray(bufferRealtime.remaining() / 2)

                        // Convert ByteBuffer to short array
                        for (i in samples.indices) {
                            samples[i] = (bufferRealtime.getShort() / 32768.0).toFloat()
                        }

                        // Reset the ByteBuffer for writing again
                        bufferRealtime.clear()

                        // Send samples for transcription
                        sendData(samples)
                    }
                }
            }
            audioRecord.stop()
            audioRecord.release()

            // Save 30 seconds of recording buffer in wav file
            WaveUtil.createWaveFile(
                wavFilePath,
                buffer30Sec.array().sliceArray(0 until totalBytesRead),
                sampleRateInHz,
                channels,
                bytesPerSample
            )
            Log.d(TAG, "Recorded file: $wavFilePath")
            sendUpdate(MSG_RECORDING_DONE)
        } catch (e: Exception) {
            Log.e(TAG, "Error...", e)
            sendUpdate(e.message)
        }
    }

    companion object {
        const val TAG = "Recorder"
        const val ACTION_STOP = "Stop"
        const val ACTION_RECORD = "Record"
        const val MSG_RECORDING = "Recording..."
        const val MSG_RECORDING_DONE = "Recording done...!"
    }
}
