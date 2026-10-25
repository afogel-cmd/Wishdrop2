package com.example.feature.productdetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.format.CurrencyFormatter
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.ActivityEvent
import com.example.domain.model.AlternativeProduct
import com.example.domain.model.PricePeriod
import com.example.domain.model.PricePoint
import com.example.domain.model.ProductComment
import com.example.domain.model.StoreOffer
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
internal fun PriceHistoryChartCard(
  history: List<PricePoint>,
  selectedPeriod: PricePeriod,
  onSelectPeriod: (PricePeriod) -> Unit,
) {
  val modelProducer = remember { CartesianChartModelProducer() }
  val prices =
    remember(history) {
      if (history.size >= 2) history.map { it.priceHuf } else listOf(32_990, 31_490, 29_990, 28_490)
    }

  LaunchedEffect(prices) {
    modelProducer.runTransaction {
      lineSeries { series(prices) }
    }
  }

  val lowFormatted = CurrencyFormatter.formatHuf(prices.minOrNull() ?: 0)
  val highFormatted = CurrencyFormatter.formatHuf(prices.maxOrNull() ?: 0)
  val chartSummary =
    stringResource(R.string.details_chart_a11y, selectedPeriod.label, lowFormatted, highFormatted)

  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("details_price_chart_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.details_price_history),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.semantics { heading() },
        )
        SingleChoiceSegmentedButtonRow {
          PricePeriod.entries.forEachIndexed { idx, period ->
            SegmentedButton(
              selected = period == selectedPeriod,
              onClick = { onSelectPeriod(period) },
              shape = SegmentedButtonDefaults.itemShape(idx, PricePeriod.entries.size),
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .testTag("chart_period_${period.label.lowercase()}"),
            ) {
              Text(text = period.label, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }

      CartesianChartHost(
        chart =
          rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
          ),
        modelProducer = modelProducer,
        modifier =
          Modifier.fillMaxWidth()
            .height(190.dp)
            .semantics { contentDescription = chartSummary },
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.details_price_low, lowFormatted),
          style = WishDropDesign.numericTypography.pillMetric,
          color = WishDropDesign.colors.onSavingsContainer,
        )
        Text(
          text = stringResource(R.string.details_price_high, highFormatted),
          style = WishDropDesign.numericTypography.pillMetric,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
internal fun RecipientDetectionCard(
  recipientName: String,
  confidencePercent: Int,
  onChangeClick: () -> Unit,
) {
  Surface(
    shape = WishDropDesign.radii.large,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
    modifier = Modifier.fillMaxWidth().testTag("details_recipient_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PersonAvatarBadge(name = recipientName, accentKey = "indigo", size = 48.dp)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = WishDropIcons.Sparkle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = stringResource(R.string.details_recipient_detection),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(text = recipientName, style = MaterialTheme.typography.titleLarge)
        Text(
          text = stringResource(R.string.details_confidence, confidencePercent),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilledTonalButton(
        onClick = onChangeClick,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(stringResource(R.string.details_change_recipient))
      }
    }
  }
}

@Composable
internal fun SharedListAssignmentCard(
  sharedListTitle: String?,
  isSecretGift: Boolean,
  hiddenFromMemberName: String?,
  onChangeClick: () -> Unit,
) {
  val extended = WishDropDesign.colors
  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("details_shared_list_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = if (isSecretGift) extended.coralContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor =
          if (isSecretGift) extended.onCoralContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isSecretGift) WishDropIcons.Shield else WishDropIcons.Group,
            contentDescription = null,
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.details_shared_list_label),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = sharedListTitle ?: stringResource(R.string.details_shared_list_none),
          style = MaterialTheme.typography.titleMedium,
        )
        if (isSecretGift && !hiddenFromMemberName.isNullOrBlank()) {
          Text(
            text = "Secret Gift • Hidden from $hiddenFromMemberName",
            style = MaterialTheme.typography.labelSmall,
            color = extended.onCoralContainer,
          )
        }
      }
      FilledTonalButton(
        onClick = onChangeClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("details_change_list_button"),
      ) {
        Text(stringResource(R.string.details_change_recipient))
      }
    }
  }
}

@Composable
internal fun StoreOfferRow(offer: StoreOffer) {
  val extended = WishDropDesign.colors
  Surface(
    shape = WishDropDesign.radii.medium,
    color =
      if (offer.isBestOffer) extended.savingsContainer.copy(alpha = 0.45f)
      else MaterialTheme.colorScheme.surfaceContainerLow,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (offer.isBestOffer) extended.onSavingsContainer.copy(alpha = 0.25f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
      ),
    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = WishDropIcons.Store,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(text = offer.storeName, style = MaterialTheme.typography.titleMedium)
            if (offer.isBestOffer) {
              Surface(
                shape = CircleShape,
                color = extended.savingsContainer,
                contentColor = extended.onSavingsContainer,
              ) {
                Text(
                  text = stringResource(R.string.details_best_price),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = offer.deliveryNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Text(
        text = CurrencyFormatter.formatHuf(offer.priceHuf),
        style = WishDropDesign.numericTypography.priceMedium,
      )
    }
  }
}

@Composable
internal fun AlternativeProductCard(
  alternative: AlternativeProduct,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier = Modifier.width(196.dp).testTag("details_alt_${alternative.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      WishProductArtwork(
        imageKey = alternative.imageKey,
        productTitle = alternative.title,
        shape = WishDropDesign.radii.small,
        modifier = Modifier.fillMaxWidth().height(110.dp),
      )
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text = alternative.highlight,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
      Text(
        text = alternative.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = CurrencyFormatter.formatHuf(alternative.priceHuf),
        style = WishDropDesign.numericTypography.priceMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
internal fun ProductCommentsCard(
  comments: List<ProductComment>,
  onAddComment: (String) -> Unit,
) {
  var draftMessage by rememberSaveable { mutableStateOf("") }

  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("details_comments_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      ExpressiveSectionHeader(
        title = stringResource(R.string.details_comments_title),
        subtitle = stringResource(R.string.details_comments_sub),
      )

      if (comments.isEmpty()) {
        Text(
          text = stringResource(R.string.details_comments_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        comments.forEach { comment ->
          Surface(
            shape = WishDropDesign.radii.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag("product_comment_${comment.id}"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
            ) {
              PersonAvatarBadge(
                name = comment.authorName,
                accentKey = comment.authorAccentKey,
                size = 34.dp,
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = comment.authorName,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = comment.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = draftMessage,
          onValueChange = { draftMessage = it },
          placeholder = { Text(stringResource(R.string.details_comment_placeholder)) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("details_comment_input"),
        )
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
          IconButton(
            onClick = {
              if (draftMessage.isNotBlank()) {
                onAddComment(draftMessage)
                draftMessage = ""
              }
            },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("details_comment_send_button"),
          ) {
            Icon(
              imageVector = WishDropIcons.Send,
              contentDescription = stringResource(R.string.details_comment_send),
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun ProductActivityTimelineCard(
  events: List<ActivityEvent>,
) {
  if (events.isEmpty()) return
  PixelBentoCard(
    modifier = Modifier.fillMaxWidth().testTag("details_activity_timeline_card"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ExpressiveSectionHeader(
        title = stringResource(R.string.details_activity_title),
        subtitle = stringResource(R.string.details_activity_sub),
      )

      events.forEach { event ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PersonAvatarBadge(
            name = event.actorName,
            accentKey = event.actorAccentKey,
            size = 32.dp,
          )
          Text(
            text = event.actionSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = WishDropIcons.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}
