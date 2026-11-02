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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import com.example.core.format.EventDateUtils
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
import com.example.core.preferences.AppLanguage
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

  val updateState by viewModel.updateState.collectAsStateWithLifecycle()
  val currentVersionName = remember(context) {
    runCatching {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    ProfileScreenContent(
      state = uiState,
      updateState = updateState,
      currentVersionName = currentVersionName,
      onCheckForUpdate = { viewModel.checkForUpdate(currentVersionName) },
      onDownloadAndInstallUpdate = { info -> viewModel.downloadAndInstallUpdate(context, info) },
      onRetry = viewModel::retry,
      onOpenFamilyMember = onOpenFamilyMember,
      onAddFamilyMember = viewModel::addFamilyMember,
      onUpdateFamilyMember = viewModel::updateFamilyMember,
      onDeleteFamilyMember = viewModel::deleteFamilyMember,
      onSignInGoogle = viewModel::signInWithGoogle,
      onSignOutGoogle = viewModel::signOutCloud,
      onSyncNow = viewModel::syncNow,
      onConfigureCloud = { url, key -> viewModel.saveCustomCloudConfig(url, key) { _, _ -> } },
      onAcceptInvitation = { inviteId ->
        viewModel.acceptInvitation(inviteId, onOpenSharedList)
      },
      onDeclineInvitation = viewModel::declineInvitation,
      onThemeModeSelected = viewModel::setThemeMode,
      onLanguageSelected = viewModel::setAppLanguage,
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
  updateState: ProfileUpdateState = ProfileUpdateState.Idle,
  currentVersionName: String = "1.0",
  onCheckForUpdate: () -> Unit = {},
  onDownloadAndInstallUpdate: (com.example.domain.model.AppUpdateInfo) -> Unit = {},
  onRetry: () -> Unit = {},
  onOpenFamilyMember: (String) -> Unit = {},
  onThemeModeSelected: (ThemeMode) -> Unit = {},
  onLanguageSelected: (AppLanguage) -> Unit = {},
  onDynamicColorChanged: (Boolean) -> Unit = {},
  onPriceAlertsChanged: (Boolean) -> Unit = {},
  onEventRemindersChanged: (Boolean) -> Unit = {},
  onAiAutoCatChanged: (Boolean) -> Unit = {},
  onAiRecipientChanged: (Boolean) -> Unit = {},
  onAiConfidenceChanged: (Float) -> Unit = {},
  onUpdateProfile: (String, String, String?) -> Unit = { _, _, _ -> },
  onExportClick: () -> Unit = {},
  onImportClick: () -> Unit = {},
  onResetCatalog: () -> Unit = {},
  onAddFamilyMember: (name: String, relationship: String, eventName: String, days: Int, budget: Int, interests: List<String>, avatarUrl: String?, events: List<com.example.domain.model.FamilyMemberEvent>) -> Unit = { _, _, _, _, _, _, _, _ -> },
  onUpdateFamilyMember: (id: String, name: String, relationship: String, eventName: String, days: Int, budget: Int, interests: List<String>, avatarUrl: String?, events: List<com.example.domain.model.FamilyMemberEvent>?) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
  onDeleteFamilyMember: (String) -> Unit = {},
  onSignInGoogle: (displayName: String, email: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
  onSignOutGoogle: () -> Unit = {},
  onSyncNow: () -> Unit = {},
  onConfigureCloud: (String, String) -> Unit = { _, _ -> },
  onAcceptInvitation: (String) -> Unit = {},
  onDeclineInvitation: (String) -> Unit = {},
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
      val context = LocalContext.current
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

          item(key = "unified_user_card", contentType = "user_card") {
            UnifiedProfileGoogleSyncCard(
              preferences = prefs,
              realtimeStatus = state.realtimeStatus,
              onSignInGoogle = onSignInGoogle,
              onSignOut = onSignOutGoogle,
              onSyncNow = onSyncNow,
              onEditProfile = { showEditDialog = true },
              onConfigureCloud = onConfigureCloud,
            )
          }

          if (state.pendingInvitations.isNotEmpty()) {
            item(key = "pending_invitations", contentType = "invitations") {
              PendingInvitationsSection(
                invitations = state.pendingInvitations,
                onAcceptInvitation = onAcceptInvitation,
                onDeclineInvitation = onDeclineInvitation,
              )
            }
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

          item(key = "language_settings", contentType = "settings_card") {
            PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_language_section),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.semantics { heading() },
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                  AppLanguage.entries.forEachIndexed { idx, lang ->
                    SegmentedButton(
                      selected = prefs.appLanguage == lang,
                      onClick = { onLanguageSelected(lang) },
                      shape = SegmentedButtonDefaults.itemShape(idx, AppLanguage.entries.size),
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("profile_lang_${lang.name.lowercase()}"),
                    ) {
                      Text(lang.displayName)
                    }
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
                AppUpdateComponent(
                  currentVersion = currentVersionName,
                  updateState = updateState,
                  onCheckForUpdate = onCheckForUpdate,
                  onDownloadAndInstall = onDownloadAndInstallUpdate,
                )
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
            runCatching {
              val avatarDir = java.io.File(context.filesDir, "avatars").apply { mkdirs() }
              val avatarFile = java.io.File(avatarDir, "user_avatar_${System.currentTimeMillis()}.jpg")
              context.contentResolver.openInputStream(uri)?.use { input ->
                avatarFile.outputStream().use { output ->
                  input.copyTo(output)
                }
              }
              editAvatarUrl = android.net.Uri.fromFile(avatarFile).toString()
            }.onFailure {
              editAvatarUrl = uri.toString()
            }
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
        FamilyMemberProfileDialog(
          member = null,
          isNewMember = true,
          onDismiss = { showAddMemberDialog = false },
          onSave = { name, relationship, eventName, daysUntilEvent, budgetHuf, interests, avatarUrl, events ->
            onAddFamilyMember(
              name,
              relationship,
              eventName,
              daysUntilEvent,
              budgetHuf,
              interests,
              avatarUrl,
              events,
            )
            showAddMemberDialog = false
          },
        )
      }

      val currentEditingMember = editingMember
      if (currentEditingMember != null) {
        FamilyMemberProfileDialog(
          member = currentEditingMember,
          isNewMember = false,
          onDismiss = { editingMember = null },
          onSave = { name, relationship, eventName, daysUntilEvent, budgetHuf, interests, avatarUrl, events ->
            onUpdateFamilyMember(
              currentEditingMember.id,
              name,
              relationship,
              eventName,
              daysUntilEvent,
              budgetHuf,
              interests,
              avatarUrl,
              events,
            )
            editingMember = null
          },
          onDelete = {
            onDeleteFamilyMember(currentEditingMember.id)
            editingMember = null
          },
        )
      }
    }
  }
}

@Composable
private fun AppUpdateComponent(
  currentVersion: String,
  updateState: ProfileUpdateState,
  onCheckForUpdate: () -> Unit,
  onDownloadAndInstall: (com.example.domain.model.AppUpdateInfo) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = stringResource(R.string.profile_update_card_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.profile_current_version, currentVersion),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          when (updateState) {
            ProfileUpdateState.Checking -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                  text = stringResource(R.string.profile_update_checking),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            is ProfileUpdateState.UpToDate -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = stringResource(R.string.profile_update_up_to_date),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
              }
            }
            else -> {}
          }
        }

        when (updateState) {
          is ProfileUpdateState.UpdateAvailable -> {
            Surface(
              shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = stringResource(R.string.profile_update_available, updateState.info.latestVersion),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                val notes = updateState.info.releaseNotes?.trim()?.ifBlank { null }
                  ?: stringResource(R.string.profile_update_default_notes)
                Text(
                  text = notes,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                  onClick = { onDownloadAndInstall(updateState.info) },
                  modifier = Modifier.fillMaxWidth(),
                  shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                ) {
                  Icon(WishDropIcons.CloudSynced, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.profile_update_install_button))
                }
              }
            }
          }
          is ProfileUpdateState.Downloading -> {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              LinearProgressIndicator(
                progress = { updateState.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
              )
              Text(
                text = stringResource(R.string.profile_update_downloading, updateState.percent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          is ProfileUpdateState.Error -> {
            Text(
              text = updateState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
              onClick = onCheckForUpdate,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(WishDropIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.profile_update_check_button))
            }
          }
          else -> {
            OutlinedButton(
              onClick = onCheckForUpdate,
              modifier = Modifier.fillMaxWidth(),
              enabled = updateState !is ProfileUpdateState.Checking,
            ) {
              Icon(WishDropIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.profile_update_check_button))
            }
          }
        }
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
