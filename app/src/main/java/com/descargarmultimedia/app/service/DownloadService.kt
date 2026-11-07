package com.descargarmultimedia.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import com.descargarmultimedia.app.di.appContainer
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que procesa la cola de a una descarga por vez
 * y mantiene la notificación de progreso. Se detiene solo al vaciar la cola.
 *
 * Todo el control (decidir si hay trabajo, detenerse) ocurre en el hilo
 * principal; el trabajo pesado lo hace el [com.descargarmultimedia.app.domain.DownloadProcessor]
 * en segundo plano.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var worker: Job? = null
    private var lastStartId = 0
    private var lastRenderAt = 0L
    private var lastRenderedStatus: DownloadStatus? = null

    private val container by lazy { appContainer }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId

        if (intent?.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_ITEM_ID)?.let(container.downloadManager::cancel)
            if (worker == null) stopSelf(startId)
            return START_NOT_STICKY
        }

        // Debe llamarse enseguida después de startForegroundService().
        startForeground(
            DownloadNotifications.PROGRESS_NOTIFICATION_ID,
            DownloadNotifications.progress(this, null, container.downloadQueue.queuedCount()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (worker == null) {
            worker = scope.launch { runQueue() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runQueue() {
        val queue = container.downloadQueue
        while (true) {
            val item = queue.claimNext() ?: break
            render(item, force = true)

            val observer = scope.launch {
                queue.items.collect { list ->
                    list.firstOrNull { it.id == item.id }
                        ?.takeIf { it.status.isActive }
                        ?.let { render(it) }
                }
            }
            try {
                container.downloadProcessor.process(item)
            } finally {
                observer.cancel()
            }

            queue.find(item.id)?.let { finished ->
                DownloadNotifications.finished(this, finished)?.let { notification ->
                    notificationManager.notify(finished.id.hashCode(), notification)
                }
            }
        }

        // Sin suspender entre la última revisión y estas líneas: no se pierde ningún pedido nuevo.
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(lastStartId)
    }

    /** Actualiza la notificación fija, como mucho ~1 vez por segundo para no saturar al sistema. */
    private fun render(item: DownloadItem, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val statusChanged = item.status != lastRenderedStatus
        if (!force && !statusChanged && now - lastRenderAt < PROGRESS_THROTTLE_MS) return
        lastRenderAt = now
        lastRenderedStatus = item.status
        notificationManager.notify(
            DownloadNotifications.PROGRESS_NOTIFICATION_ID,
            DownloadNotifications.progress(this, item, container.downloadQueue.queuedCount()),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CANCEL = "com.descargarmultimedia.app.action.CANCEL"
        const val EXTRA_ITEM_ID = "item_id"
        private const val PROGRESS_THROTTLE_MS = 1_000L

        /** Agrega una descarga a la cola y se asegura de que el servicio esté corriendo. */
        fun enqueue(context: Context, url: String, format: DownloadFormat) {
            context.appContainer.downloadManager.enqueue(url, format)
            start(context)
        }

        /** Reactiva el servicio (p. ej. después de reintentar una descarga). */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }
    }
}
