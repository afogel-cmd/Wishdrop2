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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import com.example.core.format.EventDateUtils
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = stringResource(R.string.gift_ai_recommendations_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = stringResource(R.string.gift_ai_recommendations_sub, recipient.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = { showDeepAnalysisDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Sparkle,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Mélyebb Analízis",
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  FilledTonalButton(
                    onClick = onGenerateAiRecommendations,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Újra-generálás",
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PersonFormDialog(
  dialogTitle: String,
  initialName: String = "",
  initialRelation: String = "Partner",
  initialEventName: String = "Születésnap",
  initialDaysUntilEvent: Int = 30,
  initialInterests: List<String> = emptyList(),
  nameTestTag: String = "",
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(initialName) }
  var relationship by rememberSaveable { mutableStateOf(initialRelation) }

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
        // Name
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Név *") },
          placeholder = { Text("pl. Dalma, Attila, Ármin") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .then(if (nameTestTag.isNotEmpty()) Modifier.testTag(nameTestTag) else Modifier),
        )

        // Relationship chips & custom
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

        // Upcoming event selection (Születésnap / Évforduló / stb.)
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
                label = { Text(type) },
              )
            }
          }
          if (selectedEventType == "Egyéb") {
            OutlinedTextField(
              value = customEventName,
              onValueChange = { customEventName = it },
              label = { Text("Egyedi alkalom megnevezése") },
              placeholder = { Text("pl. Diplomaosztó, Házavató") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // Event date button
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Esemény dátuma", style = MaterialTheme.typography.labelMedium)
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
            Text("$formattedDateStr ($daysRemainingStr)")
          }
        }

        // Interactive interests management
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Érdeklődési körök", style = MaterialTheme.typography.labelMedium)
          if (selectedInterests.isNotEmpty()) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              selectedInterests.forEach { interest ->
                FilterChip(
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

          // Suggestion chips
          Text(
            "Gyors hozzáadás:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            EventDateUtils.POPULAR_GIFT_INTERESTS
              .filter { it !in selectedInterests }
              .take(6)
              .forEach { suggestion ->
                FilterChip(
                  selected = false,
                  onClick = { selectedInterests = selectedInterests + suggestion },
                  label = { Text("+ $suggestion") },
                )
              }
          }

          // Add custom interest input
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = customInterestInput,
              onValueChange = { customInterestInput = it },
              label = { Text("Egyedi hobbi") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
              onClick = {
                val trimmed = customInterestInput.trim()
                if (trimmed.isNotEmpty() && trimmed !in selectedInterests) {
                  selectedInterests = selectedInterests + trimmed
                  customInterestInput = ""
                }
              },
              enabled = customInterestInput.isNotBlank(),
            ) {
              Text("Hozzáad")
            }
          }
        }
      }
    },
    confirmButton = {
      FilledTonalButton(
        onClick = {
          if (name.isNotBlank()) {
            val finalEventName = when (selectedEventType) {
              "Születésnap" -> "$name Születésnap"
              "Évforduló" -> "Évforduló"
              "Karácsony" -> "Karácsony"
              "Névnap" -> "$name Névnap"
              "Egyéb" -> customEventName.ifBlank { "Különleges alkalom" }
              else -> selectedEventType
            }
            val interestsCsv = selectedInterests.joinToString(", ")
            onConfirm(name.trim(), relationship.trim(), finalEventName, calculatedDays, interestsCsv)
          }
        },
        enabled = name.isNotBlank(),
      ) {
        Text(stringResource(R.string.action_save))
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

@Composable
private fun EditPersonDialog(
  recipient: GiftRecipient,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  PersonFormDialog(
    dialogTitle = "Személy / Családtag szerkesztése",
    initialName = recipient.name,
    initialRelation = recipient.relationship,
    initialEventName = recipient.upcomingEventName,
    initialDaysUntilEvent = recipient.daysUntilEvent,
    initialInterests = recipient.interests,
    nameTestTag = "gift_edit_person_name",
    onDismiss = onDismiss,
    onConfirm = onConfirm,
  )
}

@Composable
private fun AddPersonDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  PersonFormDialog(
    dialogTitle = stringResource(R.string.dialog_add_person_title),
    initialName = "",
    initialRelation = "Partner",
    initialEventName = "Születésnap",
    initialDaysUntilEvent = 30,
    initialInterests = listOf("Könyvek", "Kávé", "Virágok"),
    nameTestTag = "gift_new_person_name",
    onDismiss = onDismiss,
    onConfirm = onConfirm,
  )
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
