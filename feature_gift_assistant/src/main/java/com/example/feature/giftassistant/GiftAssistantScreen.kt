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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
  modifier: Modifier = Modifier,
) {
  var showAddInterestDialog by rememberSaveable { mutableStateOf(false) }
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
              ExpressiveSectionHeader(
                title = stringResource(R.string.gift_ai_recommendations_title),
                subtitle = stringResource(R.string.gift_ai_recommendations_sub, recipient.name),
              )
              state.aiRecommendations.forEach { rec ->
                AiGiftRecommendationCard(
                  recommendation = rec,
                  onSaveOrView = { onSaveRecommendation(rec) },
                )
              }
            }
          }
        }
      }
    }
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

@Composable
private fun AddPersonDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var relation by rememberSaveable { mutableStateOf("") }
  var event by rememberSaveable { mutableStateOf("Birthday") }
  var days by rememberSaveable { mutableStateOf("30") }
  var interests by rememberSaveable { mutableStateOf("Flowers, Books, Coffee") }

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
        OutlinedTextField(
          value = days,
          onValueChange = { days = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.dialog_person_days)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
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
            onConfirm(name, relation, event, days.toIntOrNull() ?: 30, interests)
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
