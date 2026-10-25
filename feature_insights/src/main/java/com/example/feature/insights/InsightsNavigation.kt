package com.example.feature.insights

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.navigation.InsightsRoute

fun NavGraphBuilder.insightsScreen(
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: () -> Unit,
) {
  composable<InsightsRoute> {
    InsightsRouteScreen(
      onOpenProductDetails = onOpenProductDetails,
      onOpenAddProduct = onOpenAddProduct,
    )
  }
}
