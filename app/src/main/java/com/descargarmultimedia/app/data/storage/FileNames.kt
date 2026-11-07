package com.descargarmultimedia.app.data.storage

internal object FileNames {

    private val FORBIDDEN = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private const val MAX_BASE_LENGTH = 150

    /** Quita caracteres que el almacenamiento de Android no admite y limita el largo. */
    fun sanitize(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        val hasExtension = dot > 0 && dot < fileName.length - 1
        val base = if (hasExtension) fileName.substring(0, dot) else fileName
        val extension = if (hasExtension) fileName.substring(dot) else ""

        val cleanBase = base
            .replace(FORBIDDEN, "_")
            .trim()
            .trimEnd('.')
            .take(MAX_BASE_LENGTH)
            .ifBlank { "descarga" }
        return cleanBase + extension.lowercase()
    }
}
