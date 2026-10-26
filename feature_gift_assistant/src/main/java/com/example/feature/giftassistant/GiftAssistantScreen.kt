package com.example.feature.giftassistant

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.ExpressiveChip
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishDropListShimmer
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.AiRecommendation
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import com.example.domain.model.Product

@Composable
fun GiftAssistantRouteScreen(
  initialRecipientId: String?,
  onOpenProductDetails: (String) -> Unit,
  viewModel: GiftAssistantViewModel = hiltViewModel(),
) {
  LaunchedEffect(initialRecipientId) {
    viewModel.selectInitialRecipientIfProvided(initialRecipientId)
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  val snackbarMessage = (uiState as? GiftAssistantUiState.Success)?.snackbarMessage
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    GiftAssistantScreen(
      uiState = uiState,
      onRetry = viewModel::retry,
      onSelectRecipient = viewModel::selectRecipient,
      onUpdateStage = viewModel::updateStage,
      onAddInterest = viewModel::addInterest,
      onRemoveInterest = viewModel::removeInterest,
      onOpenProductDetails = onOpenProductDetails,
      onSaveRecommendation = { rec ->
        if (rec.linkedProductId != null) {
          onOpenProductDetails(rec.linkedProductId!!)
        } else {
          viewModel.saveRecommendation(rec)
        }
      },
      onAddPerson = viewModel::addNewPerson,
      onUpdatePerson = { id, name, rel, event, days, interests ->
        viewModel.updatePerson(id, name, rel, event, days, interests)
      },
      onDeletePerson = viewModel::deletePerson,
      onDismissRecommendation = viewModel::dismissRecommendation,
      onGenerateAiRecommendations = viewModel::generateAiRecommendations,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
fun GiftAssistantScreen(
  uiState: GiftAssistantUiState,
  onRetry: () -> Unit,
  onSelectRecipient: (String) -> Unit,
  onUpdateStage: (GiftStage) -> Unit,
  onAddInterest: (String) -> Unit,
  onRemoveInterest: (String) -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onSaveRecommendation: (AiRecommendation) -> Unit,
  onAddPerson: (String, String, String, Int, String) -> Unit,
  onUpdatePerson: (String, String, String, String, Int, String) -> Unit = { _, _, _, _, _, _ -> },
  onDeletePerson: (String) -> Unit = {},
  onDismissRecommendation: (String) -> Unit = {},
  onGenerateAiRecommendations: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showAddPersonDialog by rememberSaveable { mutableStateOf(false) }
  val spacing = WishDropDesign.spacing

  when (uiState) {
    GiftAssistantUiState.Loading -> WishDropListShimmer(cardCount = 3, modifier = modifier)
    is GiftAssistantUiState.Error -> {
      Box(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        WishDropErrorState(
          message = uiState.message,
          onRetry = onRetry,
          modifier = Modifier.widthIn(max = 520.dp),
        )
      }
    }
    GiftAssistantUiState.Empty -> {
      Box(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        WishDropEmptyState(
          icon = WishDropIcons.GiftFilled,
          title = stringResource(R.string.gift_title),
          description = stringResource(R.string.gift_subtitle),
          actionLabel = stringResource(R.string.gift_add_person),
          onActionClick = { showAddPersonDialog = true },
          modifier = Modifier.widthIn(max = 520.dp),
        )
      }
    }
    is GiftAssistantUiState.Success -> {
      GiftAssistantSuccessContent(
        state = uiState,
        onSelectRecipient = onSelectRecipient,
        onUpdateStage = onUpdateStage,
        onAddInterest = onAddInterest,
        onRemoveInterest = onRemoveInterest,
        onOpenProductDetails = onOpenProductDetails,
        onSaveRecommendation = onSaveRecommendation,
        onOpenAddPersonDialog = { showAddPersonDialog = true },
        onUpdatePerson = onUpdatePerson,
        onDeletePerson = onDeletePerson,
        onDismissRecommendation = onDismissRecommendation,
        onGenerateAiRecommendations = onGenerateAiRecommendations,
        modifier = modifier,
      )
    }
  }

  if (showAddPersonDialog) {
    AddPersonDialog(
      onDismiss = { showAddPersonDialog = false },
      onConfirm = { name, relation, event, days, interests ->
        onAddPerson(name, relation, event, days, interests)
        showAddPersonDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GiftAssistantSuccessContent(
  state: GiftAssistantUiState.Success,
  onSelectRecipient: (String) -> Unit,
  onUpdateStage: (GiftStage) -> Unit,
  onAddInterest: (String) -> Unit,
  onRemoveInterest: (String) -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onSaveRecommendation: (AiRecommendation) -> Unit,
  onOpenAddPersonDialog: () -> Unit,
  onUpdatePerson: (String, String, String, String, Int, String) -> Unit,
  onDeletePerson: (String) -> Unit,
  onDismissRecommendation: (String) -> Unit,
  onGenerateAiRecommendations: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showAddInterestDialog by rememberSaveable { mutableStateOf(false) }
  var showEditPersonDialog by rememberSaveable { mutableStateOf(false) }
  var showDeletePersonDialog by rememberSaveable { mutableStateOf(false) }
  var showDeepAnalysisDialog by rememberSaveable { mutableStateOf(false) }
  val spacing = WishDropDesign.spacing

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = spacing.maxContentWidth)
          .testTag("gift_assistant_list"),
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.gift_title),
              style = MaterialTheme.typography.headlineLarge,
            )
            Text(
              text = stringResource(R.string.gift_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          FilledTonalIconButton(
            onClick = onOpenAddPersonDialog,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("gift_add_person_button"),
          ) {
            Icon(
              imageVector = WishDropIcons.PersonAdd,
              contentDescription = stringResource(R.string.gift_add_person),
            )
          }
        }
      }

      item(key = "people_carousel", contentType = "carousel") {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(spacing.md),
          modifier = Modifier.testTag("gift_people_carousel"),
        ) {
          items(
            items = state.recipients,
            key = { it.id },
            contentType = { "person_card" },
          ) { person ->
            PersonCarouselCard(
              recipient = person,
              selected = person.id == state.selectedRecipient.id,
              onClick = { onSelectRecipient(person.id) },
            )
          }
        }
      }

      item(key = "person_overview", contentType = "overview") {
        AnimatedContent(
          targetState = state.selectedRecipient,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "person_overview_transition",
        ) { recipient ->
          Column(verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = recipient.name,
                  style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                  text = "${recipient.relationship} • ${recipient.upcomingEventName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedIconButton(
                  onClick = { showEditPersonDialog = true },
                  modifier = Modifier.testTag("gift_edit_person_button"),
                ) {
                  Icon(
                    imageVector = WishDropIcons.Edit,
                    contentDescription = "Edit person",
                  )
                }
                OutlinedIconButton(
                  onClick = { showDeletePersonDialog = true },
                  modifier = Modifier.testTag("gift_delete_person_button"),
                ) {
                  Icon(
                    imageVector = WishDropIcons.Delete,
                    contentDescription = "Delete person",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }

            GiftProgressExpressiveStepper(
              currentStage = recipient.giftStage,
              onStageSelected = onUpdateStage,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
              ExpressiveSectionHeader(
                title = stringResource(R.string.gift_interests_title),
                subtitle = stringResource(R.string.gift_interests_subtitle),
                actionLabel = stringResource(R.string.gift_add_interest),
                onActionClick = { showAddInterestDialog = true },
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                recipient.interests.forEachIndexed { idx, interest ->
                  ExpressiveChip(
                    label = interest,
                    colorIndex = idx,
                    onClick = { onRemoveInterest(interest) },
                  )
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
              ExpressiveSectionHeader(
                title = stringResource(R.string.gift_saved_ideas_title),
                subtitle =
                  stringResource(
                    R.string.gift_event_days,
                    recipient.upcomingEventName,
                    recipient.daysUntilEvent,
                  ),
              )
              if (state.savedIdeas.isEmpty()) {
                WishDropEmptyState(
                  icon = WishDropIcons.BookmarkAdd,
                  title = stringResource(R.string.gift_saved_ideas_title),
                  description = stringResource(R.string.gift_saved_ideas_empty, recipient.name),
                )
              } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                  items(
                    items = state.savedIdeas,
                    key = { it.id },
                    contentType = { "saved_idea" },
                  ) { idea ->
                    SavedIdeaMiniCard(
                      product = idea,
                      onClick = { onOpenProductDetails(idea.id) },
                    )
                  }
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.gift_ai_recommendations_title),
                    style = MaterialTheme.typography.titleLarge,
                  )
                  Text(
                    text = stringResource(R.string.gift_ai_recommendations_sub, recipient.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { showDeepAnalysisDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Sparkle,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mélyebb Analízis")
                  }
                  FilledTonalButton(
                    onClick = onGenerateAiRecommendations,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Text("Újra-generálás")
                  }
                }
              }
              if (state.aiRecommendations.isEmpty()) {
                WishDropEmptyState(
                  icon = WishDropIcons.GiftFilled,
                  title = "Nincsenek aktív ötletek",
                  description = "Generálj új személyre szabott ajándékötleteket ${recipient.name} részére az érdeklődési körei alapján!",
                  actionLabel = "Új ötletek generálása AI-val",
                  onActionClick = onGenerateAiRecommendations,
                )
              } else {
                state.aiRecommendations.forEach { rec ->
                  AiGiftRecommendationCard(
                    recommendation = rec,
                    onSaveOrView = { onSaveRecommendation(rec) },
                    onDismiss = { onDismissRecommendation(rec.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditPersonDialog) {
    EditPersonDialog(
      recipient = state.selectedRecipient,
      onDismiss = { showEditPersonDialog = false },
      onConfirm = { name, relation, event, days, interests ->
        onUpdatePerson(state.selectedRecipient.id, name, relation, event, days, interests)
        showEditPersonDialog = false
      },
    )
  }

  if (showDeepAnalysisDialog) {
    DeepProfileAnalysisDialog(
      recipientName = state.selectedRecipient.name,
      currentInterests = state.selectedRecipient.interests,
      currentBudgetHuf = state.selectedRecipient.budgetHuf,
      onDismiss = { showDeepAnalysisDialog = false },
      onConfirm = { interestsCsv, budgetHuf, notes ->
        onUpdatePerson(
          state.selectedRecipient.id,
          state.selectedRecipient.name,
          state.selectedRecipient.relationship,
          state.selectedRecipient.upcomingEventName,
          state.selectedRecipient.daysUntilEvent,
          interestsCsv,
        )
        onGenerateAiRecommendations()
        showDeepAnalysisDialog = false
      },
    )
  }

  if (showDeletePersonDialog) {
    AlertDialog(
      onDismissRequest = { showDeletePersonDialog = false },
      title = { Text("Törlés megerősítése") },
      text = { Text("Biztosan törölni szeretnéd ${state.selectedRecipient.name} személyt?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeletePerson(state.selectedRecipient.id)
            showDeletePersonDialog = false
          }
        ) {
          Text("Törlés", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeletePersonDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showAddInterestDialog) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddInterestDialog = false },
      title = { Text(stringResource(R.string.dialog_add_interest_title)) },
      text = {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text(stringResource(R.string.dialog_interest_hint)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("gift_interest_input"),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onAddInterest(text)
            showAddInterestDialog = false
          }
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddInterestDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

private fun calculateDaysUntilRecurringEvent(targetDateMillis: Long): Int {
  val calendarToday = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }
  val calendarTarget = java.util.Calendar.getInstance().apply {
    timeInMillis = targetDateMillis
  }

  val todayYear = calendarToday.get(java.util.Calendar.YEAR)
  val targetMonth = calendarTarget.get(java.util.Calendar.MONTH)
  val targetDay = calendarTarget.get(java.util.Calendar.DAY_OF_MONTH)

  val nextOccurrence = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.YEAR, todayYear)
    set(java.util.Calendar.MONTH, targetMonth)
    set(java.util.Calendar.DAY_OF_MONTH, targetDay)
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }

  if (nextOccurrence.before(calendarToday)) {
    nextOccurrence.add(java.util.Calendar.YEAR, 1)
  }

  val diffMillis = nextOccurrence.timeInMillis - calendarToday.timeInMillis
  return (diffMillis / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPersonDialog(
  recipient: GiftRecipient,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(recipient.name) }
  var relation by rememberSaveable { mutableStateOf(recipient.relationship) }
  var event by rememberSaveable { mutableStateOf(recipient.upcomingEventName) }
  var interests by rememberSaveable { mutableStateOf(recipient.interests.joinToString(", ")) }

  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var selectedMillis by rememberSaveable {
    mutableStateOf(System.currentTimeMillis() + recipient.daysUntilEvent.toLong() * 86_400_000L)
  }

  val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = selectedMillis
  )

  val calculatedDays = calculateDaysUntilRecurringEvent(selectedMillis)
  val formatter = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault())
  val formattedDateStr = formatter.format(java.util.Date(selectedMillis))

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Személy / Születésnap szerkesztése") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.dialog_person_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("gift_edit_person_name"),
        )
        OutlinedTextField(
          value = relation,
          onValueChange = { relation = it },
          label = { Text(stringResource(R.string.dialog_person_relation)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = event,
          onValueChange = { event = it },
          label = { Text(stringResource(R.string.dialog_person_event)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = WishDropIcons.Cake,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Esemény dátuma: $formattedDateStr ($calculatedDays nap múlva)")
        }
        OutlinedTextField(
          value = interests,
          onValueChange = { interests = it },
          label = { Text(stringResource(R.string.dialog_person_interests)) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank()) {
            onConfirm(name, relation, event, calculatedDays, interests)
          }
        }
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let {
              selectedMillis = it
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var relation by rememberSaveable { mutableStateOf("") }
  var event by rememberSaveable { mutableStateOf("Birthday") }
  var interests by rememberSaveable { mutableStateOf("Flowers, Books, Coffee") }

  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var selectedMillis by rememberSaveable {
    mutableStateOf(System.currentTimeMillis() + 30L * 86_400_000L)
  }

  val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = selectedMillis
  )

  val calculatedDays = calculateDaysUntilRecurringEvent(selectedMillis)
  val formatter = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault())
  val formattedDateStr = formatter.format(java.util.Date(selectedMillis))

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.dialog_add_person_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.dialog_person_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("gift_new_person_name"),
        )
        OutlinedTextField(
          value = relation,
          onValueChange = { relation = it },
          label = { Text(stringResource(R.string.dialog_person_relation)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = event,
          onValueChange = { event = it },
          label = { Text(stringResource(R.string.dialog_person_event)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = WishDropIcons.Cake,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Esemény dátuma: $formattedDateStr ($calculatedDays nap múlva)")
        }
        OutlinedTextField(
          value = interests,
          onValueChange = { interests = it },
          label = { Text(stringResource(R.string.dialog_person_interests)) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank()) {
            onConfirm(name, relation, event, calculatedDays, interests)
          }
        }
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let {
              selectedMillis = it
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

@Preview(showBackground = true, name = "Gift Assistant Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Gift Assistant Dark")
@Composable
private fun GiftAssistantPreview() {
  val dalma =
    GiftRecipient(
      id = "rec_dalma",
      name = "Dalma",
      relationship = "Partner",
      avatarKey = "avatar_dalma",
      upcomingEventName = "Dalma Birthday",
      daysUntilEvent = 15,
      budgetHuf = 45_000,
      interests = listOf("Flowers", "Books", "Coffee", "Home Decor", "Technology"),
      giftStage = GiftStage.WATCHING,
      accentKey = "coral",
    )
  WishDropTheme(useDynamicColor = false) {
    GiftAssistantScreen(
      uiState =
        GiftAssistantUiState.Success(
          recipients = listOf(dalma),
          selectedRecipient = dalma,
          savedIdeas =
            listOf(
              Product(
                id = "prod_lego_orchid",
                title = "LEGO Orchid",
                store = "Alza",
                currentPriceHuf = 16_490,
                originalPriceHuf = 18_990,
                priceChangePercent = -13,
                savingsHuf = 2_500,
                imageKey = "lego_orchid",
                categories = listOf("Home", "Gift"),
                tags = listOf("Flowers"),
              )
            ),
          aiRecommendations =
            listOf(
              AiRecommendation(
                id = "ai_rec_lego_bonsai",
                recipientId = "rec_dalma",
                title = "LEGO Bonsai",
                store = "LEGO Store",
                priceHuf = 17_990,
                imageKey = "lego_bonsai",
                matchPercentage = 92,
                reason = "User likes flowers and home decoration.",
                categories = listOf("Home", "Gift"),
                tags = listOf("Flowers", "Home Decor"),
              )
            ),
        ),
      onRetry = {},
      onSelectRecipient = {},
      onUpdateStage = {},
      onAddInterest = {},
      onRemoveInterest = {},
      onOpenProductDetails = {},
      onSaveRecommendation = {},
      onAddPerson = { _, _, _, _, _ -> },
    )
  }
}
