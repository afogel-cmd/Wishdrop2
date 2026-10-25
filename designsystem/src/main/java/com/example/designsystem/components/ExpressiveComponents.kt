package com.example.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.format.CurrencyFormatter
import com.example.designsystem.R
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2026 Pixel Bento Card container with subtle tonal surface elevation and hairline border.
 */
@Composable
fun PixelBentoCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  shape: Shape = WishDropDesign.radii.large,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  content: @Composable ColumnScope.() -> Unit,
) {
  val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = shape,
      color = containerColor,
      border = border,
      modifier = modifier,
    ) {
      Column(content = content)
    }
  } else {
    Surface(
      shape = shape,
      color = containerColor,
      border = border,
      modifier = modifier,
    ) {
      Column(content = content)
    }
  }
}

@Composable
fun PriceDropBadge(
  percentChange: Int,
  savingsHuf: Int = 0,
  modifier: Modifier = Modifier,
) {
  if (percentChange >= 0 && savingsHuf <= 0) return
  val extended = WishDropDesign.colors
  val dropA11y =
    if (percentChange < 0) {
      stringResource(R.string.cd_price_drop_badge, abs(percentChange))
    } else {
      ""
    }
  Row(
    modifier = modifier.semantics(mergeDescendants = true) {},
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (percentChange < 0) {
      Surface(
        color = extended.savingsContainer,
        contentColor = extended.onSavingsContainer,
        shape = CircleShape,
      ) {
        Text(
          text = CurrencyFormatter.formatPercentDrop(percentChange),
          style = WishDropDesign.numericTypography.pillMetric,
          modifier =
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp).semantics {
              stateDescription = dropA11y
            },
        )
      }
    }
    if (savingsHuf > 0) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
      ) {
        Text(
          text = stringResource(R.string.label_save_amount, CurrencyFormatter.formatHuf(savingsHuf)),
          style = WishDropDesign.numericTypography.pillMetric,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
fun AiMatchBadge(
  matchPercentage: Int,
  modifier: Modifier = Modifier,
) {
  val extended = WishDropDesign.colors
  Surface(
    modifier = modifier.semantics(mergeDescendants = true) {},
    color = extended.amberContainer,
    contentColor = extended.onAmberContainer,
    shape = CircleShape,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = WishDropIcons.Sparkle,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = stringResource(R.string.label_match_percent, matchPercentage),
        style = WishDropDesign.numericTypography.pillMetric,
      )
    }
  }
}

@Composable
fun ExpressiveChip(
  label: String,
  selected: Boolean = false,
  colorIndex: Int = 0,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val extended = WishDropDesign.colors
  val pair =
    when (colorIndex.mod(5)) {
      0 -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
      1 -> Pair(extended.coralContainer, extended.onCoralContainer)
      2 -> Pair(extended.savingsContainer, extended.onSavingsContainer)
      3 -> Pair(extended.amberContainer, extended.onAmberContainer)
      else -> Pair(extended.skyContainer, extended.onSkyContainer)
    }

  val containerColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary
        else pair.first.copy(alpha = 0.88f),
      label = "chip_bg",
    )
  val contentColor by
    animateColorAsState(
      targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else pair.second,
      label = "chip_fg",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (selected) 1.03f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "chip_scale",
    )
  val selectedStateText =
    stringResource(if (selected) R.string.state_selected else R.string.state_not_selected)

  Surface(
    modifier =
      modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .then(
          if (onClick != null) {
            Modifier.minimumInteractiveComponentSize()
              .clip(CircleShape)
              .clickable(role = Role.Tab, onClick = onClick)
              .semantics { stateDescription = selectedStateText }
          } else {
            Modifier
          }
        ),
    shape = CircleShape,
    color = containerColor,
    contentColor = contentColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (selected) {
        Icon(
          imageVector = WishDropIcons.Check,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun ExpressiveSectionHeader(
  title: String,
  subtitle: String? = null,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (actionLabel != null && onActionClick != null) {
      TextButton(
        onClick = onActionClick,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun ExpressiveAnalyzingOrb(
  stepDescription: String,
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "orb_transition")
  val rotation by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Restart),
      label = "orb_rotation",
    )
  val pulse by
    infinite.animateFloat(
      initialValue = 0.93f,
      targetValue = 1.07f,
      animationSpec =
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "orb_pulse",
    )

  val extended = WishDropDesign.colors
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(24.dp)
        .semantics { liveRegion = LiveRegionMode.Polite },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier.size(152.dp).graphicsLayer {
          scaleX = pulse
          scaleY = pulse
        },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        rotate(rotation) {
          val path = Path()
          val cx = size.width / 2f
          val cy = size.height / 2f
          val baseR = size.minDimension * 0.42f
          val waveR = size.minDimension * 0.06f
          val lobes = 8
          val steps = 120
          for (i in 0..steps) {
            val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
            val r = baseR + waveR * sin(lobes * theta)
            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          path.close()
          drawPath(
            path = path,
            brush =
              Brush.linearGradient(
                listOf(extended.heroStart, extended.heroMiddle, extended.heroEnd)
              ),
          )
        }
      }
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = WishDropIcons.Sparkle,
          contentDescription = null,
          tint = extended.onHero,
          modifier = Modifier.size(32.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.label_analyzing_product),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stepDescription,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(20.dp))
    LinearProgressIndicator(
      progress = { progress.coerceIn(0.08f, 1f) },
      modifier = Modifier.width(232.dp).height(8.dp).clip(CircleShape),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
  }
}
