package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUpdaterTest {

    private class UpdateDownloader(
        var result: EngineUpdateResult = EngineUpdateResult.UPDATED,
        var fail: Boolean = false,
    ) : MediaDownloader {
        var updates = 0

        override suspend fun ensureReady() = Unit

        override suspend fun download(request: DownloadRequest, onProgress: (DownloadProgress) -> Unit): File =
            throw UnsupportedOperationException()

        override fun cancel(id: String) = Unit

        override suspend fun updateEngine(): EngineUpdateResult {
            updates++
            if (fail) throw DownloadFailedException("Sin conexión")
            return result
        }

        override fun engineVersion(): String = "test"
    }

    private val hour = 60L * 60 * 1000
    private val minute = 60L * 1000
    private var clock = 10_000L * hour

    private fun updater(downloader: UpdateDownloader, store: FakeUpdateStore) =
        EngineUpdater(downloader, store) { clock }

    @Test
    fun `si nunca se actualizo esta vencido y se actualiza`() = runBlocking {
        val downloader = UpdateDownloader()
        val store = FakeUpdateStore()
        val updater = updater(downloader, store)

        assertTrue(updater.isStale())
        updater.refreshIfStale()

        assertEquals(1, downloader.updates)
        assertEquals(clock, store.lastSuccessMillis)
        assertFalse(updater.isStale())
    }

    @Test
    fun `si se actualizo hace menos de un dia no vuelve a hacerlo`() = runBlocking {
        val downloader = UpdateDownloader()
        val store = FakeUpdateStore(lastSuccessMillis = clock - 23 * hour, lastAttemptMillis = clock - 23 * hour)
        val updater = updater(downloader, store)

        assertFalse(updater.isStale())
        updater.refreshIfStale()

        assertEquals(0, downloader.updates)
    }

    @Test
    fun `pasado un dia vuelve a actualizar`() = runBlocking {
        val downloader = UpdateDownloader(EngineUpdateResult.ALREADY_UP_TO_DATE)
        val store = FakeUpdateStore(lastSuccessMillis = clock - 25 * hour, lastAttemptMillis = clock - 25 * hour)
        val updater = updater(downloader, store)

        updater.refreshIfStale()

        assertEquals(1, downloader.updates)
        assertEquals(clock, store.lastSuccessMillis)
    }

    @Test
    fun `sin internet no falla, no insiste en cada descarga y reintenta mas tarde`() = runBlocking {
        val downloader = UpdateDownloader(fail = true)
        val store = FakeUpdateStore()
        val updater = updater(downloader, store)

        updater.refreshIfStale() // no lanza
        assertEquals(1, downloader.updates)
        assertEquals(0L, store.lastSuccessMillis)
        assertEquals(clock, store.lastAttemptMillis)
        assertTrue(updater.isStale())

        clock += 5 * minute
        updater.refreshIfStale()
        assertEquals(1, downloader.updates)

        clock += 30 * minute
        updater.refreshIfStale()
        assertEquals(2, downloader.updates)
    }

    @Test
    fun `tras un fallo, una version nueva pide reintentar`() = runBlocking {
        val downloader = UpdateDownloader(EngineUpdateResult.UPDATED)
        val store = FakeUpdateStore(lastSuccessMillis = clock - 2 * hour, lastAttemptMillis = clock - 2 * hour)

        assertTrue(updater(downloader, store).refreshAfterFailure())
        assertEquals(1, downloader.updates)
    }

    @Test
    fun `tras un fallo, si ya estaba al dia no vale la pena reintentar`() = runBlocking {
        val downloader = UpdateDownloader(EngineUpdateResult.ALREADY_UP_TO_DATE)
        val store = FakeUpdateStore(lastSuccessMillis = clock - 2 * hour, lastAttemptMillis = clock - 2 * hour)

        assertFalse(updater(downloader, store).refreshAfterFailure())
        assertEquals(1, downloader.updates)
    }

    @Test
    fun `tras un fallo, si se acaba de actualizar no lo intenta de nuevo`() = runBlocking {
        val downloader = UpdateDownloader()
        val store = FakeUpdateStore(lastSuccessMillis = clock - 2 * minute, lastAttemptMillis = clock - 2 * minute)

        assertFalse(updater(downloader, store).refreshAfterFailure())
        assertEquals(0, downloader.updates)
    }

    @Test
    fun `tras un fallo, si la actualizacion falla no pide reintentar`() = runBlocking {
        val downloader = UpdateDownloader(fail = true)
        val store = FakeUpdateStore(lastSuccessMillis = clock - 2 * hour, lastAttemptMillis = clock - 2 * hour)

        assertFalse(updater(downloader, store).refreshAfterFailure())
    }

    @Test
    fun `un reloj movido al pasado cuenta como vencido`() = runBlocking {
        val downloader = UpdateDownloader()
        val store = FakeUpdateStore(lastSuccessMillis = clock + 100 * hour, lastAttemptMillis = clock + 100 * hour)
        val updater = updater(downloader, store)

        assertTrue(updater.isStale())
        updater.refreshIfStale()
        assertEquals(1, downloader.updates)
    }

    @Test
    fun `la actualizacion manual siempre intenta y deja pasar el error`() = runBlocking {
        val store = FakeUpdateStore(lastSuccessMillis = clock, lastAttemptMillis = clock)
        val ok = UpdateDownloader(EngineUpdateResult.ALREADY_UP_TO_DATE)
        assertEquals(EngineUpdateResult.ALREADY_UP_TO_DATE, updater(ok, store).updateNow())
        assertEquals(1, ok.updates)

        val broken = UpdateDownloader(fail = true)
        val error = try {
            updater(broken, store).updateNow()
            null
        } catch (e: DownloadFailedException) {
            e
        }
        assertTrue(error != null)
    }
}
