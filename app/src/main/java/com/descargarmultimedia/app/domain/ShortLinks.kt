package com.descargarmultimedia.app.domain

/**
 * Reconoce enlaces de videos cortos (YouTube Shorts, TikTok, reels de Instagram y Facebook).
 * De esos no se descarga el video: se busca la canción completa que suena en ellos.
 */
object ShortLinks {

    private val URL_PARTS = Regex("""^https?://([^/?#:]+)(?::\d+)?([^?#]*)""", RegexOption.IGNORE_CASE)

    /** Dominio del enlace en minúsculas y sin "www." ni "m.", o null si no es un enlace http(s). */
    fun hostOf(url: String): String? =
        URL_PARTS.find(url.trim())?.groupValues?.get(1)?.lowercase()?.removePrefix("www.")?.removePrefix("m.")

    fun isShort(url: String): Boolean {
        val parts = URL_PARTS.find(url.trim()) ?: return false
        val host = hostOf(url) ?: return false
        val path = parts.groupValues[2].lowercase()
        return when {
            host == "youtube.com" -> path.startsWith("/shorts/")
            host == "tiktok.com" || host.endsWith(".tiktok.com") -> true
            host == "instagram.com" -> path.contains("/reel/")
            host == "facebook.com" -> path.startsWith("/reel/") || path.startsWith("/share/r/")
            else -> false
        }
    }
}
