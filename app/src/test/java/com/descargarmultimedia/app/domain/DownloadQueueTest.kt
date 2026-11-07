package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadQueueTest {

    @Test
    fun `procesa en el orden en que se agregaron`() {
        val queue = DownloadQueue()
        val first = queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        val second = queue.enqueue("https://a.com/2", DownloadFormat.AUDIO)

        assertEquals(first.id, queue.claimNext()?.id)
        assertEquals(second.id, queue.claimNext()?.id)
        assertNull(queue.claimNext())
    }

    @Test
    fun `al tomar una descarga queda en curso y no se toma dos veces`() {
        val queue = DownloadQueue()
        val item = queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)

        queue.claimNext()
        assertEquals(DownloadStatus.DOWNLOADING, queue.find(item.id)?.status)
        assertEquals(0, queue.queuedCount())
        assertNull(queue.claimNext())
    }

    @Test
    fun `una descarga cancelada no se pisa con avances tardios`() {
        val queue = DownloadQueue()
        val item = queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        queue.claimNext()
        queue.markCanceled(item.id)

        queue.update(item.id) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 80f) }

        assertEquals(DownloadStatus.CANCELED, queue.find(item.id)?.status)
    }

    @Test
    fun `una descarga cancelada en espera no se procesa`() {
        val queue = DownloadQueue()
        val item = queue.enqueue("https://a.com/1", DownloadFormat.AUDIO)
        queue.markCanceled(item.id)
        assertNull(queue.claimNext())
    }

    @Test
    fun `limpiar quita solo las terminadas`() {
        val queue = DownloadQueue()
        val done = queue.enqueue("https://a.com/1", DownloadFormat.VIDEO)
        val waiting = queue.enqueue("https://a.com/2", DownloadFormat.VIDEO)
        queue.markCanceled(done.id)

        queue.clearFinished()

        assertEquals(listOf(waiting.id), queue.items.value.map { it.id })
    }
}
