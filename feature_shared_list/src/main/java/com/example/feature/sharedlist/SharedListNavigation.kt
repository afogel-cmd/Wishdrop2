package com.example.feature.sharedlist

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.navigation.SharedListDetailsRoute

fun NavGraphBuilder.sharedListScreen(
  onNavigateBack: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
) {
  composable<SharedListDetailsRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<SharedListDetailsRoute>()
    SharedListRouteScreen(
      listId = route.listId,
      onNavigateBack = onNavigateBack,
      onOpenProductDetails = onOpenProductDetails,
    )
  }
}
