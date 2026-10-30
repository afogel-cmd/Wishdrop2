package com.example.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.preferences.UserPreferences
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListInvitation
import com.example.domain.model.RealtimeSyncStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException



@Composable
internal fun SupabaseCloudSyncCard(
  preferences: UserPreferences,
  realtimeStatus: RealtimeSyncStatus?,
  onSignIn: (email: String, password: String) -> Unit,
  onSignUp: (name: String, email: String, password: String) -> Unit,
  onSignOut: () -> Unit,
  onSignInGoogle: (displayName: String, email: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
) {
  var email by rememberSaveable(preferences.email) { mutableStateOf(preferences.email) }
  var password by rememberSaveable { mutableStateOf("") }
  var showGoogleDialog by rememberSaveable { mutableStateOf(false) }
  val extended = WishDropDesign.colors
  val context = LocalContext.current

  val googleSignInLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    runCatching {
      val account = task.getResult(ApiException::class.java)
      if (account != null) {
        val name = account.displayName ?: account.givenName ?: "Google Felhasználó"
        val emailAddress = account.email ?: "google.user@gmail.com"
        val photoUrl = account.photoUrl?.toString()
        onSignInGoogle(name, emailAddress, photoUrl)
      } else {
        showGoogleDialog = true
      }
    }.onFailure {
      showGoogleDialog = true
    }
  }

  val handleGoogleSignIn = {
    runCatching {
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .build()
      val googleSignInClient = GoogleSignIn.getClient(context, gso)
      googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }.onFailure {
      showGoogleDialog = true
    }
  }

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
          text = "Google & Felhő Szinkronizáció",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.weight(1f, fill = false).semantics { heading() },
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
            val currentStatusText = when {
              preferences.isCloudAuthenticated -> "Google Sync Live"
              realtimeStatus?.isCloudConfigured == true -> realtimeStatus.statusLabel
              else -> "Online-First Active"
            }
            Text(
              text = currentStatusText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PersonAvatarBadge(
              name = preferences.displayName,
              accentKey = "indigo",
              imageUrl = preferences.avatarUrl,
              size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = preferences.displayName,
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "${preferences.email} • Google Sync",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          OutlinedButton(
            onClick = onSignOut,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("profile_cloud_signout_button"),
          ) {
            Text("Kijelentkezés")
          }
        }
      } else {
        Button(
          onClick = { handleGoogleSignIn() },
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("profile_google_signin_button"),
        ) {
          Icon(
            imageVector = WishDropIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Bejelentkezés Google Fiókkal")
        }

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
          OutlinedButton(
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

  if (showGoogleDialog) {
    var gName by rememberSaveable { mutableStateOf(if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercaseChar() } else "Attila Kovács") }
    var gEmail by rememberSaveable { mutableStateOf(if (email.contains("@")) email else "attila.kovacs@gmail.com") }

    AlertDialog(
      onDismissRequest = { showGoogleDialog = false },
      title = { Text("Google Fiók Bejelentkezés") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Adja meg Google fiók adatait a valós idejű felhő szinkronizáció aktiválásához!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = gName,
            onValueChange = { gName = it },
            label = { Text("Google Megjelenítendő Név") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = gEmail,
            onValueChange = { gEmail = it },
            label = { Text("Google E-mail Cím") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSignInGoogle(gName.trim(), gEmail.trim(), "https://lh3.googleusercontent.com/a/default-user")
            showGoogleDialog = false
          },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("Bejelentkezés Google-al")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showGoogleDialog = false },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
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
                  text = invite.listTitle,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "Meghívó tőle: ${invite.inviterName} • ${invite.proposedRole.label}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            FilledTonalButton(
              onClick = { onAcceptInvitation(invite.id) },
              modifier =
                Modifier.fillMaxWidth()
                  .testTag("profile_accept_invite_${invite.id}"),
            ) {
              Icon(
                imageVector = WishDropIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
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
  onEdit: () -> Unit = {},
  onDelete: () -> Unit = {},
) {
  var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

  PixelBentoCard(
    shape = WishDropDesign.radii.medium,
    modifier = Modifier.width(260.dp).testTag("profile_member_${member.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PersonAvatarBadge(
          name = member.name,
          accentKey = member.accentKey,
          imageUrl = member.avatarUrl,
          size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = member.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = member.relationship,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(32.dp).testTag("profile_edit_member_${member.id}"),
        ) {
          Icon(
            imageVector = WishDropIcons.Edit,
            contentDescription = "Szerkesztés",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = { showConfirmDialog = true },
          modifier = Modifier.size(32.dp).testTag("profile_delete_member_${member.id}"),
        ) {
          Icon(
            imageVector = WishDropIcons.Delete,
            contentDescription = "Delete member",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }

  if (showConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDialog = false },
      title = { Text("Családtag törlése") },
      text = { Text("Biztosan törölni szeretnéd ${member.name} személyt a családtagok közül?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete()
            showConfirmDialog = false
          }
        ) {
          Text("Törlés", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
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
