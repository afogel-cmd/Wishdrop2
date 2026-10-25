package com.example.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.navigation.HomeRoute

fun NavGraphBuilder.homeScreen(
  onOpenSearch: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
  onOpenGiftAssistant: (String?) -> Unit,
  onOpenWishlistAll: () -> Unit,
  onOpenSharedList: (String) -> Unit,
) {
  composable<HomeRoute> {
    HomeRouteScreen(
      onOpenSearch = onOpenSearch,
      onOpenProfile = onOpenProfile,
      onOpenProductDetails = onOpenProductDetails,
      onOpenAddProduct = onOpenAddProduct,
      onOpenGiftAssistant = onOpenGiftAssistant,
      onOpenWishlistAll = onOpenWishlistAll,
      onOpenSharedList = onOpenSharedList,
    )
  }
}
