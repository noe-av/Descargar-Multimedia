package com.descargarmultimedia.app.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTrackerTest {

    @Test
    fun `una sola pista avanza de 0 a 100`() {
        val tracker = ProgressTracker()
        tracker.onLine("[info] abc: Downloading 1 format(s): 18", -1f, -1)
        tracker.onLine("[download] Destination: /tmp/Mi cancion.mp4", -1f, -1)

        val half = tracker.onLine("[download]  50.0% of 10.00MiB at 1MiB/s ETA 00:05", 50f, 5)
        assertNotNull(half)
        assertEquals(50f, half!!.percent, 0.01f)
        assertEquals(5L, half.etaSeconds)
        assertEquals("Mi cancion", half.title)
        assertEquals(false, half.isFinalizing)
    }

    @Test
    fun `video y audio se combinan en un avance que no retrocede`() {
        val tracker = ProgressTracker()
        tracker.onLine("[info] abc: Downloading 1 format(s): 399+251", -1f, -1)
        tracker.onLine("[download] Destination: /tmp/Titulo.f399.mp4", -1f, -1)
        val videoHalf = tracker.onLine("[download]  50.0% of 20MiB ETA 00:10", 50f, 10)!!
        assertEquals(25f, videoHalf.percent, 0.01f)

        val videoDone = tracker.onLine("[download] 100.0% of 20MiB ETA 00:00", 100f, 0)!!
        assertEquals(50f, videoDone.percent, 0.01f)

        // Empieza la pista de audio: el 100 anterior sigue "pegado" y NO debe contarse como avance,
        // así que el total se queda en 50 (sin cambios, por eso no se emite nada nuevo).
        val audioStart = tracker.onLine("[download] Destination: /tmp/Titulo.f251.webm", 100f, 0)
        assertNull(audioStart)

        val audioHalf = tracker.onLine("[download]  50.0% of 3MiB ETA 00:02", 50f, 2)!!
        assertEquals(75f, audioHalf.percent, 0.01f)
        assertEquals("Titulo", audioHalf.title)
    }

    @Test
    fun `al unir o convertir pasa a finalizando`() {
        val tracker = ProgressTracker()
        tracker.onLine("[download] Destination: /tmp/a.webm", -1f, -1)
        val progress = tracker.onLine("[ExtractAudio] Destination: /tmp/a.mp3", 100f, 0)!!
        assertTrue(progress.isFinalizing)
        assertEquals(100f, progress.percent, 0.01f)
        assertNull(progress.etaSeconds)
    }

    @Test
    fun `una linea que no cambia nada devuelve null`() {
        val tracker = ProgressTracker()
        tracker.onLine("[download] Destination: /tmp/a.mp4", -1f, -1)
        tracker.onLine("[download]  10.0% of 1MiB ETA 00:09", 10f, 9)
        assertNull(tracker.onLine("[debug] algo irrelevante", 10f, 9))
    }

    @Test
    fun `resume el error de yt-dlp`() {
        val raw = "WARNING: algo\nERROR: [youtube] abc: Sign in to confirm you're not a bot\n"
        assertEquals("[youtube] abc: Sign in to confirm you're not a bot", summarizeError(raw))
        assertEquals("", summarizeError(null))
    }
}
