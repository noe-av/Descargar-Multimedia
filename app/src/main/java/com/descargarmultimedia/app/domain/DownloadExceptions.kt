package com.descargarmultimedia.app.domain

/** La descarga fue cancelada por el usuario. */
class DownloadCanceledException : Exception()

/** La descarga falló; [message] es un resumen legible del motivo. */
class DownloadFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * No se pudo identificar la canción del video corto, o no existe completa.
 * [songLabel] es "Artista - Canción" si se llegó a identificar.
 */
class SongNotFoundException(val songLabel: String? = null) : Exception("No se encontró la canción")
