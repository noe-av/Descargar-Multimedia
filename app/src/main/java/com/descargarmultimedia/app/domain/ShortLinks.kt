package com.descargarmultimedia.app.domain

/**
 * Reconoce enlaces de videos cortos (YouTube Shorts, TikTok, reels de Instagram y Facebook).
 * De esos no se descarga el video: se busca la canción completa que suena en ellos.
 */
object ShortLinks {

    private val URL_PARTS = Regex("""^https?://([^/?#:]+)(?::\d+)?([^?#]*)""", RegexOption.IGNORE_CASE)

    fun isShort(url: String): Boolean {
        val parts = URL_PARTS.find(url.trim()) ?: return false
        val host = parts.groupValues[1].lowercase().removePrefix("www.").removePrefix("m.")
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
