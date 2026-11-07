package com.descargarmultimedia.app.ui.main

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.descargarmultimedia.app.di.appContainer
import com.descargarmultimedia.app.service.DownloadService
import com.descargarmultimedia.app.ui.theme.DescargarMultimediaTheme
import com.descargarmultimedia.app.util.NotificationPermission

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(appContainer) }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (NotificationPermission.mustRequest(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DescargarMultimediaTheme {
                MainScreen(
                    viewModel = viewModel,
                    onStartDownload = { url, format ->
                        DownloadService.enqueue(this, url, format)
                    },
                    onRetry = { id ->
                        if (viewModel.retry(id)) DownloadService.start(this)
                    },
                )
            }
        }
    }
}
