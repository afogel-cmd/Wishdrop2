package com.example.feature.giftassistant

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.format.CurrencyFormatter
import com.example.designsystem.components.AiMatchBadge
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.domain.model.AiRecommendation
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import com.example.domain.model.Product

@Composable
internal fun PersonCarouselCard(
  recipient: GiftRecipient,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val scale by
    animateFloatAsState(
      targetValue = if (selected) 1.02f else 0.98f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "person_scale",
    )
  val containerColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
      label = "person_bg",
    )

  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.large,
    containerColor = containerColor,
    modifier =
      Modifier.width(176.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .semantics(mergeDescendants = true) {
          stateDescription = if (selected) "Selected" else "Not selected"
        }
        .testTag("gift_person_card_${recipient.id}"),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        PersonAvatarBadge(
          name = recipient.name,
          accentKey = recipient.accentKey,
          size = 52.dp,
        )
        Surface(
          shape = CircleShape,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Text(
            text = "${recipient.daysUntilEvent}d",
            style = WishDropDesign.numericTypography.pillMetric,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = recipient.name,
          style = MaterialTheme.typography.titleLarge,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = recipient.upcomingEventName,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
internal fun GiftProgressExpressiveStepper(
  currentStage: GiftStage,
  onStageSelected: (GiftStage) -> Unit,
  modifier: Modifier = Modifier,
) {
  val stages = GiftStage.entries
  PixelBentoCard(
    shape = WishDropDesign.radii.large,
    modifier = modifier.fillMaxWidth().testTag("gift_progress_stepper"),
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
        Column {
          Text(
            text = stringResource(R.string.gift_progress_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(R.string.gift_progress_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = CircleShape,
          color = WishDropDesign.colors.savingsContainer,
          contentColor = WishDropDesign.colors.onSavingsContainer,
        ) {
          Text(
            text = currentStage.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        stages.forEachIndexed { idx, stage ->
          val isCompleted = stage.stepIndex <= currentStage.stepIndex
          val isCurrent = stage == currentStage
          val circleBg by
            animateColorAsState(
              targetValue =
                when {
                  isCurrent -> MaterialTheme.colorScheme.primary
                  isCompleted -> MaterialTheme.colorScheme.primaryContainer
                  else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
              label = "step_bg_$idx",
            )
          val circleFg =
            when {
              isCurrent -> MaterialTheme.colorScheme.onPrimary
              isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

          Column(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clip(WishDropDesign.radii.small)
                .clickable(role = Role.Tab) { onStageSelected(stage) }
                .padding(vertical = 4.dp)
                .semantics {
                  stateDescription = if (isCurrent) "Current stage" else stage.label
                }
                .testTag("gift_step_${stage.name.lowercase()}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier.size(30.dp).clip(CircleShape).background(circleBg),
              contentAlignment = Alignment.Center,
            ) {
              if (stage.stepIndex < currentStage.stepIndex) {
                Icon(
                  imageVector = WishDropIcons.Check,
                  contentDescription = null,
                  tint = circleFg,
                  modifier = Modifier.size(16.dp),
                )
              } else {
                Text(
                  text = "${idx + 1}",
                  style = WishDropDesign.numericTypography.pillMetric,
                  color = circleFg,
                )
              }
            }
            Text(
              text = stage.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
              color =
                if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun SavedIdeaMiniCard(
  product: Product,
  onClick: () -> Unit,
) {
  PixelBentoCard(
    onClick = onClick,
    shape = WishDropDesign.radii.medium,
    modifier =
      Modifier.width(208.dp)
        .semantics(mergeDescendants = true) {}
        .testTag("gift_saved_idea_${product.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      WishProductArtwork(
        imageKey = product.imageKey,
        productTitle = product.title,
        shape = WishDropDesign.radii.small,
        modifier = Modifier.fillMaxWidth().height(112.dp),
      )
      Text(
        text = product.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = CurrencyFormatter.formatHuf(product.currentPriceHuf),
        style = WishDropDesign.numericTypography.pillMetric,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
internal fun AiGiftRecommendationCard(
  recommendation: AiRecommendation,
  onSaveOrView: () -> Unit,
) {
  val alreadySaved = recommendation.linkedProductId != null
  PixelBentoCard(
    shape = WishDropDesign.radii.large,
    modifier = Modifier.fillMaxWidth().testTag("gift_ai_rec_${recommendation.id}"),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WishProductArtwork(
          imageKey = recommendation.imageKey,
          productTitle = recommendation.title,
          shape = WishDropDesign.radii.medium,
          modifier = Modifier.size(96.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AiMatchBadge(matchPercentage = recommendation.matchPercentage)
          Text(
            text = recommendation.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "${recommendation.store} • ${CurrencyFormatter.formatHuf(recommendation.priceHuf)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Surface(
        shape = WishDropDesign.radii.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Text(
          text = recommendation.reason,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        if (alreadySaved) {
          FilledTonalButton(
            onClick = onSaveOrView,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("gift_rec_view_${recommendation.id}"),
          ) {
            Icon(
              imageVector = WishDropIcons.BookmarkSaved,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.gift_view_in_wishlist))
          }
        } else {
          Button(
            onClick = onSaveOrView,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("gift_rec_save_${recommendation.id}"),
          ) {
            Icon(
              imageVector = WishDropIcons.BookmarkAdd,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.gift_save_to_wishlist))
          }
        }
      }
    }
  }
}
