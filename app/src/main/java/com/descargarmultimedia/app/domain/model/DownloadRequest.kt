package com.descargarmultimedia.app.domain.model

import java.io.File

/** Pedido concreto que recibe el motor de descarga. */
data class DownloadRequest(
    val id: String,
    val url: String,
    val format: DownloadFormat,
    val outputDir: File,
)
