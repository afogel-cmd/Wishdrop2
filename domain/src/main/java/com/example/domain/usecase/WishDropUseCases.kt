package com.example.domain.usecase

import com.example.domain.ai.AiProductExtractor
import com.example.domain.ai.ExtractionProgress
import com.example.domain.model.ActivityEvent
import com.example.domain.model.AddInputMode
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
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BackupRepository
import com.example.domain.repository.CollaborationRepository
import com.example.domain.repository.GiftRepository
import com.example.domain.repository.InsightsRepository
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.SharedListRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDashboardUseCase
@Inject
constructor(private val productRepository: ProductRepository) {
  operator fun invoke(): Flow<DashboardSummary> = productRepository.observeDashboardSummary()
}

data class WishlistStreamBundle(
  val allProducts: List<Product>,
  val smartCollections: List<SmartCollection>,
  val categories: List<String>,
)

class ObserveWishlistCatalogUseCase
@Inject
constructor(private val productRepository: ProductRepository) {
  operator fun invoke(): Flow<WishlistStreamBundle> =
    combine(
      productRepository.observeAllProducts(),
      productRepository.observeSmartCollections(),
      productRepository.observeAvailableCategories(),
    ) { products, collections, categories ->
      WishlistStreamBundle(
        allProducts = products,
        smartCollections = collections,
        categories = categories,
      )
    }
}

data class ProductDetailsBundle(
  val product: Product?,
  val priceHistory: List<PricePoint>,
  val allRecipients: List<GiftRecipient>,
  val comments: List<ProductComment> = emptyList(),
  val activityEvents: List<ActivityEvent> = emptyList(),
  val sharedLists: List<SharedListSummary> = emptyList(),
)

class ObserveProductDetailsUseCase
@Inject
constructor(
  private val productRepository: ProductRepository,
  private val giftRepository: GiftRepository,
  private val collaborationRepository: CollaborationRepository,
  private val sharedListRepository: SharedListRepository,
) {
  operator fun invoke(productId: String, period: PricePeriod): Flow<ProductDetailsBundle> {
    val coreFlow =
      combine(
        productRepository.observeProductById(productId),
        productRepository.observePriceHistory(productId, period),
        giftRepository.observeRecipients(),
      ) { product, history, recipients ->
        Triple(product, history, recipients)
      }
    val collabFlow =
      combine(
        collaborationRepository.observeCommentsForProduct(productId),
        collaborationRepository.observeActivityForProduct(productId),
        sharedListRepository.observeVisibleSharedLists(),
      ) { comments, activity, lists ->
        Triple(comments, activity, lists)
      }
    return combine(coreFlow, collabFlow) { (product, history, recipients), (comments, activity, lists) ->
      ProductDetailsBundle(
        product = product,
        priceHistory = history,
        allRecipients = recipients,
        comments = comments,
        activityEvents = activity,
        sharedLists = lists,
      )
    }
  }
}

class ManageWishlistItemUseCase
@Inject
constructor(
  private val productRepository: ProductRepository,
  private val collaborationRepository: CollaborationRepository,
) {
  suspend fun setPurchased(productId: String, purchased: Boolean) =
    productRepository.setPurchased(productId, purchased)

  suspend fun setArchived(productId: String, archived: Boolean) =
    productRepository.setArchived(productId, archived)

  suspend fun deleteProduct(productId: String) =
    productRepository.deleteProduct(productId)

  suspend fun togglePriceAlert(productId: String) =
    productRepository.togglePriceAlert(productId)

  suspend fun assignRecipient(productId: String, recipientId: String?, confidence: Int = 98) =
    productRepository.assignRecipient(productId, recipientId, confidence)

  suspend fun assignSharedList(productId: String, sharedListId: String?) =
    productRepository.assignProductToSharedList(productId, sharedListId)

  suspend fun addComment(productId: String, message: String) =
    collaborationRepository.addComment(productId, message)

  suspend fun saveDraft(draft: ExtractedProductDraft): String =
    productRepository.saveDraftProduct(draft)

  suspend fun addProductPhoto(productId: String, photoUrl: String) =
    productRepository.addProductPhoto(productId, photoUrl)
}

class ManageSharedListUseCase
@Inject
constructor(
  private val sharedListRepository: SharedListRepository,
  private val collaborationRepository: CollaborationRepository,
) {
  fun observeVisibleLists(): Flow<List<SharedListSummary>> =
    sharedListRepository.observeVisibleSharedLists()

  fun observeListDetails(listId: String): Flow<SharedListDetails?> =
    sharedListRepository.observeSharedListDetails(listId)

  suspend fun createList(
    title: String,
    emoji: String,
    subtitle: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ): String =
    sharedListRepository.createSharedList(title, emoji, subtitle, visibility, hiddenFromMemberId)

  suspend fun updateVisibility(
    listId: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ) = sharedListRepository.updateListVisibility(listId, visibility, hiddenFromMemberId)

  suspend fun updateListDetails(
    listId: String,
    title: String,
    emoji: String,
    subtitle: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ) = sharedListRepository.updateSharedList(listId, title, emoji, subtitle, visibility, hiddenFromMemberId)

  suspend fun deleteList(listId: String) = sharedListRepository.deleteSharedList(listId)

  suspend fun updateMemberRole(listId: String, memberId: String, role: ListMemberRole) =
    sharedListRepository.updateMemberRole(listId, memberId, role)

  suspend fun removeMember(listId: String, memberId: String) =
    sharedListRepository.removeMember(listId, memberId)

  suspend fun addQuickProductIdea(
    listId: String,
    title: String,
    store: String,
    priceHuf: Int,
  ): String = sharedListRepository.addProductIdeaToList(listId, title, store, priceHuf)

  suspend fun inviteByEmail(
    listId: String,
    email: String,
    role: ListMemberRole,
  ): Result<ListInvitation> = collaborationRepository.sendInvitation(listId, email, role)

  suspend fun acceptInvitation(invitationId: String): Result<String> =
    collaborationRepository.acceptInvitation(invitationId)
}

class ManageFamilyAuthUseCase
@Inject
constructor(
  private val authRepository: AuthRepository,
  private val collaborationRepository: CollaborationRepository,
) {
  fun observePersonas(): Flow<List<FamilyPersona>> = authRepository.observeFamilyPersonas()

  fun observePendingInvitations(): Flow<List<ListInvitation>> =
    collaborationRepository.observePendingInvitations()

  fun observeRealtimeSyncStatus(): Flow<RealtimeSyncStatus> =
    collaborationRepository.observeRealtimeSyncStatus()

  suspend fun signIn(email: String, password: String): Result<String> =
    authRepository.signInWithSupabase(email, password)

  suspend fun signUp(displayName: String, email: String, password: String): Result<String> =
    authRepository.signUpWithSupabase(displayName, email, password)

  suspend fun signOut() = authRepository.signOutCloudSession()

  suspend fun switchActivePersona(memberId: String) =
    authRepository.switchActivePersona(memberId)

  suspend fun deleteFamilyMember(memberId: String) =
    authRepository.deleteFamilyMember(memberId)
}

class ManageGiftPlanUseCase
@Inject
constructor(private val giftRepository: GiftRepository) {
  fun observeRecipients() = giftRepository.observeRecipients()

  fun observeSavedIdeas(recipientId: String) =
    giftRepository.observeSavedIdeasForRecipient(recipientId)

  fun observeRecommendations(recipientId: String) =
    giftRepository.observeRecommendationsForRecipient(recipientId)

  suspend fun updateStage(recipientId: String, stage: GiftStage) =
    giftRepository.updateGiftStage(recipientId, stage)

  suspend fun addInterest(recipientId: String, interest: String) =
    giftRepository.addInterest(recipientId, interest)

  suspend fun removeInterest(recipientId: String, interest: String) =
    giftRepository.removeInterest(recipientId, interest)

  suspend fun saveRecommendation(recommendation: AiRecommendation): String =
    giftRepository.saveRecommendationToWishlist(recommendation)

  suspend fun dismissRecommendation(recommendationId: String) =
    giftRepository.dismissRecommendation(recommendationId)

  suspend fun generateAiRecommendations(recipientId: String) =
    giftRepository.generateAiRecommendations(recipientId)

  suspend fun deletePerson(recipientId: String) =
    giftRepository.deleteRecipient(recipientId)

  suspend fun updatePerson(
    recipientId: String,
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    interests: List<String>,
    avatarUrl: String? = null,
  ) = giftRepository.updateRecipient(
    recipientId = recipientId,
    name = name,
    relationship = relationship,
    eventName = eventName,
    daysUntilEvent = daysUntilEvent,
    interests = interests,
    avatarUrl = avatarUrl,
  )

  suspend fun addPerson(
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    budgetHuf: Int,
    interests: List<String>,
    avatarUrl: String? = null,
  ): String =
    giftRepository.addRecipient(name, relationship, eventName, daysUntilEvent, budgetHuf, interests, avatarUrl)
}

class AnalyzeProductInputUseCase
@Inject
constructor(private val extractor: AiProductExtractor) {
  operator fun invoke(
    mode: AddInputMode,
    url: String,
    manualName: String,
    manualStore: String,
    manualPriceHuf: Int,
    manualNotes: String,
    barcode: String,
  ): Flow<ExtractionProgress> =
    when (mode) {
      AddInputMode.URL -> extractor.extractFromUrl(url)
      AddInputMode.MANUAL ->
        extractor.extractFromManual(manualName, manualStore, manualPriceHuf, manualNotes)
      AddInputMode.BARCODE -> extractor.extractFromBarcode(barcode)
    }
}

class ObserveInsightsReportUseCase
@Inject
constructor(private val insightsRepository: InsightsRepository) {
  operator fun invoke(): Flow<InsightsReport> = insightsRepository.observeInsightsReport()
}

class ManageBackupUseCase
@Inject
constructor(private val backupRepository: BackupRepository) {
  suspend fun exportJson(): String = backupRepository.exportJsonBackup()

  suspend fun importJson(payload: String): Result<Int> = backupRepository.importJsonBackup(payload)

  suspend fun resetCatalog() = backupRepository.resetToDefaultCatalog()
}
