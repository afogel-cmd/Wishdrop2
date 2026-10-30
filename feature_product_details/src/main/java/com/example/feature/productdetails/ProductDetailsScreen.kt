package com.example.feature.productdetails

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.format.CurrencyFormatter
import com.example.core.preferences.ThemeMode
import com.example.designsystem.components.ExpressiveChip
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.PriceDropBadge
import com.example.designsystem.components.WishDropDetailsShimmer
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.ActivityEvent
import com.example.domain.model.ListVisibility
import com.example.domain.model.PricePeriod
import com.example.domain.model.Product
import com.example.domain.model.ProductComment
import com.example.domain.model.StoreOffer

@Composable
fun ProductDetailsRouteScreen(
  productId: String,
  onNavigateBack: () -> Unit,
  onOpenAlternativeProduct: (String) -> Unit,
  viewModel: ProductDetailsViewModel = hiltViewModel(),
) {
  LaunchedEffect(productId) { viewModel.loadProduct(productId) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  ProductDetailsScreenContent(
    state = uiState,
    onNavigateBack = onNavigateBack,
    onRetry = viewModel::retry,
    onSelectPeriod = viewModel::selectPeriod,
    onTogglePurchased = viewModel::togglePurchased,
    onTogglePriceAlert = viewModel::togglePriceAlert,
    onAssignRecipient = viewModel::assignRecipient,
    onAssignSharedList = viewModel::assignSharedList,
    onAddComment = viewModel::addComment,
    onAddProductPhoto = viewModel::addProductPhoto,
    onDeleteProduct = {
      viewModel.deleteProduct(onDeleted = onNavigateBack)
    },
    onShareProduct = { product ->
      val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(
            Intent.EXTRA_TEXT,
            "${product.title} — ${CurrencyFormatter.formatHuf(product.currentPriceHuf)} at ${product.store}",
          )
        }
      context.startActivity(Intent.createChooser(sendIntent, product.title))
    },
    onOpenAlternativeProduct = onOpenAlternativeProduct,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsScreenContent(
  state: ProductDetailsUiState,
  onNavigateBack: () -> Unit,
  onRetry: () -> Unit,
  onSelectPeriod: (PricePeriod) -> Unit,
  onTogglePurchased: () -> Unit,
  onTogglePriceAlert: () -> Unit,
  onAssignRecipient: (String?) -> Unit,
  onShareProduct: (Product) -> Unit,
  onOpenAlternativeProduct: (String) -> Unit,
  onAssignSharedList: (String?) -> Unit = {},
  onAddComment: (String) -> Unit = {},
  onAddProductPhoto: (String) -> Unit = {},
  onDeleteProduct: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showRecipientPicker by rememberSaveable { mutableStateOf(false) }
  var showSharedListPicker by rememberSaveable { mutableStateOf(false) }
  val spacing = WishDropDesign.spacing

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    when (state) {
      ProductDetailsUiState.Loading -> {
        WishDropDetailsShimmer(modifier = Modifier.padding(innerPadding))
      }

      is ProductDetailsUiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(spacing.lg),
          contentAlignment = Alignment.Center,
        ) {
          WishDropErrorState(
            message = state.message,
            onRetry = onRetry,
            modifier = Modifier.widthIn(max = spacing.maxSheetWidth),
          )
        }
      }

      ProductDetailsUiState.Empty -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(spacing.lg),
          contentAlignment = Alignment.Center,
        ) {
          WishDropEmptyState(
            icon = WishDropIcons.WishlistFilled,
            title = stringResource(R.string.details_empty_title),
            description = stringResource(R.string.details_empty_desc),
            actionLabel = stringResource(R.string.details_empty_action),
            onActionClick = onNavigateBack,
            modifier = Modifier.widthIn(max = spacing.maxSheetWidth),
          )
        }
      }

      is ProductDetailsUiState.Success -> {
        val product = state.product
        val alertStateText =
          stringResource(
            if (product.isPriceAlertEnabled) R.string.details_alert_on_state
            else R.string.details_alert_off_state
          )

        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.TopCenter,
        ) {
          LazyColumn(
            modifier =
              Modifier.fillMaxSize()
                .widthIn(max = spacing.maxContentWidth)
                .testTag("product_details_list"),
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
                      Modifier.minimumInteractiveComponentSize().testTag("details_back_button"),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.ArrowBack,
                      contentDescription = stringResource(R.string.details_back),
                    )
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  ) {
                    IconButton(
                      onClick = onTogglePriceAlert,
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .semantics { stateDescription = alertStateText }
                          .testTag("details_alert_button"),
                    ) {
                      Icon(
                        imageVector =
                          if (product.isPriceAlertEnabled) WishDropIcons.AlertActive
                          else WishDropIcons.AlertOff,
                        contentDescription = stringResource(R.string.details_price_alert),
                        tint =
                          if (product.isPriceAlertEnabled) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  ) {
                    IconButton(
                      onClick = { onShareProduct(product) },
                      modifier =
                        Modifier.minimumInteractiveComponentSize().testTag("details_share_button"),
                    ) {
                      Icon(
                        imageVector = WishDropIcons.Share,
                        contentDescription = stringResource(R.string.details_share),
                      )
                    }
                  }

                  var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

                  if (showDeleteConfirmDialog) {
                    AlertDialog(
                      onDismissRequest = { showDeleteConfirmDialog = false },
                      title = { Text("Termék törlése") },
                      text = {
                        Text("Biztosan törölni szeretnéd a(z) \"${product.title}\" terméket a listádról?")
                      },
                      confirmButton = {
                        TextButton(
                          onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteProduct()
                          },
                          colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                          ),
                        ) {
                          Text("Törlés")
                        }
                      },
                      dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                          Text("Mégse")
                        }
                      },
                    )
                  }

                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  ) {
                    IconButton(
                      onClick = { showDeleteConfirmDialog = true },
                      modifier =
                        Modifier.minimumInteractiveComponentSize().testTag("details_delete_button"),
                    ) {
                      Icon(
                        imageVector = WishDropIcons.Delete,
                        contentDescription = "Termék törlése",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
              }
            }

            item(key = "hero_image", contentType = "hero_image") {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val galleryImages = remember(product.imageKey, product.additionalImages) {
                  (listOf(product.imageKey) + product.additionalImages).distinct().filter { it.isNotBlank() }
                }
                var selectedImageIndex by remember { mutableStateOf(0) }
                val currentImage = galleryImages.getOrElse(selectedImageIndex) { product.imageKey }

                WishProductArtwork(
                  imageKey = currentImage,
                  productTitle = product.title,
                  shape = WishDropDesign.radii.extraLarge,
                  modifier = Modifier.fillMaxWidth().height(268.dp).testTag("details_hero_image"),
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (galleryImages.size > 1) {
                    LazyRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f, fill = false),
                    ) {
                      itemsIndexed(galleryImages) { idx, imgKey ->
                        Box(
                          modifier = Modifier
                            .size(52.dp)
                            .clip(WishDropDesign.radii.small)
                            .border(
                              width = if (idx == selectedImageIndex) 2.dp else 0.dp,
                              color = if (idx == selectedImageIndex) MaterialTheme.colorScheme.primary else Color.Transparent,
                              shape = WishDropDesign.radii.small,
                            )
                            .clickable { selectedImageIndex = idx }
                        ) {
                          WishProductArtwork(
                            imageKey = imgKey,
                            productTitle = product.title,
                            modifier = Modifier.fillMaxSize(),
                          )
                        }
                      }
                    }
                  } else {
                    Spacer(modifier = Modifier.width(4.dp))
                  }

                  val photoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                  ) { uri ->
                    if (uri != null) {
                      onAddProductPhoto(uri.toString())
                    }
                  }

                  OutlinedButton(
                    onClick = {
                      photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Add,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kép hozzáadása")
                  }
                }
              }
            }

            item(key = "title_and_price", contentType = "price_hero") {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                  text = product.store,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = product.title,
                  style = MaterialTheme.typography.headlineLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.semantics { heading() },
                )

                PixelBentoCard(
                  modifier = Modifier.fillMaxWidth().testTag("details_price_block"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = stringResource(R.string.details_current_price),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                      ) {
                        Text(
                          text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
                          style = WishDropDesign.numericTypography.priceHero,
                          color = MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                        )
                        if (product.originalPriceHuf > product.currentPriceHuf) {
                          Text(
                            text = CurrencyFormatter.formatHuf(product.originalPriceHuf),
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                          )
                        }
                      }
                      PriceDropBadge(
                        percentChange = product.priceChangePercent,
                        savingsHuf = product.savingsHuf,
                      )
                    }

                    Button(
                      onClick = onTogglePurchased,
                      modifier =
                        Modifier.fillMaxWidth()
                          .minimumInteractiveComponentSize()
                          .testTag("details_purchase_button"),
                      colors =
                        if (product.isPurchased) {
                          ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                          )
                        } else {
                          ButtonDefaults.buttonColors()
                        },
                    ) {
                      Icon(
                        imageVector = WishDropIcons.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text =
                          stringResource(
                            if (product.isPurchased) R.string.details_purchased_done
                            else R.string.details_mark_purchased
                          )
                      )
                    }
                  }
                }
              }
            }

            item(key = "price_chart", contentType = "chart") {
              PriceHistoryChartCard(
                history = state.priceHistory,
                selectedPeriod = state.selectedPeriod,
                onSelectPeriod = onSelectPeriod,
              )
            }

            item(key = "ai_taxonomy", contentType = "taxonomy") {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExpressiveSectionHeader(title = stringResource(R.string.details_ai_categories))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  product.categories.forEachIndexed { idx, cat ->
                    ExpressiveChip(label = cat, colorIndex = idx)
                  }
                }

                ExpressiveSectionHeader(title = stringResource(R.string.details_ai_tags))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  product.tags.forEachIndexed { idx, tag ->
                    ExpressiveChip(label = "#$tag", colorIndex = idx + 2)
                  }
                }
              }
            }

            item(key = "recipient_detection", contentType = "recipient") {
              RecipientDetectionCard(
                recipientName = product.recipientName ?: "Attila",
                confidencePercent = product.recipientConfidence.takeIf { it > 0 } ?: 92,
                onChangeClick = { showRecipientPicker = true },
              )
            }

            item(key = "shared_list_assignment", contentType = "shared_list") {
              val assignedList = state.sharedLists.firstOrNull { it.id == product.sharedListId }
              val hiddenName =
                assignedList?.hiddenFromMemberName
                  ?: state.allRecipients.firstOrNull { it.id == product.hiddenFromMemberId }?.name
              SharedListAssignmentCard(
                sharedListTitle = assignedList?.let { "${it.emoji} ${it.title}" },
                isSecretGift =
                  assignedList?.visibility == ListVisibility.HIDDEN ||
                    !product.hiddenFromMemberId.isNullOrBlank(),
                hiddenFromMemberName = hiddenName,
                onChangeClick = { showSharedListPicker = true },
              )
            }

            item(key = "comments_section", contentType = "comments") {
              ProductCommentsCard(
                comments = state.comments,
                onAddComment = onAddComment,
              )
            }

            if (state.activityEvents.isNotEmpty()) {
              item(key = "activity_timeline", contentType = "activity") {
                ProductActivityTimelineCard(events = state.activityEvents)
              }
            }

            if (product.storeOffers.isNotEmpty()) {
              item(key = "store_comparison", contentType = "store_offers") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  ExpressiveSectionHeader(title = stringResource(R.string.details_store_comparison))
                  product.storeOffers.forEach { offer ->
                    StoreOfferRow(offer = offer)
                  }
                }
              }
            }

            if (product.alternatives.isNotEmpty()) {
              item(key = "alternatives", contentType = "alternatives") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  ExpressiveSectionHeader(
                    title = stringResource(R.string.details_alternative_products)
                  )
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                      items = product.alternatives,
                      key = { it.id },
                      contentType = { "alternative_card" },
                    ) { alt ->
                      AlternativeProductCard(
                        alternative = alt,
                        onClick = { onOpenAlternativeProduct(alt.id) },
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (showRecipientPicker) {
          AlertDialog(
            onDismissRequest = { showRecipientPicker = false },
            title = { Text(stringResource(R.string.details_assign_recipient_title)) },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.allRecipients.forEach { rec ->
                  Surface(
                    shape = WishDropDesign.radii.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier =
                      Modifier.fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) {
                          onAssignRecipient(rec.id)
                          showRecipientPicker = false
                        },
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      PersonAvatarBadge(name = rec.name, accentKey = rec.accentKey, size = 38.dp)
                      Column {
                        Text(text = rec.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                          text = rec.upcomingEventName,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                }
              }
            },
            confirmButton = {
              TextButton(
                onClick = { showRecipientPicker = false },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Text(stringResource(R.string.details_close))
              }
            },
          )
        }

        if (showSharedListPicker) {
          AlertDialog(
            onDismissRequest = { showSharedListPicker = false },
            title = { Text(stringResource(R.string.details_assign_list_title)) },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                  shape = WishDropDesign.radii.small,
                  color = MaterialTheme.colorScheme.surfaceContainerLow,
                  modifier =
                    Modifier.fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .clickable(role = Role.Button) {
                        onAssignSharedList(null)
                        showSharedListPicker = false
                      },
                ) {
                  Text(
                    text = stringResource(R.string.details_shared_list_none),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(12.dp),
                  )
                }
                state.sharedLists.forEach { list ->
                  Surface(
                    shape = WishDropDesign.radii.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier =
                      Modifier.fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) {
                          onAssignSharedList(list.id)
                          showSharedListPicker = false
                        },
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(text = list.emoji, style = MaterialTheme.typography.titleMedium)
                      Column {
                        Text(text = list.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                          text = list.subtitle,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                }
              }
            },
            confirmButton = {
              TextButton(onClick = { showSharedListPicker = false }) {
                Text(stringResource(R.string.details_close))
              }
            },
          )
        }
      }
    }
  }
}

private val PreviewProductDetailsState =
  ProductDetailsUiState.Success(
    product =
      Product(
        id = "prod_pixel_buds_2a",
        title = "Google Pixel Buds 2a",
        store = "Yettel",
        currentPriceHuf = 28_490,
        originalPriceHuf = 32_990,
        priceChangePercent = -13,
        savingsHuf = 4_500,
        imageKey = "pixel_buds",
        categories = listOf("Electronics", "Audio", "Earbuds"),
        tags = listOf("Google", "Pixel", "Bluetooth", "Android", "Wireless"),
        recipientName = "Attila",
        recipientConfidence = 96,
        sharedListId = "sl_christmas_2027",
        storeOffers =
          listOf(
            StoreOffer("Yettel", 28_490, "Free pickup", true, true),
            StoreOffer("Alza", 29_990, "AlzaBox", true, false),
            StoreOffer("MediaMarkt", 32_990, "In-store", true, false),
          ),
      ),
    comments =
      listOf(
        ProductComment(
          id = "c1",
          productId = "prod_pixel_buds_2a",
          authorMemberId = "rec_dalma",
          authorName = "Dalma",
          authorAccentKey = "coral",
          message = "Looks perfect for Christmas",
          createdAtMillis = 0L,
        ),
        ProductComment(
          id = "c2",
          productId = "prod_pixel_buds_2a",
          authorMemberId = "rec_attila",
          authorName = "Attila",
          authorAccentKey = "indigo",
          message = "Wait for Black Friday",
          createdAtMillis = 0L,
        ),
      ),
    activityEvents =
      listOf(
        ActivityEvent(
          id = "a1",
          actorMemberId = "rec_attila",
          actorName = "Attila",
          actionSummary = "Attila added Google Pixel Buds 2a",
          createdAtMillis = 0L,
        )
      ),
  )

@Preview(showBackground = true, name = "Product Details — Light")
@Composable
private fun ProductDetailsLightPreview() {
  WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
    ProductDetailsScreenContent(
      state = PreviewProductDetailsState,
      onNavigateBack = {},
      onRetry = {},
      onSelectPeriod = {},
      onTogglePurchased = {},
      onTogglePriceAlert = {},
      onAssignRecipient = {},
      onShareProduct = {},
      onOpenAlternativeProduct = {},
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Product Details — Dark",
)
@Composable
private fun ProductDetailsDarkPreview() {
  WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
    ProductDetailsScreenContent(
      state = PreviewProductDetailsState,
      onNavigateBack = {},
      onRetry = {},
      onSelectPeriod = {},
      onTogglePurchased = {},
      onTogglePriceAlert = {},
      onAssignRecipient = {},
      onShareProduct = {},
      onOpenAlternativeProduct = {},
    )
  }
}
