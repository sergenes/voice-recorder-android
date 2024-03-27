package com.sergey.nes.recorder.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.ui.home.HomeRepository
import com.sergey.nes.recorder.ui.home.HomeScreenView
import com.sergey.nes.recorder.ui.home.HomeVewModel
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.whispertflite.asr.IWhisperListener
import com.sergey.nes.recorder.whispertflite.asr.WAVRecorder
import com.sergey.nes.recorder.whispertflite.asr.Whisper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

interface MainActivityInterface {
    fun micPermissions(): Boolean
    fun showErrorMessage(message: String)
}

class MainActivity : ComponentActivity(), MainActivityInterface {
    //    private val audioRecorder by lazy { AudioRecorder(applicationContext) }
    private val audioRecorder by lazy { WAVRecorder(applicationContext) }
    private val audioPlayer by lazy { AudioPlayer(applicationContext) }
    private val whisper by lazy { Whisper(this) }


    override fun micPermissions(): Boolean {
        // Check if the permission has already been granted
        val permissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }

    override fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeRepository = HomeRepository(this)
        val homeVewModel = HomeVewModel(repository = homeRepository, whisper = whisper)
        setContent {
            StoryRecTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    HomeScreenView(
                        activity = this,
                        viewModel = homeVewModel,
                        audioPlayer = audioPlayer,
                        audioRecorder = audioRecorder
                    )
                }
            }
        }
        // Call the method to copy specific file types from assets to data folder
        val extensionsToCopy = arrayOf("pcm", "bin", "wav", "tflite")
        copyAssetsWithExtensionsToDataFolder(this, extensionsToCopy)

        val modelPath: String
        val vocabPath: String
        val useMultilingual = false // TODO: change multilingual flag as per model used

        if (useMultilingual) {
            // Multilingual model and vocab
            modelPath = getFilePath("whisper-tiny.tflite")
            vocabPath = getFilePath("filters_vocab_multilingual.bin")
        } else {
            // English-only model and vocab
            modelPath = getFilePath("whisper-tiny-en.tflite")
            vocabPath = getFilePath("filters_vocab_en.bin")
        }

        whisper.loadModel(modelPath, vocabPath, useMultilingual)
        whisper.setListener(object : IWhisperListener {
            override fun onUpdateReceived(message: String) {
                Log.d("MainActivity", "Update is received, Message: $message")
//                if (message == Whisper.MSG_PROCESSING) {
//                } else if (message == Whisper.MSG_FILE_NOT_FOUND) {
//                    // write code as per need to handled this error
//                }
            }

            override fun onResultReceived(result: String, audioFileId: String) {
                Log.d("MainActivity", "Result: $result")
                homeVewModel.saveTranscription(transcription = result, audioFileId = audioFileId) {
                    showErrorMessage(it)
                }
            }
        })
    }

    private fun getFilePath(assetName: String): String {

        val outfile = File(filesDir, assetName)
        if (!outfile.exists()) {
            Log.d("TAG", "File not found - " + outfile.absolutePath)
        }
        Log.d("TAG", "Returned asset path: " + outfile.absolutePath)
        return outfile.absolutePath
    }

    private fun copyAssetsWithExtensionsToDataFolder(context: Context, extensions: Array<String>) {
        val assetManager = context.assets
        try {
            // Specify the destination directory in the app's data folder
            val destFolder = context.filesDir.absolutePath
            for (extension in extensions) {
                // List all files in the assets folder with the specified extension
                val assetFiles = assetManager.list("")
                for (assetFileName in assetFiles!!) {
                    if (assetFileName.endsWith(".$extension")) {
                        val outFile = File(destFolder, assetFileName)
                        if (outFile.exists()) continue
                        val inputStream = assetManager.open(assetFileName)
                        val outputStream: OutputStream = FileOutputStream(outFile)

                        // Copy the file from assets to the data folder
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        inputStream.close()
                        outputStream.flush()
                        outputStream.close()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}