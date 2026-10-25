package com.example.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.format.CurrencyFormatter
import com.example.core.preferences.ThemeMode
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.PriceDropBadge
import com.example.designsystem.components.WishDropDashboardShimmer
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.DashboardSummary
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListVisibility
import com.example.domain.model.Product
import com.example.domain.model.SharedListSummary
import com.example.domain.model.UpcomingEvent

@Composable
fun HomeRouteScreen(
  onOpenSearch: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  onOpenGiftAssistant: (String?) -> Unit,
  onOpenWishlistAll: () -> Unit,
  onOpenSharedList: (String) -> Unit,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HomeScreen(
    uiState = uiState,
    onRetry = viewModel::retry,
    onOpenSearch = onOpenSearch,
    onOpenProfile = onOpenProfile,
    onOpenProductDetails = onOpenProductDetails,
    onOpenAddProduct = onOpenAddProduct,
    onOpenGiftAssistant = onOpenGiftAssistant,
    onOpenWishlistAll = onOpenWishlistAll,
    onOpenSharedList = onOpenSharedList,
    onCreateSharedList = { title, emoji, subtitle, hiddenId ->
      viewModel.createSharedList(title, emoji, subtitle, hiddenId, onOpenSharedList)
    },
  )
}

@Composable
fun HomeScreen(
  uiState: HomeUiState,
  onRetry: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  onOpenGiftAssistant: (String?) -> Unit,
  onOpenWishlistAll: () -> Unit,
  onOpenSharedList: (String) -> Unit = {},
  onCreateSharedList: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  var showCreateListDialog by rememberSaveable { mutableStateOf(false) }

  when (uiState) {
    HomeUiState.Loading -> WishDropDashboardShimmer(modifier = modifier)
    is HomeUiState.Error -> {
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
    is HomeUiState.Empty -> {
      Column(
        modifier =
          modifier
            .fillMaxSize()
            .widthIn(max = spacing.maxContentWidth)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
      ) {
        HomeTopBar(
          userDisplayName = uiState.userDisplayName,
          onSearchClick = onOpenSearch,
          onProfileClick = onOpenProfile,
        )
        WishDropEmptyState(
          icon = WishDropIcons.Sparkle,
          title = stringResource(R.string.home_empty_title),
          description = stringResource(R.string.home_empty_subtitle),
          actionLabel = stringResource(R.string.action_add_product),
          onActionClick = { onOpenAddProduct("URL") },
        )
      }
    }
    is HomeUiState.Success -> {
      HomeScreenContent(
        userDisplayName = uiState.userDisplayName,
        summary = uiState.summary,
        onOpenSearch = onOpenSearch,
        onOpenProfile = onOpenProfile,
        onOpenProductDetails = onOpenProductDetails,
        onOpenAddProduct = onOpenAddProduct,
        onOpenGiftAssistant = onOpenGiftAssistant,
        onOpenWishlistAll = onOpenWishlistAll,
        onOpenSharedList = onOpenSharedList,
        onCreateListClick = { showCreateListDialog = true },
        modifier = modifier,
      )
      if (showCreateListDialog) {
        CreateSharedListDialog(
          recipients = uiState.familyRecipients,
          onDismiss = { showCreateListDialog = false },
          onCreate = onCreateSharedList,
        )
      }
    }
  }
}

@Composable
private fun HomeScreenContent(
  userDisplayName: String,
  summary: DashboardSummary,
  onOpenSearch: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  onOpenGiftAssistant: (String?) -> Unit,
  onOpenWishlistAll: () -> Unit,
  onOpenSharedList: (String) -> Unit,
  onCreateListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = spacing.maxContentWidth)
          .testTag("home_scroll_list"),
      contentPadding =
        PaddingValues(
          start = spacing.lg,
          end = spacing.lg,
          top = spacing.md,
          bottom = spacing.xxxl,
        ),
      verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
      item(key = "top_bar", contentType = "header") {
        HomeTopBar(
          userDisplayName = userDisplayName,
          onSearchClick = onOpenSearch,
          onProfileClick = onOpenProfile,
        )
      }

      item(key = "hero_card", contentType = "hero") {
        ExpressiveHeroCard(
          activeWishes = summary.activeWishesCount,
          sharedListsCount = summary.sharedListsCount,
          potentialSavingsHuf = summary.potentialSavingsHuf,
          onHeroClick = onOpenWishlistAll,
        )
      }

      item(key = "quick_actions", contentType = "actions") {
        QuickActionsRow(
          onAddProduct = { onOpenAddProduct("URL") },
          onScanProduct = { onOpenAddProduct("BARCODE") },
          onFindGift = { onOpenGiftAssistant(null) },
          onSearch = onOpenSearch,
        )
      }

      if (summary.sharedLists.isNotEmpty()) {
        item(key = "shared_lists", contentType = "shared_lists") {
          HomeSharedListsSection(
            sharedLists = summary.sharedLists,
            onOpenSharedList = onOpenSharedList,
            onCreateListClick = onCreateListClick,
          )
        }
      }

      item(key = "recommendations", contentType = "carousel") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
          ExpressiveSectionHeader(
            title = stringResource(R.string.section_todays_recommendations),
            subtitle = stringResource(R.string.section_todays_recommendations_sub),
            actionLabel = stringResource(R.string.action_see_all),
            onActionClick = onOpenWishlistAll,
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.testTag("home_recommendations_row"),
          ) {
            items(
              items = summary.recommendations,
              key = { it.id },
              contentType = { "deal_card" },
            ) { product ->
              RecommendationDealCard(
                product = product,
                onClick = { onOpenProductDetails(product.id) },
              )
            }
          }
        }
      }

      item(key = "events_header", contentType = "section_header") {
        ExpressiveSectionHeader(
          title = stringResource(R.string.section_upcoming_events),
          subtitle = stringResource(R.string.section_upcoming_events_sub),
          actionLabel = stringResource(R.string.action_find_gift),
          onActionClick = { onOpenGiftAssistant(null) },
        )
      }

      items(
        items = summary.upcomingEvents,
        key = { it.id },
        contentType = { "event_card" },
      ) { event ->
        UpcomingEventCard(
          event = event,
          onClick = { onOpenGiftAssistant(event.recipientId) },
        )
      }
    }
  }
}

@Composable
private fun HomeTopBar(
  userDisplayName: String,
  onSearchClick: () -> Unit,
  onProfileClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column {
      Text(
        text = stringResource(R.string.home_brand_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.home_tagline),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        IconButton(
          onClick = onSearchClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("home_search_button"),
        ) {
          Icon(
            imageVector = WishDropIcons.SearchOutlined,
            contentDescription = stringResource(R.string.cd_home_search),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
              role = Role.Button,
              onClickLabel = stringResource(R.string.cd_home_profile),
              onClick = onProfileClick,
            )
            .testTag("home_profile_avatar"),
        contentAlignment = Alignment.Center,
      ) {
        PersonAvatarBadge(
          name = userDisplayName,
          accentKey = "indigo",
          size = 44.dp,
        )
      }
    }
  }
}

@Composable
private fun ExpressiveHeroCard(
  activeWishes: Int,
  sharedListsCount: Int,
  potentialSavingsHuf: Int,
  onHeroClick: () -> Unit,
) {
  val extended = WishDropDesign.colors
  Card(
    onClick = onHeroClick,
    shape = WishDropDesign.radii.extraLarge,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    modifier =
      Modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) {}
        .testTag("home_hero_card"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(extended.heroStart, extended.heroMiddle, extended.heroEnd),
              start = Offset(0f, 0f),
              end = Offset(920f, 680f),
            )
          )
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
          color = Color.White.copy(alpha = 0.14f),
          radius = size.minDimension * 0.54f,
          center = Offset(size.width * 0.88f, size.height * 0.16f),
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.08f),
          radius = size.minDimension * 0.36f,
          center = Offset(size.width * 0.12f, size.height * 0.90f),
        )
      }
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Surface(
          color = Color.White.copy(alpha = 0.18f),
          contentColor = extended.onHero,
          shape = CircleShape,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = WishDropIcons.Sparkle,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = stringResource(R.string.hero_badge_ai),
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = CurrencyFormatter.formatHuf(potentialSavingsHuf),
            style = WishDropDesign.numericTypography.heroCurrency,
            color = extended.onHero,
          )
          Text(
            text = stringResource(R.string.hero_potential_savings_label),
            style = MaterialTheme.typography.titleMedium,
            color = extended.onHero.copy(alpha = 0.88f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HeroStatPill(
            text = stringResource(R.string.hero_active_wishes, activeWishes),
            modifier = Modifier.weight(1f),
          )
          HeroStatPill(
            text = stringResource(R.string.hero_shared_lists, sharedListsCount),
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun HeroStatPill(text: String, modifier: Modifier = Modifier) {
  val extended = WishDropDesign.colors
  Surface(
    modifier = modifier,
    shape = WishDropDesign.radii.medium,
    color = Color.White.copy(alpha = 0.17f),
    contentColor = extended.onHero,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun QuickActionsRow(
  onAddProduct: () -> Unit,
  onScanProduct: () -> Unit,
  onFindGift: () -> Unit,
  onSearch: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = stringResource(R.string.section_quick_actions),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      QuickActionTile(
        label = stringResource(R.string.action_add_product),
        icon = WishDropIcons.AddLink,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        testTag = "quick_action_add_product",
        onClick = onAddProduct,
        modifier = Modifier.weight(1f),
      )
      QuickActionTile(
        label = stringResource(R.string.action_scan_product),
        icon = WishDropIcons.BarcodeScan,
        containerColor = WishDropDesign.colors.savingsContainer,
        contentColor = WishDropDesign.colors.onSavingsContainer,
        testTag = "quick_action_scan_product",
        onClick = onScanProduct,
        modifier = Modifier.weight(1f),
      )
      QuickActionTile(
        label = stringResource(R.string.action_find_gift),
        icon = WishDropIcons.GiftFilled,
        containerColor = WishDropDesign.colors.coralContainer,
        contentColor = WishDropDesign.colors.onCoralContainer,
        testTag = "quick_action_find_gift",
        onClick = onFindGift,
        modifier = Modifier.weight(1f),
      )
      QuickActionTile(
        label = stringResource(R.string.action_search),
        icon = WishDropIcons.SearchFilled,
        containerColor = WishDropDesign.colors.amberContainer,
        contentColor = WishDropDesign.colors.onAmberContainer,
        testTag = "quick_action_search",
        onClick = onSearch,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun QuickActionTile(
  label: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    color = containerColor,
    contentColor = contentColor,
    modifier = modifier.minimumInteractiveComponentSize().testTag(testTag),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(26.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun RecommendationDealCard(
  product: Product,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.large,
    modifier =
      Modifier.width(260.dp)
        .semantics(mergeDescendants = true) {}
        .testTag("home_recommendation_${product.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      WishProductArtwork(
        imageKey = product.imageKey,
        productTitle = product.title,
        shape = WishDropDesign.radii.medium,
        modifier = Modifier.fillMaxWidth().height(152.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = product.store,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = product.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
          style = WishDropDesign.numericTypography.cardPrice,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      PriceDropBadge(
        percentChange = product.priceChangePercent,
        savingsHuf = product.savingsHuf,
      )
    }
  }
}

@Composable
private fun UpcomingEventCard(
  event: UpcomingEvent,
  onClick: () -> Unit,
) {
  val extended = WishDropDesign.colors
  val isHoliday = event.iconKey == "holiday"
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier =
      Modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) {}
        .testTag("home_event_${event.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = WishDropDesign.radii.small,
        color = if (isHoliday) extended.savingsContainer else extended.coralContainer,
        contentColor = if (isHoliday) extended.onSavingsContainer else extended.onCoralContainer,
        modifier = Modifier.size(52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isHoliday) WishDropIcons.Holiday else WishDropIcons.Cake,
            contentDescription = null,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = event.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.event_saved_ideas_count, event.savedIdeasCount),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(R.string.event_days_remaining, event.daysRemaining),
            style = WishDropDesign.numericTypography.pillMetric,
          )
          Icon(
            imageVector = WishDropIcons.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }
  }
}

@Preview(showBackground = true, name = "Home Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Home Dark")
@Composable
private fun HomeScreenPreview() {
  WishDropTheme(
    themeMode = ThemeMode.SYSTEM,
    useDynamicColor = false,
  ) {
    HomeScreen(
      uiState =
        HomeUiState.Success(
          userDisplayName = "Attila Kovács",
          summary =
            DashboardSummary(
              activeWishesCount = 17,
              sharedListsCount = 3,
              upcomingEventsCount = 3,
              potentialSavingsHuf = 18_400,
              sharedLists =
                listOf(
                  SharedListSummary(
                    id = "sl_christmas_2027",
                    title = "Christmas 2027",
                    emoji = "🎄",
                    subtitle = "Family holiday wishlist & Secret Santa coordination",
                    ownerMemberId = "rec_attila",
                    visibility = ListVisibility.SHARED,
                    memberNames = listOf("Attila", "Dalma"),
                    itemCount = 27,
                  )
                ),
              recommendations =
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
                    categories = listOf("Electronics", "Audio"),
                    tags = listOf("Google", "Pixel"),
                  )
                ),
              upcomingEvents =
                listOf(
                  UpcomingEvent("1", "Dalma Birthday", 15, "rec_dalma", "cake", 5),
                  UpcomingEvent("2", "Christmas", 61, "rec_armin", "holiday", 8),
                  UpcomingEvent("3", "Olívia Birthday", 74, "rec_olivia", "cake", 2),
                ),
            ),
        ),
      onRetry = {},
      onOpenSearch = {},
      onOpenProfile = {},
      onOpenProductDetails = {},
      onOpenAddProduct = {},
      onOpenGiftAssistant = {},
      onOpenWishlistAll = {},
    )
  }
}
