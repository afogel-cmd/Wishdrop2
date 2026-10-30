package com.example.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.designsystem.R
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign

@Composable
fun WishProductArtwork(
  imageKey: String,
  productTitle: String,
  modifier: Modifier = Modifier,
  shape: Shape = WishDropDesign.radii.medium,
) {
  val context = LocalContext.current
  val contentDescription = stringResource(R.string.cd_product_image, productTitle)
  val localDrawableRes =
    remember(imageKey) {
      when (imageKey) {
        "pixel_buds" -> R.drawable.img_pixel_buds
        "pixel_watch" -> R.drawable.img_pixel_watch
        "lego_orchid" -> R.drawable.img_lego_orchid
        "nothing_ear" -> R.drawable.img_nothing_ear
        else -> null
      }
    }

  val isNetworkOrLocalUrl = remember(imageKey) {
    imageKey.startsWith("http://") ||
      imageKey.startsWith("https://") ||
      imageKey.startsWith("//") ||
      imageKey.startsWith("content://") ||
      imageKey.startsWith("file://") ||
      imageKey.startsWith("/")
  }

  Box(modifier = modifier.clip(shape), contentAlignment = Alignment.Center) {
    when {
      localDrawableRes != null -> {
        Image(
          painter = painterResource(id = localDrawableRes),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      isNetworkOrLocalUrl -> {
        val safeImageUrl = remember(imageKey) {
          if (imageKey.startsWith("//")) "https:$imageKey" else imageKey
        }
        val imageRequest = remember(safeImageUrl) {
          ImageRequest.Builder(context)
            .data(safeImageUrl)
            .crossfade(true)
            .listener(
              onStart = {
                android.util.Log.d("WishProductArtwork", "Coil loading: $safeImageUrl")
              },
              onSuccess = { _, _ ->
                android.util.Log.d("WishProductArtwork", "Coil loaded: $safeImageUrl")
              },
              onError = { _, result ->
                android.util.Log.e("WishProductArtwork", "Coil error for $safeImageUrl: ${result.throwable.message}", result.throwable)
              }
            )
            .build()
        }
        SubcomposeAsyncImage(
          model = imageRequest,
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
          loading = {
            StudioIllustrationBackdrop(imageKey = "default", title = productTitle)
          },
          error = {
            StudioIllustrationBackdrop(imageKey = imageKey, title = productTitle)
          },
        )
      }
      else -> {
        StudioIllustrationBackdrop(imageKey = imageKey, title = productTitle)
      }
    }
  }
}

@Composable
private fun StudioIllustrationBackdrop(imageKey: String, title: String) {
  val resolvedKey = remember(imageKey, title) {
    if (imageKey == "art_book" || imageKey == "default" || imageKey.isBlank() || imageKey.startsWith("http")) {
      val normalized = title.lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ö", "o")
        .replace("ő", "o")
        .replace("ú", "u")
        .replace("ü", "u")
        .replace("ű", "u")
      when {
        normalized.contains("szaritogep") || normalized.contains("mosogep") || normalized.contains("dryer") || normalized.contains("washer") || normalized.contains("bosch") || normalized.contains("hoszivattyus") -> "dryer"
        normalized.contains("buds") || normalized.contains("fulhallgato") -> "pixel_buds"
        normalized.contains("watch") || normalized.contains("okosora") -> "pixel_watch"
        normalized.contains("bonsai") -> "lego_bonsai"
        normalized.contains("orchid") -> "lego_orchid"
        normalized.contains("nothing") -> "nothing_ear"
        normalized.contains("coffee") || normalized.contains("kettle") || normalized.contains("kave") -> "coffee_kettle"
        else -> imageKey
      }
    } else {
      imageKey
    }
  }
  val spec = remember(resolvedKey) { studioSpecFor(resolvedKey) }
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = listOf(spec.bgStart, spec.bgEnd),
            start = Offset.Zero,
            end = Offset(400f, 400f),
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = spec.accentGlow.copy(alpha = 0.35f),
        radius = size.minDimension * 0.44f,
        center = Offset(size.width * 0.72f, size.height * 0.28f),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.42f),
        topLeft = Offset(size.width * 0.18f, size.height * 0.68f),
        size = Size(size.width * 0.64f, size.height * 0.18f),
        cornerRadius = CornerRadius(24f, 24f),
      )
    }
    Box(
      modifier =
        Modifier.size(56.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.88f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = spec.icon,
        contentDescription = title,
        tint = spec.iconTint,
        modifier = Modifier.size(30.dp),
      )
    }
  }
}

@Immutable
private data class StudioArtSpec(
  val bgStart: Color,
  val bgEnd: Color,
  val accentGlow: Color,
  val iconTint: Color,
  val icon: ImageVector,
)

private fun studioSpecFor(imageKey: String): StudioArtSpec =
  when (imageKey) {
    "lego_bonsai" ->
      StudioArtSpec(
        bgStart = Color(0xFFDFF6EA),
        bgEnd = Color(0xFFB5E6CE),
        accentGlow = Color(0xFFFF9DB5),
        iconTint = Color(0xFF136F4B),
        icon = WishDropIcons.Botanical,
      )
    "wool_cap" ->
      StudioArtSpec(
        bgStart = Color(0xFFF7E7D6),
        bgEnd = Color(0xFFE7C9AC),
        accentGlow = Color(0xFFD69E6B),
        iconTint = Color(0xFF5D3A1A),
        icon = WishDropIcons.Apparel,
      )
    "pixel_stand" ->
      StudioArtSpec(
        bgStart = Color(0xFFE5E3FF),
        bgEnd = Color(0xFFC9C4FF),
        accentGlow = Color(0xFF6C63FF),
        iconTint = Color(0xFF3A2FA0),
        icon = WishDropIcons.Dock,
      )
    "anker_buds",
    "samsung_buds" ->
      StudioArtSpec(
        bgStart = Color(0xFFDCEBFF),
        bgEnd = Color(0xFFBDD8FF),
        accentGlow = Color(0xFF5A9CFF),
        iconTint = Color(0xFF174A8B),
        icon = WishDropIcons.Headphones,
      )
    "coffee_kettle" ->
      StudioArtSpec(
        bgStart = Color(0xFFFBE6DA),
        bgEnd = Color(0xFFF3C6B0),
        accentGlow = Color(0xFFE07A5F),
        iconTint = Color(0xFF6D2E1C),
        icon = WishDropIcons.Coffee,
      )
    "art_book" ->
      StudioArtSpec(
        bgStart = Color(0xFFFFE3EC),
        bgEnd = Color(0xFFFFC4D8),
        accentGlow = Color(0xFFFF6F91),
        iconTint = Color(0xFF7A1E3E),
        icon = WishDropIcons.Book,
      )
    "e_reader" ->
      StudioArtSpec(
        bgStart = Color(0xFFE0F4F4),
        bgEnd = Color(0xFFBBE5E5),
        accentGlow = Color(0xFF00A896),
        iconTint = Color(0xFF065A52),
        icon = WishDropIcons.EReader,
      )
    "camera_instant" ->
      StudioArtSpec(
        bgStart = Color(0xFFFFF0C9),
        bgEnd = Color(0xFFFFDA8A),
        accentGlow = Color(0xFFFF9F1C),
        iconTint = Color(0xFF6E4400),
        icon = WishDropIcons.Camera,
      )
    "tech_accessory" ->
      StudioArtSpec(
        bgStart = Color(0xFFE4E9F7),
        bgEnd = Color(0xFFC5D0EE),
        accentGlow = Color(0xFF5C7CFA),
        iconTint = Color(0xFF233876),
        icon = WishDropIcons.Accessory,
      )
    "dryer",
    "washer",
    "appliance" ->
      StudioArtSpec(
        bgStart = Color(0xFFE8F0FE),
        bgEnd = Color(0xFFD2E3FC),
        accentGlow = Color(0xFF4285F4),
        iconTint = Color(0xFF1967D2),
        icon = WishDropIcons.Washer,
      )
    else ->
      StudioArtSpec(
        bgStart = Color(0xFFF6E6DC),
        bgEnd = Color(0xFFE8C8B5),
        accentGlow = Color(0xFFD98A6C),
        iconTint = Color(0xFF6B3824),
        icon = WishDropIcons.Living,
      )
  }

@Composable
fun PersonAvatarBadge(
  name: String,
  accentKey: String,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
  size: Dp = 52.dp,
) {
  val avatarDescription = stringResource(R.string.cd_person_avatar, name)
  val palette =
    remember(accentKey) {
      when (accentKey) {
        "coral" -> Pair(Color(0xFFFFD8D0), Color(0xFF7D2415))
        "mint" -> Pair(Color(0xFFC6F6E5), Color(0xFF07543B))
        "lavender" -> Pair(Color(0xFFE9D8FD), Color(0xFF4C1D95))
        else -> Pair(Color(0xFFDCE2FF), Color(0xFF28208C))
      }
    }

  if (!imageUrl.isNullOrBlank()) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
      ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(true)
        .allowHardware(false)
        .build()
    }
    SubcomposeAsyncImage(
      model = imageRequest,
      contentDescription = avatarDescription,
      contentScale = ContentScale.Crop,
      modifier =
        modifier
          .size(size)
          .clip(CircleShape)
          .background(palette.first),
      error = {
        val initials = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "W" }
        Box(
          modifier = modifier.size(size).clip(CircleShape).background(palette.first),
          contentAlignment = Alignment.Center,
        ) {
          Text(text = initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.second)
        }
      },
    )
  } else {
    val initials =
      remember(name) {
        name
          .trim()
          .split(" ")
          .filter { it.isNotEmpty() }
          .take(2)
          .joinToString("") { it.first().uppercaseChar().toString() }
          .ifEmpty { "W" }
      }

    Box(
      modifier =
        modifier
          .size(size)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(palette.first, palette.first.copy(alpha = 0.78f))
            )
          )
          .padding(4.dp)
          .semantics { contentDescription = avatarDescription },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style =
          if (size >= 56.dp) MaterialTheme.typography.titleLarge
          else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = palette.second,
      )
    }
  }
}
