package com.descargarmultimedia.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.descargarmultimedia.app.di.AppContainer
import com.descargarmultimedia.app.domain.DownloadManager
import com.descargarmultimedia.app.domain.EngineUpdateResult
import com.descargarmultimedia.app.domain.MediaDownloader
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Resultado de intentar actualizar el motor, para mostrarlo en un aviso. */
enum class EngineMessage { UPDATED, UP_TO_DATE, FAILED }

data class MainUiState(
    val input: String = "",
    val invalidUrl: Boolean = false,
    val engineVersion: String? = null,
    val isUpdatingEngine: Boolean = false,
    val engineMessage: EngineMessage? = null,
)

class MainViewModel(
    private val downloads: DownloadManager,
    private val downloader: MediaDownloader,
) : ViewModel() {

    val items: StateFlow<List<DownloadItem>> = downloads.items

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshEngineVersion()
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text, invalidUrl = false) }
    }

    /** Valida el enlace escrito; si es válido, lo entrega a [start] y limpia el campo. */
    fun onDownloadClick(format: DownloadFormat, start: (url: String, format: DownloadFormat) -> Unit) {
        val url = UrlExtractor.firstUrl(_uiState.value.input)
        if (url == null) {
            _uiState.update { it.copy(invalidUrl = true) }
            return
        }
        start(url, format)
        _uiState.update { it.copy(input = "", invalidUrl = false) }
    }

    fun cancel(id: String) = downloads.cancel(id)

    /** Reintenta una descarga fallida; devuelve true si se volvió a encolar. */
    fun retry(id: String): Boolean = downloads.retry(id) != null

    fun clearFinished() = downloads.clearFinished()

    fun updateEngine() {
        if (_uiState.value.isUpdatingEngine) return
        _uiState.update { it.copy(isUpdatingEngine = true) }
        viewModelScope.launch {
            val message = try {
                when (downloader.updateEngine()) {
                    EngineUpdateResult.UPDATED -> EngineMessage.UPDATED
                    EngineUpdateResult.ALREADY_UP_TO_DATE -> EngineMessage.UP_TO_DATE
                }
            } catch (e: Exception) {
                EngineMessage.FAILED
            }
            _uiState.update { it.copy(isUpdatingEngine = false, engineMessage = message) }
            refreshEngineVersion()
        }
    }

    fun onEngineMessageShown() {
        _uiState.update { it.copy(engineMessage = null) }
    }

    private fun refreshEngineVersion() {
        viewModelScope.launch {
            val version = withContext(Dispatchers.IO) { downloader.engineVersion() }
            _uiState.update { it.copy(engineVersion = version) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(container.downloadManager, container.downloader) as T
            }
    }
}
