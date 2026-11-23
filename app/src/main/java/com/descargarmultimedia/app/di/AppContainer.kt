package com.descargarmultimedia.app.di

import android.content.Context
import com.descargarmultimedia.app.DescargarMultimediaApp
import com.descargarmultimedia.app.data.engine.PrefsEngineUpdateStore
import com.descargarmultimedia.app.data.engine.YtDlpDownloader
import com.descargarmultimedia.app.data.engine.YtDlpSongFinder
import com.descargarmultimedia.app.data.storage.MediaStoreSaver
import com.descargarmultimedia.app.domain.DownloadManager
import com.descargarmultimedia.app.domain.DownloadProcessor
import com.descargarmultimedia.app.domain.DownloadQueue
import com.descargarmultimedia.app.domain.EngineUpdater
import com.descargarmultimedia.app.domain.MediaDownloader
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual: aquí se crean y conectan todas las piezas.
 * Hay una sola instancia, creada por [DescargarMultimediaApp].
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val workRoot: File =
        appContext.getExternalFilesDir("downloads") ?: File(appContext.filesDir, "downloads")

    val downloadQueue = DownloadQueue()
    val downloader: MediaDownloader = YtDlpDownloader(appContext)
    val engineUpdater = EngineUpdater(downloader, PrefsEngineUpdateStore(appContext))
    val downloadManager = DownloadManager(downloadQueue, downloader)
    val downloadProcessor = DownloadProcessor(
        queue = downloadQueue,
        downloader = downloader,
        engineUpdater = engineUpdater,
        songFinder = YtDlpSongFinder(),
        saver = MediaStoreSaver(appContext),
        workRoot = workRoot,
    )

    /** Al abrir la app: limpia restos de descargas interrumpidas y prepara el motor de antemano. */
    fun warmUp() {
        appScope.launch {
            workRoot.listFiles().orEmpty()
                .filter { downloadQueue.find(it.name) == null }
                .forEach { it.deleteRecursively() }
            runCatching { downloader.ensureReady() }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as DescargarMultimediaApp).container
