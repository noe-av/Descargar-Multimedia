package com.descargarmultimedia.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchTest {

    @Test
    fun `normaliza tildes, mayusculas y signos`() {
        assertEquals("beyonce halo live", TextMatch.normalize("Beyoncé – Halo (Live)"))
    }

    @Test
    fun `compact quita los espacios para comparar nombres de canal`() {
        assertEquals("ladygaga", TextMatch.compact("Lady Gaga"))
        assertEquals("ladygagavevo", TextMatch.compact("LadyGagaVEVO"))
    }

    @Test
    fun `una frase se busca por palabras completas`() {
        assertTrue(TextMatch.containsPhrase("Home - Edith Whiskers", "home"))
        assertFalse(TextMatch.containsPhrase("Homeless people", "home"))
        assertTrue(TextMatch.containsPhrase("Canción del Mariachi", "cancion"))
    }

    @Test
    fun `una frase vacia nunca coincide`() {
        assertFalse(TextMatch.containsPhrase("cualquier texto", ""))
        assertFalse(TextMatch.containsPhrase("cualquier texto", "  ()  "))
    }
}
