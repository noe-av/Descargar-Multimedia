package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ejecuta UNA descarga de principio a fin: descarga en una carpeta temporal,
 * guarda el resultado y deja el estado final en la cola.
 */
class DownloadProcessor(
    private val queue: DownloadQueue,
    private val downloader: MediaDownloader,
    private val saver: MediaSaver,
    private val workRoot: File,
) {
    suspend fun process(item: DownloadItem) = withContext(Dispatchers.IO) {
        val workDir = File(workRoot, item.id)
        try {
            workDir.mkdirs()
            downloader.ensureReady()

            val file = downloader.download(
                DownloadRequest(item.id, item.url, item.format, workDir),
            ) { progress ->
                queue.update(item.id) { it.withProgress(progress) }
            }

            queue.update(item.id) {
                it.copy(status = DownloadStatus.PROCESSING, progress = 100f, etaSeconds = null)
            }
            val uri = saver.save(file, item.format)
            queue.update(item.id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f,
                    etaSeconds = null,
                    outputUri = uri,
                )
            }
        } catch (e: CancellationException) {
            queue.markCanceled(item.id)
            throw e
        } catch (e: DownloadCanceledException) {
            queue.markCanceled(item.id)
        } catch (e: Exception) {
            queue.update(item.id) {
                it.copy(status = DownloadStatus.FAILED, etaSeconds = null, errorMessage = e.message)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }
}
