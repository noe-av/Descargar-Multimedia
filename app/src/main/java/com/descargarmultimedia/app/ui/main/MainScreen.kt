package com.descargarmultimedia.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.descargarmultimedia.app.R
import com.descargarmultimedia.app.domain.model.DownloadFormat
import com.descargarmultimedia.app.domain.model.DownloadItem
import com.descargarmultimedia.app.domain.model.DownloadStatus
import com.descargarmultimedia.app.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onStartDownload: (url: String, format: DownloadFormat) -> Unit,
    onRetry: (id: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloads by viewModel.items.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val updatedText = stringResource(R.string.engine_updated)
    val upToDateText = stringResource(R.string.engine_up_to_date)
    val failedText = stringResource(R.string.engine_update_failed)
    LaunchedEffect(state.engineMessage) {
        val text = when (state.engineMessage) {
            EngineMessage.UPDATED -> updatedText
            EngineMessage.UP_TO_DATE -> upToDateText
            EngineMessage.FAILED -> failedText
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(text)
        viewModel.onEngineMessageShown()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
        ) {
            UrlInput(
                value = state.input,
                isError = state.invalidUrl,
                onValueChange = viewModel::onInputChange,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.onDownloadClick(DownloadFormat.AUDIO, onStartDownload) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.share_music)) }
                Button(
                    onClick = { viewModel.onDownloadClick(DownloadFormat.VIDEO, onStartDownload) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.share_video)) }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.main_downloads),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (downloads.any { it.status.isTerminal }) {
                    TextButton(onClick = viewModel::clearFinished) {
                        Text(stringResource(R.string.main_clear_finished))
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.main_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Las más nuevas arriba
                    items(downloads.asReversed(), key = { it.id }) { item ->
                        DownloadRow(
                            item = item,
                            onCancel = { viewModel.cancel(item.id) },
                            onRetry = { onRetry(item.id) },
                        )
                    }
                }
            }

            EngineFooter(
                version = state.engineVersion,
                isUpdating = state.isUpdatingEngine,
                onUpdate = viewModel::updateEngine,
            )
        }
    }
}

@Composable
private fun UrlInput(value: String, isError: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.main_url_label)) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.main_url_invalid)) }
        } else {
            null
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.main_clear_input),
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DownloadRow(item: DownloadItem, onCancel: () -> Unit, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = statusText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> LinearProgressIndicator(
                        progress = { (item.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 12.dp),
                    )
                    DownloadStatus.SEARCHING,
                    DownloadStatus.PROCESSING,
                    -> LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 12.dp),
                    )
                    else -> Unit
                }
            }

            when (item.status) {
                DownloadStatus.QUEUED,
                DownloadStatus.SEARCHING,
                DownloadStatus.DOWNLOADING,
                DownloadStatus.PROCESSING,
                -> IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                    )
                }
                DownloadStatus.FAILED,
                DownloadStatus.CANCELED,
                DownloadStatus.NOT_FOUND,
                -> IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_retry),
                    )
                }
                DownloadStatus.COMPLETED -> Unit
            }
        }
    }
}

@Composable
private fun statusText(item: DownloadItem): String {
    val format = stringResource(
        if (item.format == DownloadFormat.AUDIO) R.string.format_music else R.string.format_video,
    )
    val status = when (item.status) {
        DownloadStatus.QUEUED -> stringResource(R.string.status_queued)
        DownloadStatus.SEARCHING -> stringResource(R.string.status_searching)
        DownloadStatus.DOWNLOADING -> {
            val percent = item.progress.toInt()
            val eta = item.etaSeconds
            if (eta != null) {
                stringResource(R.string.status_downloading_eta, percent, TimeFormat.eta(eta))
            } else {
                stringResource(R.string.status_downloading, percent)
            }
        }
        DownloadStatus.PROCESSING -> stringResource(R.string.status_processing)
        DownloadStatus.COMPLETED -> stringResource(R.string.status_completed)
        DownloadStatus.FAILED -> {
            val detail = item.errorMessage
            if (detail.isNullOrBlank()) {
                stringResource(R.string.status_failed)
            } else {
                stringResource(R.string.status_failed_detail, detail)
            }
        }
        DownloadStatus.CANCELED -> stringResource(R.string.status_canceled)
        DownloadStatus.NOT_FOUND -> stringResource(R.string.status_not_found)
    }
    return "$format · $status"
}

@Composable
private fun EngineFooter(version: String?, isUpdating: Boolean, onUpdate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.engine_label,
                version ?: stringResource(R.string.engine_unknown),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onUpdate, enabled = !isUpdating) {
            Text(
                stringResource(
                    if (isUpdating) R.string.engine_updating else R.string.engine_update,
                ),
            )
        }
    }
}
