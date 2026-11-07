package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadStatus
import kotlinx.coroutines.flow.StateFlow

/** Punto de entrada para la interfaz: agregar, cancelar y reintentar descargas. */
class DownloadManager(
    private val queue: DownloadQueue,
    private val downloader: MediaDownloader,
) {
    val items: StateFlow<List<DownloadItem>> = queue.items

    fun enqueue(url: String, format: DownloadFormat): DownloadItem = queue.enqueue(url, format)

    fun cancel(id: String) {
        val item = queue.find(id) ?: return
        if (item.status.isTerminal) return
        val wasRunning = item.status != DownloadStatus.QUEUED
        queue.markCanceled(id)
        if (wasRunning) downloader.cancel(id)
    }

    /** Vuelve a encolar una descarga fallida o cancelada. */
    fun retry(id: String): DownloadItem? {
        val item = queue.find(id) ?: return null
        if (item.status != DownloadStatus.FAILED && item.status != DownloadStatus.CANCELED) return null
        queue.remove(id)
        return queue.enqueue(item.url, item.format)
    }

    fun clearFinished() = queue.clearFinished()
}
