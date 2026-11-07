package com.descargarmultimedia.app.data.engine

import com.descargarmultimedia.app.domain.model.DownloadProgress
import java.io.File

/**
 * Traduce las líneas de texto que imprime yt-dlp en un [DownloadProgress].
 *
 * yt-dlp reporta el porcentaje de CADA pista por separado (video y audio
 * se bajan aparte y se unen al final), así que aquí se combinan en un solo
 * avance total que no retrocede.
 */
internal class ProgressTracker {

    private var totalStreams = 1
    private var destinations = 0
    private var title: String? = null
    private var finalizing = false
    private var stalePercent: Float? = null
    private var last: DownloadProgress? = null

    /**
     * @param line línea de salida de yt-dlp.
     * @param streamPercent último porcentaje de la pista actual (-1 si aún no hay).
     * @param etaSeconds último tiempo restante estimado (-1 si no hay).
     * @return un avance nuevo, o null si nada cambió.
     */
    fun onLine(line: String, streamPercent: Float, etaSeconds: Long): DownloadProgress? {
        FORMAT_COUNT.find(line)?.let { match ->
            totalStreams = match.groupValues[1].split('+').size.coerceAtLeast(1)
        }

        DESTINATION.find(line)?.let { match ->
            destinations++
            if (title == null) title = titleFrom(match.groupValues[1])
            // Al empezar otra pista, el porcentaje anterior (100) sigue "pegado" unas líneas.
            if (destinations > 1) stalePercent = streamPercent
        }

        if (FINALIZING_PREFIXES.any { line.startsWith(it) }) finalizing = true

        val currentPercent = when {
            streamPercent < 0f -> 0f
            stalePercent != null && streamPercent == stalePercent -> 0f
            else -> {
                stalePercent = null
                streamPercent
            }
        }

        val overall = if (finalizing) {
            100f
        } else {
            val finished = (destinations - 1).coerceIn(0, totalStreams - 1)
            (finished + currentPercent.coerceIn(0f, 100f) / 100f) / totalStreams * 100f
        }

        val progress = DownloadProgress(
            percent = overall,
            etaSeconds = etaSeconds.takeIf { it >= 0 && !finalizing },
            title = title,
            isFinalizing = finalizing,
        )
        if (progress == last) return null
        last = progress
        return progress
    }

    private fun titleFrom(path: String): String =
        File(path.trim()).nameWithoutExtension.replace(FORMAT_SUFFIX, "")

    private companion object {
        val FORMAT_COUNT = Regex("""Downloading \d+ format\(s\):\s*(\S+)""")
        val DESTINATION = Regex("""^\[download]\s+Destination:\s*(.+)$""")
        val FORMAT_SUFFIX = Regex("""\.f\d+$""")
        val FINALIZING_PREFIXES = listOf(
            "[Merger]",
            "[ExtractAudio]",
            "[VideoConvertor]",
            "[VideoRemuxer]",
            "[EmbedMetadata]",
            "[Metadata]",
            "[Fixup",
        )
    }
}
