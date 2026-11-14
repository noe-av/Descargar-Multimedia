package com.descargarmultimedia.app.data.engine

import com.descargarmultimedia.app.domain.model.SongInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongOutputParserTest {

    @Test
    fun `las plantillas tienen la cantidad de campos que el lector espera`() {
        assertEquals(3, SongOutputParser.SONG_TEMPLATE.split(SongOutputParser.SEPARATOR).size)
        assertEquals(5, SongOutputParser.SEARCH_TEMPLATE.split(SongOutputParser.SEPARATOR).size)
    }

    @Test
    fun `lee la cancion y el artista`() {
        val song = SongOutputParser.parseSong("Blinding Lights|~|The Weeknd|~|The Weeknd\n")
        assertEquals(SongInfo("Blinding Lights", "The Weeknd"), song)
    }

    @Test
    fun `si artist viene vacio usa el primer artista`() {
        val song = SongOutputParser.parseSong("Canción|~||~|Artista X")
        assertEquals(SongInfo("Canción", "Artista X"), song)
    }

    @Test
    fun `ignora advertencias antes de la linea de datos`() {
        val song = SongOutputParser.parseSong("WARNING: algo raro\nTema|~|Alguien|~|Alguien")
        assertEquals(SongInfo("Tema", "Alguien"), song)
    }

    @Test
    fun `un sonido original no es una cancion`() {
        assertNull(SongOutputParser.parseSong("original sound - usuario|~|usuario|~|usuario"))
        assertNull(SongOutputParser.parseSong("Sonido original - María|~|María|~|María"))
        assertNull(SongOutputParser.parseSong("original sound|~||~|"))
    }

    @Test
    fun `sin cancion o sin artista no hay resultado`() {
        assertNull(SongOutputParser.parseSong("|~||~||~|"))
        assertNull(SongOutputParser.parseSong("Tema|~||~|"))
        assertNull(SongOutputParser.parseSong(""))
        assertNull(SongOutputParser.parseSong("texto sin separadores"))
    }

    @Test
    fun `lee los resultados de la busqueda y salta las lineas danadas`() {
        val output = listOf(
            "abc123|~|The Weeknd - Blinding Lights|~|TheWeekndVEVO|~|TheWeekndVEVO|~|262.0",
            "xyz789|~|Otro tema|~||~|Un Canal|~|",
            "linea rota",
            "id1|~|Titulo con |~| el separador|~|c|~|u|~|10",
            "",
        ).joinToString("\n")

        val results = SongOutputParser.parseSearch(output)

        assertEquals(2, results.size)
        assertEquals("abc123", results[0].id)
        assertEquals("TheWeekndVEVO", results[0].channel)
        assertEquals(262L, results[0].durationSeconds)
        assertEquals("https://www.youtube.com/watch?v=abc123", results[0].url)
        assertEquals("Un Canal", results[1].channel)
        assertNull(results[1].durationSeconds)
    }
}
