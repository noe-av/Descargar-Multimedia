package com.descargarmultimedia.app.domain

/** Memoria falsa para las fechas de actualización del motor. */
class FakeUpdateStore(
    override var lastSuccessMillis: Long = 0L,
    override var lastAttemptMillis: Long = 0L,
) : EngineUpdateStore

/**
 * Un actualizador que cree que el motor se actualizó hace un momento: nunca actualiza
 * ni pide reintentar. Sirve para las pruebas que no tratan de actualizaciones.
 */
fun freshUpdater(downloader: MediaDownloader): EngineUpdater {
    val now = 1_000_000_000_000L
    return EngineUpdater(downloader, FakeUpdateStore(lastSuccessMillis = now, lastAttemptMillis = now)) { now }
}
