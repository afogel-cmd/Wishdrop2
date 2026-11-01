package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.icon.WishDropIcons
import com.example.domain.model.AppUpdateInfo

@Composable
fun AppUpdateDialog(
  updateInfo: AppUpdateInfo,
  downloadPercent: Int?,
  onDismiss: () -> Unit,
  onInstall: () -> Unit,
) {
  val isDownloading = downloadPercent != null

  AlertDialog(
    onDismissRequest = {
      if (!isDownloading) onDismiss()
    },
    icon = {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(52.dp),
      ) {
        Icon(
          imageVector = WishDropIcons.Sparkle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier
            .padding(14.dp)
            .size(24.dp),
        )
      }
    },
    title = {
      Text(
        text = stringResource(R.string.update_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(
            R.string.update_dialog_version,
            updateInfo.latestVersion,
            updateInfo.currentVersion,
          ),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )

        val notesText = updateInfo.releaseNotes?.trim()?.ifBlank { null }
          ?: stringResource(R.string.update_default_notes)

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Újdonságok / Változások:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = notesText,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        if (isDownloading) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            LinearProgressIndicator(
              progress = { (downloadPercent ?: 0) / 100f },
              modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Text(
              text = stringResource(R.string.update_downloading_progress, downloadPercent ?: 0),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onInstall,
        enabled = !isDownloading,
      ) {
        Icon(WishDropIcons.CloudSynced, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.update_action_install))
      }
    },
    dismissButton = {
      if (!isDownloading) {
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.update_action_later))
        }
      }
    },
  )
}
