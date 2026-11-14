package com.descargarmultimedia.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortLinksTest {

    @Test
    fun `reconoce los videos cortos de cada red`() {
        val shorts = listOf(
            "https://www.youtube.com/shorts/abc123",
            "https://youtube.com/shorts/abc123?si=xyz",
            "https://m.youtube.com/shorts/abc123",
            "https://www.tiktok.com/@usuario/video/7300000000000000000",
            "https://vm.tiktok.com/ZMabc123/",
            "https://vt.tiktok.com/ZSabc123/",
            "HTTPS://WWW.TIKTOK.COM/@usuario/video/1",
            "https://www.instagram.com/reel/Cabc123/",
            "https://www.instagram.com/share/reel/BAabc123/",
            "https://www.facebook.com/reel/123456789",
            "https://m.facebook.com/reel/123456789",
            "https://www.facebook.com/share/r/1AbCdEf/",
        )
        shorts.forEach { assertTrue("debía ser short: $it", ShortLinks.isShort(it)) }
    }

    @Test
    fun `los videos normales no son shorts`() {
        val normal = listOf(
            "https://www.youtube.com/watch?v=abc123",
            "https://youtu.be/abc123",
            "https://www.youtube.com/shorts",
            "https://www.instagram.com/p/abc123/",
            "https://www.facebook.com/watch/?v=123",
            "https://fb.watch/abc123/",
            "https://nottiktok.com/video/1",
            "https://vimeo.com/123456",
            "hola mundo",
            "",
        )
        normal.forEach { assertFalse("no debía ser short: $it", ShortLinks.isShort(it)) }
    }
}
