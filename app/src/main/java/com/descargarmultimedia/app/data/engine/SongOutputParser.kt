package com.descargarmultimedia.app.data.engine

import com.descargarmultimedia.app.domain.model.SearchCandidate
import com.descargarmultimedia.app.domain.model.SongInfo
import com.descargarmultimedia.app.util.TextMatch

/**
 * Plantillas de `--print` de yt-dlp y lectura de su salida.
 * Cada línea trae varios campos separados por [SEPARATOR]; un campo ausente sale vacío.
 */
internal object SongOutputParser {

    const val SEPARATOR = "|~|"

    /** Del video corto: canción, artista, y primer artista (por si "artist" viniera vacío). */
    const val SONG_TEMPLATE = "%(track|)s|~|%(artist|)s|~|%(artists.0|)s"

    /** De cada resultado de búsqueda: id, título, canal, subidor y duración en segundos. */
    const val SEARCH_TEMPLATE =
        "%(id)s|~|%(title)s|~|%(channel|)s|~|%(uploader|)s|~|%(duration|)s"

    /** Sonidos que no son una canción: TikTok los llama "original sound" (según el idioma). */
    private val ORIGINAL_SOUND_PREFIXES = listOf(
        "original sound", "original audio", "sonido original", "audio original",
        "som original", "son original", "suono originale", "originalton",
    )

    /** Devuelve la canción del video, o null si no la tiene (o es un "sonido original"). */
    fun parseSong(output: String): SongInfo? {
        val line = output.lines().firstOrNull { it.contains(SEPARATOR) } ?: return null
        val parts = line.split(SEPARATOR)
        if (parts.size < 3) return null

        val track = parts[0].trim()
        val artist = parts[1].trim().ifBlank { parts[2].trim() }
        if (track.isEmpty() || artist.isEmpty()) return null
        if (isOriginalSound(track)) return null
        return SongInfo(track = track, artist = artist)
    }

    /** Resultados de la búsqueda, en el orden en que los devolvió YouTube. */
    fun parseSearch(output: String): List<SearchCandidate> =
        output.lines().mapNotNull { line ->
            val parts = line.split(SEPARATOR)
            if (parts.size != 5) return@mapNotNull null
            val id = parts[0].trim()
            if (id.isEmpty() || id.contains(' ')) return@mapNotNull null
            SearchCandidate(
                id = id,
                title = parts[1].trim(),
                channel = parts[2].trim().ifBlank { parts[3].trim() }.ifBlank { null },
                durationSeconds = parts[4].trim().toDoubleOrNull()?.toLong(),
            )
        }

    private fun isOriginalSound(track: String): Boolean {
        val normalized = TextMatch.normalize(track)
        return ORIGINAL_SOUND_PREFIXES.any { normalized.startsWith(it) }
    }
}
