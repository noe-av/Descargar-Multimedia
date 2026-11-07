package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadProgress
import com.descargarmultimedia.app.domain.model.DownloadRequest
import java.io.File

enum class EngineUpdateResult { UPDATED, ALREADY_UP_TO_DATE }

/**
 * Contrato del motor que baja el contenido. Hoy lo implementa yt-dlp,
 * pero el resto de la app no depende de eso.
 */
interface MediaDownloader {
    /** Prepara el motor (extrae binarios la primera vez). Es seguro llamarlo varias veces. */
    suspend fun ensureReady()

    /**
     * Descarga el contenido en [DownloadRequest.outputDir] y devuelve el archivo final.
     * @throws DownloadCanceledException si se canceló con [cancel].
     * @throws DownloadFailedException si el motor reportó un error.
     */
    suspend fun download(request: DownloadRequest, onProgress: (DownloadProgress) -> Unit): File

    /** Cancela la descarga con ese id (si está en curso). */
    fun cancel(id: String)

    /** Descarga la última versión del motor, para cuando un sitio cambia y deja de funcionar. */
    suspend fun updateEngine(): EngineUpdateResult

    fun engineVersion(): String?
}
