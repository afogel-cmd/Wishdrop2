package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
  @PrimaryKey val id: String,
  val authUid: String? = null,
  val email: String,
  val displayName: String,
  val avatarKey: String = "avatar_attila",
  val linkedMemberId: String,
  val createdAtMillis: Long,
)

@Serializable
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
  @PrimaryKey val id: String,
  val ownerUserId: String,
  val name: String,
  val email: String,
  val relationship: String,
  val avatarKey: String,
  val accentKey: String,
  val createdAtMillis: Long,
)

@Serializable
@Entity(
  tableName = "shared_lists",
  indices = [Index("ownerMemberId"), Index("visibility"), Index("hiddenFromMemberId")],
)
data class SharedListEntity(
  @PrimaryKey val id: String,
  val title: String,
  val emoji: String,
  val subtitle: String,
  val ownerMemberId: String,
  val visibility: String,
  val hiddenFromMemberId: String? = null,
  val hiddenFromMemberName: String? = null,
  val baselineItemCount: Int = 0,
  val eventDateLabel: String = "",
  val updatedAtMillis: Long,
)

@Serializable
@Entity(
  tableName = "list_members",
  primaryKeys = ["listId", "memberId"],
  indices = [Index("listId"), Index("memberId")],
)
data class ListMemberEntity(
  val listId: String,
  val memberId: String,
  val memberName: String,
  val memberEmail: String,
  val avatarKey: String,
  val accentKey: String,
  val role: String,
  val joinedAtMillis: Long,
)

@Serializable
@Entity(
  tableName = "list_invitations",
  indices = [Index("listId"), Index("inviteeEmail"), Index("status")],
)
data class ListInvitationEntity(
  @PrimaryKey val id: String,
  val listId: String,
  val listTitle: String,
  val inviterName: String,
  val inviteeEmail: String,
  val proposedRole: String,
  val status: String,
  val createdAtMillis: Long,
)

@Serializable
@Entity(
  tableName = "products",
  indices =
    [
      Index("recipientId"),
      Index("wishlistId"),
      Index("sharedListId"),
      Index("hiddenFromMemberId"),
      Index("isPurchased"),
      Index("isArchived"),
    ],
)
data class ProductEntity(
  @PrimaryKey val id: String,
  val title: String,
  val store: String,
  val currentPriceHuf: Int,
  val originalPriceHuf: Int,
  val priceChangePercent: Int,
  val savingsHuf: Int,
  val imageKey: String,
  val sourceUrl: String = "",
  val barcode: String = "",
  val categoriesJson: String,
  val tagsJson: String,
  val recipientId: String? = null,
  val recipientName: String? = null,
  val recipientConfidence: Int = 0,
  val wishlistId: String = "wl_personal",
  val sharedListId: String? = null,
  val hiddenFromMemberId: String? = null,
  val collectionName: String = "Personal",
  val isPurchased: Boolean = false,
  val isArchived: Boolean = false,
  val isPriceAlertEnabled: Boolean = true,
  val isFeaturedDeal: Boolean = false,
  val notes: String = "",
  val createdAtMillis: Long,
  val alternativesJson: String = "[]",
  val storeOffersJson: String = "[]",
  val additionalImagesJson: String = "[]",
)

@Serializable
@Entity(
  tableName = "product_tags",
  primaryKeys = ["productId", "tagName"],
  indices = [Index("productId"), Index("tagName")],
)
data class ProductTagCrossRefEntity(
  val productId: String,
  val tagName: String,
)

@Serializable
@Entity(
  tableName = "comments",
  indices = [Index("productId"), Index("sharedListId"), Index("createdAtMillis")],
)
data class CommentEntity(
  @PrimaryKey val id: String,
  val productId: String,
  val sharedListId: String? = null,
  val authorMemberId: String,
  val authorName: String,
  val authorAccentKey: String = "indigo",
  val message: String,
  val createdAtMillis: Long,
)

@Serializable
@Entity(
  tableName = "activity_events",
  indices = [Index("productId"), Index("sharedListId"), Index("createdAtMillis")],
)
data class ActivityEventEntity(
  @PrimaryKey val id: String,
  val productId: String? = null,
  val sharedListId: String? = null,
  val actorMemberId: String,
  val actorName: String,
  val actorAccentKey: String = "indigo",
  val actionSummary: String,
  val eventType: String = "ITEM_UPDATED",
  val createdAtMillis: Long,
)

@Serializable
@Entity(tableName = "wishlists")
data class WishlistEntity(
  @PrimaryKey val id: String,
  val title: String,
  val subtitle: String,
  val iconKey: String,
  val filterQuery: String,
  val isSmartCollection: Boolean = true,
  val sortOrder: Int = 0,
)

@Serializable
@Entity(
  tableName = "price_history",
  indices = [Index("productId"), Index("timestampMillis")],
)
data class PriceHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val productId: String,
  val timestampMillis: Long,
  val priceHuf: Int,
  val storeName: String,
)

@Serializable
@Entity(tableName = "gift_recipients")
data class GiftRecipientEntity(
  @PrimaryKey val id: String,
  val name: String,
  val relationship: String,
  val avatarKey: String,
  val upcomingEventName: String,
  val daysUntilEvent: Int,
  val budgetHuf: Int,
  val interestsJson: String,
  val giftStageName: String,
  val accentKey: String,
  val avatarUrl: String? = null,
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val iconKey: String,
  val colorIndex: Int,
)

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
  @PrimaryKey val name: String,
  val usageCount: Int,
)

@Serializable
@Entity(
  tableName = "recommendations",
  indices = [Index("recipientId")],
)
data class RecommendationEntity(
  @PrimaryKey val id: String,
  val recipientId: String,
  val title: String,
  val store: String,
  val priceHuf: Int,
  val imageKey: String,
  val matchPercentage: Int,
  val reason: String,
  val categoriesJson: String,
  val tagsJson: String,
  val linkedProductId: String? = null,
)

@Serializable
data class WishDropBackupPayload(
  val version: Int = 2,
  val exportedAtMillis: Long,
  val products: List<ProductEntity>,
  val wishlists: List<WishlistEntity>,
  val priceHistory: List<PriceHistoryEntity>,
  val recipients: List<GiftRecipientEntity>,
  val categories: List<CategoryEntity>,
  val tags: List<TagEntity>,
  val recommendations: List<RecommendationEntity>,
  val sharedLists: List<SharedListEntity> = emptyList(),
  val listMembers: List<ListMemberEntity> = emptyList(),
  val comments: List<CommentEntity> = emptyList(),
  val activityEvents: List<ActivityEventEntity> = emptyList(),
  val invitations: List<ListInvitationEntity> = emptyList(),
)
