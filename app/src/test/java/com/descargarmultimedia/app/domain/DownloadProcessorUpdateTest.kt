package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La descarga se recupera sola cuando yt-dlp está desactualizado. */
class DownloadProcessorUpdateTest {

    private class ScriptedDownloader : MediaDownloader {
        /** Cuántas descargas fallan antes de que una salga bien. */
        var failuresBeforeSuccess = 0
        var updateResult = EngineUpdateResult.UPDATED
        var updateFails = false
        var onUpdate: () -> Unit = {}

        var downloads = 0
        var updates = 0
        val events = mutableListOf<String>()

        override suspend fun ensureReady() = Unit

        override suspend fun download(request: DownloadRequest, onProgress: (DownloadProgress) -> Unit): File {
            downloads++
            events += "download"
            if (downloads <= failuresBeforeSuccess) throw DownloadFailedException("ERROR: sitio cambiado")
            return File(request.outputDir, "cancion.mp3").apply { writeText("datos") }
        }

        override fun cancel(id: String) = Unit

        override suspend fun updateEngine(): EngineUpdateResult {
            updates++
            events += "update"
            onUpdate()
            if (updateFails) throw DownloadFailedException("Sin conexión")
            return updateResult
        }

        override fun engineVersion(): String = "test"
    }

    private class FakeSaver : MediaSaver {
        val saved = mutableListOf<String>()

        override fun save(file: File, format: DownloadFormat): String {
            saved += file.name
            return "content://media/${file.name}"
        }
    }

    private object NoSongFinder : SongFinder {
        override suspend fun findFullSong(id: String, shortUrl: String, format: DownloadFormat): SongMatch =
            throw AssertionError("No debía buscar")
    }

    private val hour = 60L * 60 * 1000
    private val now = 10_000L * hour
    private val workRoot: File = Files.createTempDirectory("descargar-multimedia-update-test").toFile()

    private class Setup(val downloader: ScriptedDownloader, val queue: DownloadQueue, val saver: FakeSaver, val processor: DownloadProcessor)

    private fun setup(store: FakeUpdateStore, configure: ScriptedDownloader.(DownloadQueue) -> Unit = {}): Setup {
        val queue = DownloadQueue()
        val downloader = ScriptedDownloader().apply { configure(queue) }
        val saver = FakeSaver()
        val updater = EngineUpdater(downloader, store) { now }
        return Setup(downloader, queue, saver, DownloadProcessor(queue, downloader, updater, NoSongFinder, saver, workRoot))
    }

    /** Motor actualizado hace 2 horas: no vencido, pero tampoco "recién actualizado". */
    private fun okStore() = FakeUpdateStore(lastSuccessMillis = now - 2 * hour, lastAttemptMillis = now - 2 * hour)

    private fun run(s: Setup, format: DownloadFormat = DownloadFormat.AUDIO): DownloadStatus = runBlocking {
        s.queue.enqueue("https://www.youtube.com/watch?v=abc", format)
        val item = s.queue.claimNext()!!
        s.processor.process(item)
        s.queue.find(item.id)!!.status
    }

    @Test
    fun `si la descarga falla, actualiza yt-dlp y reintenta sola`() {
        val s = setup(okStore()) { failuresBeforeSuccess = 1 }

        val status = run(s)

        assertEquals(DownloadStatus.COMPLETED, status)
        assertEquals(listOf("download", "update", "download"), s.downloader.events)
        assertEquals(1, s.saver.saved.size)
    }

    @Test
    fun `si ya estaba al dia no reintenta y deja el error original`() {
        val s = setup(okStore()) {
            failuresBeforeSuccess = 5
            updateResult = EngineUpdateResult.ALREADY_UP_TO_DATE
        }

        val status = run(s)

        assertEquals(DownloadStatus.FAILED, status)
        assertEquals(1, s.downloader.downloads)
        assertEquals(1, s.downloader.updates)
        assertEquals("ERROR: sitio cambiado", s.queue.items.value.single().errorMessage)
    }

    @Test
    fun `reintenta una sola vez aunque vuelva a fallar`() {
        val s = setup(okStore()) { failuresBeforeSuccess = 5 }

        val status = run(s)

        assertEquals(DownloadStatus.FAILED, status)
        assertEquals(2, s.downloader.downloads)
        assertEquals(1, s.downloader.updates)
    }

    @Test
    fun `si no hay internet para actualizar, la descarga queda como fallida`() {
        val s = setup(okStore()) {
            failuresBeforeSuccess = 5
            updateFails = true
        }

        val status = run(s)

        assertEquals(DownloadStatus.FAILED, status)
        assertEquals(1, s.downloader.downloads)
    }

    @Test
    fun `si el motor esta vencido, se actualiza antes de descargar`() {
        val s = setup(FakeUpdateStore())

        val status = run(s)

        assertEquals(DownloadStatus.COMPLETED, status)
        assertEquals(listOf("update", "download"), s.downloader.events)
    }

    @Test
    fun `si el motor esta al dia no actualiza nada en una descarga exitosa`() {
        val s = setup(okStore())

        val status = run(s)

        assertEquals(DownloadStatus.COMPLETED, status)
        assertEquals(listOf("download"), s.downloader.events)
    }

    @Test
    fun `si no hay internet para la actualizacion diaria igual intenta descargar`() {
        val s = setup(FakeUpdateStore()) { updateFails = true }

        val status = run(s)

        assertEquals(DownloadStatus.COMPLETED, status)
        assertEquals(listOf("update", "download"), s.downloader.events)
    }

    @Test
    fun `cancelar mientras se actualiza no descarga nada`() {
        lateinit var queue: DownloadQueue
        val s = setup(FakeUpdateStore()) { q ->
            queue = q
            onUpdate = { q.items.value.forEach { q.markCanceled(it.id) } }
        }

        val status = run(s)

        assertEquals(DownloadStatus.CANCELED, status)
        assertEquals(0, s.downloader.downloads)
        assertTrue(s.saver.saved.isEmpty())
    }

    @Test
    fun `una cancion no encontrada no dispara actualizaciones`() {
        val queue = DownloadQueue()
        val downloader = ScriptedDownloader()
        val updater = EngineUpdater(downloader, okStore()) { now }
        val finder = object : SongFinder {
            override suspend fun findFullSong(id: String, shortUrl: String, format: DownloadFormat): SongMatch =
                throw SongNotFoundException()
        }
        val processor = DownloadProcessor(queue, downloader, updater, finder, FakeSaver(), workRoot)
        queue.enqueue("https://www.tiktok.com/@u/video/1", DownloadFormat.AUDIO)

        runBlocking { processor.process(queue.claimNext()!!) }

        assertEquals(DownloadStatus.NOT_FOUND, queue.items.value.single().status)
        assertEquals(0, downloader.updates)
        assertEquals(0, downloader.downloads)
    }
}
