package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.SearchCandidate
import com.descargarmultimedia.app.domain.model.SongInfo
import com.descargarmultimedia.app.util.TextMatch

/**
 * Elige, entre los resultados de una búsqueda, el que de verdad es la canción completa.
 *
 * Es preferible no descargar nada a descargar una canción equivocada: si ningún resultado
 * coincide con el título Y con el artista, devuelve null.
 */
object CandidateSelector {

    private const val MIN_SECONDS = 60L
    private const val MAX_SECONDS = 15 * 60L

    /** Versiones que no son la canción original; se descartan salvo que el título buscado las incluya. */
    private val UNWANTED = listOf(
        "live", "en vivo", "cover", "karaoke", "instrumental", "reaction", "reaccion",
        "tutorial", "lesson", "slowed", "reverb", "sped up", "speed up", "nightcore",
        "8d", "remix", "mashup", "parody", "parodia",
    )
    private val AUDIO_HINTS = listOf("audio")
    private val VIDEO_HINTS = listOf(
        "official video", "official music video", "music video", "video oficial",
        "videoclip", "video clip", "clip officiel", "mv",
    )
    private val LYRICS_HINTS = listOf("lyrics", "lyric video", "letra")

    fun best(
        song: SongInfo,
        format: DownloadFormat,
        candidates: List<SearchCandidate>,
    ): SearchCandidate? =
        candidates.withIndex()
            .mapNotNull { (index, candidate) ->
                score(song, format, candidate, index)?.let { it to candidate }
            }
            .maxByOrNull { it.first }
            ?.second

    /** Puntaje del resultado, o null si debe descartarse. */
    private fun score(
        song: SongInfo,
        format: DownloadFormat,
        candidate: SearchCandidate,
        index: Int,
    ): Int? {
        val seconds = candidate.durationSeconds
        if (seconds != null && seconds !in MIN_SECONDS..MAX_SECONDS) return null
        if (!TextMatch.containsPhrase(candidate.title, song.coreTrack)) return null

        val channel = candidate.channel.orEmpty()
        val compactChannel = TextMatch.compact(channel)
        val artistInTitle = song.artistNames.any { TextMatch.containsPhrase(candidate.title, it) }
        val artistInChannel = song.artistNames.any { name ->
            val compactName = TextMatch.compact(name)
            compactName.isNotEmpty() && compactChannel.contains(compactName)
        }
        if (!artistInTitle && !artistInChannel) return null

        val isOtherVersion = UNWANTED.any { word ->
            TextMatch.containsPhrase(candidate.title, word) &&
                !TextMatch.containsPhrase(song.track, word)
        }
        if (isOtherVersion) return null

        // El orden de YouTube cuenta un poco: a igualdad de puntos gana el primero.
        var score = 100 - index * 3
        if (artistInChannel) score += 15
        if (compactChannel.contains("vevo")) score += 10

        val isTopicChannel = channel.trim().endsWith("- Topic", ignoreCase = true)
        when (format) {
            DownloadFormat.AUDIO -> {
                if (isTopicChannel) score += 30
                if (hasAny(candidate.title, AUDIO_HINTS)) score += 20
            }
            DownloadFormat.VIDEO -> {
                // Los canales "- Topic" suben un video con una imagen fija: se evitan.
                if (isTopicChannel) score -= 30
                if (hasAny(candidate.title, VIDEO_HINTS)) score += 30
                if (hasAny(candidate.title, LYRICS_HINTS)) score -= 10
            }
        }
        return score
    }

    private fun hasAny(title: String, phrases: List<String>): Boolean =
        phrases.any { TextMatch.containsPhrase(title, it) }
}
