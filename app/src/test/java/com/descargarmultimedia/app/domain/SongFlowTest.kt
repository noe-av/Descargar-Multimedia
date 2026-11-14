package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Flujo de una descarga cuando el enlace es de un video corto (short, reel, TikTok). */
class SongFlowTest {

    private class RecordingDownloader : MediaDownloader {
        val requests = mutableListOf<DownloadRequest>()

        override suspend fun ensureReady() = Unit

        override suspend fun download(
            request: DownloadRequest,
            onProgress: (DownloadProgress) -> Unit,
        ): File {
            requests += request
            return File(request.outputDir, "resultado.mp3").apply { writeText("datos") }
        }

        override fun cancel(id: String) = Unit

        override suspend fun updateEngine() = EngineUpdateResult.ALREADY_UP_TO_DATE

        override fun engineVersion(): String = "test"
    }

    private class RecordingSaver : MediaSaver {
        val saved = mutableListOf<String>()

        override fun save(file: File, format: DownloadFormat): String {
            saved += file.name
            return "content://media/${file.name}"
        }
    }

    private class ScriptedFinder(
        private val script: suspend (id: String, format: DownloadFormat) -> SongMatch,
    ) : SongFinder {
        val calls = mutableListOf<String>()
        val formats = mutableListOf<DownloadFormat>()

        override suspend fun findFullSong(id: String, shortUrl: String, format: DownloadFormat): SongMatch {
            calls += shortUrl
            formats += format
            return script(id, format)
        }
    }

    private val workRoot: File = Files.createTempDirectory("descargar-multimedia-song-test").toFile()
    private val tiktok = "https://www.tiktok.com/@usuario/video/7300000000000000000"
    private val match = SongMatch("The Weeknd - Blinding Lights", "https://www.youtube.com/watch?v=abc123")

    private inner class Harness(script: suspend (DownloadQueue, String, DownloadFormat) -> SongMatch) {
        val queue = DownloadQueue()
        val downloader = RecordingDownloader()
        val saver = RecordingSaver()
        val finder = ScriptedFinder { id, format -> script(queue, id, format) }
        private val processor = DownloadProcessor(queue, downloader, finder, saver, workRoot)

        fun process(url: String, format: DownloadFormat = DownloadFormat.AUDIO): DownloadItem = runBlocking {
            val item = queue.enqueue(url, format)
            processor.process(queue.claimNext()!!)
            queue.find(item.id)!!
        }
    }

    @Test
    fun `un short con la cancion encontrada descarga la cancion completa y no el short`() {
        val h = Harness { _, _, _ -> match }

        val result = h.process(tiktok)

        val request = h.downloader.requests.single()
        assertEquals(match.url, request.url)
        assertEquals(match.label, request.outputName)
        assertEquals(listOf(tiktok), h.finder.calls)
        assertEquals(DownloadStatus.COMPLETED, result.status)
        assertEquals(match.label, result.title)
        assertEquals(1, h.saver.saved.size)
    }

    @Test
    fun `el formato elegido llega al buscador`() {
        val h = Harness { _, _, _ -> match }

        h.process(tiktok, DownloadFormat.VIDEO)

        assertEquals(listOf(DownloadFormat.VIDEO), h.finder.formats)
        assertEquals(DownloadFormat.VIDEO, h.downloader.requests.single().format)
    }

    @Test
    fun `si la cancion no se encuentra no se descarga nada`() {
        val h = Harness { _, _, _ -> throw SongNotFoundException("Artista X - Tema Y") }

        val result = h.process(tiktok)

        assertEquals(DownloadStatus.NOT_FOUND, result.status)
        assertEquals("Artista X - Tema Y", result.title)
        assertTrue(h.downloader.requests.isEmpty())
        assertTrue(h.saver.saved.isEmpty())
        assertNull(result.outputUri)
    }

    @Test
    fun `si ni siquiera se identifica la cancion tampoco se descarga nada`() {
        val h = Harness { _, _, _ -> throw SongNotFoundException() }

        val result = h.process(tiktok)

        assertEquals(DownloadStatus.NOT_FOUND, result.status)
        assertNull(result.title)
        assertEquals(tiktok, result.displayName)
        assertTrue(h.downloader.requests.isEmpty())
        assertTrue(h.saver.saved.isEmpty())
    }

    @Test
    fun `un video normal se descarga tal cual sin buscar nada`() {
        val h = Harness { _, _, _ -> throw AssertionError("no debía buscar") }
        val url = "https://www.youtube.com/watch?v=abc123"

        val result = h.process(url)

        assertTrue(h.finder.calls.isEmpty())
        assertEquals(url, h.downloader.requests.single().url)
        assertNull(h.downloader.requests.single().outputName)
        assertEquals(DownloadStatus.COMPLETED, result.status)
    }

    @Test
    fun `cancelar durante la busqueda no descarga ni guarda nada`() {
        val h = Harness { queue, id, _ ->
            queue.markCanceled(id)
            match
        }

        val result = h.process(tiktok)

        assertEquals(DownloadStatus.CANCELED, result.status)
        assertTrue(h.downloader.requests.isEmpty())
        assertTrue(h.saver.saved.isEmpty())
    }

    @Test
    fun `un error del buscador deja la descarga como fallida`() {
        val h = Harness { _, _, _ -> throw DownloadFailedException("Video privado") }

        val result = h.process(tiktok)

        assertEquals(DownloadStatus.FAILED, result.status)
        assertEquals("Video privado", result.errorMessage)
        assertTrue(h.downloader.requests.isEmpty())
    }

    @Test
    fun `una cancion no encontrada se puede reintentar y se limpia con las terminadas`() {
        val h = Harness { _, _, _ -> throw SongNotFoundException() }
        val manager = DownloadManager(h.queue, h.downloader)
        val notFound = h.process(tiktok)

        val retried = manager.retry(notFound.id)

        assertNotNull(retried)
        assertEquals(DownloadStatus.QUEUED, h.queue.find(retried!!.id)?.status)
        assertNull(h.queue.find(notFound.id))

        h.queue.markCanceled(retried.id)
        h.queue.clearFinished()
        assertTrue(h.queue.items.value.isEmpty())
    }
}
