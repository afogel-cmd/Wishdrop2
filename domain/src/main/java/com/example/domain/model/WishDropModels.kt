package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GiftStage(val stepIndex: Int, val label: String) {
  IDEA(0, "Idea"),
  CONSIDERING(1, "Considering"),
  WATCHING(2, "Watching"),
  ORDERED(3, "Ordered"),
  WRAPPED(4, "Wrapped"),
  DELIVERED(5, "Delivered"),
}

@Serializable
enum class ListVisibility(val label: String) {
  PRIVATE("Private"),
  SHARED("Shared"),
  HIDDEN("Hidden • Secret Gift"),
  OWNER_ONLY("Owner Only"),
}

@Serializable
enum class ListMemberRole(val label: String) {
  OWNER("Owner"),
  EDITOR("Editor"),
  VIEWER("Viewer"),
  HIDDEN_GIFT_MANAGER("Hidden Gift Manager"),
}

@Serializable
enum class InvitationStatus {
  PENDING,
  ACCEPTED,
  DECLINED,
}

@Serializable
enum class PricePeriod(val label: String, val days: Int?) {
  DAYS_30("30D", 30),
  DAYS_90("90D", 90),
  YEAR_1("1Y", 365),
  ALL("ALL", null),
}

@Serializable
enum class AddInputMode {
  URL,
  MANUAL,
  BARCODE,
}

@Serializable
data class AlternativeProduct(
  val id: String,
  val title: String,
  val store: String,
  val priceHuf: Int,
  val imageKey: String,
  val highlight: String,
)

@Serializable
data class StoreOffer(
  val storeName: String,
  val priceHuf: Int,
  val deliveryNote: String,
  val inStock: Boolean = true,
  val isBestOffer: Boolean = false,
)

@Serializable
data class Product(
  val id: String,
  val title: String,
  val store: String,
  val currentPriceHuf: Int,
  val originalPriceHuf: Int,
  val priceChangePercent: Int,
  val savingsHuf: Int,
  val imageKey: String,
  val sourceUrl: String = "",
  val barcode: String = "",
  val categories: List<String>,
  val tags: List<String>,
  val recipientId: String? = null,
  val recipientName: String? = null,
  val recipientConfidence: Int = 0,
  val sharedListId: String? = null,
  val hiddenFromMemberId: String? = null,
  val collectionName: String = "Personal",
  val isPurchased: Boolean = false,
  val isArchived: Boolean = false,
  val isPriceAlertEnabled: Boolean = true,
  val isFeaturedDeal: Boolean = false,
  val notes: String = "",
  val createdAtMillis: Long = 0L,
  val alternatives: List<AlternativeProduct> = emptyList(),
  val storeOffers: List<StoreOffer> = emptyList(),
)

@Serializable
data class PricePoint(
  val id: Long = 0L,
  val productId: String,
  val timestampMillis: Long,
  val priceHuf: Int,
  val storeName: String,
)

@Serializable
data class GiftRecipient(
  val id: String,
  val name: String,
  val relationship: String,
  val avatarKey: String,
  val upcomingEventName: String,
  val daysUntilEvent: Int,
  val budgetHuf: Int,
  val interests: List<String>,
  val giftStage: GiftStage,
  val accentKey: String,
)

@Serializable
data class AiRecommendation(
  val id: String,
  val recipientId: String,
  val title: String,
  val store: String,
  val priceHuf: Int,
  val imageKey: String,
  val matchPercentage: Int,
  val reason: String,
  val categories: List<String>,
  val tags: List<String>,
  val linkedProductId: String? = null,
)

@Serializable
data class SmartCollection(
  val id: String,
  val title: String,
  val subtitle: String,
  val productCount: Int,
  val iconKey: String,
  val filterQuery: String,
)

@Serializable
data class UpcomingEvent(
  val id: String,
  val title: String,
  val daysRemaining: Int,
  val recipientId: String?,
  val iconKey: String,
  val savedIdeasCount: Int,
)

@Serializable
data class SharedListMember(
  val listId: String,
  val memberId: String,
  val memberName: String,
  val memberEmail: String,
  val avatarKey: String,
  val accentKey: String,
  val role: ListMemberRole,
)

@Serializable
data class SharedListSummary(
  val id: String,
  val title: String,
  val emoji: String,
  val subtitle: String,
  val ownerMemberId: String,
  val visibility: ListVisibility,
  val hiddenFromMemberId: String? = null,
  val hiddenFromMemberName: String? = null,
  val memberNames: List<String> = emptyList(),
  val itemCount: Int = 0,
  val eventDateLabel: String = "",
  val updatedAtMillis: Long = 0L,
)

@Serializable
data class ProductComment(
  val id: String,
  val productId: String,
  val sharedListId: String? = null,
  val authorMemberId: String,
  val authorName: String,
  val authorAccentKey: String = "indigo",
  val message: String,
  val createdAtMillis: Long,
)

@Serializable
data class ActivityEvent(
  val id: String,
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
data class ListInvitation(
  val id: String,
  val listId: String,
  val listTitle: String,
  val inviterName: String,
  val inviteeEmail: String,
  val proposedRole: ListMemberRole,
  val status: InvitationStatus,
  val createdAtMillis: Long,
)

@Serializable
data class SharedListDetails(
  val summary: SharedListSummary,
  val members: List<SharedListMember>,
  val products: List<Product>,
  val activityFeed: List<ActivityEvent>,
  val pendingInvitations: List<ListInvitation>,
  val canEdit: Boolean = true,
  val isRealtimeConnected: Boolean = true,
)

@Serializable
data class RealtimeSyncStatus(
  val isCloudConfigured: Boolean,
  val isRealtimeSocketActive: Boolean,
  val lastSyncedMillis: Long,
  val statusLabel: String,
)

@Serializable
data class FamilyPersona(
  val memberId: String,
  val displayName: String,
  val email: String,
  val relationship: String,
  val accentKey: String,
)

@Serializable
data class DashboardSummary(
  val activeWishesCount: Int,
  val upcomingEventsCount: Int,
  val potentialSavingsHuf: Int,
  val recommendations: List<Product>,
  val upcomingEvents: List<UpcomingEvent>,
  val sharedListsCount: Int = 0,
  val sharedLists: List<SharedListSummary> = emptyList(),
  val realtimeStatusLabel: String = "Realtime Sync Active",
)

@Serializable
data class CategoryDistribution(
  val categoryName: String,
  val percentage: Int,
  val itemCount: Int,
  val totalValueHuf: Int,
  val colorIndex: Int,
)

@Serializable
data class TagStat(
  val tag: String,
  val count: Int,
)

@Serializable
data class InsightsReport(
  val yearlySavedHuf: Int,
  val activePotentialSavingsHuf: Int,
  val topSavedProduct: Product?,
  val categoryDistribution: List<CategoryDistribution>,
  val commonTags: List<TagStat>,
  val biggestPriceDrops: List<Product>,
)

@Serializable
data class RecipientPrediction(
  val recipientId: String,
  val recipientName: String,
  val confidencePercent: Int,
  val explanation: String,
)

@Serializable
data class ExtractedProductDraft(
  val title: String,
  val imageKey: String,
  val store: String,
  val priceHuf: Int,
  val originalPriceHuf: Int,
  val sourceUrl: String = "",
  val barcode: String = "",
  val categories: List<String>,
  val tags: List<String>,
  val recipientPrediction: RecipientPrediction?,
  val alternatives: List<AlternativeProduct>,
  val storeOffers: List<StoreOffer>,
  val sharedListId: String? = null,
)
