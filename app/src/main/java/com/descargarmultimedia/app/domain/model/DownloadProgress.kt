package com.descargarmultimedia.app.domain.model

/**
 * Avance reportado por el motor de descarga.
 *
 * @property percent avance total de 0 a 100 (suma todas las pistas, p. ej. video + audio).
 * @property etaSeconds segundos estimados restantes, o null si se desconocen.
 * @property title título detectado del video, o null si todavía no se conoce.
 * @property isFinalizing true cuando ya se descargó todo y solo falta unir/convertir.
 */
data class DownloadProgress(
    val percent: Float,
    val etaSeconds: Long?,
    val title: String?,
    val isFinalizing: Boolean,
)
