package com.example.feature.sharedlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.format.CurrencyFormatter
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.PriceDropBadge
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.ActivityEvent
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListInvitation
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.Product
import com.example.domain.model.SharedListMember

@Composable
internal fun SecretGiftShieldBanner(hiddenFromName: String) {
  val extended = WishDropDesign.colors
  Surface(
    shape = WishDropDesign.radii.large,
    color = extended.coralContainer.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, extended.onCoralContainer.copy(alpha = 0.28f)),
    modifier = Modifier.fillMaxWidth().testTag("shared_list_secret_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = extended.onCoralContainer.copy(alpha = 0.14f),
        contentColor = extended.onCoralContainer,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = WishDropIcons.SecretHidden, contentDescription = null)
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.shared_list_secret_shield_title),
          style = MaterialTheme.typography.titleMedium,
          color = extended.onCoralContainer,
        )
        Text(
          text = stringResource(R.string.shared_list_secret_shield_desc, hiddenFromName),
          style = MaterialTheme.typography.bodySmall,
          color = extended.onCoralContainer.copy(alpha = 0.88f),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisibilitySelectorCard(
  currentVisibility: ListVisibility,
  hiddenFromMemberId: String?,
  recipients: List<GiftRecipient>,
  onUpdateVisibility: (ListVisibility, String?) -> Unit,
) {
  PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.shared_list_visibility_section),
        style = MaterialTheme.typography.titleMedium,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ListVisibility.entries.forEach { mode ->
          val labelRes = when (mode) {
            ListVisibility.PRIVATE -> R.string.shared_list_visibility_private
            ListVisibility.SHARED -> R.string.shared_list_visibility_shared
            ListVisibility.HIDDEN -> R.string.shared_list_visibility_hidden
            ListVisibility.OWNER_ONLY -> R.string.shared_list_visibility_owner_only
          }
          FilterChip(
            selected = currentVisibility == mode,
            onClick = {
              val targetHidden =
                if (mode == ListVisibility.HIDDEN) hiddenFromMemberId ?: "rec_dalma" else null
              onUpdateVisibility(mode, targetHidden)
            },
            label = { Text(stringResource(labelRes)) },
            leadingIcon = {
              val icon =
                when (mode) {
                  ListVisibility.PRIVATE,
                  ListVisibility.OWNER_ONLY -> WishDropIcons.Lock
                  ListVisibility.HIDDEN -> WishDropIcons.SecretHidden
                  ListVisibility.SHARED -> WishDropIcons.Group
                }
              Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("visibility_chip_${mode.name.lowercase()}"),
          )
        }
      }

      if (currentVisibility == ListVisibility.HIDDEN && recipients.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          recipients
            .filter { it.id != "rec_attila" }
            .forEach { rec ->
              FilterChip(
                selected = hiddenFromMemberId == rec.id,
                onClick = { onUpdateVisibility(ListVisibility.HIDDEN, rec.id) },
                label = { Text("Hide from ${rec.name}") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
        }
      }
    }
  }
}

@Composable
internal fun CollaboratorMemberCard(
  member: SharedListMember,
  onClick: () -> Unit,
  onRemove: (() -> Unit)? = null,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier =
      Modifier.width(210.dp)
        .minimumInteractiveComponentSize()
        .testTag("shared_member_${member.memberId}"),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PersonAvatarBadge(name = member.memberName, accentKey = member.accentKey, size = 42.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = member.memberName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = member.role.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (onRemove != null) {
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(28.dp).testTag("shared_delete_member_${member.memberId}"),
        ) {
          Icon(
            imageVector = WishDropIcons.Delete,
            contentDescription = "Remove member",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun PendingInvitationRowCard(
  invitation: ListInvitation,
  onAccept: () -> Unit,
) {
  Surface(
    shape = WishDropDesign.radii.medium,
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    modifier = Modifier.fillMaxWidth().testTag("shared_invite_${invitation.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = WishDropIcons.Mail,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = invitation.inviteeEmail,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Meghívó tőle: ${invitation.inviterName} • ${invitation.proposedRole.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      FilledTonalButton(
        onClick = onAccept,
        modifier =
          Modifier.fillMaxWidth()
            .testTag("accept_invite_button_${invitation.id}"),
      ) {
        Icon(
          imageVector = WishDropIcons.Check,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.shared_list_accept_join))
      }
    }
  }
}

@Composable
internal fun SharedListProductItemCard(
  product: Product,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.large,
    modifier = Modifier.fillMaxWidth().testTag("shared_list_product_${product.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      WishProductArtwork(
        imageKey = product.imageKey,
        productTitle = product.title,
        shape = WishDropDesign.radii.medium,
        modifier = Modifier.size(84.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = product.store,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          if (product.isPurchased) {
            Text(
              text = "Purchased ✓",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = WishDropDesign.colors.onSavingsContainer,
            )
          }
        }
        Text(
          text = product.title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
            style = WishDropDesign.numericTypography.cardPrice,
          )
          PriceDropBadge(percentChange = product.priceChangePercent)
        }
      }
    }
  }
}

@Composable
internal fun SharedListActivityRow(event: ActivityEvent) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PersonAvatarBadge(name = event.actorName, accentKey = event.actorAccentKey, size = 34.dp)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = event.actionSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = event.eventType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InviteCollaboratorDialog(
  listTitle: String = "Közös kívánságlista",
  knownEmails: List<String> = emptyList(),
  onDismiss: () -> Unit,
  onSend: (String, ListMemberRole) -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var selectedRole by rememberSaveable { mutableStateOf(ListMemberRole.EDITOR) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val cleanEmail = email.trim().lowercase()

  val isAppUser = androidx.compose.runtime.remember(cleanEmail, knownEmails) {
    cleanEmail.isNotEmpty() && (
      cleanEmail.contains("pixelmail") ||
      cleanEmail.contains("attila") ||
      cleanEmail.contains("dalma") ||
      cleanEmail.contains("armin") ||
      knownEmails.any { it.trim().lowercase() == cleanEmail }
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.dialog_invite_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(stringResource(R.string.dialog_invite_email)) },
          placeholder = { Text("dalma.szabo@pixelmail.hu") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth().testTag("shared_invite_email_input"),
        )

        if (email.isNotBlank()) {
          if (isAppUser) {
            androidx.compose.material3.Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.AlertActive,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "WishDrop felhasználó — Értesítést kap az appban 🔔",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
          } else {
            androidx.compose.material3.Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.Mail,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                  text = "Még nincs appja — Értesítő emailt fog kapni ✉️",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            }
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ListMemberRole.entries
            .filter { it != ListMemberRole.OWNER }
            .forEach { role ->
              FilterChip(
                selected = selectedRole == role,
                onClick = { selectedRole = role },
                label = { Text(role.label) },
              )
            }
        }

        OutlinedButton(
          onClick = {
            val inviteMessage = """
              Szia! Meghívtalak a(z) "$listTitle" WishDrop közös kívánságlistámra!
              
              1. Töltsd le az appot a GitHubról:
              👉 https://github.com/aistudio/wishdrop/releases
              
              2. Csatlakozz az appban a listához!
            """.trimIndent()

            val shareIntent = android.content.Intent().apply {
              action = android.content.Intent.ACTION_SEND
              putExtra(android.content.Intent.EXTRA_TEXT, inviteMessage)
              type = "text/plain"
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Meghívó link megosztása WishDrop-hoz"))
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
          Icon(imageVector = WishDropIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Meghívó Link Megosztása (GitHub)")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (email.isNotBlank()) {
            onSend(email.trim(), selectedRole)
            if (!isAppUser) {
              com.example.core.notification.WishDropNotificationManager.sendEmailInvitation(
                context = context,
                recipientEmail = email.trim(),
                inviterName = "WishDrop",
                listTitle = listTitle,
                role = selectedRole.label,
                isHungarian = true,
              )
            }
            onDismiss()
          }
        },
        enabled = email.isNotBlank() && email.contains("@"),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("shared_invite_send_button"),
      ) {
        Icon(imageVector = WishDropIcons.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.dialog_invite_send))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun QuickAddSharedWishDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, Int) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var store by rememberSaveable { mutableStateOf("Alza") }
  var price by rememberSaveable { mutableStateOf("24990") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.dialog_quick_add_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.dialog_quick_add_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("shared_quick_add_name"),
        )
        OutlinedTextField(
          value = store,
          onValueChange = { store = it },
          label = { Text(stringResource(R.string.dialog_quick_add_store)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = price,
          onValueChange = { price = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.dialog_quick_add_price)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank()) {
            onConfirm(name, store, price.toIntOrNull() ?: 24_990)
            onDismiss()
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun MemberRolePickerDialog(
  member: SharedListMember,
  onDismiss: () -> Unit,
  onSelectRole: (ListMemberRole) -> Unit,
  onRemoveMember: () -> Unit = {},
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${stringResource(R.string.dialog_role_title)} — ${member.memberName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListMemberRole.entries.forEach { role ->
          Surface(
            shape = WishDropDesign.radii.small,
            color =
              if (member.role == role) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button) {
                  onSelectRole(role)
                  onDismiss()
                },
          ) {
            Text(
              text = role.label,
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
        TextButton(
          onClick = {
            onRemoveMember()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
          Icon(
            imageVector = WishDropIcons.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tag eltávolítása a listáról", color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
