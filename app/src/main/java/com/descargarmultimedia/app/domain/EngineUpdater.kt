package com.descargarmultimedia.app.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Dónde se recuerda cuándo se actualizó el motor (debe sobrevivir a cerrar la app). */
interface EngineUpdateStore {
    /** Última actualización completada con éxito (ms desde 1970), o 0 si nunca hubo una. */
    var lastSuccessMillis: Long

    /** Último intento, haya salido bien o mal (ms desde 1970), o 0 si nunca hubo uno. */
    var lastAttemptMillis: Long
}

/**
 * Mantiene yt-dlp al día sin que el usuario tenga que hacer nada.
 *
 * Los sitios (sobre todo YouTube) cambian seguido y un yt-dlp viejo empieza a fallar.
 * Hay dos defensas:
 * - [refreshIfStale]: antes de descargar, actualiza si pasó más de un día desde la última vez.
 * - [refreshAfterFailure]: si una descarga falla, actualiza al instante para reintentarla.
 *
 * Todo está serializado con un candado: actualizar reemplaza el binario de yt-dlp,
 * así que nunca debe correr a la vez que otra actualización.
 */
class EngineUpdater(
    private val downloader: MediaDownloader,
    private val store: EngineUpdateStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    /** true si hace más de un día que el motor no se actualiza (o nunca se actualizó). */
    fun isStale(): Boolean = elapsedSince(store.lastSuccessMillis) >= STALE_AFTER_MS

    /** Actualiza si hace falta. Nunca falla: sin internet, simplemente se sigue con el motor actual. */
    suspend fun refreshIfStale() {
        mutex.withLock {
            if (!isStale()) return
            // Si el último intento falló hace poco (p. ej. sin internet), no insistir en cada descarga.
            if (elapsedSince(store.lastAttemptMillis) < FAILED_ATTEMPT_COOLDOWN_MS) return
            attempt()
        }
    }

    /**
     * Se llama cuando una descarga falló. Actualiza el motor y devuelve true solo si
     * apareció una versión nueva, que es cuando vale la pena reintentar.
     */
    suspend fun refreshAfterFailure(): Boolean = mutex.withLock {
        // Si se acaba de actualizar, el motor ya es el más nuevo: el fallo es de otra causa.
        if (elapsedSince(store.lastAttemptMillis) < RECENT_ATTEMPT_MS) return@withLock false
        attempt() == EngineUpdateResult.UPDATED
    }

    /** Actualización pedida a mano (botón "Actualizar"): siempre intenta y deja pasar el error. */
    suspend fun updateNow(): EngineUpdateResult = mutex.withLock {
        store.lastAttemptMillis = now()
        downloader.updateEngine().also { store.lastSuccessMillis = now() }
    }

    private suspend fun attempt(): EngineUpdateResult? {
        store.lastAttemptMillis = now()
        return try {
            downloader.updateEngine().also { store.lastSuccessMillis = now() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Tiempo desde [stamp]. Una fecha futura (reloj movido) cuenta como "hace muchísimo". */
    private fun elapsedSince(stamp: Long): Long {
        val elapsed = now() - stamp
        return if (elapsed < 0) Long.MAX_VALUE else elapsed
    }

    companion object {
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000
        const val FAILED_ATTEMPT_COOLDOWN_MS = 30L * 60 * 1000
        const val RECENT_ATTEMPT_MS = 10L * 60 * 1000
    }
}
