package com.descargarmultimedia.app.data.engine

import android.content.Context
import com.descargarmultimedia.app.domain.EngineUpdateStore

/** Guarda las fechas de actualización del motor en SharedPreferences. */
class PrefsEngineUpdateStore(context: Context) : EngineUpdateStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override var lastSuccessMillis: Long
        get() = prefs.getLong(KEY_SUCCESS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_SUCCESS, value).apply()
        }

    override var lastAttemptMillis: Long
        get() = prefs.getLong(KEY_ATTEMPT, 0L)
        set(value) {
            prefs.edit().putLong(KEY_ATTEMPT, value).apply()
        }

    private companion object {
        const val FILE_NAME = "engine_update"
        const val KEY_SUCCESS = "last_success"
        const val KEY_ATTEMPT = "last_attempt"
    }
}
