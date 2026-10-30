package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feature.addproduct.addProductScreen
import com.example.feature.giftassistant.giftAssistantScreen
import com.example.feature.home.homeScreen
import com.example.feature.insights.insightsScreen
import com.example.feature.productdetails.productDetailsScreen
import com.example.feature.profile.profileScreen
import com.example.feature.sharedlist.sharedListScreen
import com.example.feature.wishlist.wishlistScreen
import com.example.navigation.AddProductRoute
import com.example.navigation.GiftAssistantRoute
import com.example.navigation.HomeRoute
import com.example.navigation.InsightsRoute
import com.example.navigation.ProductDetailsRoute
import com.example.navigation.ProfileRoute
import com.example.navigation.TopLevelDestination
import com.example.navigation.WishDropNavMotion
import com.example.navigation.WishlistRoute
import com.example.navigation.navigateToAddProduct
import com.example.navigation.navigateToGiftAssistantFor
import com.example.navigation.navigateToProductDetails
import com.example.navigation.navigateToSharedList
import com.example.navigation.navigateToTopLevel
import com.example.navigation.navigateToWishlistSearch

@Composable
fun WishDropAppShell(
  widthSizeClass: WindowWidthSizeClass,
  navController: NavHostController = rememberNavController(),
) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
  val showNavigationChrome by
    remember(currentDestination) { derivedStateOf { isTopLevelDestination(currentDestination) } }

  Row(modifier = Modifier.fillMaxSize()) {
    if (!isCompact && showNavigationChrome) {
      NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("wishdrop_navigation_rail"),
      ) {
        TopLevelDestination.entries.forEach { destination ->
          val selected = isDestinationSelected(currentDestination, destination)
          val label = getDestinationLabel(destination)
          NavigationRailItem(
            selected = selected,
            onClick = { navController.navigateToTopLevel(destination) },
            icon = {
              Icon(
                imageVector =
                  if (selected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = label,
              )
            },
            label = {
              Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            modifier = Modifier.testTag(destination.testTag),
          )
        }
      }
    }

    Scaffold(
      modifier = Modifier.weight(1f),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        AnimatedVisibility(
          visible = isCompact && showNavigationChrome,
          enter = slideInVertically { it } + fadeIn(tween(220)),
          exit = slideOutVertically { it } + fadeOut(tween(180)),
        ) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("wishdrop_bottom_bar"),
          ) {
            TopLevelDestination.entries.forEach { destination ->
              val selected = isDestinationSelected(currentDestination, destination)
              val label = getDestinationLabel(destination)
              NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination) },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag(destination.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { with(WishDropNavMotion) { defaultEnter() } },
        exitTransition = { with(WishDropNavMotion) { defaultExit() } },
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        homeScreen(
          onOpenSearch = navController::navigateToWishlistSearch,
          onOpenProfile = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) },
          onOpenProductDetails = navController::navigateToProductDetails,
          onOpenAddProduct = navController::navigateToAddProduct,
          onOpenGiftAssistant = navController::navigateToGiftAssistantFor,
          onOpenWishlistAll = { navController.navigateToTopLevel(TopLevelDestination.WISHLIST) },
          onOpenSharedList = navController::navigateToSharedList,
        )

        giftAssistantScreen(
          onOpenProductDetails = navController::navigateToProductDetails,
        )

        wishlistScreen(
          onOpenProductDetails = navController::navigateToProductDetails,
          onOpenAddProduct = navController::navigateToAddProduct,
        )

        insightsScreen(
          onOpenProductDetails = navController::navigateToProductDetails,
          onOpenAddProduct = { navController.navigateToAddProduct("URL") },
        )

        profileScreen(
          onOpenFamilyMember = navController::navigateToGiftAssistantFor,
          onOpenSharedList = navController::navigateToSharedList,
        )

        sharedListScreen(
          onNavigateBack = { navController.popBackStack() },
          onOpenProductDetails = navController::navigateToProductDetails,
        )

        productDetailsScreen(
          onNavigateBack = { navController.popBackStack() },
          onOpenAlternativeProduct = { altId ->
            if (altId.startsWith("prod_")) {
              navController.navigateToProductDetails(altId)
            } else {
              navController.navigateToGiftAssistantFor("rec_dalma")
            }
          },
        )

        addProductScreen(
          onNavigateBack = { navController.popBackStack() },
          onProductSaved = { newProductId ->
            navController.navigate(ProductDetailsRoute(newProductId)) {
              popUpTo<AddProductRoute> { inclusive = true }
            }
          },
        )
      }
    }
  }
}

private fun isTopLevelDestination(destination: NavDestination?): Boolean {
  if (destination == null) return true
  return destination.hasRoute<HomeRoute>() ||
    destination.hasRoute<GiftAssistantRoute>() ||
    destination.hasRoute<WishlistRoute>() ||
    destination.hasRoute<InsightsRoute>() ||
    destination.hasRoute<ProfileRoute>()
}

private fun isDestinationSelected(
  destination: NavDestination?,
  item: TopLevelDestination,
): Boolean {
  if (destination == null) return item == TopLevelDestination.HOME
  return when (item) {
    TopLevelDestination.HOME -> destination.hasRoute<HomeRoute>()
    TopLevelDestination.GIFT_ASSISTANT -> destination.hasRoute<GiftAssistantRoute>()
    TopLevelDestination.WISHLIST -> destination.hasRoute<WishlistRoute>()
    TopLevelDestination.INSIGHTS -> destination.hasRoute<InsightsRoute>()
    TopLevelDestination.PROFILE -> destination.hasRoute<ProfileRoute>()
  }
}

@Composable
private fun getDestinationLabel(destination: TopLevelDestination): String =
  when (destination) {
    TopLevelDestination.HOME -> stringResource(R.string.nav_home)
    TopLevelDestination.GIFT_ASSISTANT -> stringResource(R.string.nav_gift_assistant)
    TopLevelDestination.WISHLIST -> stringResource(R.string.nav_wishlist)
    TopLevelDestination.INSIGHTS -> stringResource(R.string.nav_insights)
    TopLevelDestination.PROFILE -> stringResource(R.string.nav_profile)
  }
