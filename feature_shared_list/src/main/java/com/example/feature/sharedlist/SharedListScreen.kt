package com.example.feature.sharedlist

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.preferences.ThemeMode
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishDropListShimmer
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.ActivityEvent
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.Product
import com.example.domain.model.SharedListDetails
import com.example.domain.model.SharedListMember
import com.example.domain.model.SharedListSummary

@Composable
fun SharedListRouteScreen(
  listId: String,
  onNavigateBack: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  viewModel: SharedListViewModel = hiltViewModel(),
) {
  LaunchedEffect(listId) { viewModel.loadList(listId) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  val statusMessage = (uiState as? SharedListUiState.Success)?.statusMessage
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
    SharedListScreenContent(
      state = uiState,
      onNavigateBack = onNavigateBack,
      onRetry = viewModel::retry,
      onOpenProductDetails = onOpenProductDetails,
      onUpdateVisibility = viewModel::updateVisibility,
      onUpdateMemberRole = viewModel::updateMemberRole,
      onSendInvitation = viewModel::sendInvitation,
      onAcceptInvitation = viewModel::acceptInvitation,
      onAddQuickProduct = viewModel::addQuickProduct,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
fun SharedListScreenContent(
  state: SharedListUiState,
  onNavigateBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onUpdateVisibility: (ListVisibility, String?) -> Unit,
  onUpdateMemberRole: (String, ListMemberRole) -> Unit,
  onSendInvitation: (String, ListMemberRole) -> Unit,
  onAcceptInvitation: (String) -> Unit,
  onAddQuickProduct: (String, String, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  var showInviteDialog by rememberSaveable { mutableStateOf(false) }
  var showQuickAddDialog by rememberSaveable { mutableStateOf(false) }
  var editingMember by remember { mutableStateOf<SharedListMember?>(null) }

  when (state) {
    SharedListUiState.Loading -> WishDropListShimmer(cardCount = 4, modifier = modifier)

    is SharedListUiState.Error -> {
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

    SharedListUiState.HiddenOrMissing -> {
      Box(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        WishDropEmptyState(
          icon = WishDropIcons.SecretHidden,
          title = stringResource(R.string.shared_list_hidden_empty_title),
          description = stringResource(R.string.shared_list_hidden_empty_desc),
          actionLabel = stringResource(R.string.shared_list_back),
          onActionClick = onNavigateBack,
          modifier = Modifier.widthIn(max = spacing.maxSheetWidth),
        )
      }
    }

    is SharedListUiState.Success -> {
      val details = state.details
      val summary = details.summary
      val extended = WishDropDesign.colors

      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = spacing.maxContentWidth)
              .testTag("shared_list_details_scroll"),
          contentPadding =
            PaddingValues(
              start = spacing.lg,
              end = spacing.lg,
              top = spacing.sm,
              bottom = spacing.xxxl,
            ),
          verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
          item(key = "top_bar", contentType = "top_bar") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                IconButton(
                  onClick = onNavigateBack,
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("shared_list_back_button"),
                ) {
                  Icon(
                    imageVector = WishDropIcons.ArrowBack,
                    contentDescription = stringResource(R.string.shared_list_back),
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                  onClick = { showInviteDialog = true },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("shared_list_invite_button"),
                ) {
                  Icon(
                    imageVector = WishDropIcons.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.shared_list_invite))
                }
                if (details.canEdit) {
                  Button(
                    onClick = { showQuickAddDialog = true },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("shared_list_add_wish_button"),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Add,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.shared_list_add_idea))
                  }
                }
              }
            }
          }

          item(key = "header_hero", contentType = "hero") {
            Card(
              shape = WishDropDesign.radii.extraLarge,
              colors = CardDefaults.cardColors(containerColor = Color.Transparent),
              modifier = Modifier.fillMaxWidth().testTag("shared_list_hero_card"),
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(
                      Brush.linearGradient(
                        colors = listOf(extended.heroStart, extended.heroMiddle, extended.heroEnd),
                        start = Offset(0f, 0f),
                        end = Offset(860f, 520f),
                      )
                    )
                    .padding(24.dp)
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = Color.White.copy(alpha = 0.18f),
                      contentColor = extended.onHero,
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Icon(
                          imageVector = WishDropIcons.CloudSynced,
                          contentDescription = null,
                          modifier = Modifier.size(14.dp),
                        )
                        Text(
                          text = stringResource(R.string.shared_list_realtime_live),
                          style = MaterialTheme.typography.labelMedium,
                        )
                      }
                    }

                    Surface(
                      shape = CircleShape,
                      color = Color.White.copy(alpha = 0.18f),
                      contentColor = extended.onHero,
                    ) {
                      Text(
                        text = stringResource(R.string.shared_list_items_badge, summary.itemCount),
                        style = WishDropDesign.numericTypography.pillMetric,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                      )
                    }
                  }

                  Text(
                    text = "${summary.emoji} ${summary.title}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = extended.onHero,
                    modifier = Modifier.semantics { heading() },
                  )
                  Text(
                    text = summary.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.onHero.copy(alpha = 0.88f),
                  )
                }
              }
            }
          }

          if (!summary.hiddenFromMemberName.isNullOrBlank()) {
            item(key = "secret_shield", contentType = "secret_banner") {
              SecretGiftShieldBanner(hiddenFromName = summary.hiddenFromMemberName!!)
            }
          }

          if (details.canEdit) {
            item(key = "visibility_selector", contentType = "visibility") {
              VisibilitySelectorCard(
                currentVisibility = summary.visibility,
                hiddenFromMemberId = summary.hiddenFromMemberId,
                recipients = state.familyRecipients,
                onUpdateVisibility = onUpdateVisibility,
              )
            }
          }

          item(key = "members_section", contentType = "members") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              ExpressiveSectionHeader(
                title = stringResource(R.string.shared_list_members_title),
                subtitle = stringResource(R.string.shared_list_members_sub),
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                  items = details.members,
                  key = { it.memberId },
                  contentType = { "collaborator" },
                ) { member ->
                  CollaboratorMemberCard(
                    member = member,
                    onClick = { if (details.canEdit) editingMember = member },
                  )
                }
              }
            }
          }

          if (details.pendingInvitations.isNotEmpty()) {
            item(key = "pending_invitations", contentType = "invitations") {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpressiveSectionHeader(title = stringResource(R.string.shared_list_pending_invites))
                details.pendingInvitations.forEach { invite ->
                  PendingInvitationRowCard(
                    invitation = invite,
                    onAccept = { onAcceptInvitation(invite.id) },
                  )
                }
              }
            }
          }

          item(key = "products_header", contentType = "section_header") {
            ExpressiveSectionHeader(
              title = stringResource(R.string.shared_list_products_title),
              subtitle = summary.eventDateLabel,
            )
          }

          if (details.products.isEmpty()) {
            item(key = "empty_products", contentType = "empty") {
              WishDropEmptyState(
                icon = WishDropIcons.GiftFilled,
                title = stringResource(R.string.shared_list_products_empty_title),
                description = stringResource(R.string.shared_list_products_empty_desc),
                actionLabel = stringResource(R.string.shared_list_add_idea),
                onActionClick = { showQuickAddDialog = true },
              )
            }
          } else {
            items(
              items = details.products,
              key = { it.id },
              contentType = { "shared_product" },
            ) { product ->
              SharedListProductItemCard(
                product = product,
                onClick = { onOpenProductDetails(product.id) },
              )
            }
          }

          if (details.activityFeed.isNotEmpty()) {
            item(key = "activity_feed", contentType = "activity") {
              PixelBentoCard(modifier = Modifier.fillMaxWidth().testTag("shared_list_activity_card")) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  ExpressiveSectionHeader(
                    title = stringResource(R.string.shared_list_activity_title),
                    subtitle = stringResource(R.string.shared_list_activity_sub),
                  )
                  details.activityFeed.forEach { event ->
                    SharedListActivityRow(event = event)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showInviteDialog) {
    InviteCollaboratorDialog(
      onDismiss = { showInviteDialog = false },
      onSend = onSendInvitation,
    )
  }

  if (showQuickAddDialog) {
    QuickAddSharedWishDialog(
      onDismiss = { showQuickAddDialog = false },
      onConfirm = onAddQuickProduct,
    )
  }

  editingMember?.let { member ->
    MemberRolePickerDialog(
      member = member,
      onDismiss = { editingMember = null },
      onSelectRole = { role -> onUpdateMemberRole(member.memberId, role) },
    )
  }
}

@Preview(showBackground = true, name = "Shared List — Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Shared List — Dark")
@Composable
private fun SharedListScreenPreview() {
  WishDropTheme(themeMode = ThemeMode.SYSTEM, useDynamicColor = false) {
    SharedListScreenContent(
      state =
        SharedListUiState.Success(
          details =
            SharedListDetails(
              summary =
                SharedListSummary(
                  id = "sl_christmas_2027",
                  title = "Christmas 2027",
                  emoji = "🎄",
                  subtitle = "Family holiday wishlist & Secret Santa coordination",
                  ownerMemberId = "rec_attila",
                  visibility = ListVisibility.SHARED,
                  memberNames = listOf("Attila", "Dalma"),
                  itemCount = 27,
                  eventDateLabel = "Dec 24 • 61 days",
                ),
              members =
                listOf(
                  SharedListMember(
                    "sl_christmas_2027",
                    "rec_attila",
                    "Attila",
                    "attila@pixelmail.hu",
                    "A",
                    "indigo",
                    ListMemberRole.OWNER,
                  ),
                  SharedListMember(
                    "sl_christmas_2027",
                    "rec_dalma",
                    "Dalma",
                    "dalma@pixelmail.hu",
                    "D",
                    "coral",
                    ListMemberRole.EDITOR,
                  ),
                ),
              products =
                listOf(
                  Product(
                    id = "prod_pixel_buds_2a",
                    title = "Google Pixel Buds 2a",
                    store = "Yettel",
                    currentPriceHuf = 28_490,
                    originalPriceHuf = 32_990,
                    priceChangePercent = -13,
                    savingsHuf = 4_500,
                    imageKey = "pixel_buds",
                    categories = listOf("Electronics"),
                    tags = listOf("Google", "Pixel"),
                  )
                ),
              activityFeed =
                listOf(
                  ActivityEvent(
                    id = "act_1",
                    actorMemberId = "rec_attila",
                    actorName = "Attila",
                    actionSummary = "Attila added Google Pixel Buds 2a",
                    createdAtMillis = 0L,
                  )
                ),
              pendingInvitations = emptyList(),
            )
        ),
      onNavigateBack = {},
      onRetry = {},
      onOpenProductDetails = {},
      onUpdateVisibility = { _, _ -> },
      onUpdateMemberRole = { _, _ -> },
      onSendInvitation = { _, _ -> },
      onAcceptInvitation = {},
      onAddQuickProduct = { _, _, _ -> },
    )
  }
}
