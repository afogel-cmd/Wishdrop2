package com.example.feature.profile

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserPreferences
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishDropListShimmer
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import kotlin.math.roundToInt

@Composable
fun ProfileRouteScreen(
  onOpenFamilyMember: (String) -> Unit,
  onOpenSharedList: (String) -> Unit = {},
  viewModel: ProfileViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingExportPayload by remember { mutableStateOf<String?>(null) }

  val createDocumentLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      val payload = pendingExportPayload
      if (uri != null && payload != null) {
        runCatching {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(payload.toByteArray(Charsets.UTF_8))
          }
        }
      }
      pendingExportPayload = null
    }

  val openDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val text =
          runCatching {
              context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            .getOrNull()
        if (!text.isNullOrBlank()) {
          viewModel.importBackupJson(text)
        }
      }
    }

  val statusMessage = (uiState as? ProfileUiState.Success)?.statusMessage
  LaunchedEffect(statusMessage) {
    statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeMessage()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    ProfileScreenContent(
      state = uiState,
      onRetry = viewModel::retry,
      onOpenFamilyMember = onOpenFamilyMember,
      onAddFamilyMember = viewModel::addFamilyMember,
      onUpdateFamilyMember = viewModel::updateFamilyMember,
      onDeleteFamilyMember = viewModel::deleteFamilyMember,
      onSignInCloud = viewModel::signInCloud,
      onSignInGoogle = viewModel::signInWithGoogle,
      onSignUpCloud = viewModel::signUpCloud,
      onSignOutCloud = viewModel::signOutCloud,
      onAcceptInvitation = { inviteId ->
        viewModel.acceptInvitation(inviteId, onOpenSharedList)
      },
      onThemeModeSelected = viewModel::setThemeMode,
      onDynamicColorChanged = viewModel::setDynamicColor,
      onPriceAlertsChanged = viewModel::setPriceAlerts,
      onEventRemindersChanged = viewModel::setEventReminders,
      onAiAutoCatChanged = viewModel::setAiAutoCategorization,
      onAiRecipientChanged = viewModel::setAiRecipientMatching,
      onAiConfidenceChanged = viewModel::setAiConfidenceThreshold,
      onUpdateProfile = viewModel::updateProfile,
      onExportClick = {
        viewModel.exportBackupJson { json ->
          pendingExportPayload = json
          createDocumentLauncher.launch("wishdrop_backup.json")
        }
      },
      onImportClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
      onResetCatalog = viewModel::resetCatalog,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
fun ProfileScreenContent(
  state: ProfileUiState,
  onRetry: () -> Unit,
  onOpenFamilyMember: (String) -> Unit,
  onThemeModeSelected: (ThemeMode) -> Unit,
  onDynamicColorChanged: (Boolean) -> Unit,
  onPriceAlertsChanged: (Boolean) -> Unit,
  onEventRemindersChanged: (Boolean) -> Unit,
  onAiAutoCatChanged: (Boolean) -> Unit,
  onAiRecipientChanged: (Boolean) -> Unit,
  onAiConfidenceChanged: (Float) -> Unit,
  onUpdateProfile: (String, String, String?) -> Unit,
  onExportClick: () -> Unit,
  onImportClick: () -> Unit,
  onResetCatalog: () -> Unit,
  onAddFamilyMember: (name: String, relationship: String, eventName: String, days: Int, budget: Int, interests: List<String>, avatarUrl: String?) -> Unit = { _, _, _, _, _, _, _ -> },
  onUpdateFamilyMember: (id: String, name: String, relationship: String, eventName: String, days: Int, interests: List<String>, avatarUrl: String?) -> Unit = { _, _, _, _, _, _, _ -> },
  onDeleteFamilyMember: (String) -> Unit = {},
  onSignInCloud: (String, String) -> Unit = { _, _ -> },
  onSignInGoogle: (displayName: String, email: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
  onSignUpCloud: (String, String, String) -> Unit = { _, _, _ -> },
  onSignOutCloud: () -> Unit = {},
  onAcceptInvitation: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing

  when (state) {
    ProfileUiState.Loading -> {
      WishDropListShimmer(cardCount = 4, modifier = modifier)
    }

    is ProfileUiState.Error -> {
      Box(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        WishDropErrorState(
          message = state.message,
          onRetry = onRetry,
          modifier = Modifier.widthIn(max = spacing.maxSheetWidth),
        )
      }
    }

    is ProfileUiState.Success -> {
      var showEditDialog by rememberSaveable { mutableStateOf(false) }
      var showAddMemberDialog by rememberSaveable { mutableStateOf(false) }
      var editingMember by remember { mutableStateOf<GiftRecipient?>(null) }
      val prefs = state.preferences

      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = spacing.maxContentWidth)
              .testTag("profile_list"),
          contentPadding =
            PaddingValues(
              start = spacing.lg,
              end = spacing.lg,
              top = spacing.md,
              bottom = spacing.xxxl,
            ),
          verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
          item(key = "header", contentType = "header") {
            Text(
              text = stringResource(R.string.profile_title),
              style = MaterialTheme.typography.headlineLarge,
              modifier = Modifier.semantics { heading() },
            )
          }

          item(key = "user_card", contentType = "user_card") {
            PixelBentoCard(
              modifier = Modifier.fillMaxWidth().testTag("profile_user_card"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                PersonAvatarBadge(
                  name = prefs.displayName,
                  accentKey = "indigo",
                  imageUrl = prefs.avatarUrl,
                  size = 60.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = prefs.displayName, style = MaterialTheme.typography.titleLarge)
                  Text(
                    text = prefs.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                FilledTonalButton(
                  onClick = { showEditDialog = true },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("profile_edit_button"),
                ) {
                  Text(stringResource(R.string.profile_edit))
                }
              }
            }
          }

          if (state.pendingInvitations.isNotEmpty()) {
            item(key = "pending_invitations", contentType = "invitations") {
              PendingInvitationsSection(
                invitations = state.pendingInvitations,
                onAcceptInvitation = onAcceptInvitation,
              )
            }
          }

          item(key = "cloud_sync_card", contentType = "cloud_sync") {
            SupabaseCloudSyncCard(
              preferences = prefs,
              realtimeStatus = state.realtimeStatus,
              onSignIn = onSignInCloud,
              onSignInGoogle = onSignInGoogle,
              onSignUp = onSignUpCloud,
              onSignOut = onSignOutCloud,
            )
          }

          item(key = "family_members", contentType = "family_members") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ExpressiveSectionHeader(
                  title = stringResource(R.string.profile_family_members),
                  subtitle = stringResource(R.string.profile_family_sub),
                  modifier = Modifier.weight(1f, fill = false),
                )
                FilledTonalButton(
                  onClick = { showAddMemberDialog = true },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("profile_add_family_member_button"),
                ) {
                  Icon(imageVector = WishDropIcons.Add, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Új családtag")
                }
              }
              if (state.familyMembers.isEmpty()) {
                WishDropEmptyState(
                  icon = WishDropIcons.GiftFilled,
                  title = stringResource(R.string.profile_family_empty_title),
                  description = stringResource(R.string.profile_family_empty_desc),
                )
              } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  items(
                    items = state.familyMembers,
                    key = { it.id },
                    contentType = { "family_chip" },
                  ) { member ->
                    FamilyMemberChipCard(
                      member = member,
                      onClick = { onOpenFamilyMember(member.id) },
                      onEdit = { editingMember = member },
                      onDelete = { onDeleteFamilyMember(member.id) },
                    )
                  }
                }
              }
            }
          }

          item(key = "theme_settings", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_theme_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                  ThemeMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                      selected = prefs.themeMode == mode,
                      onClick = { onThemeModeSelected(mode) },
                      shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("profile_theme_${mode.name.lowercase()}"),
                    ) {
                      Text(mode.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                    }
                  }
                }
                SettingSwitchRow(
                  title = stringResource(R.string.profile_dynamic_color),
                  subtitle = stringResource(R.string.profile_dynamic_color_sub),
                  checked = prefs.useDynamicColor,
                  onCheckedChange = onDynamicColorChanged,
                  testTag = "profile_dynamic_color_switch",
                )
              }
            }
          }

          item(key = "notification_settings", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_notifications_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                SettingSwitchRow(
                  title = stringResource(R.string.profile_price_alerts),
                  checked = prefs.priceDropNotifications,
                  onCheckedChange = onPriceAlertsChanged,
                  testTag = "profile_price_alerts_switch",
                )
                SettingSwitchRow(
                  title = stringResource(R.string.profile_event_reminders),
                  checked = prefs.eventReminders,
                  onCheckedChange = onEventRemindersChanged,
                  testTag = "profile_event_reminders_switch",
                )
              }
            }
          }

          item(key = "ai_settings", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_ai_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                SettingSwitchRow(
                  title = stringResource(R.string.profile_ai_auto_cat),
                  checked = prefs.aiAutoCategorization,
                  onCheckedChange = onAiAutoCatChanged,
                  testTag = "profile_ai_cat_switch",
                )
                SettingSwitchRow(
                  title = stringResource(R.string.profile_ai_recipient),
                  checked = prefs.aiRecipientMatching,
                  onCheckedChange = onAiRecipientChanged,
                  testTag = "profile_ai_recipient_switch",
                )
                Column {
                  Text(
                    text =
                      stringResource(
                        R.string.profile_ai_confidence,
                        (prefs.aiConfidenceThreshold * 100).roundToInt(),
                      ),
                    style = MaterialTheme.typography.bodyMedium,
                  )
                  Slider(
                    value = prefs.aiConfidenceThreshold,
                    onValueChange = onAiConfidenceChanged,
                    valueRange = 0.50f..0.98f,
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("profile_ai_confidence_slider"),
                  )
                }
              }
            }
          }

          item(key = "data_portability", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_data_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  FilledTonalButton(
                    onClick = onExportClick,
                    modifier =
                      Modifier.weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("profile_export_button"),
                  ) {
                    Icon(imageVector = WishDropIcons.Export, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.profile_export_data))
                  }
                  OutlinedButton(
                    onClick = onImportClick,
                    modifier =
                      Modifier.weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("profile_import_button"),
                  ) {
                    Icon(imageVector = WishDropIcons.Import, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.profile_import_data))
                  }
                }
              }
            }
          }

          item(key = "account_card", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_account_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = WishDropIcons.VaultVerified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = stringResource(R.string.profile_vault_status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = onResetCatalog,
                  modifier =
                    Modifier.fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .testTag("profile_reset_catalog_button"),
                ) {
                  Icon(imageVector = WishDropIcons.Restore, contentDescription = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(stringResource(R.string.profile_reset_catalog))
                }
              }
            }
          }
        }
      }

      if (showEditDialog) {
        var editName by rememberSaveable { mutableStateOf(prefs.displayName) }
        var editEmail by rememberSaveable { mutableStateOf(prefs.email) }
        var editAvatarUrl by rememberSaveable { mutableStateOf(prefs.avatarUrl) }

        val profilePhotoPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.PickVisualMedia()
        ) { uri ->
          if (uri != null) {
            editAvatarUrl = uri.toString()
          }
        }

        AlertDialog(
          onDismissRequest = { showEditDialog = false },
          title = { Text(stringResource(R.string.dialog_edit_profile)) },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                PersonAvatarBadge(
                  name = editName.ifBlank { "Profil" },
                  accentKey = "indigo",
                  imageUrl = editAvatarUrl,
                  size = 52.dp,
                )
                OutlinedButton(
                  onClick = {
                    profilePhotoPicker.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                ) {
                  Text("Profilfotó választása")
                }
              }
              OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text(stringResource(R.string.dialog_display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = editEmail,
                onValueChange = { editEmail = it },
                label = { Text(stringResource(R.string.dialog_email)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          },
          confirmButton = {
            TextButton(
              onClick = {
                onUpdateProfile(editName, editEmail, editAvatarUrl)
                showEditDialog = false
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(stringResource(R.string.action_save))
            }
          },
          dismissButton = {
            TextButton(
              onClick = { showEditDialog = false },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(stringResource(R.string.action_cancel))
            }
          },
        )
      }

      if (showAddMemberDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var relationship by rememberSaveable { mutableStateOf("Partner") }
        var eventName by rememberSaveable { mutableStateOf("Születésnap") }
        var daysText by rememberSaveable { mutableStateOf("30") }
        var budgetText by rememberSaveable { mutableStateOf("25000") }
        var interestsText by rememberSaveable { mutableStateOf("") }
        var avatarUrl by rememberSaveable { mutableStateOf<String?>(null) }

        val memberPhotoPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.PickVisualMedia()
        ) { uri ->
          if (uri != null) {
            avatarUrl = uri.toString()
          }
        }

        AlertDialog(
          onDismissRequest = { showAddMemberDialog = false },
          title = { Text("Új családtag felvitele") },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                PersonAvatarBadge(
                  name = name.ifBlank { "Új" },
                  accentKey = "coral",
                  imageUrl = avatarUrl,
                  size = 48.dp,
                )
                OutlinedButton(
                  onClick = {
                    memberPhotoPicker.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                ) {
                  Text("Fotó feltöltése")
                }
              }
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Név") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Kapcsolat (pl. Partner, Gyerek, Szülő)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Következő esemény neve") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it },
                label = { Text("Napok az eseményig") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = { Text("Ajándék keret (Ft)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = interestsText,
                onValueChange = { interestsText = it },
                label = { Text("Érdeklődési körök (vesszővel elválasztva)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          },
          confirmButton = {
            TextButton(
              onClick = {
                if (name.isNotBlank()) {
                  val interests = interestsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                  onAddFamilyMember(
                    name.trim(),
                    relationship.trim().ifBlank { "Családtag" },
                    eventName.trim().ifBlank { "Születésnap" },
                    daysText.toIntOrNull() ?: 30,
                    budgetText.toIntOrNull() ?: 20000,
                    interests,
                    avatarUrl,
                  )
                  showAddMemberDialog = false
                }
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("Hozzáadás")
            }
          },
          dismissButton = {
            TextButton(
              onClick = { showAddMemberDialog = false },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(stringResource(R.string.action_cancel))
            }
          },
        )
      }

      val currentEditingMember = editingMember
      if (currentEditingMember != null) {
        var name by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.name) }
        var relationship by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.relationship) }
        var eventName by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.upcomingEventName) }
        var daysText by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.daysUntilEvent.toString()) }
        var interestsText by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.interests.joinToString(", ")) }
        var avatarUrl by rememberSaveable(currentEditingMember.id) { mutableStateOf(currentEditingMember.avatarUrl) }

        val editMemberPhotoPicker = rememberLauncherForActivityResult(
          ActivityResultContracts.PickVisualMedia()
        ) { uri ->
          if (uri != null) {
            avatarUrl = uri.toString()
          }
        }

        AlertDialog(
          onDismissRequest = { editingMember = null },
          title = { Text("Családtag szerkesztése") },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                PersonAvatarBadge(
                  name = name.ifBlank { "Családtag" },
                  accentKey = currentEditingMember.accentKey,
                  imageUrl = avatarUrl,
                  size = 48.dp,
                )
                OutlinedButton(
                  onClick = {
                    editMemberPhotoPicker.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                ) {
                  Text("Fotó módosítása")
                }
              }
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Név") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Kapcsolat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Következő esemény") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it },
                label = { Text("Napok az eseményig") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = interestsText,
                onValueChange = { interestsText = it },
                label = { Text("Érdeklődési körök") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          },
          confirmButton = {
            TextButton(
              onClick = {
                if (name.isNotBlank()) {
                  val interests = interestsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                  onUpdateFamilyMember(
                    currentEditingMember.id,
                    name.trim(),
                    relationship.trim(),
                    eventName.trim(),
                    daysText.toIntOrNull() ?: 30,
                    interests,
                    avatarUrl,
                  )
                  editingMember = null
                }
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("Mentés")
            }
          },
          dismissButton = {
            TextButton(
              onClick = { editingMember = null },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(stringResource(R.string.action_cancel))
            }
          },
        )
      }
    }
  }
}

private val PreviewProfileState =
  ProfileUiState.Success(
    preferences = UserPreferences(),
    personas = emptyList(),
    familyMembers =
      listOf(
        GiftRecipient(
          id = "rec_dalma",
          name = "Dalma",
          relationship = "Partner",
          avatarKey = "D",
          upcomingEventName = "Dalma Birthday",
          daysUntilEvent = 15,
          budgetHuf = 45_000,
          interests = listOf("Flowers", "Books", "Coffee"),
          giftStage = GiftStage.ORDERED,
          accentKey = "coral",
        ),
      ),
  )

@Preview(showBackground = true, name = "Profile — Light")
@Composable
private fun ProfileLightPreview() {
  WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
    ProfileScreenContent(
      state = PreviewProfileState,
      onRetry = {},
      onOpenFamilyMember = {},
      onThemeModeSelected = {},
      onDynamicColorChanged = {},
      onPriceAlertsChanged = {},
      onEventRemindersChanged = {},
      onAiAutoCatChanged = {},
      onAiRecipientChanged = {},
      onAiConfidenceChanged = {},
      onUpdateProfile = { _, _, _ -> },
      onExportClick = {},
      onImportClick = {},
      onResetCatalog = {},
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Profile — Dark",
)
@Composable
private fun ProfileDarkPreview() {
  WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
    ProfileScreenContent(
      state = PreviewProfileState,
      onRetry = {},
      onOpenFamilyMember = {},
      onThemeModeSelected = {},
      onDynamicColorChanged = {},
      onPriceAlertsChanged = {},
      onEventRemindersChanged = {},
      onAiAutoCatChanged = {},
      onAiRecipientChanged = {},
      onAiConfidenceChanged = {},
      onUpdateProfile = { _, _, _ -> },
      onExportClick = {},
      onImportClick = {},
      onResetCatalog = {},
    )
  }
}
