package com.descargarmultimedia.app.domain

import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lista de descargas en memoria. Es segura para usarse desde varios hilos
 * (la interfaz, el servicio y el motor la modifican a la vez).
 */
class DownloadQueue {

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    fun enqueue(url: String, format: DownloadFormat): DownloadItem {
        val item = DownloadItem(id = UUID.randomUUID().toString(), url = url, format = format)
        _items.update { it + item }
        return item
    }

    /**
     * Toma la siguiente descarga en espera y la marca como en curso.
     * Devuelve null si no queda ninguna.
     */
    fun claimNext(): DownloadItem? {
        var claimed: DownloadItem? = null
        _items.update { list ->
            val next = list.firstOrNull { it.status == DownloadStatus.QUEUED }
            claimed = next
            if (next == null) {
                list
            } else {
                list.map { if (it.id == next.id) it.copy(status = DownloadStatus.DOWNLOADING) else it }
            }
        }
        return claimed?.copy(status = DownloadStatus.DOWNLOADING)
    }

    /** Modifica una descarga. Se ignora si ya terminó, para no pisar un "cancelada". */
    fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.update { list ->
            list.map { if (it.id == id && !it.status.isTerminal) transform(it) else it }
        }
    }

    fun markCanceled(id: String) {
        update(id) { it.copy(status = DownloadStatus.CANCELED, etaSeconds = null) }
    }

    fun find(id: String): DownloadItem? = _items.value.firstOrNull { it.id == id }

    fun queuedCount(): Int = _items.value.count { it.status == DownloadStatus.QUEUED }

    fun remove(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    fun clearFinished() {
        _items.update { list -> list.filterNot { it.status.isTerminal } }
    }
}
