package com.descargarmultimedia.app.data.engine

import com.descargarmultimedia.app.domain.CandidateSelector
import com.descargarmultimedia.app.domain.DownloadCanceledException
import com.descargarmultimedia.app.domain.DownloadFailedException
import com.descargarmultimedia.app.domain.SongFinder
import com.descargarmultimedia.app.domain.SongMatch
import com.descargarmultimedia.app.domain.SongNotFoundException
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.SearchCandidate
import com.descargarmultimedia.app.domain.model.SongInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/**
 * Implementación de [SongFinder] con yt-dlp, en dos pasos:
 * 1. Lee del video corto el título y el artista de su música (sin descargar nada).
 * 2. Busca esa canción en YouTube y elige la versión completa con [CandidateSelector].
 *
 * Reconocer la canción por su audio no está soportado: si el sitio no informa la música
 * (hoy es el caso de Instagram y Facebook), el resultado es [SongNotFoundException].
 */
class YtDlpSongFinder : SongFinder {

    override suspend fun findFullSong(
        id: String,
        shortUrl: String,
        format: DownloadFormat,
    ): SongMatch {
        val song = identify(id, shortUrl) ?: throw SongNotFoundException()
        val candidates = search(id, song)
        val best = CandidateSelector.best(song, format, candidates)
            ?: throw SongNotFoundException(song.label)
        return SongMatch(label = song.label, url = best.url)
    }

    private suspend fun identify(id: String, shortUrl: String): SongInfo? {
        val request = YoutubeDLRequest(shortUrl)
        request.addOption("--no-playlist")
        request.addOption("--print", SongOutputParser.SONG_TEMPLATE)
        return SongOutputParser.parseSong(run(id, request))
    }

    private suspend fun search(id: String, song: SongInfo): List<SearchCandidate> {
        val request = YoutubeDLRequest("ytsearch$RESULT_COUNT:${song.searchQuery}")
        request.addOption("--flat-playlist")
        request.addOption("--print", SongOutputParser.SEARCH_TEMPLATE)
        return SongOutputParser.parseSearch(run(id, request))
    }

    /** Ejecuta yt-dlp y devuelve lo que imprimió. Misma id que la descarga: cancelar la corta todo. */
    private suspend fun run(id: String, request: YoutubeDLRequest): String =
        runInterruptible(Dispatchers.IO) {
            try {
                YoutubeDL.execute(request, id, false).out
            } catch (e: YoutubeDL.CanceledException) {
                throw DownloadCanceledException()
            } catch (e: YoutubeDLException) {
                throw DownloadFailedException(summarizeError(e.message), e)
            }
        }

    private companion object {
        const val RESULT_COUNT = 10
    }
}
