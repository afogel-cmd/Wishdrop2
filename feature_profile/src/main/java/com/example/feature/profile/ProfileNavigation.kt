package com.example.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.navigation.ProfileRoute

fun NavGraphBuilder.profileScreen(
  onOpenFamilyMember: (String) -> Unit,
  onOpenSharedList: (String) -> Unit = {},
) {
  composable<ProfileRoute> {
    ProfileRouteScreen(
      onOpenFamilyMember = onOpenFamilyMember,
      onOpenSharedList = onOpenSharedList,
    )
  }
}
