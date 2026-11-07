package com.descargarmultimedia.app.util

/** Saca el enlace de un texto compartido (p. ej. "Mira esto https://youtu.be/abc"). */
object UrlExtractor {

    private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ')', ']', '!')

    /** Devuelve el primer enlace http(s) del texto, o null si no hay ninguno. */
    fun firstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return URL_REGEX.find(text)?.value?.trimEnd(*TRAILING_PUNCTUATION)?.takeIf { it.isNotEmpty() }
    }
}
