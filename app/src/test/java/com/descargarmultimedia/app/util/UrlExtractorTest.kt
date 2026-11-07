package com.descargarmultimedia.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun `extrae el enlace de un texto compartido por YouTube`() {
        val text = "Mira este video https://youtu.be/dQw4w9WgXcQ?si=abc123"
        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=abc123", UrlExtractor.firstUrl(text))
    }

    @Test
    fun `acepta un enlace solo`() {
        assertEquals(
            "https://www.facebook.com/watch/?v=123",
            UrlExtractor.firstUrl("https://www.facebook.com/watch/?v=123"),
        )
    }

    @Test
    fun `quita puntuacion pegada al final`() {
        assertEquals(
            "https://example.com/video",
            UrlExtractor.firstUrl("(mira https://example.com/video)."),
        )
    }

    @Test
    fun `devuelve el primero si hay varios`() {
        assertEquals("https://a.com/1", UrlExtractor.firstUrl("https://a.com/1 y https://b.com/2"))
    }

    @Test
    fun `devuelve null si no hay enlace`() {
        assertNull(UrlExtractor.firstUrl("hola mundo"))
        assertNull(UrlExtractor.firstUrl(""))
        assertNull(UrlExtractor.firstUrl(null))
    }
}
