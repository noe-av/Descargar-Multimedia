package com.descargarmultimedia.app.data.engine

import android.content.Context
import com.descargarmultimedia.app.domain.DownloadCanceledException
import com.descargarmultimedia.app.domain.DownloadFailedException
import com.descargarmultimedia.app.domain.EngineUpdateResult
import com.descargarmultimedia.app.domain.MediaDownloader
import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Implementación de [MediaDownloader] con yt-dlp + FFmpeg (librería youtubedl-android). */
class YtDlpDownloader(private val context: Context) : MediaDownloader {

    private val initMutex = Mutex()

    @Volatile
    private var ready = false

    override suspend fun ensureReady() {
        if (ready) return
        initMutex.withLock {
            if (ready) return
            withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.init(context)
                    FFmpeg.init(context)
                } catch (e: YoutubeDLException) {
                    throw DownloadFailedException(summarizeError(e.message), e)
                }
            }
            ready = true
        }
    }

    override suspend fun download(
        request: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit,
    ): File {
        ensureReady()
        val tracker = ProgressTracker()
        val ytRequest = YtDlpArguments.build(request)

        // runInterruptible: si se cancela la corutina, se interrumpe el hilo y yt-dlp se detiene.
        runInterruptible(Dispatchers.IO) {
            try {
                YoutubeDL.execute(ytRequest, request.id, false) { percent, eta, line ->
                    tracker.onLine(line, percent, eta)?.let(onProgress)
                }
            } catch (e: YoutubeDL.CanceledException) {
                throw DownloadCanceledException()
            } catch (e: YoutubeDLException) {
                throw DownloadFailedException(summarizeError(e.message), e)
            }
        }
        return findOutputFile(request.outputDir)
    }

    override fun cancel(id: String) {
        YoutubeDL.destroyProcessById(id)
    }

    override suspend fun updateEngine(): EngineUpdateResult {
        ensureReady()
        return withContext(Dispatchers.IO) {
            try {
                val status = YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY)
                if (status == YoutubeDL.UpdateStatus.DONE) {
                    EngineUpdateResult.UPDATED
                } else {
                    EngineUpdateResult.ALREADY_UP_TO_DATE
                }
            } catch (e: YoutubeDLException) {
                throw DownloadFailedException(summarizeError(e.message), e)
            }
        }
    }

    override fun engineVersion(): String? =
        runCatching { YoutubeDL.versionName(context) }.getOrNull()

    /** El archivo final es el más grande con una extensión conocida (ignora restos de pistas sueltas). */
    private fun findOutputFile(dir: File): File {
        val candidates = dir.listFiles().orEmpty().filter { file ->
            file.isFile &&
                file.extension.lowercase() in FINAL_EXTENSIONS &&
                !PARTIAL_STREAM_NAME.containsMatchIn(file.nameWithoutExtension)
        }
        return candidates.maxByOrNull { it.length() }
            ?: throw DownloadFailedException("No se encontró el archivo descargado")
    }

    private companion object {
        val FINAL_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "mp3", "m4a", "aac", "ogg", "opus")
        val PARTIAL_STREAM_NAME = Regex("""\.f\d+$""")
    }
}
