package com.example.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized semantic icon system for WishDrop.
 * Pairs Filled icons for active/primary states with Outlined/AutoMirrored variants.
 */
@Immutable
object WishDropIcons {
  // Navigation (Selected / Unselected pairs)
  val HomeFilled: ImageVector = Icons.Filled.Home
  val HomeOutlined: ImageVector = Icons.Outlined.Home
  val GiftFilled: ImageVector = Icons.Filled.CardGiftcard
  val GiftOutlined: ImageVector = Icons.Outlined.CardGiftcard
  val WishlistFilled: ImageVector = Icons.Filled.Inventory2
  val WishlistOutlined: ImageVector = Icons.Outlined.Inventory2
  val InsightsFilled: ImageVector = Icons.Filled.Insights
  val InsightsOutlined: ImageVector = Icons.Outlined.Insights
  val ProfileFilled: ImageVector = Icons.Filled.Person
  val ProfileOutlined: ImageVector = Icons.Outlined.Person

  // Directional & System
  val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
  val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
  val Send: ImageVector = Icons.AutoMirrored.Filled.Send
  val TrendingDown: ImageVector = Icons.AutoMirrored.Filled.TrendingDown
  val SearchFilled: ImageVector = Icons.Filled.Search
  val SearchOutlined: ImageVector = Icons.Outlined.Search
  val Clear: ImageVector = Icons.Filled.Clear
  val Delete: ImageVector = Icons.Filled.Delete
  val Edit: ImageVector = Icons.Filled.Edit
  val Add: ImageVector = Icons.Filled.Add
  val Check: ImageVector = Icons.Filled.Check
  val CheckCircle: ImageVector = Icons.Filled.CheckCircle
  val Share: ImageVector = Icons.Filled.Share
  val Refresh: ImageVector = Icons.Filled.Refresh
  val ErrorOutline: ImageVector = Icons.Filled.ErrorOutline

  // Collaboration, Secret Gifts & Cloud Sync
  val Group: ImageVector = Icons.Filled.Group
  val Lock: ImageVector = Icons.Filled.Lock
  val SecretHidden: ImageVector = Icons.Filled.VisibilityOff
  val Shield: ImageVector = Icons.Filled.Shield
  val Comment: ImageVector = Icons.Filled.ChatBubbleOutline
  val History: ImageVector = Icons.Filled.History
  val Mail: ImageVector = Icons.Filled.MailOutline
  val CloudSynced: ImageVector = Icons.Filled.CloudDone

  // AI & Product Actions
  val Sparkle: ImageVector = Icons.Filled.AutoAwesome
  val AddLink: ImageVector = Icons.Filled.AddLink
  val ManualEntry: ImageVector = Icons.Filled.EditNote
  val BarcodeScan: ImageVector = Icons.Filled.QrCodeScanner
  val Camera: ImageVector = Icons.Filled.CameraAlt
  val Archive: ImageVector = Icons.Filled.Archive
  val BookmarkSaved: ImageVector = Icons.Filled.BookmarkAdded
  val BookmarkAdd: ImageVector = Icons.Filled.BookmarkBorder
  val AlertActive: ImageVector = Icons.Filled.NotificationsActive
  val AlertOff: ImageVector = Icons.Outlined.NotificationsOff
  val Store: ImageVector = Icons.Filled.Storefront

  // Categories, People & Celebrations
  val PersonAdd: ImageVector = Icons.Filled.PersonAdd
  val Cake: ImageVector = Icons.Filled.Cake
  val Holiday: ImageVector = Icons.Filled.Park
  val Favorite: ImageVector = Icons.Filled.Favorite
  val Devices: ImageVector = Icons.Filled.Devices
  val Savings: ImageVector = Icons.Filled.Savings
  val Trophy: ImageVector = Icons.Filled.EmojiEvents

  // Studio Illustration Symbols
  val Botanical: ImageVector = Icons.Filled.LocalFlorist
  val Apparel: ImageVector = Icons.Filled.Checkroom
  val Dock: ImageVector = Icons.Filled.Dock
  val Headphones: ImageVector = Icons.Filled.Headphones
  val Coffee: ImageVector = Icons.Filled.Coffee
  val Book: ImageVector = Icons.Filled.MenuBook
  val EReader: ImageVector = Icons.Filled.AutoStories
  val Accessory: ImageVector = Icons.Filled.Mouse
  val Living: ImageVector = Icons.Filled.Weekend

  // Data & Vault
  val Export: ImageVector = Icons.Filled.FileUpload
  val Import: ImageVector = Icons.Filled.FileDownload
  val Restore: ImageVector = Icons.Filled.Restore
  val VaultVerified: ImageVector = Icons.Filled.VerifiedUser
}
