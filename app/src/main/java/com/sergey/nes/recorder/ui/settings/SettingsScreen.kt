package com.sergey.nes.recorder.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sergey.nes.recorder.app.MainActivityInterface
import com.sergey.nes.recorder.ui.home.UiAction


@Composable
fun SettingsScreenView(
    activity: MainActivityInterface,
) {
    SettingsViewContent(onUiAction = {

    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsViewContent(
    onUiAction: (UiAction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text(
                    text = "Some Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {  })
}