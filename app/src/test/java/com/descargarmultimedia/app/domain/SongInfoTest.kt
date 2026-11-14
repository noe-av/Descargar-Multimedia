package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.SongInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SongInfoTest {

    @Test
    fun `la etiqueta es artista guion cancion`() {
        assertEquals("Beyoncé - Halo", SongInfo("Halo", "Beyoncé").label)
    }

    @Test
    fun `el titulo base no incluye feat, parentesis ni versiones`() {
        assertEquals("Bad Habits", SongInfo("Bad Habits (feat. Alguien)", "Ed Sheeran").coreTrack)
        assertEquals("Mundian To Bach Ke", SongInfo("Mundian To Bach Ke - Motivo Remix", "Panjabi MC").coreTrack)
        assertEquals("Levitating", SongInfo("Levitating feat. DaBaby", "Dua Lipa").coreTrack)
        assertEquals("Satisfaction", SongInfo("(I Can't Get No) Satisfaction", "The Rolling Stones").coreTrack)
    }

    @Test
    fun `separa varios artistas`() {
        assertEquals(listOf("A", "B"), SongInfo("t", "A, B").artistNames)
        assertEquals(listOf("Simon", "Garfunkel"), SongInfo("t", "Simon & Garfunkel").artistNames)
        assertEquals(listOf("Calvin Harris", "Rihanna"), SongInfo("t", "Calvin Harris feat. Rihanna").artistNames)
        assertEquals(listOf("Bad Bunny", "Jhay Cortez"), SongInfo("t", "Bad Bunny x Jhay Cortez").artistNames)
    }

    @Test
    fun `un nombre con una x dentro no se parte`() {
        assertEquals(listOf("Rex Orange County"), SongInfo("t", "Rex Orange County").artistNames)
    }

    @Test
    fun `la busqueda usa el primer artista y el titulo base`() {
        assertEquals(
            "The Weeknd Blinding Lights",
            SongInfo("Blinding Lights (Radio Edit)", "The Weeknd, Otro").searchQuery,
        )
    }
}
