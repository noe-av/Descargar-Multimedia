package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ejecuta UNA descarga de principio a fin: mantiene yt-dlp al día, busca la canción
 * completa si hace falta, descarga en una carpeta temporal, guarda el resultado y deja
 * el estado final en la cola.
 */
class DownloadProcessor(
    private val queue: DownloadQueue,
    private val downloader: MediaDownloader,
    private val engineUpdater: EngineUpdater,
    private val songFinder: SongFinder,
    private val saver: MediaSaver,
    private val workRoot: File,
) {
    /** Qué se descarga de verdad: el enlace original, o la canción completa encontrada. */
    private class Source(val url: String, val fileName: String?)

    suspend fun process(item: DownloadItem) = withContext(Dispatchers.IO) {
        val workDir = File(workRoot, item.id)
        try {
            workDir.mkdirs()
            downloader.ensureReady()
            refreshEngineIfStale(item)

            val file = downloadWithRetry(item, workDir)

            throwIfCanceled(item.id)
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
        } catch (e: SongNotFoundException) {
            // No se descarga nada: solo queda el aviso de que la canción no apareció.
            queue.update(item.id) {
                it.copy(
                    status = DownloadStatus.NOT_FOUND,
                    etaSeconds = null,
                    title = e.songLabel ?: it.title,
                )
            }
        } catch (e: Exception) {
            queue.update(item.id) {
                it.copy(status = DownloadStatus.FAILED, etaSeconds = null, errorMessage = e.message)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    /** Si pasó más de un día desde la última actualización de yt-dlp, la hace antes de empezar. */
    private suspend fun refreshEngineIfStale(item: DownloadItem) {
        if (!engineUpdater.isStale()) return
        queue.update(item.id) { it.copy(status = DownloadStatus.UPDATING) }
        engineUpdater.refreshIfStale()
        throwIfCanceled(item.id)
    }

    /**
     * Descarga. Si el motor falla, lo actualiza y reintenta UNA sola vez: lo más común es
     * que el sitio haya cambiado y el yt-dlp instalado ya no lo entienda. El usuario no
     * tiene que hacer nada.
     */
    private suspend fun downloadWithRetry(item: DownloadItem, workDir: File): File =
        try {
            downloadOnce(item, workDir)
        } catch (e: DownloadFailedException) {
            throwIfCanceled(item.id)
            queue.update(item.id) {
                it.copy(status = DownloadStatus.UPDATING, progress = 0f, etaSeconds = null)
            }
            if (!engineUpdater.refreshAfterFailure()) throw e
            throwIfCanceled(item.id)

            workDir.deleteRecursively() // restos de la descarga fallida
            workDir.mkdirs()
            downloadOnce(item, workDir)
        }

    private suspend fun downloadOnce(item: DownloadItem, workDir: File): File {
        queue.update(item.id) {
            it.copy(status = DownloadStatus.DOWNLOADING, progress = 0f, etaSeconds = null)
        }
        val source = resolveSource(item)
        throwIfCanceled(item.id)

        return downloader.download(
            DownloadRequest(item.id, source.url, item.format, workDir, source.fileName),
        ) { progress ->
            queue.update(item.id) { it.withProgress(progress) }
        }
    }

    /** Descarga el enlace tal cual, o lo reemplaza por la canción completa si así corresponde. */
    private suspend fun resolveSource(item: DownloadItem): Source {
        if (!SongSearchPolicy.shouldSearchSong(item.url, item.format)) {
            return Source(item.url, fileName = null)
        }

        queue.update(item.id) { it.copy(status = DownloadStatus.SEARCHING) }
        val match = songFinder.findFullSong(item.id, item.url, item.format)
        queue.update(item.id) { it.copy(status = DownloadStatus.DOWNLOADING, title = match.label) }
        return Source(match.url, fileName = match.label)
    }

    /** Si el usuario canceló entre dos pasos, no se sigue (ni se guarda nada). */
    private fun throwIfCanceled(id: String) {
        if (queue.find(id)?.status == DownloadStatus.CANCELED) throw DownloadCanceledException()
    }
}
