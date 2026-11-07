package com.descargarmultimedia.app.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {

    @Test
    fun `reemplaza caracteres no permitidos`() {
        assertEquals("a_b_c_d.mp4", FileNames.sanitize("a/b:c?d.mp4"))
    }

    @Test
    fun `conserva extension en minuscula`() {
        assertEquals("cancion.mp3", FileNames.sanitize("cancion.MP3"))
    }

    @Test
    fun `un nombre en blanco recibe uno por defecto`() {
        assertEquals("descarga.mp4", FileNames.sanitize("  .mp4"))
    }

    @Test
    fun `limita el largo del nombre`() {
        val result = FileNames.sanitize("x".repeat(400) + ".mp4")
        assertTrue(result.length <= 154)
        assertTrue(result.endsWith(".mp4"))
    }
}
