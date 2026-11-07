package com.descargarmultimedia.app.data.engine

private const val MAX_ERROR_LENGTH = 160

/** yt-dlp devuelve mucho texto; nos quedamos con la línea "ERROR:" más útil. */
internal fun summarizeError(raw: String?): String {
    val lines = raw.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
    val error = lines.lastOrNull { it.startsWith("ERROR:") } ?: lines.lastOrNull() ?: return ""
    return error.removePrefix("ERROR:").trim().take(MAX_ERROR_LENGTH)
}
