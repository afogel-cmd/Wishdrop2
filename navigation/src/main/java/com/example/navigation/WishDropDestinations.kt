package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.designsystem.icon.WishDropIcons
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

@Serializable data class GiftAssistantRoute(val initialRecipientId: String? = null)

@Serializable
data class WishlistRoute(
  val initialCategory: String? = null,
  val initialCollectionQuery: String? = null,
  val focusSearch: Boolean = false,
)

@Serializable data object InsightsRoute

@Serializable data object ProfileRoute

@Serializable data class ProductDetailsRoute(val productId: String)

@Serializable data class AddProductRoute(val initialMode: String = "URL")

@Serializable data class SharedListDetailsRoute(val listId: String)

enum class TopLevelDestination(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME(
    label = "Home",
    selectedIcon = WishDropIcons.HomeFilled,
    unselectedIcon = WishDropIcons.HomeOutlined,
    testTag = "nav_home_tab",
  ),
  GIFT_ASSISTANT(
    label = "Gifts",
    selectedIcon = WishDropIcons.GiftFilled,
    unselectedIcon = WishDropIcons.GiftOutlined,
    testTag = "nav_gift_assistant_tab",
  ),
  WISHLIST(
    label = "Wishlist",
    selectedIcon = WishDropIcons.WishlistFilled,
    unselectedIcon = WishDropIcons.WishlistOutlined,
    testTag = "nav_wishlist_tab",
  ),
  INSIGHTS(
    label = "Insights",
    selectedIcon = WishDropIcons.InsightsFilled,
    unselectedIcon = WishDropIcons.InsightsOutlined,
    testTag = "nav_insights_tab",
  ),
  PROFILE(
    label = "Profile",
    selectedIcon = WishDropIcons.ProfileFilled,
    unselectedIcon = WishDropIcons.ProfileOutlined,
    testTag = "nav_profile_tab",
  ),
}

fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
  val route: Any =
    when (destination) {
      TopLevelDestination.HOME -> HomeRoute
      TopLevelDestination.GIFT_ASSISTANT -> GiftAssistantRoute()
      TopLevelDestination.WISHLIST -> WishlistRoute()
      TopLevelDestination.INSIGHTS -> InsightsRoute
      TopLevelDestination.PROFILE -> ProfileRoute
    }
  navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

fun NavController.navigateToProductDetails(productId: String) {
  navigate(ProductDetailsRoute(productId)) { launchSingleTop = true }
}

fun NavController.navigateToSharedList(listId: String) {
  navigate(SharedListDetailsRoute(listId)) { launchSingleTop = true }
}

fun NavController.navigateToAddProduct(mode: String = "URL") {
  navigate(AddProductRoute(mode)) { launchSingleTop = true }
}

fun NavController.navigateToGiftAssistantFor(recipientId: String?) {
  navigate(GiftAssistantRoute(recipientId)) { launchSingleTop = true }
}

fun NavController.navigateToWishlistSearch() {
  navigate(WishlistRoute(focusSearch = true)) { launchSingleTop = true }
}

object WishDropNavMotion {
  fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnter(): EnterTransition =
    fadeIn(tween(240, easing = FastOutSlowInEasing)) +
      scaleIn(initialScale = 0.97f, animationSpec = tween(240, easing = FastOutSlowInEasing))

  fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExit(): ExitTransition =
    fadeOut(tween(180, easing = FastOutSlowInEasing))
}
