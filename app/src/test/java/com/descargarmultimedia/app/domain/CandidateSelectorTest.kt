package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.SearchCandidate
import com.descargarmultimedia.app.domain.model.SongInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandidateSelectorTest {

    private val song = SongInfo("Blinding Lights", "The Weeknd")

    private val topic = SearchCandidate("topic", "Blinding Lights", "The Weeknd - Topic", 200)
    private val officialVideo = SearchCandidate(
        "vevo", "The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO", 262,
    )

    private fun best(
        format: DownloadFormat,
        vararg candidates: SearchCandidate,
        song: SongInfo = this.song,
    ): String? = CandidateSelector.best(song, format, candidates.toList())?.id

    @Test
    fun `para musica prefiere el audio del canal Topic`() {
        assertEquals("topic", best(DownloadFormat.AUDIO, officialVideo, topic))
    }

    @Test
    fun `para video prefiere el videoclip oficial y evita el canal Topic`() {
        assertEquals("vevo", best(DownloadFormat.VIDEO, topic, officialVideo))
    }

    @Test
    fun `descarta versiones en vivo, covers y videos cortos`() {
        val live = SearchCandidate("live", "The Weeknd - Blinding Lights (Live at the Super Bowl)", "TheWeekndVEVO", 250)
        val cover = SearchCandidate("cover", "Blinding Lights - Cover by Alguien", "Alguien", 230)
        val tooShort = SearchCandidate("short", "Blinding Lights - The Weeknd", "Fan", 30)
        assertNull(best(DownloadFormat.AUDIO, live, cover, tooShort))
    }

    @Test
    fun `descarta compilaciones demasiado largas`() {
        val mix = SearchCandidate("mix", "The Weeknd - Blinding Lights (1 hour)", "Loops", 3600)
        assertNull(best(DownloadFormat.AUDIO, mix))
    }

    @Test
    fun `no acepta una cancion con el mismo nombre de otro artista`() {
        val other = SearchCandidate("other", "Blinding Lights", "Un Desconocido", 200)
        assertNull(best(DownloadFormat.AUDIO, other))
    }

    @Test
    fun `el titulo debe coincidir como palabra completa`() {
        val homeless = SearchCandidate("homeless", "Artista - Homeless", "Artista", 200)
        val home = SongInfo("Home", "Artista")
        assertNull(best(DownloadFormat.AUDIO, homeless, song = home))
    }

    @Test
    fun `descarta remixes salvo que la cancion buscada sea el remix`() {
        val remix = SearchCandidate("remix", "The Weeknd - Blinding Lights (Remix)", "Remix Co", 200)
        assertEquals("vevo", best(DownloadFormat.AUDIO, remix, officialVideo))
        assertEquals(
            "remix",
            best(DownloadFormat.AUDIO, remix, song = SongInfo("Blinding Lights (Remix)", "The Weeknd")),
        )
    }

    @Test
    fun `reconoce al artista por el nombre del canal pegado`() {
        val gaga = SongInfo("Poker Face", "Lady Gaga")
        val result = SearchCandidate("gaga", "Poker Face (Official Audio)", "LadyGagaVEVO", 237)
        assertEquals("gaga", best(DownloadFormat.AUDIO, result, song = gaga))
    }

    @Test
    fun `acepta un resultado sin duracion conocida`() {
        val unknown = SearchCandidate("sin-duracion", "The Weeknd - Blinding Lights", "The Weeknd - Topic", null)
        assertEquals("sin-duracion", best(DownloadFormat.AUDIO, unknown))
    }

    @Test
    fun `a igualdad gana el primer resultado de la busqueda`() {
        val first = SearchCandidate("primero", "The Weeknd - Blinding Lights", "Canal Uno", 200)
        val second = SearchCandidate("segundo", "The Weeknd - Blinding Lights", "Canal Dos", 200)
        assertEquals("primero", best(DownloadFormat.AUDIO, first, second))
    }

    @Test
    fun `sin resultados no hay nada que elegir`() {
        assertNull(best(DownloadFormat.AUDIO))
    }
}
