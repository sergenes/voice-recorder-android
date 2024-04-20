package com.sergey.nes.recorder.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.ui.home.HomeRepository
import com.sergey.nes.recorder.ui.home.HomeScreenView
import com.sergey.nes.recorder.ui.home.HomeVewModel
import com.sergey.nes.recorder.ui.settings.SettingsScreenView
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import com.sergey.nes.recorder.whispertflite.asr.WAVRecorder
import com.sergey.nes.recorder.whispertflite.asr.Whisper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

interface MainActivityInterface {
    fun shareFileViaEmail(path: String, date: String)
}

class MainActivity : ComponentActivity(), MainActivityInterface {
    //    private val audioRecorder by lazy { AudioRecorder(applicationContext) }
    private val audioRecorder by lazy { WAVRecorder(applicationContext) }
    private val audioPlayer by lazy { AudioPlayer(applicationContext) }
    private val whisper by lazy { Whisper(this) }


    private fun micPermissions(): Boolean {
        // Check if the permission has already been granted
        val permissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }

    fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StoryRecTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val homeRepository = HomeRepository(this)
                    val homeVewModel = HomeVewModel(
                        micPermission = micPermissions(),
                        repository = homeRepository,
                        whisper = whisper,
                        navController = navController
                    )
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            HomeScreenView(
                                activity = this@MainActivity,
                                viewModel = homeVewModel,
                                audioPlayer = audioPlayer,
                                audioRecorder = audioRecorder
                            )
                        }
                        composable("settings") {
                            SettingsScreenView(
                                activity = this@MainActivity
                            )
                        }
                    }

                }
            }
        }
        // Call the method to copy specific file types from assets to data folder
        val extensionsToCopy = arrayOf("pcm", "bin", "wav", "tflite")
        copyAssetsWithExtensionsToDataFolder(this, extensionsToCopy)

        val modelPath: String
        val vocabPath: String
        val useMultilingual = true
        val useHe = false

        if (useHe) {
            // Multilingual model and vocab
            modelPath = getFilePath("whisper-tiny-he.tflite")
            vocabPath = getFilePath("filters_vocab_multilingual.bin")
        } else if (useMultilingual) {
            // Multilingual model and vocab
            modelPath = getFilePath("whisper.tflite")
            vocabPath = getFilePath("filters_vocab_multilingual.bin")
        } else {
            // English-only model and vocab
            modelPath = getFilePath("whisper-tiny-en.tflite")
            vocabPath = getFilePath("filters_vocab_en.bin")
        }

        whisper.loadModel(modelPath, vocabPath, useMultilingual)
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

            // Check if at least one asset with any of the provided extensions exists
            val existingFiles = File(destFolder).list()
            val shouldCopyAssets = existingFiles?.none { fileName ->
                extensions.any { extension ->
                    fileName.endsWith(".$extension")
                }
            } ?: true

            if (shouldCopyAssets) {
                for (extension in extensions) {
                    // List all files in the assets folder with the specified extension
                    val assetFiles = assetManager.list("") ?: emptyArray()
                    for (assetFileName in assetFiles) {
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
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("IntentReset")
    override fun shareFileViaEmail(path: String, date: String) {

        val fileName = "${path}.${Config.FILE_EXTENSION}"
        val audioFile = File(
            getExternalFilesDir(Config.RECORDINGS_FOLDER),
            fileName
        )

        println("shareViaEmail.path=>${path}")
        try {
            val fileUri =
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", audioFile)
            println("shareViaEmail.fileUri=>${fileUri}")
            grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val utype = contentResolver.getType(fileUri)
            println("shareViaEmail.utype=>${utype}")
            val message =
                "This audio file was recorded with the 'Story Rec Demo App',\n\n\nplease visit https://github.com/sergenes/voice-recorder-android for more information."

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Story Rec Demo App at: $date")
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                setDataAndType(fileUri, utype)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(emailIntent, "Share Recording"))
        } catch (e: Exception) {
            println("is exception raises during sending mail$e")
        }
    }
}