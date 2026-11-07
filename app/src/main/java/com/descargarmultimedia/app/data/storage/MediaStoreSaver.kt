package com.descargarmultimedia.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.descargarmultimedia.app.domain.MediaSaver
import com.descargarmultimedia.app.domain.model.DownloadFormat
import java.io.File
import java.io.IOException

/**
 * Guarda los archivos con MediaStore, la forma oficial en Android 10+:
 * no requiere permisos de almacenamiento y el archivo aparece en
 * Descargas (video) o Música (audio), dentro de la carpeta "Descargar Multimedia".
 */
class MediaStoreSaver(private val context: Context) : MediaSaver {

    override fun save(file: File, format: DownloadFormat): String {
        val resolver = context.contentResolver
        val displayName = FileNames.sanitize(file.name)

        val (collection, directory) = when (format) {
            DownloadFormat.AUDIO ->
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                    Environment.DIRECTORY_MUSIC
            DownloadFormat.VIDEO ->
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                    Environment.DIRECTORY_DOWNLOADS
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, MimeTypes.forFile(file.extension, format))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$APP_FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("No se pudo crear el archivo en el almacenamiento")

        try {
            val output = resolver.openOutputStream(uri)
                ?: throw IOException("No se pudo abrir el archivo de destino")
            output.use { out -> file.inputStream().use { input -> input.copyTo(out) } }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // no dejar un archivo a medias
            throw e
        }

        file.delete()
        return uri.toString()
    }

    private companion object {
        const val APP_FOLDER = "Descargar Multimedia"
    }
}
