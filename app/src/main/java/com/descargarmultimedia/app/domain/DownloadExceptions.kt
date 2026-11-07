package com.descargarmultimedia.app.domain

/** La descarga fue cancelada por el usuario. */
class DownloadCanceledException : Exception()

/** La descarga falló; [message] es un resumen legible del motivo. */
class DownloadFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)
