package com.descargarmultimedia.app.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.descargarmultimedia.app.R
import com.descargarmultimedia.app.domain.model.DownloadFormat

/**
 * La ventana emergente: solo dos opciones, Música o Video.
 * Si lo compartido no tiene un enlace, avisa y ofrece cerrar.
 */
@Composable
fun ShareDialog(
    url: String?,
    onFormatSelected: (DownloadFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.share_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (url == null) {
                    Text(text = stringResource(R.string.share_no_link))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.share_close))
                    }
                } else {
                    Text(
                        text = url,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onFormatSelected(DownloadFormat.AUDIO) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.share_music)) }
                        Button(
                            onClick = { onFormatSelected(DownloadFormat.VIDEO) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.share_video)) }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.share_cancel))
                    }
                }
            }
        }
    }
}
