package com.descargarmultimedia.app.domain.model

/** Una descarga dentro de la cola. Es inmutable: los cambios crean una copia. */
data class DownloadItem(
    val id: String,
    val url: String,
    val format: DownloadFormat,
    val title: String? = null,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val etaSeconds: Long? = null,
    val errorMessage: String? = null,
    val outputUri: String? = null,
) {
    /** Nombre a mostrar: el título si ya se conoce, si no, el enlace. */
    val displayName: String
        get() = title?.takeIf { it.isNotBlank() } ?: url

    fun withProgress(update: DownloadProgress): DownloadItem = copy(
        status = if (update.isFinalizing) DownloadStatus.PROCESSING else DownloadStatus.DOWNLOADING,
        progress = update.percent,
        etaSeconds = update.etaSeconds,
        title = update.title ?: title,
    )
}
