package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchPolicyTest {

    private fun search(url: String, format: DownloadFormat) = SongSearchPolicy.shouldSearchSong(url, format)

    @Test
    fun `musica desde youtube baja el audio del mismo video, incluso en shorts`() {
        assertFalse(search("https://www.youtube.com/watch?v=abc", DownloadFormat.AUDIO))
        assertFalse(search("https://youtu.be/abc", DownloadFormat.AUDIO))
        assertFalse(search("https://music.youtube.com/watch?v=abc", DownloadFormat.AUDIO))
        assertFalse(search("https://www.youtube.com/shorts/abc", DownloadFormat.AUDIO))
    }

    @Test
    fun `musica desde otras redes sociales busca la cancion`() {
        assertTrue(search("https://www.tiktok.com/@u/video/1", DownloadFormat.AUDIO))
        assertTrue(search("https://vm.tiktok.com/ZMabc/", DownloadFormat.AUDIO))
        assertTrue(search("https://www.instagram.com/reel/abc/", DownloadFormat.AUDIO))
        assertTrue(search("https://www.instagram.com/p/abc/", DownloadFormat.AUDIO))
        assertTrue(search("https://m.facebook.com/watch/?v=1", DownloadFormat.AUDIO))
        assertTrue(search("https://fb.watch/abc/", DownloadFormat.AUDIO))
        assertTrue(search("https://x.com/user/status/1", DownloadFormat.AUDIO))
        assertTrue(search("https://twitter.com/user/status/1", DownloadFormat.AUDIO))
    }

    @Test
    fun `musica desde otros sitios baja el enlace tal cual`() {
        assertFalse(search("https://soundcloud.com/artista/tema", DownloadFormat.AUDIO))
        assertFalse(search("https://vimeo.com/123", DownloadFormat.AUDIO))
    }

    @Test
    fun `un dominio que solo se parece no cuenta como red social`() {
        assertFalse(search("https://notx.com/a", DownloadFormat.AUDIO))
        assertFalse(search("https://faketiktok.com/a", DownloadFormat.AUDIO))
    }

    @Test
    fun `video busca la cancion solo en videos cortos`() {
        assertTrue(search("https://www.youtube.com/shorts/abc", DownloadFormat.VIDEO))
        assertTrue(search("https://www.tiktok.com/@u/video/1", DownloadFormat.VIDEO))
        assertTrue(search("https://www.instagram.com/reel/abc/", DownloadFormat.VIDEO))
        assertFalse(search("https://www.youtube.com/watch?v=abc", DownloadFormat.VIDEO))
        assertFalse(search("https://www.instagram.com/p/abc/", DownloadFormat.VIDEO))
    }

    @Test
    fun `un texto que no es un enlace no busca nada`() {
        assertFalse(search("hola", DownloadFormat.AUDIO))
        assertFalse(search("hola", DownloadFormat.VIDEO))
    }
}
