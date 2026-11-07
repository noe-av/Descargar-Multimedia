package com.descargarmultimedia.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object NotificationPermission {

    /** En Android 13+ hay que pedir permiso para mostrar notificaciones. */
    fun mustRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
}
