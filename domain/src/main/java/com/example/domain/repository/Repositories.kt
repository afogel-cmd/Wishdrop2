package com.example.domain.repository

import com.example.domain.model.ActivityEvent
import com.example.domain.model.AiRecommendation
import com.example.domain.model.DashboardSummary
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.FamilyPersona
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import com.example.domain.model.InsightsReport
import com.example.domain.model.ListInvitation
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.PricePeriod
import com.example.domain.model.PricePoint
import com.example.domain.model.Product
import com.example.domain.model.ProductComment
import com.example.domain.model.RealtimeSyncStatus
import com.example.domain.model.SharedListDetails
import com.example.domain.model.SharedListSummary
import com.example.domain.model.SmartCollection
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
  fun observeDashboardSummary(): Flow<DashboardSummary>

  fun observeActiveProducts(): Flow<List<Product>>

  fun observeAllProducts(): Flow<List<Product>>

  fun observeSmartCollections(): Flow<List<SmartCollection>>

  fun observeAvailableCategories(): Flow<List<String>>

  fun observeProductById(productId: String): Flow<Product?>

  fun observePriceHistory(productId: String, period: PricePeriod): Flow<List<PricePoint>>

  suspend fun saveDraftProduct(draft: ExtractedProductDraft): String

  suspend fun setPurchased(productId: String, purchased: Boolean)

  suspend fun setArchived(productId: String, archived: Boolean)

  suspend fun togglePriceAlert(productId: String)

  suspend fun assignRecipient(productId: String, recipientId: String?, confidence: Int = 100)

  suspend fun assignProductToSharedList(productId: String, sharedListId: String?)
}

interface SharedListRepository {
  fun observeVisibleSharedLists(): Flow<List<SharedListSummary>>

  fun observeSharedListDetails(listId: String): Flow<SharedListDetails?>

  suspend fun createSharedList(
    title: String,
    emoji: String,
    subtitle: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ): String

  suspend fun updateListVisibility(
    listId: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  )

  suspend fun updateMemberRole(listId: String, memberId: String, role: ListMemberRole)

  suspend fun addProductIdeaToList(
    listId: String,
    title: String,
    store: String,
    priceHuf: Int,
  ): String
}

interface CollaborationRepository {
  fun observeCommentsForProduct(productId: String): Flow<List<ProductComment>>

  fun observeActivityForProduct(productId: String): Flow<List<ActivityEvent>>

  fun observePendingInvitations(): Flow<List<ListInvitation>>

  fun observeRealtimeSyncStatus(): Flow<RealtimeSyncStatus>

  suspend fun addComment(productId: String, message: String)

  suspend fun sendInvitation(
    listId: String,
    inviteeEmail: String,
    proposedRole: ListMemberRole,
  ): Result<ListInvitation>

  suspend fun acceptInvitation(invitationId: String): Result<String>
}

interface AuthRepository {
  fun observeFamilyPersonas(): Flow<List<FamilyPersona>>

  suspend fun signInWithSupabase(email: String, password: String): Result<String>

  suspend fun signUpWithSupabase(
    displayName: String,
    email: String,
    password: String,
  ): Result<String>

  suspend fun signOutCloudSession()

  suspend fun switchActivePersona(memberId: String)
}

interface GiftRepository {
  fun observeRecipients(): Flow<List<GiftRecipient>>

  fun observeRecipientById(recipientId: String): Flow<GiftRecipient?>

  fun observeSavedIdeasForRecipient(recipientId: String): Flow<List<Product>>

  fun observeRecommendationsForRecipient(recipientId: String): Flow<List<AiRecommendation>>

  suspend fun updateGiftStage(recipientId: String, stage: GiftStage)

  suspend fun addInterest(recipientId: String, interest: String)

  suspend fun removeInterest(recipientId: String, interest: String)

  suspend fun addRecipient(
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    budgetHuf: Int,
    interests: List<String>,
  ): String

  suspend fun saveRecommendationToWishlist(recommendation: AiRecommendation): String
}

interface InsightsRepository {
  fun observeInsightsReport(): Flow<InsightsReport>
}

interface BackupRepository {
  suspend fun exportJsonBackup(): String

  suspend fun importJsonBackup(jsonPayload: String): Result<Int>

  suspend fun resetToDefaultCatalog()
}
