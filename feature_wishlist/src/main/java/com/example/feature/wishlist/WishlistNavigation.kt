package com.example.feature.wishlist

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.navigation.WishlistRoute

fun NavGraphBuilder.wishlistScreen(
  onOpenProductDetails: (String) -> Unit,
  onOpenAddProduct: (String) -> Unit,
) {
  composable<WishlistRoute> { entry ->
    val args = entry.toRoute<WishlistRoute>()
    WishlistRouteScreen(
      initialCategory = args.initialCategory,
      initialCollectionQuery = args.initialCollectionQuery,
      focusSearch = args.focusSearch,
      onOpenProductDetails = onOpenProductDetails,
      onOpenAddProduct = onOpenAddProduct,
    )
  }
}
