package com.example.feature.productdetails

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.navigation.ProductDetailsRoute

fun NavGraphBuilder.productDetailsScreen(
  onNavigateBack: () -> Unit,
  onOpenAlternativeProduct: (String) -> Unit,
) {
  composable<ProductDetailsRoute> { entry ->
    val args = entry.toRoute<ProductDetailsRoute>()
    ProductDetailsRouteScreen(
      productId = args.productId,
      onNavigateBack = onNavigateBack,
      onOpenAlternativeProduct = onOpenAlternativeProduct,
    )
  }
}
