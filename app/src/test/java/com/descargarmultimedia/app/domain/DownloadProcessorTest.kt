package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadProcessorTest {

    private class FakeDownloader(
        private val behavior: (DownloadRequest, (DownloadProgress) -> Unit) -> File,
    ) : MediaDownloader {
        val canceledIds = mutableListOf<String>()

        override suspend fun ensureReady() = Unit

        override suspend fun download(
            request: DownloadRequest,
            onProgress: (DownloadProgress) -> Unit,
        ): File = behavior(request, onProgress)

        override fun cancel(id: String) {
            canceledIds += id
        }

        override suspend fun updateEngine() = EngineUpdateResult.ALREADY_UP_TO_DATE

        override fun engineVersion(): String = "test"
    }

    private class FakeSaver : MediaSaver {
        val saved = mutableListOf<String>()

        override fun save(file: File, format: com.descargarmultimedia.app.domain.model.DownloadFormat): String {
            saved += file.name
            return "content://media/${file.name}"
        }
    }

    /** Para enlaces normales: si el procesador intentara buscar una canción, la prueba falla. */
    private object NoSongFinder : SongFinder {
        override suspend fun findFullSong(id: String, shortUrl: String, format: DownloadFormat): SongMatch =
            throw AssertionError("No debía buscar una canción para un enlace normal")
    }

    private val workRoot: File = Files.createTempDirectory("descargar-multimedia-test").toFile()

    private fun fileIn(request: DownloadRequest, name: String): File =
        File(request.outputDir, name).apply { writeText("datos") }

    @Test
    fun `una descarga exitosa termina completada y limpia la carpeta temporal`() = runBlocking {
        val queue = DownloadQueue()
        val saver = FakeSaver()
        val downloader = FakeDownloader { request, onProgress ->
            onProgress(DownloadProgress(40f, 12, "Mi video", false))
            fileIn(request, "Mi video.mp4")
        }
        val processor = DownloadProcessor(queue, downloader, NoSongFinder, saver, workRoot)
        queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        val item = queue.claimNext()!!

        processor.process(item)

        val result = queue.find(item.id)!!
        assertEquals(DownloadStatus.COMPLETED, result.status)
        assertEquals("Mi video", result.title)
        assertEquals("content://media/Mi video.mp4", result.outputUri)
        assertEquals(listOf("Mi video.mp4"), saver.saved)
        assertFalse(File(workRoot, item.id).exists())
    }

    @Test
    fun `un error del motor deja la descarga como fallida con su mensaje`() = runBlocking {
        val queue = DownloadQueue()
        val downloader = FakeDownloader { _, _ -> throw DownloadFailedException("Video no disponible") }
        val processor = DownloadProcessor(queue, downloader, NoSongFinder, FakeSaver(), workRoot)
        queue.enqueue("https://a.com/1", DownloadFormat.AUDIO)
        val item = queue.claimNext()!!

        processor.process(item)

        val result = queue.find(item.id)!!
        assertEquals(DownloadStatus.FAILED, result.status)
        assertEquals("Video no disponible", result.errorMessage)
        assertNull(result.outputUri)
    }

    @Test
    fun `una cancelacion queda como cancelada y no como error`() = runBlocking {
        val queue = DownloadQueue()
        val downloader = FakeDownloader { _, _ -> throw DownloadCanceledException() }
        val processor = DownloadProcessor(queue, downloader, NoSongFinder, FakeSaver(), workRoot)
        queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        val item = queue.claimNext()!!

        processor.process(item)

        assertEquals(DownloadStatus.CANCELED, queue.find(item.id)?.status)
    }

    @Test
    fun `cancelar una descarga en curso avisa al motor, una en espera no`() {
        val queue = DownloadQueue()
        val downloader = FakeDownloader { _, _ -> File("nada") }
        val manager = DownloadManager(queue, downloader)
        val running = manager.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        val waiting = manager.enqueue("https://a.com/2", DownloadFormat.VIDEO)
        queue.claimNext() // la primera pasa a "en curso"

        manager.cancel(running.id)
        manager.cancel(waiting.id)

        assertEquals(listOf(running.id), downloader.canceledIds)
        assertEquals(DownloadStatus.CANCELED, queue.find(running.id)?.status)
        assertEquals(DownloadStatus.CANCELED, queue.find(waiting.id)?.status)
    }

    @Test
    fun `reintentar vuelve a encolar solo las fallidas o canceladas`() {
        val queue = DownloadQueue()
        val manager = DownloadManager(queue, FakeDownloader { _, _ -> File("nada") })
        val item = manager.enqueue("https://a.com/1", DownloadFormat.AUDIO)

        assertNull(manager.retry(item.id)) // en espera: no se puede reintentar

        manager.cancel(item.id)
        val retried = manager.retry(item.id)

        assertNotNull(retried)
        assertTrue(retried!!.id != item.id)
        assertEquals(DownloadFormat.AUDIO, retried.format)
        assertEquals(DownloadStatus.QUEUED, queue.find(retried.id)?.status)
        assertNull(queue.find(item.id))
    }
}
