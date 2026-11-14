package com.descargarmultimedia.app.domain.model

import java.io.File

/**
 * Pedido concreto que recibe el motor de descarga.
 *
 * @property outputName nombre del archivo sin extensión; si es null se usa el título del video.
 */
data class DownloadRequest(
    val id: String,
    val url: String,
    val format: DownloadFormat,
    val outputDir: File,
    val outputName: String? = null,
)
