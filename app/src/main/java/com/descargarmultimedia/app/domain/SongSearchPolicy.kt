package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat

/**
 * Decide si hay que buscar la canción completa o descargar el enlace tal cual.
 *
 * - Música desde YouTube: se baja el audio de ese mismo video (incluidos los Shorts).
 * - Música desde otra red social (TikTok, Instagram, Facebook, X): ahí no hay una canción
 *   que bajar directamente, así que se busca la canción que suena.
 * - Video: solo se busca cuando el enlace es un video corto; el resto se baja tal cual.
 */
object SongSearchPolicy {

    private val SOCIAL_HOSTS = setOf(
        "tiktok.com",
        "instagram.com",
        "facebook.com",
        "fb.watch",
        "fb.com",
        "twitter.com",
        "x.com",
    )

    fun shouldSearchSong(url: String, format: DownloadFormat): Boolean = when (format) {
        DownloadFormat.AUDIO -> isSocialNetwork(url)
        DownloadFormat.VIDEO -> ShortLinks.isShort(url)
    }

    private fun isSocialNetwork(url: String): Boolean {
        val host = ShortLinks.hostOf(url) ?: return false
        return SOCIAL_HOSTS.any { host == it || host.endsWith(".$it") }
    }
}
