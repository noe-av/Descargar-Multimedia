package com.descargarmultimedia.app.ui.share

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.descargarmultimedia.app.R
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.service.DownloadService
import com.descargarmultimedia.app.ui.theme.DescargarMultimediaTheme
import com.descargarmultimedia.app.util.NotificationPermission
import com.descargarmultimedia.app.util.UrlExtractor

/**
 * Aparece en el menú "Compartir" de YouTube, Facebook, etc.
 * Muestra la ventana Música / Video encima de la app de origen,
 * encola la descarga y se cierra para que el usuario siga donde estaba.
 */
class ShareReceiverActivity : ComponentActivity() {

    private var pending: Pair<String, DownloadFormat>? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Se descarga igual aunque el usuario rechace las notificaciones.
            startPendingDownload()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        val url = UrlExtractor.firstUrl(sharedText)

        setContent {
            DescargarMultimediaTheme {
                ShareDialog(
                    url = url,
                    onFormatSelected = { format -> onFormatSelected(requireNotNull(url), format) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun onFormatSelected(url: String, format: DownloadFormat) {
        pending = url to format
        if (NotificationPermission.mustRequest(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPendingDownload()
        }
    }

    private fun startPendingDownload() {
        pending?.let { (url, format) ->
            DownloadService.enqueue(this, url, format)
            Toast.makeText(this, R.string.share_added, Toast.LENGTH_SHORT).show()
        }
        pending = null
        finish()
    }
}
