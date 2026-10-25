package com.example.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListVisibility
import com.example.domain.model.SharedListSummary

@Composable
internal fun HomeSharedListsSection(
  sharedLists: List<SharedListSummary>,
  onOpenSharedList: (String) -> Unit,
  onCreateListClick: () -> Unit,
) {
  val spacing = WishDropDesign.spacing
  Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
    ExpressiveSectionHeader(
      title = stringResource(R.string.section_shared_lists),
      subtitle = stringResource(R.string.section_shared_lists_sub),
      actionLabel = stringResource(R.string.action_create_list),
      onActionClick = onCreateListClick,
      modifier = Modifier.testTag("home_shared_lists_header"),
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(spacing.md),
      contentPadding = PaddingValues(vertical = 2.dp),
      modifier = Modifier.testTag("home_shared_lists_row"),
    ) {
      items(
        items = sharedLists,
        key = { it.id },
        contentType = { "shared_list_card" },
      ) { list ->
        HomeSharedListCard(
          list = list,
          onClick = { onOpenSharedList(list.id) },
        )
      }
    }
  }
}

@Composable
private fun HomeSharedListCard(
  list: SharedListSummary,
  onClick: () -> Unit,
) {
  val extended = WishDropDesign.colors
  val isSecret =
    list.visibility == ListVisibility.HIDDEN || !list.hiddenFromMemberName.isNullOrBlank()

  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.large,
    containerColor =
      if (isSecret) {
        extended.coralContainer.copy(alpha = 0.45f)
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    modifier =
      Modifier.width(248.dp)
        .semantics(mergeDescendants = true) {}
        .testTag("home_shared_list_${list.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color =
            if (isSecret) {
              extended.coralContainer
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
          contentColor =
            if (isSecret) {
              extended.onCoralContainer
            } else {
              MaterialTheme.colorScheme.onPrimaryContainer
            },
        ) {
          Text(
            text = stringResource(R.string.shared_list_items_count, list.itemCount),
            style = WishDropDesign.numericTypography.pillMetric,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        if (isSecret) {
          Icon(
            imageVector = WishDropIcons.Shield,
            contentDescription = null,
            tint = extended.onCoralContainer,
            modifier = Modifier.size(18.dp),
          )
        } else {
          Icon(
            imageVector = WishDropIcons.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "${list.emoji} ${list.title}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = list.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (isSecret && !list.hiddenFromMemberName.isNullOrBlank()) {
        Surface(
          shape = CircleShape,
          color = extended.coralContainer,
          contentColor = extended.onCoralContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = WishDropIcons.SecretHidden,
              contentDescription = null,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = stringResource(R.string.shared_list_secret_badge, list.hiddenFromMemberName!!),
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          list.memberNames.take(3).forEachIndexed { idx, name ->
            val accent =
              when (idx) {
                0 -> "indigo"
                1 -> "coral"
                else -> "teal"
              }
            PersonAvatarBadge(name = name, accentKey = accent, size = 24.dp)
          }
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = list.memberNames.joinToString(", "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreateSharedListDialog(
  recipients: List<GiftRecipient>,
  onDismiss: () -> Unit,
  onCreate: (title: String, emoji: String, subtitle: String, hiddenFromMemberId: String?) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf("") }
  var emoji by rememberSaveable { mutableStateOf("🎁") }
  var subtitle by rememberSaveable { mutableStateOf("") }
  var secretMode by rememberSaveable { mutableStateOf(false) }
  var selectedHiddenId by rememberSaveable {
    mutableStateOf(recipients.firstOrNull()?.id)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.create_list_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it.take(4) },
            label = { Text(stringResource(R.string.create_list_emoji_label)) },
            singleLine = true,
            modifier = Modifier.width(88.dp).testTag("create_list_emoji_input"),
          )
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.create_list_title_label)) },
            placeholder = { Text(stringResource(R.string.create_list_title_placeholder)) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("create_list_title_input"),
          )
        }
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text(stringResource(R.string.create_list_subtitle_label)) },
          placeholder = { Text(stringResource(R.string.create_list_subtitle_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_list_subtitle_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.create_list_secret_toggle),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          )
          Switch(
            checked = secretMode,
            onCheckedChange = { secretMode = it },
            modifier = Modifier.testTag("create_list_secret_switch"),
          )
        }
        if (secretMode && recipients.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            recipients.forEach { recipient ->
              FilterChip(
                selected = selectedHiddenId == recipient.id,
                onClick = { selectedHiddenId = recipient.id },
                label = { Text(recipient.name) },
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(
            title,
            emoji,
            subtitle,
            if (secretMode) selectedHiddenId else null,
          )
          onDismiss()
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("create_list_confirm_button"),
      ) {
        Text(stringResource(R.string.create_list_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.create_list_cancel))
      }
    },
  )
}
