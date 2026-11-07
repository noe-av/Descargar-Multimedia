package com.descargarmultimedia.app.data.storage

import com.descargarmultimedia.app.domain.model.DownloadFormat

internal object MimeTypes {

    private val BY_EXTENSION = mapOf(
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "mov" to "video/quicktime",
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "ogg" to "audio/ogg",
        "opus" to "audio/ogg",
    )

    fun forFile(extension: String, format: DownloadFormat): String =
        BY_EXTENSION[extension.lowercase()] ?: when (format) {
            DownloadFormat.AUDIO -> "audio/mpeg"
            DownloadFormat.VIDEO -> "video/mp4"
        }
}
