package com.descargarmultimedia.app

import android.app.Application
import com.descargarmultimedia.app.di.AppContainer
import com.descargarmultimedia.app.service.DownloadNotifications

class DescargarMultimediaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DownloadNotifications.registerChannels(this)
        container.warmUp()
    }
}
