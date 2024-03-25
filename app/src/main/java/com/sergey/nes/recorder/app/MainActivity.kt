package com.sergey.nes.recorder.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.sergey.nes.recorder.ui.theme.StoryRecTheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sergey.nes.recorder.tools.AudioPlayer
import com.sergey.nes.recorder.tools.AudioRecorder
import com.sergey.nes.recorder.ui.home.HomeRepository
import com.sergey.nes.recorder.ui.home.HomeScreenView
import com.sergey.nes.recorder.ui.home.HomeVewModel

interface MainActivityInterface {
    fun micPermissions(): Boolean
    fun showErrorMessage(message: String)
}

class MainActivity : ComponentActivity(), MainActivityInterface {
    private val audioRecorder by lazy { AudioRecorder(applicationContext) }
    private val audioPlayer by lazy { AudioPlayer(applicationContext) }

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
        setContent {
            StoryRecTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val homeRepository = HomeRepository(this)
                    val homeVewModel = HomeVewModel(homeRepository)
                    HomeScreenView(
                        activity = this,
                        viewModel = homeVewModel,
                        audioPlayer = audioPlayer,
                        audioRecorder = audioRecorder
                    )
                }
            }
        }
    }
}