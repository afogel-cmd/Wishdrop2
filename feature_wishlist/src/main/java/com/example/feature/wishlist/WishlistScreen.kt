package com.example.feature.wishlist

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.domain.model.SharedListSummary
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.format.CurrencyFormatter
import com.example.designsystem.components.ExpressiveChip
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.PriceDropBadge
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishDropListShimmer
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.Product
import com.example.domain.model.SmartCollection

@Composable
fun WishlistRouteScreen(
  initialCategory: String?,
  initialCollectionQuery: String?,
  focusSearch: Boolean,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  viewModel: WishlistViewModel = hiltViewModel(),
) {
  LaunchedEffect(initialCategory, initialCollectionQuery) {
    viewModel.applyInitialArgs(initialCategory, initialCollectionQuery)
  }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val undoLabel = stringResource(R.string.snackbar_undo)

  val swipeEvent = (uiState as? WishlistUiState.Content)?.swipeEvent
  LaunchedEffect(swipeEvent) {
    val event = swipeEvent ?: return@LaunchedEffect
    val label =
      if (event.type == SwipeUndoType.PURCHASED) {
        "Marked \"${event.productTitle}\" as Purchased"
      } else {
        "Archived \"${event.productTitle}\""
      }
    val result =
      snackbarHostState.showSnackbar(
        message = label,
        actionLabel = undoLabel,
        duration = SnackbarDuration.Short,
      )
    if (result == SnackbarResult.ActionPerformed) {
      viewModel.undoSwipe(event)
    } else {
      viewModel.consumeSwipeEvent()
    }
  }

  WishlistScreen(
    uiState = uiState,
    focusSearch = focusSearch,
    snackbarHostState = snackbarHostState,
    onRetry = viewModel::retry,
    onSearchChanged = viewModel::onSearchQueryChanged,
    onSelectFilter = viewModel::onFilterSelected,
    onClearFilters = viewModel::clearAllFilters,
    onSelectCollection = viewModel::onSmartCollectionClicked,
    onOpenProductDetails = onOpenProductDetails,
    onSwipePurchased = viewModel::markPurchasedBySwipe,
    onSwipeArchive = viewModel::markArchivedBySwipe,
    onOpenAddProduct = onOpenAddProduct,
    onAssignSharedList = viewModel::assignProductToSharedList,
  )
}

@Composable
fun WishlistScreen(
  uiState: WishlistUiState,
  focusSearch: Boolean,
  snackbarHostState: SnackbarHostState,
  onRetry: () -> Unit,
  onSearchChanged: (String) -> Unit,
  onSelectFilter: (String) -> Unit,
  onClearFilters: () -> Unit,
  onSelectCollection: (String) -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onSwipePurchased: (Product) -> Unit,
  onSwipeArchive: (Product) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  onAssignSharedList: (productId: String, sharedListId: String?) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  var fabExpanded by rememberSaveable { mutableStateOf(false) }
  val spacing = WishDropDesign.spacing

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      if (uiState is WishlistUiState.Content) {
        ExpandableWishlistFab(
          expanded = fabExpanded,
          onToggle = { fabExpanded = !fabExpanded },
          onSelectMode = { mode ->
            fabExpanded = false
            onOpenAddProduct(mode)
          },
        )
      }
    },
  ) { innerPadding ->
    when (uiState) {
      WishlistUiState.Loading ->
        WishDropListShimmer(cardCount = 4, modifier = Modifier.padding(innerPadding))
      is WishlistUiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(spacing.lg),
          contentAlignment = Alignment.Center,
        ) {
          WishDropErrorState(
            message = uiState.message,
            onRetry = onRetry,
            modifier = Modifier.widthIn(max = 520.dp),
          )
        }
      }
      is WishlistUiState.Content -> {
        WishlistLoadedContent(
          state = uiState,
          focusSearch = focusSearch,
          onSearchChanged = onSearchChanged,
          onSelectFilter = onSelectFilter,
          onClearFilters = onClearFilters,
          onSelectCollection = onSelectCollection,
          onOpenProductDetails = onOpenProductDetails,
          onSwipePurchased = onSwipePurchased,
          onSwipeArchive = onSwipeArchive,
          onAssignSharedList = onAssignSharedList,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}

@Composable
private fun WishlistLoadedContent(
  state: WishlistUiState.Content,
  focusSearch: Boolean,
  onSearchChanged: (String) -> Unit,
  onSelectFilter: (String) -> Unit,
  onClearFilters: () -> Unit,
  onSelectCollection: (String) -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onSwipePurchased: (Product) -> Unit,
  onSwipeArchive: (Product) -> Unit,
  onAssignSharedList: (productId: String, sharedListId: String?) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  val spacing = WishDropDesign.spacing

  LaunchedEffect(focusSearch) {
    if (focusSearch) {
      runCatching { focusRequester.requestFocus() }
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = spacing.maxContentWidth)) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
      ) {
        Text(
          text = stringResource(R.string.wishlist_title),
          style = MaterialTheme.typography.headlineLarge,
        )
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchChanged,
          placeholder = { Text(stringResource(R.string.wishlist_search_placeholder)) },
          leadingIcon = {
            Icon(imageVector = WishDropIcons.SearchFilled, contentDescription = null)
          },
          trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChanged("") }) {
                Icon(
                  imageVector = WishDropIcons.Clear,
                  contentDescription = stringResource(R.string.cd_clear_search),
                )
              }
            }
          },
          singleLine = true,
          shape = CircleShape,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              unfocusedBorderColor = Color.Transparent,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .focusRequester(focusRequester)
              .testTag("wishlist_search_input"),
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.testTag("wishlist_category_chips"),
        ) {
          items(
            items = state.filterChips,
            key = { it },
            contentType = { "filter_chip" },
          ) { filterName ->
            ExpressiveChip(
              label = filterName,
              selected = state.selectedFilter == filterName,
              colorIndex = state.filterChips.indexOf(filterName),
              onClick = { onSelectFilter(filterName) },
              modifier = Modifier.testTag("wishlist_chip_${filterName.lowercase()}"),
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("wishlist_products_list"),
        contentPadding =
          PaddingValues(
            start = spacing.lg,
            end = spacing.lg,
            top = spacing.sm,
            bottom = 96.dp,
          ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
      ) {
        item(key = "smart_collections", contentType = "collections") {
          Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ExpressiveSectionHeader(title = stringResource(R.string.wishlist_smart_collections))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
              items(
                items = state.smartCollections,
                key = { it.id },
                contentType = { "smart_collection" },
              ) { collection ->
                SmartCollectionCard(
                  collection = collection,
                  selected = state.activeCollectionQuery == collection.filterQuery,
                  onClick = { onSelectCollection(collection.filterQuery) },
                )
              }
            }
          }
        }

        item(key = "items_header", contentType = "section_header") {
          ExpressiveSectionHeader(
            title = stringResource(R.string.wishlist_items_header, state.filteredProducts.size),
            subtitle = stringResource(R.string.wishlist_swipe_hint),
          )
        }

        if (state.filteredProducts.isEmpty()) {
          item(key = "empty_wishlist", contentType = "empty") {
            WishDropEmptyState(
              icon = WishDropIcons.WishlistFilled,
              title = stringResource(R.string.wishlist_empty_title),
              description = stringResource(R.string.wishlist_empty_subtitle),
              actionLabel = stringResource(R.string.filter_all),
              onActionClick = onClearFilters,
            )
          }
        } else {
          items(
            items = state.filteredProducts,
            key = { it.id },
            contentType = { "wishlist_product" },
          ) { product ->
            SwipeableWishlistProductItem(
              product = product,
              sharedLists = state.sharedLists,
              onClick = { onOpenProductDetails(product.id) },
              onSwipePurchased = { onSwipePurchased(product) },
              onSwipeArchive = { onSwipeArchive(product) },
              onAssignSharedList = onAssignSharedList,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SmartCollectionCard(
  collection: SmartCollection,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val extended = WishDropDesign.colors
  val icon =
    when (collection.iconKey) {
      "electronics" -> WishDropIcons.Devices
      "google" -> WishDropIcons.Sparkle
      "audio" -> WishDropIcons.Headphones
      "person" -> WishDropIcons.Favorite
      "home" -> WishDropIcons.Living
      else -> WishDropIcons.Holiday
    }
  val bg =
    when {
      selected -> MaterialTheme.colorScheme.primaryContainer
      collection.iconKey == "electronics" -> extended.skyContainer.copy(alpha = 0.65f)
      collection.iconKey == "google" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
      collection.iconKey == "audio" -> extended.amberContainer.copy(alpha = 0.65f)
      collection.iconKey == "person" -> extended.coralContainer.copy(alpha = 0.65f)
      collection.iconKey == "home" -> extended.skyContainer.copy(alpha = 0.50f)
      else -> extended.savingsContainer.copy(alpha = 0.65f)
    }

  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    containerColor = bg,
    modifier =
      Modifier.width(180.dp)
        .semantics(mergeDescendants = true) {}
        .testTag("smart_collection_${collection.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = collection.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.wishlist_collection_count, collection.productCount),
          style = WishDropDesign.numericTypography.pillMetric,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwipeableWishlistProductItem(
  product: Product,
  sharedLists: List<SharedListSummary>,
  onClick: () -> Unit,
  onSwipePurchased: () -> Unit,
  onSwipeArchive: () -> Unit,
  onAssignSharedList: (productId: String, sharedListId: String?) -> Unit,
) {
  var showSharedListDialog by rememberSaveable { mutableStateOf(false) }
  val purchasedLabel = stringResource(R.string.swipe_purchased_label)
  val archiveLabel = stringResource(R.string.swipe_archive_label)
  val assignedList = remember(product.sharedListId, sharedLists) {
    sharedLists.firstOrNull { it.id == product.sharedListId }
  }

  val dismissState =
    rememberSwipeToDismissBoxState(
      confirmValueChange = { value ->
        when (value) {
          SwipeToDismissBoxValue.StartToEnd -> {
            onSwipePurchased()
            true
          }
          SwipeToDismissBoxValue.EndToStart -> {
            onSwipeArchive()
            true
          }
          SwipeToDismissBoxValue.Settled -> false
        }
      }
    )

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
      val direction = dismissState.dismissDirection
      val isRight = direction == SwipeToDismissBoxValue.StartToEnd
      val bgColor =
        when (direction) {
          SwipeToDismissBoxValue.StartToEnd -> WishDropDesign.colors.savingsContainer
          SwipeToDismissBoxValue.EndToStart -> WishDropDesign.colors.coralContainer
          SwipeToDismissBoxValue.Settled -> Color.Transparent
        }
      val fgColor =
        if (isRight) WishDropDesign.colors.onSavingsContainer
        else WishDropDesign.colors.onCoralContainer

      Box(
        modifier =
          Modifier.fillMaxSize()
            .clip(WishDropDesign.radii.large)
            .background(bgColor)
            .padding(horizontal = 24.dp),
        contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd,
      ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = if (isRight) WishDropIcons.CheckCircle else WishDropIcons.Archive,
              contentDescription = null,
              tint = fgColor,
            )
            Text(
              text = if (isRight) purchasedLabel else archiveLabel,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = fgColor,
            )
          }
        }
      }
    },
  ) {
    PixelBentoCard(
      onClick = onClick,
      shape = WishDropDesign.radii.large,
      modifier =
        Modifier.fillMaxWidth()
          .semantics {
            customActions =
              listOf(
                CustomAccessibilityAction(purchasedLabel) {
                  onSwipePurchased()
                  true
                },
                CustomAccessibilityAction(archiveLabel) {
                  onSwipeArchive()
                  true
                },
              )
          }
          .testTag("wishlist_product_card_${product.id}"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WishProductArtwork(
          imageKey = product.imageKey,
          productTitle = product.title,
          shape = WishDropDesign.radii.medium,
          modifier = Modifier.size(94.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = product.store,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            if (!product.recipientName.isNullOrBlank()) {
              Text(
                text = "For ${product.recipientName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
          Text(
            text = product.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
              style = WishDropDesign.numericTypography.cardPrice,
            )
            PriceDropBadge(percentChange = product.priceChangePercent)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FlowRow(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (assignedList != null) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                  Text(
                    text = "${assignedList.emoji} ${assignedList.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
              product.tags.take(2).forEach { tag ->
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                  Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }

            IconButton(
              onClick = { showSharedListDialog = true },
              modifier = Modifier.size(32.dp).testTag("wishlist_assign_shared_list_${product.id}"),
            ) {
              Icon(
                imageVector = WishDropIcons.Add,
                contentDescription = "Hozzáadás megosztott listához",
                tint = if (product.sharedListId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }

  if (showSharedListDialog) {
    AlertDialog(
      onDismissRequest = { showSharedListDialog = false },
      title = { Text("Hozzáadás megosztott listához") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (sharedLists.isEmpty()) {
            Text(
              text = "Még nincsenek megosztott listáid. Hozz létre egyet a Közös Listák lapon!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            sharedLists.forEach { sharedList ->
              val isSelected = product.sharedListId == sharedList.id
              Surface(
                shape = WishDropDesign.radii.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clickable {
                    onAssignSharedList(product.id, sharedList.id)
                    showSharedListDialog = false
                  },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(text = sharedList.emoji, style = MaterialTheme.typography.titleMedium)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = sharedList.title, style = MaterialTheme.typography.titleSmall)
                    if (sharedList.subtitle.isNotBlank()) {
                      Text(text = sharedList.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                  if (isSelected) {
                    Icon(
                      imageVector = WishDropIcons.Check,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }

            if (product.sharedListId != null) {
              TextButton(
                onClick = {
                  onAssignSharedList(product.id, null)
                  showSharedListDialog = false
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              ) {
                Icon(
                  imageVector = WishDropIcons.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Eltávolítás a megosztott listából", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSharedListDialog = false }) {
          Text("Bezárás")
        }
      },
    )
  }
}

@Composable
private fun ExpandableWishlistFab(
  expanded: Boolean,
  onToggle: () -> Unit,
  onSelectMode: (String) -> Unit,
) {
  val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fab_rotation")

  Column(
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
      exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FabMenuOption(
          label = stringResource(R.string.fab_add_link),
          icon = WishDropIcons.AddLink,
          testTag = "fab_option_url",
          onClick = { onSelectMode("URL") },
        )
        FabMenuOption(
          label = stringResource(R.string.fab_manual_product),
          icon = WishDropIcons.ManualEntry,
          testTag = "fab_option_manual",
          onClick = { onSelectMode("MANUAL") },
        )
        FabMenuOption(
          label = stringResource(R.string.fab_barcode_scan),
          icon = WishDropIcons.BarcodeScan,
          testTag = "fab_option_barcode",
          onClick = { onSelectMode("BARCODE") },
        )
      }
    }

    FloatingActionButton(
      onClick = onToggle,
      shape = WishDropDesign.radii.medium,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.testTag("wishlist_expand_fab"),
    ) {
      Icon(
        imageVector = WishDropIcons.Add,
        contentDescription =
          stringResource(
            if (expanded) R.string.fab_close_menu else R.string.fab_add_product
          ),
        modifier = Modifier.graphicsLayer { rotationZ = rotation },
      )
    }
  }
}

@Composable
private fun FabMenuOption(
  label: String,
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
) {
  ExtendedFloatingActionButton(
    onClick = onClick,
    icon = { Icon(imageVector = icon, contentDescription = null) },
    text = { Text(label) },
    shape = CircleShape,
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier.height(48.dp).testTag(testTag),
  )
}

@Preview(showBackground = true, name = "Wishlist Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Wishlist Dark")
@Composable
private fun WishlistScreenPreview() {
  WishDropTheme(useDynamicColor = false) {
    WishlistScreen(
      uiState =
        WishlistUiState.Content(
          smartCollections =
            listOf(
              SmartCollection("1", "Electronics", "", 12, "electronics", "Electronics"),
              SmartCollection("2", "Dalma", "", 7, "person", "Dalma"),
              SmartCollection("3", "Christmas", "", 8, "holiday", "Christmas"),
            ),
          filteredProducts =
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
                tags = listOf("Google", "Pixel", "Bluetooth"),
                recipientName = "Attila",
              )
            ),
        ),
      focusSearch = false,
      snackbarHostState = remember { SnackbarHostState() },
      onRetry = {},
      onSearchChanged = {},
      onSelectFilter = {},
      onClearFilters = {},
      onSelectCollection = {},
      onOpenProductDetails = {},
      onSwipePurchased = {},
      onSwipeArchive = {},
      onOpenAddProduct = {},
    )
  }
}
