package com.descargarmultimedia.app.domain.model

enum class DownloadStatus {
    QUEUED,

    /** Actualizando yt-dlp (una vez al día, o tras un fallo) antes de descargar o reintentar. */
    UPDATING,

    /** Averiguando qué canción suena en un video corto y buscando su versión completa. */
    SEARCHING,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,

    /** La canción del video corto no se pudo identificar o no existe completa: no se descargó nada. */
    NOT_FOUND;

    /** Estados finales: una descarga en uno de ellos ya no cambia. */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELED || this == NOT_FOUND

    val isActive: Boolean
        get() = !isTerminal
}
