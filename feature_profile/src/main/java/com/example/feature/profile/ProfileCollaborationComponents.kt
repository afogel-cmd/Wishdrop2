package com.example.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.preferences.UserPreferences
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.FamilyPersona
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListInvitation
import com.example.domain.model.RealtimeSyncStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivePersonaSwitcherCard(
  activeMemberId: String,
  personas: List<FamilyPersona>,
  onSwitchPersona: (String) -> Unit,
) {
  if (personas.isEmpty()) return
  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("profile_persona_switcher_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = WishDropIcons.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = stringResource(R.string.profile_active_viewer_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.semantics { heading() },
        )
      }
      Text(
        text = stringResource(R.string.profile_active_viewer_sub),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        personas.forEach { persona ->
          FilterChip(
            selected = persona.memberId == activeMemberId,
            onClick = { onSwitchPersona(persona.memberId) },
            label = { Text("${persona.displayName} (${persona.relationship})") },
            leadingIcon =
              if (persona.memberId == activeMemberId) {
                {
                  Icon(
                    imageVector = WishDropIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              } else {
                null
              },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("profile_switch_persona_${persona.memberId}"),
          )
        }
      }
    }
  }
}

@Composable
internal fun SupabaseCloudSyncCard(
  preferences: UserPreferences,
  realtimeStatus: RealtimeSyncStatus?,
  onSignIn: (email: String, password: String) -> Unit,
  onSignUp: (name: String, email: String, password: String) -> Unit,
  onSignOut: () -> Unit,
) {
  var email by rememberSaveable(preferences.email) { mutableStateOf(preferences.email) }
  var password by rememberSaveable { mutableStateOf("") }
  val extended = WishDropDesign.colors

  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("profile_cloud_sync_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.profile_cloud_sync_title),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.semantics { heading() },
        )
        Surface(
          shape = CircleShape,
          color = extended.savingsContainer,
          contentColor = extended.onSavingsContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = WishDropIcons.CloudSynced,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = realtimeStatus?.statusLabel ?: "Room + Realtime Ready",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      if (preferences.isCloudAuthenticated) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = preferences.email,
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "Authenticated • PostgREST + Phoenix Realtime Channel",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = onSignOut,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("profile_cloud_signout_button"),
          ) {
            Text(stringResource(R.string.profile_cloud_sign_out))
          }
        }
      } else {
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(stringResource(R.string.dialog_email)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_cloud_email_input"),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(stringResource(R.string.profile_cloud_password)) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_cloud_password_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onSignIn(email, password.ifBlank { "wishdrop-pass" }) },
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("profile_cloud_signin_button"),
          ) {
            Text(stringResource(R.string.profile_cloud_sign_in))
          }
          FilledTonalButton(
            onClick = {
              onSignUp(
                preferences.displayName,
                email,
                password.ifBlank { "wishdrop-pass" },
              )
            },
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("profile_cloud_signup_button"),
          ) {
            Text(stringResource(R.string.profile_cloud_sign_up))
          }
        }
      }
    }
  }
}

@Composable
internal fun PendingInvitationsSection(
  invitations: List<ListInvitation>,
  onAcceptInvitation: (String) -> Unit,
) {
  if (invitations.isEmpty()) return
  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("profile_pending_invites_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ExpressiveSectionHeader(
        title = stringResource(R.string.profile_pending_invites_title),
        subtitle = stringResource(R.string.profile_pending_invites_sub),
      )
      invitations.forEach { invite ->
        Surface(
          shape = WishDropDesign.radii.medium,
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth().testTag("profile_invite_${invite.id}"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = WishDropIcons.Mail,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(text = invite.listTitle, style = MaterialTheme.typography.titleSmall)
              Text(
                text = "From ${invite.inviterName} • ${invite.proposedRole.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            FilledTonalButton(
              onClick = { onAcceptInvitation(invite.id) },
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .testTag("profile_accept_invite_${invite.id}"),
            ) {
              Text(stringResource(R.string.profile_accept_join))
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun FamilyMemberChipCard(
  member: GiftRecipient,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_member_${member.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      PersonAvatarBadge(name = member.name, accentKey = member.accentKey, size = 42.dp)
      Column {
        Text(text = member.name, style = MaterialTheme.typography.titleSmall)
        Text(
          text = member.relationship,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
internal fun SettingSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
  subtitle: String? = null,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(WishDropDesign.radii.small)
        .toggleable(
          value = checked,
          role = Role.Switch,
          onValueChange = onCheckedChange,
        )
        .padding(vertical = 4.dp)
        .testTag(testTag),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(modifier = Modifier.width(4.dp))
    Switch(
      checked = checked,
      onCheckedChange = null,
    )
  }
}
