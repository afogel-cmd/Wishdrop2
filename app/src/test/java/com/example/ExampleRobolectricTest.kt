package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.format.CurrencyFormatter
import com.example.core.preferences.ThemeMode
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.designsystem.components.WishDropEmptyState
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.ActivityEvent
import com.example.domain.model.DashboardSummary
import com.example.domain.model.GiftStage
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.Product
import com.example.domain.model.ProductComment
import com.example.domain.model.SharedListDetails
import com.example.domain.model.SharedListMember
import com.example.domain.model.SharedListSummary
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeUiState
import com.example.feature.productdetails.ProductDetailsScreenContent
import com.example.feature.productdetails.ProductDetailsUiState
import com.example.feature.sharedlist.SharedListScreenContent
import com.example.feature.sharedlist.SharedListUiState
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app name and tagline match WishDrop specification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("WishDrop", context.getString(R.string.app_name))
    assertEquals(
      "Save. Organize. Share. Gift Smarter.",
      context.getString(R.string.app_tagline),
    )
    assertEquals(
      listOf("Idea", "Considering", "Watching", "Ordered", "Wrapped", "Delivered"),
      GiftStage.entries.map { it.label },
    )
  }

  @Test
  fun `currency formatter formats Hungarian Forint with spaces`() {
    assertEquals("28 490 Ft", CurrencyFormatter.formatHuf(28_490))
    assertEquals("18 400 Ft", CurrencyFormatter.formatHuf(18_400))
    assertEquals("47 350 Ft", CurrencyFormatter.formatHuf(47_350))
    assertEquals("▼13%", CurrencyFormatter.formatPercentDrop(-13))
  }

  @Test
  fun `asDataResult emits Loading then Success or Error`() = runTest {
    val successEmissions = flowOf(42).asDataResult().toList()
    assertEquals(listOf(DataResult.Loading, DataResult.Success(42)), successEmissions)

    val failureEmissions =
      flow<Int> { throw IllegalStateException("Database unavailable") }.asDataResult().toList()
    assertEquals(2, failureEmissions.size)
    assertTrue(failureEmissions[0] is DataResult.Loading)
    assertEquals("Database unavailable", (failureEmissions[1] as DataResult.Error).message)
  }

  @Test
  fun `home screen renders updated tagline and shared lists hero stats`() {
    composeTestRule.setContent {
      WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
        HomeScreen(
          uiState =
            HomeUiState.Success(
              userDisplayName = "Attila Kovács",
              summary =
                DashboardSummary(
                  activeWishesCount = 17,
                  sharedListsCount = 3,
                  upcomingEventsCount = 3,
                  potentialSavingsHuf = 18_400,
                  sharedLists =
                    listOf(
                      SharedListSummary(
                        id = "sl_christmas_2027",
                        title = "Christmas 2027",
                        emoji = "🎄",
                        subtitle = "Family holiday wishlist",
                        ownerMemberId = "rec_attila",
                        visibility = ListVisibility.SHARED,
                        memberNames = listOf("Attila", "Dalma"),
                        itemCount = 27,
                      )
                    ),
                  recommendations = emptyList(),
                  upcomingEvents = emptyList(),
                ),
            ),
          onRetry = {},
          onOpenSearch = {},
          onOpenProfile = {},
          onOpenProductDetails = {},
          onOpenAddProduct = {},
          onOpenGiftAssistant = {},
          onOpenWishlistAll = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Save. Organize. Share. Gift Smarter.").assertIsDisplayed()
    composeTestRule.onNodeWithText("17 Active Wishes", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("3 Shared Lists", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("home_scroll_list")
      .performScrollToNode(hasTestTag("home_shared_lists_row"))
    composeTestRule
      .onNodeWithText("🎄 Christmas 2027", useUnmergedTree = true)
      .assertIsDisplayed()
  }

  @Test
  fun `shared list screen renders secret gift shield for authorized manager`() {
    composeTestRule.setContent {
      WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
        SharedListScreenContent(
          state =
            SharedListUiState.Success(
              details =
                SharedListDetails(
                  summary =
                    SharedListSummary(
                      id = "sl_dalma_secret_gift",
                      title = "Dalma Birthday Gift",
                      emoji = "🎁",
                      subtitle = "Secret birthday surprise list",
                      ownerMemberId = "rec_attila",
                      visibility = ListVisibility.HIDDEN,
                      hiddenFromMemberId = "rec_dalma",
                      hiddenFromMemberName = "Dalma",
                      memberNames = listOf("Attila"),
                      itemCount = 2,
                    ),
                  members =
                    listOf(
                      SharedListMember(
                        "sl_dalma_secret_gift",
                        "rec_attila",
                        "Attila",
                        "attila@pixelmail.hu",
                        "A",
                        "indigo",
                        ListMemberRole.OWNER,
                      )
                    ),
                  products = emptyList(),
                  activityFeed = emptyList(),
                  pendingInvitations = emptyList(),
                )
            ),
          onNavigateBack = {},
          onRetry = {},
          onOpenProductDetails = {},
          onUpdateVisibility = { _, _ -> },
          onUpdateMemberRole = { _, _ -> },
          onSendInvitation = { _, _ -> },
          onAcceptInvitation = {},
          onAddQuickProduct = { _, _, _ -> },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("🎁 Dalma Birthday Gift").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("shared_list_details_scroll")
      .performScrollToNode(hasTestTag("shared_list_secret_banner"))
    composeTestRule.onNodeWithTag("shared_list_secret_banner").assertIsDisplayed()
  }

  @Test
  fun `product details renders family comments and activity timeline`() {
    composeTestRule.setContent {
      WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
        ProductDetailsScreenContent(
          state =
            ProductDetailsUiState.Success(
              product =
                Product(
                  id = "prod_pixel_buds_2a",
                  title = "Google Pixel Buds 2a",
                  store = "Yettel",
                  currentPriceHuf = 28_490,
                  originalPriceHuf = 32_990,
                  priceChangePercent = -13,
                  savingsHuf = 4_500,
                  imageKey = "pixel_buds",
                  categories = listOf("Electronics", "Audio"),
                  tags = listOf("Google", "Pixel", "Wireless"),
                ),
              comments =
                listOf(
                  ProductComment(
                    id = "c1",
                    productId = "prod_pixel_buds_2a",
                    authorMemberId = "rec_dalma",
                    authorName = "Dalma",
                    message = "Looks perfect for Christmas",
                    createdAtMillis = 0L,
                  )
                ),
              activityEvents =
                listOf(
                  ActivityEvent(
                    id = "a1",
                    actorMemberId = "rec_attila",
                    actorName = "Attila",
                    actionSummary = "Attila added Google Pixel Buds 2a",
                    createdAtMillis = 0L,
                  )
                ),
            ),
          onNavigateBack = {},
          onRetry = {},
          onSelectPeriod = {},
          onTogglePurchased = {},
          onTogglePriceAlert = {},
          onAssignRecipient = {},
          onShareProduct = {},
          onOpenAlternativeProduct = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("product_details_list")
      .performScrollToNode(hasText("Looks perfect for Christmas"))
    composeTestRule.onNodeWithText("Looks perfect for Christmas").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("product_details_list")
      .performScrollToNode(hasText("Attila added Google Pixel Buds 2a"))
    composeTestRule.onNodeWithText("Attila added Google Pixel Buds 2a").assertIsDisplayed()
  }

  @Test
  fun `design system error state renders and dispatches retry in dark theme`() {
    var retried = false
    composeTestRule.setContent {
      WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
        WishDropErrorState(
          message = "Unable to sync price history",
          onRetry = { retried = true },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Unable to sync price history").assertIsDisplayed()
    composeTestRule.onNodeWithText("Try again").performClick()
    composeTestRule.waitForIdle()
    assertTrue(retried)
  }

  @Test
  fun `design system empty state renders action button`() {
    var clicked = false
    composeTestRule.setContent {
      WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
        WishDropEmptyState(
          icon = WishDropIcons.WishlistFilled,
          title = "Your wishlist is ready",
          description = "Paste a product link to organize with AI.",
          actionLabel = "Add First Wish",
          onActionClick = { clicked = true },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Add First Wish").performClick()
    composeTestRule.waitForIdle()
    assertTrue(clicked)
  }
}
