package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import java.io.File

/** Guarda el archivo terminado en su ubicación pública (Descargas / Música). */
interface MediaSaver {
    /** Mueve [file] a su destino final y devuelve la dirección (URI) del archivo guardado. */
    fun save(file: File, format: DownloadFormat): String
}
