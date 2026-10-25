package com.example.feature.giftassistant

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.navigation.GiftAssistantRoute

fun NavGraphBuilder.giftAssistantScreen(
  onOpenProductDetails: (String) -> Unit,
) {
  composable<GiftAssistantRoute> { entry ->
    val args = entry.toRoute<GiftAssistantRoute>()
    GiftAssistantRouteScreen(
      initialRecipientId = args.initialRecipientId,
      onOpenProductDetails = onOpenProductDetails,
    )
  }
}
