package com.descargarmultimedia.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import com.descargarmultimedia.app.R
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadStatus
import com.descargarmultimedia.app.ui.main.MainActivity
import com.descargarmultimedia.app.util.TimeFormat

/** Crea los canales y arma las notificaciones de progreso y de descarga terminada. */
object DownloadNotifications {

    const val PROGRESS_NOTIFICATION_ID = 1001
    private const val CHANNEL_PROGRESS = "downloads_progress"
    private const val CHANNEL_DONE = "downloads_done"

    fun registerChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROGRESS,
                context.getString(R.string.channel_progress_name),
                NotificationManager.IMPORTANCE_LOW, // sin sonido: solo muestra el avance
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                context.getString(R.string.channel_done_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    /** Notificación fija mientras se descarga. [item] es null justo al arrancar el servicio. */
    fun progress(context: Context, item: DownloadItem?, queuedCount: Int): Notification {
        val builder = Notification.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context))

        if (item == null) {
            return builder
                .setContentTitle(context.getString(R.string.notif_preparing))
                .setProgress(0, 0, true)
                .build()
        }

        val line = progressLine(context, item)
        val text = if (queuedCount > 0) {
            line + " · " + context.getString(R.string.notif_queued_more, queuedCount)
        } else {
            line
        }
        builder.setContentTitle(item.displayName).setContentText(text)

        if (item.status == DownloadStatus.DOWNLOADING && item.progress > 0f) {
            builder.setProgress(100, item.progress.toInt().coerceIn(0, 100), false)
        } else {
            builder.setProgress(0, 0, true)
        }

        val cancelIntent = Intent(context, DownloadService::class.java)
            .setAction(DownloadService.ACTION_CANCEL)
            .putExtra(DownloadService.EXTRA_ITEM_ID, item.id)
        val cancelPending = PendingIntent.getService(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_stat_download),
                context.getString(R.string.notif_cancel),
                cancelPending,
            ).build(),
        )
        return builder.build()
    }

    /** Aviso individual cuando una descarga termina bien o falla. Null si no hay nada que avisar. */
    fun finished(context: Context, item: DownloadItem): Notification? {
        val builder = Notification.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setAutoCancel(true)
            .setContentText(item.displayName)

        return when (item.status) {
            DownloadStatus.COMPLETED -> builder
                .setContentTitle(context.getString(R.string.notif_done_title))
                .setContentIntent(openFileIntent(context, item))
                .build()
            DownloadStatus.FAILED -> builder
                .setContentTitle(context.getString(R.string.notif_failed_title))
                .setContentText(
                    item.errorMessage?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.notif_failed_generic),
                )
                .setContentIntent(openAppIntent(context))
                .build()
            DownloadStatus.NOT_FOUND -> builder
                .setContentTitle(context.getString(R.string.notif_not_found_title))
                .setContentIntent(openAppIntent(context))
                .build()
            else -> null // cancelada u otro estado: sin aviso
        }
    }

    private fun progressLine(context: Context, item: DownloadItem): String = when {
        item.status == DownloadStatus.UPDATING -> context.getString(R.string.notif_updating)
        item.status == DownloadStatus.SEARCHING -> context.getString(R.string.notif_searching)
        item.status == DownloadStatus.PROCESSING -> context.getString(R.string.notif_processing)
        item.progress <= 0f -> context.getString(R.string.notif_preparing)
        item.etaSeconds != null -> context.getString(
            R.string.notif_downloading_eta,
            item.progress.toInt(),
            TimeFormat.eta(item.etaSeconds),
        )
        else -> context.getString(R.string.notif_downloading, item.progress.toInt())
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openFileIntent(context: Context, item: DownloadItem): PendingIntent {
        val uri = item.outputUri ?: return openAppIntent(context)
        val mime = if (item.format == DownloadFormat.AUDIO) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(uri), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
