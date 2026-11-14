package com.descargarmultimedia.app.domain.model

/** Canción identificada a partir de un video corto (short, reel, TikTok). */
data class SongInfo(val track: String, val artist: String) {

    /** "Artista - Canción": se muestra en la lista y se usa como nombre del archivo. */
    val label: String
        get() = "$artist - $track"

    /** Cada artista por separado ("A, B & C" -> A, B, C), para comparar con los resultados. */
    val artistNames: List<String>
        get() = ARTIST_SEPARATOR.split(artist).map { it.trim() }.filter { it.isNotEmpty() }

    /** El título sin "(feat. ...)", "[Remix]" ni "- Versión": es lo que debe aparecer en el resultado. */
    val coreTrack: String
        get() {
            val withoutBrackets = BRACKETS.replace(track, " ")
            val beforeDash = withoutBrackets.substringBefore(" - ")
            val withoutFeat = FEATURING.replace(beforeDash, "")
            return withoutFeat.trim().ifBlank { track }
        }

    /** Texto con el que se busca la canción en YouTube. */
    val searchQuery: String
        get() = "${artistNames.firstOrNull() ?: artist} $coreTrack"

    private companion object {
        val ARTIST_SEPARATOR =
            Regex("""\s*(?:,|&|\s+x\s+|\s+(?:feat|ft)\.?\s+)\s*""", RegexOption.IGNORE_CASE)
        val BRACKETS = Regex("""[(\[][^)\]]*[)\]]""")
        val FEATURING = Regex("""\s+(?:feat|ft|featuring)\.?\s+.*$""", RegexOption.IGNORE_CASE)
    }
}

/** Un resultado de la búsqueda de YouTube. */
data class SearchCandidate(
    val id: String,
    val title: String,
    val channel: String?,
    val durationSeconds: Long?,
) {
    val url: String
        get() = "https://www.youtube.com/watch?v=$id"
}
