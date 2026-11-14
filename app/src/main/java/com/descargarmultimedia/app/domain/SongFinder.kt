package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat

/** La canción completa encontrada: [label] es "Artista - Canción" y [url] el enlace a descargar. */
data class SongMatch(val label: String, val url: String)

/** Averigua qué canción suena en un video corto y encuentra su versión completa. */
interface SongFinder {

    /**
     * @param id identificador de la descarga; permite cancelar la búsqueda junto con ella.
     * @param shortUrl enlace del video corto.
     * @param format el formato pedido: decide si se prefiere el audio o el videoclip.
     * @throws SongNotFoundException si no se pudo identificar la canción o no existe completa.
     * @throws DownloadCanceledException si se canceló durante la búsqueda.
     * @throws DownloadFailedException si el motor falló (por ejemplo, el video es privado).
     */
    suspend fun findFullSong(id: String, shortUrl: String, format: DownloadFormat): SongMatch
}
