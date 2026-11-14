package com.descargarmultimedia.app.data.engine

import com.descargarmultimedia.app.data.storage.FileNames
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadRequest
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Arma las opciones de yt-dlp según el formato pedido. */
internal object YtDlpArguments {

    // Video: prefiere H.264 + AAC (se reproduce en cualquier celular), máximo 1080p.
    private const val VIDEO_FORMAT =
        "bv*[vcodec^=avc1][height<=1080]+ba[ext=m4a]/" +
            "b[ext=mp4][height<=1080]/" +
            "bv*[height<=1080]+ba/" +
            "b"

    private const val AUDIO_FORMAT = "bestaudio/best"

    fun build(request: DownloadRequest): YoutubeDLRequest {
        val ytRequest = YoutubeDLRequest(request.url)
        ytRequest.addOption("--no-playlist") // un enlace con &list= no debe bajar toda la lista
        ytRequest.addOption("--no-mtime")
        ytRequest.addOption("--newline") // una línea por cada avance, necesario para leer el progreso
        ytRequest.addOption("--windows-filenames")
        ytRequest.addOption("--trim-filenames", 120)
        ytRequest.addOption("--concurrent-fragments", 4)
        ytRequest.addOption("-o", File(request.outputDir, fileNameTemplate(request)).absolutePath)

        when (request.format) {
            DownloadFormat.AUDIO -> {
                ytRequest.addOption("-f", AUDIO_FORMAT)
                ytRequest.addOption("-x")
                ytRequest.addOption("--audio-format", "mp3")
                ytRequest.addOption("--audio-quality", 0)
                ytRequest.addOption("--embed-metadata")
            }
            DownloadFormat.VIDEO -> {
                ytRequest.addOption("-f", VIDEO_FORMAT)
                ytRequest.addOption("--merge-output-format", "mp4")
            }
        }
        return ytRequest
    }

    /** Con nombre propio ("Artista - Canción") se usa tal cual; si no, el título del video. */
    private fun fileNameTemplate(request: DownloadRequest): String {
        val name = request.outputName ?: return "%(title)s.%(ext)s"
        // En una plantilla de yt-dlp un "%" literal se escribe "%%".
        return FileNames.sanitizeBase(name).replace("%", "%%") + ".%(ext)s"
    }
}
