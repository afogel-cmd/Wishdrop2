package com.example.feature.insights

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.format.CurrencyFormatter
import com.example.core.preferences.ThemeMode
import com.example.designsystem.R as DesignR
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.PriceDropBadge
import com.example.designsystem.components.WishDropDashboardShimmer
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.CategoryDistribution
import com.example.domain.model.InsightsReport
import com.example.domain.model.Product
import com.example.domain.model.TagStat

@Composable
fun InsightsRouteScreen(
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: () -> Unit = {},
  viewModel: InsightsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  InsightsScreenContent(
    state = uiState,
    onRetry = viewModel::retry,
    onOpenProductDetails = onOpenProductDetails,
    onOpenAddProduct = onOpenAddProduct,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreenContent(
  state: InsightsUiState,
  onRetry: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  val extended = WishDropDesign.colors

  when (state) {
    InsightsUiState.Loading -> {
      WishDropDashboardShimmer(modifier = modifier)
    }

    is InsightsUiState.Error -> {
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

    InsightsUiState.Empty -> {
      Box(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        WishDropEmptyState(
          icon = WishDropIcons.InsightsFilled,
          title = stringResource(R.string.insights_empty_title),
          description = stringResource(R.string.insights_empty_desc),
          actionLabel = stringResource(R.string.insights_empty_action),
          onActionClick = onOpenAddProduct,
          modifier = Modifier.widthIn(max = spacing.maxSheetWidth),
        )
      }
    }

    is InsightsUiState.Success -> {
      val report = state.report
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = spacing.maxContentWidth)
              .testTag("insights_list"),
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
            Column {
              Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
              )
              Text(
                text = stringResource(R.string.insights_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          item(key = "yearly_savings", contentType = "hero") {
            Card(
              shape = WishDropDesign.radii.extraLarge,
              colors = CardDefaults.cardColors(containerColor = Color.Transparent),
              modifier =
                Modifier.fillMaxWidth()
                  .semantics(mergeDescendants = true) {}
                  .testTag("insights_savings_card"),
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(
                      Brush.linearGradient(
                        colors = listOf(extended.heroStart, extended.heroMiddle, extended.heroEnd),
                        start = Offset.Zero,
                        end = Offset(820f, 520f),
                      )
                    )
                    .padding(28.dp)
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Savings,
                      contentDescription = null,
                      tint = extended.onHero,
                    )
                    Text(
                      text = stringResource(R.string.insights_this_year),
                      style = MaterialTheme.typography.titleMedium,
                      color = extended.onHero.copy(alpha = 0.88f),
                    )
                  }
                  Text(
                    text =
                      stringResource(
                        R.string.insights_saved_suffix,
                        CurrencyFormatter.formatHuf(report.yearlySavedHuf),
                      ),
                    style = WishDropDesign.numericTypography.priceHero,
                    color = extended.onHero,
                  )
                }
              }
            }
          }

          report.topSavedProduct?.let { topProduct ->
            item(key = "top_purchase", contentType = "top_purchase") {
              PixelBentoCard(
                onClick = { onOpenProductDetails(topProduct.id) },
                modifier = Modifier.fillMaxWidth().testTag("insights_top_purchase_card"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(18.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  WishProductArtwork(
                    imageKey = topProduct.imageKey,
                    productTitle = topProduct.title,
                    shape = WishDropDesign.radii.medium,
                    modifier = Modifier.size(84.dp),
                  )
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(
                        imageVector = WishDropIcons.Trophy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Text(
                        text = stringResource(R.string.insights_top_purchase),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Text(
                      text = topProduct.title,
                      style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                      text =
                        stringResource(
                          R.string.insights_saved_suffix,
                          CurrencyFormatter.formatHuf(topProduct.savingsHuf),
                        ),
                      style = WishDropDesign.numericTypography.priceMedium,
                      color = extended.onSavingsContainer,
                    )
                  }
                }
              }
            }
          }

          if (report.categoryDistribution.isNotEmpty()) {
            item(key = "donut_chart", contentType = "donut_chart") {
              DonutCategoryChartCard(distribution = report.categoryDistribution)
            }
          }

          if (report.commonTags.isNotEmpty()) {
            item(key = "common_tags", contentType = "tag_cloud") {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpressiveSectionHeader(title = stringResource(R.string.insights_common_tags))
                PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
                  FlowRow(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    val maxCount = (report.commonTags.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                    report.commonTags.forEachIndexed { idx, tagStat ->
                      val weightRatio = tagStat.count.toFloat() / maxCount
                      val fontSize = (12f + 6f * weightRatio).sp
                      Surface(
                        shape = CircleShape,
                        color =
                          if (idx % 2 == 0) MaterialTheme.colorScheme.primaryContainer
                          else MaterialTheme.colorScheme.secondaryContainer,
                      ) {
                        Text(
                          text = "${tagStat.tag} (${tagStat.count})",
                          fontSize = fontSize,
                          fontWeight =
                            if (weightRatio > 0.65f) FontWeight.Bold else FontWeight.Medium,
                          color =
                            if (idx % 2 == 0) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          if (report.biggestPriceDrops.isNotEmpty()) {
            item(key = "biggest_drops_header", contentType = "section_header") {
              ExpressiveSectionHeader(title = stringResource(R.string.insights_biggest_drops))
            }

            itemsIndexed(
              items = report.biggestPriceDrops,
              key = { _, item -> item.id },
              contentType = { _, _ -> "ranked_drop_item" },
            ) { index, product ->
              RankedPriceDropItem(
                rank = index + 1,
                product = product,
                onClick = { onOpenProductDetails(product.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DonutCategoryChartCard(distribution: List<CategoryDistribution>) {
  val palette = WishDropDesign.colors.chartPalette
  var selectedIndex by remember { mutableIntStateOf(0) }
  var played by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { played = true }
  val sweepProgress by
    animateFloatAsState(
      targetValue = if (played) 1f else 0f,
      animationSpec = tween(durationMillis = 800),
      label = "donut_sweep",
    )

  val active = distribution.getOrNull(selectedIndex)
  val chartA11y =
    if (active != null) {
      stringResource(R.string.insights_donut_a11y, active.categoryName, active.percentage)
    } else {
      stringResource(R.string.insights_category_distribution)
    }

  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("insights_donut_chart"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      ExpressiveSectionHeader(
        title = stringResource(R.string.insights_category_distribution),
        subtitle = stringResource(R.string.insights_category_sub),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Box(
          modifier =
            Modifier.size(164.dp).semantics(mergeDescendants = true) {
              contentDescription = chartA11y
            },
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            var startAngle = -90f
            val totalPct = distribution.sumOf { it.percentage }.coerceAtLeast(1)
            distribution.forEachIndexed { index, item ->
              val sweep = (item.percentage.toFloat() / totalPct) * 360f * sweepProgress
              val strokeWidth = if (index == selectedIndex) 38f else 28f
              drawArc(
                color = palette[index.mod(palette.size)],
                startAngle = startAngle,
                sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
              )
              startAngle += sweep
            }
          }
          if (active != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${active.percentage}%",
                style = WishDropDesign.numericTypography.heroCounter.copy(fontSize = 26.sp),
              )
              Text(
                text = active.categoryName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          distribution.forEachIndexed { idx, item ->
            val color = palette[idx.mod(palette.size)]
            val isSelected = idx == selectedIndex
            val selectedLabel =
              stringResource(
                if (isSelected) DesignR.string.state_selected else DesignR.string.state_not_selected
              )
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clip(WishDropDesign.radii.small)
                  .clickable(role = Role.Tab) { selectedIndex = idx }
                  .semantics { stateDescription = selectedLabel }
                  .padding(vertical = 8.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                Text(
                  text = item.categoryName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
              }
              Text(
                text = "${item.percentage}%",
                style = WishDropDesign.numericTypography.pillMetric,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankedPriceDropItem(
  rank: Int,
  product: Product,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier = Modifier.fillMaxWidth().testTag("insights_drop_rank_$rank"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "#$rank",
            style = WishDropDesign.numericTypography.pillMetric,
          )
        }
      }
      WishProductArtwork(
        imageKey = product.imageKey,
        productTitle = product.title,
        shape = WishDropDesign.radii.small,
        modifier = Modifier.size(56.dp),
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = product.title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
            style = WishDropDesign.numericTypography.priceMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
          )
          PriceDropBadge(
            percentChange = product.priceChangePercent,
            savingsHuf = product.savingsHuf,
          )
        }
      }
    }
  }
}

private val PreviewInsightsState =
  InsightsUiState.Success(
    report =
      InsightsReport(
        yearlySavedHuf = 47_350,
        activePotentialSavingsHuf = 18_400,
        topSavedProduct =
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
            tags = listOf("Google"),
          ),
        categoryDistribution =
          listOf(
            CategoryDistribution("Electronics", 42, 12, 420_000, 0),
            CategoryDistribution("Gift", 31, 9, 180_000, 1),
            CategoryDistribution("Home", 18, 5, 95_000, 2),
            CategoryDistribution("Other", 9, 2, 30_000, 3),
          ),
        commonTags =
          listOf(
            TagStat("Google", 8),
            TagStat("Christmas", 10),
            TagStat("Pixel", 7),
            TagStat("Home Decor", 6),
          ),
        biggestPriceDrops = emptyList(),
      )
  )

@Preview(showBackground = true, name = "Insights — Light")
@Composable
private fun InsightsLightPreview() {
  WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
    InsightsScreenContent(
      state = PreviewInsightsState,
      onRetry = {},
      onOpenProductDetails = {},
      onOpenAddProduct = {},
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Insights — Dark",
)
@Composable
private fun InsightsDarkPreview() {
  WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
    InsightsScreenContent(
      state = PreviewInsightsState,
      onRetry = {},
      onOpenProductDetails = {},
      onOpenAddProduct = {},
    )
  }
}
