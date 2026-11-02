package com.example.feature.addproduct

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.navigation.AddProductRoute

fun NavGraphBuilder.addProductScreen(
  onNavigateBack: () -> Unit,
  onProductSaved: (String) -> Unit,
) {
  composable<AddProductRoute> { entry ->
    val args = entry.toRoute<AddProductRoute>()
    AddProductRouteScreen(
      initialMode = args.initialMode,
      initialUrl = args.initialUrl,
      onNavigateBack = onNavigateBack,
      onProductSaved = onProductSaved,
    )
  }
}
