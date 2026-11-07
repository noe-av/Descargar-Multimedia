package com.descargarmultimedia.app.domain.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED;

    /** Estados finales: una descarga en uno de ellos ya no cambia. */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELED

    val isActive: Boolean
        get() = !isTerminal
}
