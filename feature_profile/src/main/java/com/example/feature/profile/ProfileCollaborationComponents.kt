package com.example.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
internal fun UnifiedProfileGoogleSyncCard(
  preferences: UserPreferences,
  realtimeStatus: RealtimeSyncStatus?,
  onSignInGoogle: (displayName: String, email: String, avatarUrl: String?) -> Unit,
  onSignOut: () -> Unit,
  onSyncNow: () -> Unit,
  onEditProfile: () -> Unit,
  onConfigureCloud: (url: String, anonKey: String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  var showGoogleDialog by rememberSaveable { mutableStateOf(false) }
  var showCloudDialog by rememberSaveable { mutableStateOf(false) }
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
    modifier = modifier.fillMaxWidth().testTag("profile_user_card"),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        PersonAvatarBadge(
          name = preferences.displayName,
          accentKey = "indigo",
          imageUrl = preferences.avatarUrl,
          size = 56.dp,
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = preferences.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = preferences.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          IconButton(
            onClick = onEditProfile,
            modifier = Modifier.testTag("profile_edit_button"),
          ) {
            Icon(
              imageVector = WishDropIcons.Edit,
              contentDescription = stringResource(R.string.profile_edit),
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      val isCloudConfigured = realtimeStatus?.isCloudConfigured == true
      val currentStatusText = when {
        isCloudConfigured -> "Supabase Felhő Szinkron Aktív"
        preferences.isCloudAuthenticated -> "Google Profil Aktív • Helyi Cache"
        else -> "Helyi Adattár • Felhő nincs csatlakoztatva"
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCloudConfigured) extended.savingsContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (isCloudConfigured) extended.onSavingsContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = WishDropIcons.CloudSynced,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = if (isCloudConfigured) extended.onSavingsContainer else MaterialTheme.colorScheme.outline,
            )
            Text(
              text = currentStatusText,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
          TextButton(
            onClick = { showCloudDialog = true },
            modifier = Modifier.height(32.dp),
          ) {
            Text(
              text = if (isCloudConfigured) "Beállítások" else "Felhő beállítása",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (preferences.isCloudAuthenticated) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = onSyncNow,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = WishDropIcons.CloudSynced,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Szinkronizálás")
          }
          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("profile_cloud_signout_button"),
          ) {
            Text("Kijelentkezés")
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Button(
            onClick = { handleGoogleSignIn() },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("profile_google_signin_button"),
          ) {
            Icon(
              imageVector = WishDropIcons.CloudSynced,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bejelentkezés Google Fiókkal")
          }
          Text(
            text = "Jelentkezz be Google fiókoddal a listák szinkronizációjához és a profilképed betöltéséhez!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TextButton(
            onClick = { showGoogleDialog = true },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text(
              text = "Google fiók adatok megadása",
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }
    }
  }

  if (showGoogleDialog) {
    var gName by rememberSaveable {
      mutableStateOf(
        if (preferences.email.contains("@") && preferences.displayName != "Saját profil") {
          preferences.displayName
        } else {
          "Attila Kovács"
        }
      )
    }
    var gEmail by rememberSaveable {
      mutableStateOf(
        if (preferences.email.contains("@") && preferences.email != "felhasznalo@gmail.com") {
          preferences.email
        } else {
          "attila.kovacs@gmail.com"
        }
      )
    }
    var gAvatarUrl by rememberSaveable {
      mutableStateOf(
        preferences.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&h=200&q=80"
      )
    }

    AlertDialog(
      onDismissRequest = { showGoogleDialog = false },
      title = { Text("Google Fiók Bejelentkezés") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Add meg Google fiók adataidat a felhő szinkronizációhoz és a profilkép megjelenítéséhez!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = gName,
            onValueChange = { gName = it },
            label = { Text("Google Név") },
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
          OutlinedTextField(
            value = gAvatarUrl,
            onValueChange = { gAvatarUrl = it },
            label = { Text("Google Profilkép URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSignInGoogle(gName.trim(), gEmail.trim(), gAvatarUrl.trim().ifBlank { null })
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

  if (showCloudDialog) {
    SupabaseConfigDialog(
      initialUrl = preferences.customSupabaseUrl ?: "",
      initialKey = preferences.customSupabaseAnonKey ?: "",
      onDismiss = { showCloudDialog = false },
      onSave = { url, key ->
        onConfigureCloud(url, key)
      },
    )
  }
}

@Composable
internal fun SupabaseConfigDialog(
  initialUrl: String,
  initialKey: String,
  onDismiss: () -> Unit,
  onSave: (url: String, key: String) -> Unit,
) {
  var url by rememberSaveable { mutableStateOf(initialUrl) }
  var key by rememberSaveable { mutableStateOf(initialKey) }
  var errorText by rememberSaveable { mutableStateOf<String?>(null) }
  var isSaving by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Supabase Felhő Adatbázis") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Add meg a Supabase felhő elérési adataidat. Minden ajándék, családtag, esemény és kívánságlista azonnal mentődik a felhőbe, és újratelepítéskor vagy másik eszközön automatikusan helyreáll.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = url,
          onValueChange = { url = it; errorText = null },
          label = { Text("Project URL (https://xyz.supabase.co)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = key,
          onValueChange = { key = it; errorText = null },
          label = { Text("Anon Key") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        errorText?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (url.isBlank() || !url.startsWith("https://")) {
            errorText = "Érvényes https:// URL szükséges"
            return@Button
          }
          if (key.isBlank()) {
            errorText = "Add meg az Anon API kulcsot"
            return@Button
          }
          isSaving = true
          onSave(url.trim(), key.trim())
          onDismiss()
        },
        enabled = !isSaving,
      ) {
        Text("Mentés & Csatlakozás")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun PendingInvitationsSection(
  invitations: List<ListInvitation>,
  onAcceptInvitation: (String) -> Unit,
  onDeclineInvitation: (String) -> Unit = {},
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { onDeclineInvitation(invite.id) },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("profile_decline_invite_${invite.id}"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.Clear,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.profile_decline_invite),
                  maxLines = 1,
                )
              }
              FilledTonalButton(
                onClick = { onAcceptInvitation(invite.id) },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("profile_accept_invite_${invite.id}"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.Check,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.profile_accept_join),
                  maxLines = 1,
                )
              }
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
