package com.example.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign

/**
 * Draw-phase-only shimmer modifier that avoids layout or composition invalidation during animation.
 */
fun Modifier.wishDropShimmer(shape: Shape? = null): Modifier = composed {
  val transition = rememberInfiniteTransition(label = "pixel_shimmer")
  val translateFactor by
    transition.animateFloat(
      initialValue = -0.6f,
      targetValue = 1.8f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1250, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmer_offset",
    )

  val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
  val clipped = if (shape != null) this.clip(shape) else this

  clipped.drawWithCache {
    val widthPx = size.width.coerceAtLeast(1f)
    val startX = widthPx * translateFactor
    val brush =
      Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(startX, 0f),
        end = Offset(startX + widthPx * 0.65f, size.height),
      )
    onDrawBehind { drawRect(brush = brush) }
  }
}

@Composable
fun WishDropDashboardShimmer(modifier: Modifier = Modifier) {
  val spacing = WishDropDesign.spacing
  val loadingLabel = stringResource(R.string.state_loading_content)
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .semantics { contentDescription = loadingLabel },
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = spacing.maxContentWidth)
          .padding(horizontal = spacing.lg, vertical = spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier =
              Modifier.width(148.dp)
                .height(32.dp)
                .wishDropShimmer(WishDropDesign.radii.small)
          )
          Box(
            modifier =
              Modifier.width(210.dp)
                .height(14.dp)
                .wishDropShimmer(CircleShape)
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(modifier = Modifier.size(44.dp).wishDropShimmer(CircleShape))
          Box(modifier = Modifier.size(44.dp).wishDropShimmer(CircleShape))
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(216.dp)
            .wishDropShimmer(WishDropDesign.radii.extraLarge)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
      ) {
        repeat(4) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(82.dp)
                .wishDropShimmer(WishDropDesign.radii.medium)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
      ) {
        repeat(2) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(232.dp)
                .wishDropShimmer(WishDropDesign.radii.large)
          )
        }
      }
    }
  }
}

@Composable
fun WishDropListShimmer(
  cardCount: Int = 4,
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  val loadingLabel = stringResource(R.string.state_loading_content)
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .semantics { contentDescription = loadingLabel },
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = spacing.maxContentWidth)
          .padding(horizontal = spacing.lg, vertical = spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
      Box(
        modifier =
          Modifier.width(180.dp)
            .height(34.dp)
            .wishDropShimmer(WishDropDesign.radii.small)
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(54.dp)
            .wishDropShimmer(CircleShape)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
          Box(modifier = Modifier.width(86.dp).height(36.dp).wishDropShimmer(CircleShape))
        }
      }
      repeat(cardCount) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(118.dp)
              .wishDropShimmer(WishDropDesign.radii.large)
        )
      }
    }
  }
}

@Composable
fun WishDropDetailsShimmer(modifier: Modifier = Modifier) {
  val spacing = WishDropDesign.spacing
  val loadingLabel = stringResource(R.string.state_loading_content)
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .semantics { contentDescription = loadingLabel },
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = spacing.maxContentWidth)
          .padding(horizontal = spacing.lg, vertical = spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(modifier = Modifier.size(44.dp).wishDropShimmer(CircleShape))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(modifier = Modifier.size(44.dp).wishDropShimmer(CircleShape))
          Box(modifier = Modifier.size(44.dp).wishDropShimmer(CircleShape))
        }
      }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(268.dp)
            .wishDropShimmer(WishDropDesign.radii.extraLarge)
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(126.dp)
            .wishDropShimmer(WishDropDesign.radii.large)
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(240.dp)
            .wishDropShimmer(WishDropDesign.radii.large)
      )
    }
  }
}

@Composable
fun WishDropEmptyState(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = WishDropDesign.radii.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 28.dp, vertical = 36.dp)
          .semantics(mergeDescendants = true) {},
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = WishDropDesign.radii.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(64.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onActionClick,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(text = actionLabel)
        }
      }
    }
  }
}

@Composable
fun WishDropErrorState(
  message: String,
  onRetry: () -> Unit,
  title: String = stringResource(R.string.state_error_default_title),
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = WishDropDesign.radii.extraLarge,
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.32f)),
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { liveRegion = LiveRegionMode.Polite },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.size(56.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = WishDropIcons.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      FilledTonalButton(
        onClick = onRetry,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = WishDropIcons.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.state_action_retry))
      }
    }
  }
}
