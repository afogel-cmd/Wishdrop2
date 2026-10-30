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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
  onRetry: () -> Unit,
  onOpenFamilyMember: (String) -> Unit,
  onThemeModeSelected: (ThemeMode) -> Unit,
  onLanguageSelected: (AppLanguage) -> Unit = {},
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                PersonAvatarBadge(
                  name = prefs.displayName,
                  accentKey = "indigo",
                  imageUrl = prefs.avatarUrl,
                  size = 52.dp,
                )
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = prefs.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = prefs.email,
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
                    onClick = { showEditDialog = true },
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
        FamilyMemberDialog(
          dialogTitle = "Új családtag hozzáadása",
          confirmButtonText = "Hozzáadás",
          initialName = "",
          initialRelationship = "Partner",
          initialEventName = "Születésnap",
          initialDaysUntilEvent = 30,
          initialBudgetHuf = 25000,
          initialInterests = listOf("Könyvek", "Kávé"),
          initialAvatarUrl = null,
          accentKey = "coral",
          onDismiss = { showAddMemberDialog = false },
          onConfirm = { name, relationship, eventName, daysUntilEvent, budgetHuf, interests, avatarUrl ->
            onAddFamilyMember(
              name,
              relationship,
              eventName,
              daysUntilEvent,
              budgetHuf,
              interests,
              avatarUrl,
            )
            showAddMemberDialog = false
          },
        )
      }

      val currentEditingMember = editingMember
      if (currentEditingMember != null) {
        FamilyMemberDialog(
          dialogTitle = "Családtag szerkesztése",
          confirmButtonText = "Mentés",
          initialName = currentEditingMember.name,
          initialRelationship = currentEditingMember.relationship,
          initialEventName = currentEditingMember.upcomingEventName,
          initialDaysUntilEvent = currentEditingMember.daysUntilEvent,
          initialBudgetHuf = currentEditingMember.budgetHuf,
          initialInterests = currentEditingMember.interests,
          initialAvatarUrl = currentEditingMember.avatarUrl,
          accentKey = currentEditingMember.accentKey,
          onDismiss = { editingMember = null },
          onConfirm = { name, relationship, eventName, daysUntilEvent, budgetHuf, interests, avatarUrl ->
            onUpdateFamilyMember(
              currentEditingMember.id,
              name,
              relationship,
              eventName,
              daysUntilEvent,
              interests,
              avatarUrl,
            )
            editingMember = null
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FamilyMemberDialog(
  dialogTitle: String,
  confirmButtonText: String,
  initialName: String = "",
  initialRelationship: String = "Partner",
  initialEventName: String = "Születésnap",
  initialDaysUntilEvent: Int = 30,
  initialBudgetHuf: Int = 25_000,
  initialInterests: List<String> = emptyList(),
  initialAvatarUrl: String? = null,
  accentKey: String = "coral",
  onDismiss: () -> Unit,
  onConfirm: (
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    budgetHuf: Int,
    interests: List<String>,
    avatarUrl: String?,
  ) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(initialName) }
  var relationship by rememberSaveable { mutableStateOf(initialRelationship) }
  var avatarUrl by rememberSaveable { mutableStateOf(initialAvatarUrl) }
  var budgetText by rememberSaveable { mutableStateOf(initialBudgetHuf.toString()) }

  val eventTypes = listOf("Születésnap", "Évforduló", "Karácsony", "Névnap", "Egyéb")
  var selectedEventType by rememberSaveable {
    mutableStateOf(
      when {
        initialEventName.contains("szüli", ignoreCase = true) || initialEventName.contains("születés", ignoreCase = true) || initialEventName.contains("birthday", ignoreCase = true) -> "Születésnap"
        initialEventName.contains("évforduló", ignoreCase = true) || initialEventName.contains("anniversary", ignoreCase = true) -> "Évforduló"
        initialEventName.contains("karácsony", ignoreCase = true) || initialEventName.contains("christmas", ignoreCase = true) -> "Karácsony"
        initialEventName.contains("névnap", ignoreCase = true) || initialEventName.contains("nameday", ignoreCase = true) -> "Névnap"
        else -> if (initialEventName.isNotBlank() && initialEventName != "Születésnap") "Egyéb" else "Születésnap"
      }
    )
  }
  var customEventName by rememberSaveable {
    mutableStateOf(if (selectedEventType == "Egyéb") initialEventName else "")
  }

  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var selectedDateMillis by rememberSaveable {
    mutableStateOf(System.currentTimeMillis() + initialDaysUntilEvent.toLong() * 86_400_000L)
  }
  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

  val calculatedDays = EventDateUtils.calculateDaysUntilRecurringEvent(selectedDateMillis)
  val formattedDateStr = EventDateUtils.formatEventDate(selectedDateMillis)
  val daysRemainingStr = EventDateUtils.formatDaysRemaining(calculatedDays)

  var selectedInterests by rememberSaveable {
    mutableStateOf(initialInterests.ifEmpty { listOf("Könyvek", "Kávé") })
  }
  var customInterestInput by rememberSaveable { mutableStateOf("") }

  val photoPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      avatarUrl = uri.toString()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = dialogTitle,
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // 1. Avatar and photo upload
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          PersonAvatarBadge(
            name = name.ifBlank { "Új" },
            accentKey = accentKey,
            imageUrl = avatarUrl,
            size = 54.dp,
          )
          OutlinedButton(
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          ) {
            Icon(
              imageVector = WishDropIcons.Camera,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Fotó módosítása")
          }
        }

        // 2. Name
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Név *") },
          placeholder = { Text("pl. Dalma, Attila, Ármin") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // 3. Relationship chips & custom
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Kapcsolat", style = MaterialTheme.typography.labelMedium)
          val commonRelations = listOf("Partner", "Gyerek", "Szülő", "Testvér", "Barát", "Családtag")
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            commonRelations.forEach { rel ->
              FilterChip(
                selected = relationship.equals(rel, ignoreCase = true),
                onClick = { relationship = rel },
                label = { Text(rel) },
              )
            }
          }
          if (commonRelations.none { it.equals(relationship, ignoreCase = true) }) {
            OutlinedTextField(
              value = relationship,
              onValueChange = { relationship = it },
              label = { Text("Egyedi kapcsolat") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // 4. Upcoming event selection (Születésnap / Évforduló / stb.)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Következő esemény / alkalom", style = MaterialTheme.typography.labelMedium)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            eventTypes.forEach { type ->
              FilterChip(
                selected = selectedEventType == type,
                onClick = {
                  selectedEventType = type
                  if (type == "Karácsony") {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 25)
                    selectedDateMillis = cal.timeInMillis
                  }
                },
                label = {
                  val icon = when (type) {
                    "Születésnap" -> WishDropIcons.Cake
                    "Évforduló" -> WishDropIcons.Favorite
                    "Karácsony" -> WishDropIcons.Holiday
                    "Névnap" -> WishDropIcons.Botanical
                    else -> WishDropIcons.Sparkle
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(type)
                  }
                },
              )
            }
          }

          if (selectedEventType == "Egyéb") {
            OutlinedTextField(
              value = customEventName,
              onValueChange = { customEventName = it },
              label = { Text("Esemény megnevezése (pl. Diplomaosztó, Házavató)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // 5. Date Picker (replaces "Napok az eseményig")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Esemény dátuma", style = MaterialTheme.typography.labelMedium)
          OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Icon(
              imageVector = when (selectedEventType) {
                "Születésnap" -> WishDropIcons.Cake
                "Évforduló" -> WishDropIcons.Favorite
                "Karácsony" -> WishDropIcons.Holiday
                else -> WishDropIcons.History
              },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = formattedDateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = daysRemainingStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = "Módosítás",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        // 6. Interests (Érdeklődési körök)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Érdeklődési körök (AI ajánlások alapja)", style = MaterialTheme.typography.labelMedium)

          if (selectedInterests.isNotEmpty()) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              selectedInterests.forEach { interest ->
                InputChip(
                  selected = true,
                  onClick = { selectedInterests = selectedInterests - interest },
                  label = { Text(interest) },
                  trailingIcon = {
                    Icon(
                      imageVector = WishDropIcons.Clear,
                      contentDescription = "Törlés",
                      modifier = Modifier.size(14.dp),
                    )
                  },
                )
              }
            }
          }

          val availableSuggestions = EventDateUtils.POPULAR_GIFT_INTERESTS.filter { interest ->
            selectedInterests.none { it.equals(interest, ignoreCase = true) }
          }
          if (availableSuggestions.isNotEmpty()) {
            Text(
              text = "Gyors hozzáadás:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              availableSuggestions.take(6).forEach { suggestion ->
                SuggestionChip(
                  onClick = { selectedInterests = selectedInterests + suggestion },
                  label = { Text("+ $suggestion") },
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = customInterestInput,
              onValueChange = { customInterestInput = it },
              placeholder = { Text("Egyedi érdeklődés (pl. Jóga)...") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
              onClick = {
                val clean = customInterestInput.trim()
                if (clean.isNotBlank() && selectedInterests.none { it.equals(clean, ignoreCase = true) }) {
                  selectedInterests = selectedInterests + clean
                  customInterestInput = ""
                }
              },
              enabled = customInterestInput.isNotBlank(),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Icon(WishDropIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hozzáad")
            }
          }
        }

        // 7. Budget
        OutlinedTextField(
          value = budgetText,
          onValueChange = { budgetText = it },
          label = { Text("Ajándék keret (Ft)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank()) {
            val resolvedEventName = when (selectedEventType) {
              "Születésnap" -> "${name.trim()} születésnapja"
              "Évforduló" -> "${name.trim()} évfordulója"
              "Karácsony" -> "Karácsony"
              "Névnap" -> "${name.trim()} névnapja"
              else -> customEventName.trim().ifBlank { "${name.trim()} ünnepe" }
            }
            onConfirm(
              name.trim(),
              relationship.trim().ifBlank { "Családtag" },
              resolvedEventName,
              calculatedDays,
              budgetText.toIntOrNull() ?: 25_000,
              selectedInterests,
              avatarUrl,
            )
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(confirmButtonText)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let {
              selectedDateMillis = it
            }
            showDatePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      }
    ) {
      DatePicker(state = datePickerState)
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
