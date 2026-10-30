package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.di.IoDispatcher
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.local.WishDropDatabase
import com.example.data.local.dao.CollaborationDao
import com.example.data.local.dao.GiftRecipientDao
import com.example.data.local.dao.PriceHistoryDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.RecommendationDao
import com.example.data.local.dao.SharedListDao
import com.example.data.local.dao.TaxonomyDao
import com.example.data.local.dao.UserAndFamilyDao
import com.example.data.local.dao.WishlistDao
import com.example.data.local.entity.ActivityEventEntity
import com.example.data.local.entity.GiftRecipientEntity
import com.example.data.local.entity.ListMemberEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductTagCrossRefEntity
import com.example.data.local.entity.RecommendationEntity
import com.example.data.local.entity.SharedListEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.WishDropBackupPayload
import com.example.data.remote.SupabaseRemoteDataSource
import com.example.data.seed.WishDropSeedCatalog
import com.example.domain.model.AiRecommendation
import com.example.domain.model.AlternativeProduct
import com.example.domain.model.CategoryDistribution
import com.example.domain.model.DashboardSummary
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import com.example.domain.model.InsightsReport
import com.example.domain.model.ListVisibility
import com.example.domain.model.PricePeriod
import com.example.domain.model.PricePoint
import com.example.domain.model.Product
import com.example.domain.model.SharedListSummary
import com.example.domain.model.SmartCollection
import com.example.domain.model.StoreOffer
import com.example.domain.model.TagStat
import com.example.domain.model.UpcomingEvent
import com.example.domain.repository.BackupRepository
import com.example.domain.repository.GiftRepository
import com.example.domain.repository.InsightsRepository
import com.example.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal val repositoryJson = Json {
  ignoreUnknownKeys = true
  prettyPrint = true
}

internal fun ProductEntity.isVisibleToViewer(viewerMemberId: String): Boolean =
  hiddenFromMemberId == null || hiddenFromMemberId != viewerMemberId

internal fun SharedListEntity.isVisibleToViewer(
  viewerMemberId: String,
  memberIdsInList: Set<String>,
): Boolean {
  if (hiddenFromMemberId != null && hiddenFromMemberId == viewerMemberId) return false
  return when (visibility) {
    ListVisibility.OWNER_ONLY.name,
    ListVisibility.PRIVATE.name -> ownerMemberId == viewerMemberId
    else -> ownerMemberId == viewerMemberId || viewerMemberId in memberIdsInList
  }
}

internal fun SharedListEntity.toSummaryDomain(
  members: List<ListMemberEntity>,
  linkedProductsCount: Int,
): SharedListSummary =
  SharedListSummary(
    id = id,
    title = title,
    emoji = emoji,
    subtitle = subtitle,
    ownerMemberId = ownerMemberId,
    visibility =
      runCatching { ListVisibility.valueOf(visibility) }.getOrDefault(ListVisibility.SHARED),
    hiddenFromMemberId = hiddenFromMemberId,
    hiddenFromMemberName = hiddenFromMemberName,
    memberNames = members.map { it.memberName },
    itemCount = baselineItemCount + linkedProductsCount,
    eventDateLabel = eventDateLabel,
    updatedAtMillis = updatedAtMillis,
  )

internal fun ProductEntity.toDomain(): Product =
  Product(
    id = id,
    title = title,
    store = store,
    currentPriceHuf = currentPriceHuf,
    originalPriceHuf = originalPriceHuf,
    priceChangePercent = priceChangePercent,
    savingsHuf = savingsHuf,
    imageKey = imageKey,
    sourceUrl = sourceUrl,
    barcode = barcode,
    categories =
      runCatching { repositoryJson.decodeFromString<List<String>>(categoriesJson) }
        .getOrDefault(emptyList()),
    tags =
      runCatching { repositoryJson.decodeFromString<List<String>>(tagsJson) }
        .getOrDefault(emptyList()),
    recipientId = recipientId,
    recipientName = recipientName,
    recipientConfidence = recipientConfidence,
    sharedListId = sharedListId,
    hiddenFromMemberId = hiddenFromMemberId,
    collectionName = collectionName,
    isPurchased = isPurchased,
    isArchived = isArchived,
    isPriceAlertEnabled = isPriceAlertEnabled,
    isFeaturedDeal = isFeaturedDeal,
    notes = notes,
    createdAtMillis = createdAtMillis,
    alternatives =
      runCatching { repositoryJson.decodeFromString<List<AlternativeProduct>>(alternativesJson) }
        .getOrDefault(emptyList()),
    storeOffers =
      runCatching { repositoryJson.decodeFromString<List<StoreOffer>>(storeOffersJson) }
        .getOrDefault(emptyList()),
    additionalImages =
      runCatching { repositoryJson.decodeFromString<List<String>>(additionalImagesJson) }
        .getOrDefault(emptyList()),
  )

internal fun GiftRecipientEntity.toDomain(): GiftRecipient =
  GiftRecipient(
    id = id,
    name = name,
    relationship = relationship,
    avatarKey = avatarKey,
    upcomingEventName = upcomingEventName,
    daysUntilEvent = daysUntilEvent,
    budgetHuf = budgetHuf,
    interests =
      runCatching { repositoryJson.decodeFromString<List<String>>(interestsJson) }
        .getOrDefault(emptyList()),
    giftStage = runCatching { GiftStage.valueOf(giftStageName) }.getOrDefault(GiftStage.IDEA),
    accentKey = accentKey,
    avatarUrl = avatarUrl,
  )

internal fun RecommendationEntity.toDomain(): AiRecommendation =
  AiRecommendation(
    id = id,
    recipientId = recipientId,
    title = title,
    store = store,
    priceHuf = priceHuf,
    imageKey = imageKey,
    matchPercentage = matchPercentage,
    reason = reason,
    categories =
      runCatching { repositoryJson.decodeFromString<List<String>>(categoriesJson) }
        .getOrDefault(emptyList()),
    tags =
      runCatching { repositoryJson.decodeFromString<List<String>>(tagsJson) }
        .getOrDefault(emptyList()),
    linkedProductId = linkedProductId,
  )

@Singleton
class DatabaseInitializer
@Inject
constructor(
  private val database: WishDropDatabase,
  private val productDao: ProductDao,
  private val sharedListDao: SharedListDao,
  private val collaborationDao: CollaborationDao,
  private val userAndFamilyDao: UserAndFamilyDao,
  private val wishlistDao: WishlistDao,
  private val priceHistoryDao: PriceHistoryDao,
  private val recipientDao: GiftRecipientDao,
  private val taxonomyDao: TaxonomyDao,
  private val recommendationDao: RecommendationDao,
  @IoDispatcher ioDispatcher: CoroutineDispatcher,
) {
  private val mutex = Mutex()
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

  init {
    scope.launch { ensureSeeded() }
  }

  suspend fun ensureSeeded() {
    mutex.withLock {
      if (productDao.countAll() == 0) {
        reseedInternal()
      }
    }
  }

  suspend fun forceReseed() {
    mutex.withLock { reseedInternal() }
  }

  private suspend fun reseedInternal() {
    val products = WishDropSeedCatalog.defaultProducts()
    database.withTransaction {
      collaborationDao.clearComments()
      collaborationDao.clearActivityEvents()
      collaborationDao.clearInvitations()
      collaborationDao.clearProductTags()
      sharedListDao.clearMembers()
      sharedListDao.clearLists()
      productDao.clearAll()
      wishlistDao.clearAll()
      priceHistoryDao.clearAll()
      recipientDao.clearAll()
      taxonomyDao.clearCategories()
      taxonomyDao.clearTags()
      recommendationDao.clearAll()
      userAndFamilyDao.clearFamilyMembers()
      userAndFamilyDao.clearUsers()

      userAndFamilyDao.upsertUsers(WishDropSeedCatalog.defaultUsers())
      userAndFamilyDao.upsertFamilyMembers(WishDropSeedCatalog.defaultFamilyMembers())
      sharedListDao.upsertSharedLists(WishDropSeedCatalog.defaultSharedLists())
      sharedListDao.upsertListMembers(WishDropSeedCatalog.defaultListMembers())
      collaborationDao.upsertInvitations(WishDropSeedCatalog.defaultInvitations())
      recipientDao.upsertRecipients(WishDropSeedCatalog.defaultRecipients())
      wishlistDao.upsertWishlists(WishDropSeedCatalog.defaultWishlists())
      productDao.upsertProducts(products)
      priceHistoryDao.insertHistory(WishDropSeedCatalog.generatePriceHistoryForProducts(products))
      taxonomyDao.upsertCategories(WishDropSeedCatalog.defaultCategories())
      taxonomyDao.upsertTags(WishDropSeedCatalog.defaultTags())
      collaborationDao.upsertProductTags(WishDropSeedCatalog.buildProductTagRefs(products))
      recommendationDao.upsertRecommendations(WishDropSeedCatalog.defaultRecommendations())
      collaborationDao.upsertComments(WishDropSeedCatalog.defaultComments())
      collaborationDao.insertActivityEvents(WishDropSeedCatalog.defaultActivityEvents())
    }
  }
}

@Singleton
class RoomProductRepository
@Inject
constructor(
  private val initializer: DatabaseInitializer,
  private val productDao: ProductDao,
  private val sharedListDao: SharedListDao,
  private val collaborationDao: CollaborationDao,
  private val userAndFamilyDao: UserAndFamilyDao,
  private val priceHistoryDao: PriceHistoryDao,
  private val recipientDao: GiftRecipientDao,
  private val taxonomyDao: TaxonomyDao,
  private val preferencesRepository: UserPreferencesRepository,
  private val remoteDataSource: SupabaseRemoteDataSource,
) : ProductRepository {

  private val activeViewerFlow: Flow<String> =
    preferencesRepository.preferencesFlow.map { it.activeMemberId }.distinctUntilChanged()

  override fun observeDashboardSummary(): Flow<DashboardSummary> {
    val productsAndRecipients =
      combine(productDao.observeActiveProducts(), recipientDao.observeRecipients()) { p, r ->
        Pair(p, r)
      }
    val listsAndMembers =
      combine(sharedListDao.observeAllSharedLists(), sharedListDao.observeAllListMembers()) { l, m ->
        Pair(l, m)
      }
    return combine(
      productsAndRecipients,
      listsAndMembers,
      activeViewerFlow,
      remoteDataSource.syncStatus,
    ) { (activeEntities, recipientEntities), (listEntities, memberEntities), viewerId, syncStatus ->
      val activeProducts =
        activeEntities.filter { it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
      val potentialSavings = activeProducts.sumOf { it.savingsHuf }
      val recommendations =
        activeProducts
          .sortedWith(
            compareByDescending<Product> { it.isFeaturedDeal }.thenByDescending { it.savingsHuf }
          )
          .take(5)

      val membersByList = memberEntities.groupBy { it.listId }
      val visibleLists =
        listEntities
          .filter { entity ->
            val memberIds = membersByList[entity.id].orEmpty().map { it.memberId }.toSet()
            entity.isVisibleToViewer(viewerId, memberIds)
          }
          .map { entity ->
            val linkedCount = activeProducts.count { it.sharedListId == entity.id }
            entity.toSummaryDomain(membersByList[entity.id].orEmpty(), linkedCount)
          }

      val events =
        listOf(
          UpcomingEvent(
            id = "evt_dalma_bday",
            title = "Dalma Birthday",
            daysRemaining = recipientEntities.find { it.id == "rec_dalma" }?.daysUntilEvent ?: 15,
            recipientId = "rec_dalma",
            iconKey = "cake",
            savedIdeasCount = activeProducts.count { it.recipientId == "rec_dalma" },
          ),
          UpcomingEvent(
            id = "evt_christmas",
            title = "Christmas",
            daysRemaining = 61,
            recipientId = "rec_armin",
            iconKey = "holiday",
            savedIdeasCount = activeProducts.count { "Christmas" in it.tags },
          ),
          UpcomingEvent(
            id = "evt_olivia_bday",
            title = "Olívia Birthday",
            daysRemaining = recipientEntities.find { it.id == "rec_olivia" }?.daysUntilEvent ?: 74,
            recipientId = "rec_olivia",
            iconKey = "cake",
            savedIdeasCount = activeProducts.count { it.recipientId == "rec_olivia" },
          ),
        )

      DashboardSummary(
        activeWishesCount = activeProducts.size,
        upcomingEventsCount = events.size,
        potentialSavingsHuf = potentialSavings,
        recommendations = recommendations,
        upcomingEvents = events,
        sharedListsCount = visibleLists.count { it.visibility == ListVisibility.SHARED },
        sharedLists = visibleLists,
        realtimeStatusLabel = syncStatus.statusLabel,
      )
    }
  }

  override fun observeActiveProducts(): Flow<List<Product>> =
    combine(productDao.observeActiveProducts(), activeViewerFlow) { list, viewerId ->
      list.filter { it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
    }

  override fun observeAllProducts(): Flow<List<Product>> =
    combine(productDao.observeAllProducts(), activeViewerFlow) { list, viewerId ->
      list.filter { it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
    }

  override fun observeSmartCollections(): Flow<List<SmartCollection>> =
    observeAllProducts().map { allVisible ->
      val active = allVisible.filter { !it.isArchived }
      listOf(
        SmartCollection(
          id = "col_electronics",
          title = "Electronics",
          subtitle = "Pixel, audio & smart gear",
          productCount = active.count { "Electronics" in it.categories },
          iconKey = "electronics",
          filterQuery = "Electronics",
        ),
        SmartCollection(
          id = "col_google",
          title = "Google Ecosystem",
          subtitle = "Pixel Buds, Watch & Streamer",
          productCount = active.count { "Google" in it.tags || "Pixel" in it.tags },
          iconKey = "google",
          filterQuery = "Google",
        ),
        SmartCollection(
          id = "col_audio",
          title = "Audio",
          subtitle = "Wireless earbuds & speakers",
          productCount = active.count { "Audio" in it.categories || "Audio" in it.tags },
          iconKey = "audio",
          filterQuery = "Audio",
        ),
        SmartCollection(
          id = "col_christmas",
          title = "Christmas",
          subtitle = "Holiday gift shortlist",
          productCount =
            active.count { "Christmas" in it.tags || it.collectionName == "Christmas" },
          iconKey = "holiday",
          filterQuery = "Christmas",
        ),
        SmartCollection(
          id = "col_dalma",
          title = "Dalma",
          subtitle = "Birthday & home ideas",
          productCount = active.count { it.recipientName == "Dalma" || "Dalma" in it.tags },
          iconKey = "person",
          filterQuery = "Dalma",
        ),
        SmartCollection(
          id = "col_olivia",
          title = "Olívia",
          subtitle = "Photography & design picks",
          productCount = active.count { it.recipientName == "Olívia" || "Olívia" in it.tags },
          iconKey = "person",
          filterQuery = "Olívia",
        ),
        SmartCollection(
          id = "col_home",
          title = "Home",
          subtitle = "Botanicals, coffee & decor",
          productCount = active.count { "Home" in it.categories || "Home Decor" in it.tags },
          iconKey = "home",
          filterQuery = "Home",
        ),
      )
    }

  override fun observeAvailableCategories(): Flow<List<String>> =
    observeAllProducts().map { products ->
      val base = listOf("Electronics", "Audio", "Google", "Pixel", "Gift", "Home")
      val dynamic = products.flatMap { it.categories }.distinct()
      (base + dynamic).distinct()
    }

  override fun observeProductById(productId: String): Flow<Product?> =
    combine(productDao.observeProductById(productId), activeViewerFlow) { entity, viewerId ->
      entity?.takeIf { it.isVisibleToViewer(viewerId) }?.toDomain()
    }

  override fun observePriceHistory(productId: String, period: PricePeriod): Flow<List<PricePoint>> {
    val flow =
      if (period.days == null) {
        priceHistoryDao.observeAllHistory(productId)
      } else {
        val since = System.currentTimeMillis() - period.days!! * 86_400_000L
        priceHistoryDao.observeHistorySince(productId, since)
      }
    return flow.map { list ->
      list.map {
        PricePoint(
          id = it.id,
          productId = it.productId,
          timestampMillis = it.timestampMillis,
          priceHuf = it.priceHuf,
          storeName = it.storeName,
        )
      }
    }
  }

  override suspend fun saveDraftProduct(draft: ExtractedProductDraft): String {
    initializer.ensureSeeded()
    val id = "prod_${UUID.randomUUID().toString().take(8)}"
    val savings = (draft.originalPriceHuf - draft.priceHuf).coerceAtLeast(0)
    val pct =
      if (draft.originalPriceHuf > draft.priceHuf && draft.originalPriceHuf > 0) {
        -(((savings.toDouble() / draft.originalPriceHuf) * 100.0).roundToInt())
      } else {
        0
      }

    val targetList = draft.sharedListId?.let { sharedListDao.getSharedListById(it) }
    val entity =
      ProductEntity(
        id = id,
        title = draft.title,
        store = draft.store,
        currentPriceHuf = draft.priceHuf,
        originalPriceHuf = draft.originalPriceHuf.coerceAtLeast(draft.priceHuf),
        priceChangePercent = pct,
        savingsHuf = savings,
        imageKey = draft.imageKey,
        sourceUrl = draft.sourceUrl,
        barcode = draft.barcode,
        categoriesJson = repositoryJson.encodeToString(draft.categories),
        tagsJson = repositoryJson.encodeToString(draft.tags),
        recipientId = draft.recipientPrediction?.recipientId,
        recipientName = draft.recipientPrediction?.recipientName,
        recipientConfidence = draft.recipientPrediction?.confidencePercent ?: 0,
        sharedListId = targetList?.id,
        hiddenFromMemberId = targetList?.hiddenFromMemberId,
        collectionName = draft.categories.firstOrNull() ?: "Personal",
        isPurchased = false,
        isArchived = false,
        isPriceAlertEnabled = true,
        isFeaturedDeal = savings > 0,
        createdAtMillis = System.currentTimeMillis(),
        alternativesJson = repositoryJson.encodeToString(draft.alternatives),
        storeOffersJson = repositoryJson.encodeToString(draft.storeOffers),
        additionalImagesJson = repositoryJson.encodeToString(draft.additionalImages),
      )

    productDao.upsertProduct(entity)
    priceHistoryDao.insertHistory(WishDropSeedCatalog.generatePriceHistoryForProducts(listOf(entity)))
    val existingTags = taxonomyDao.getTagsSnapshot().associateBy { it.name }.toMutableMap()
    draft.tags.forEach { tag ->
      val current = existingTags[tag]?.usageCount ?: 0
      existingTags[tag] = TagEntity(name = tag, usageCount = current + 1)
    }
    taxonomyDao.upsertTags(existingTags.values.toList())
    collaborationDao.upsertProductTags(
      draft.tags.map { tag -> ProductTagCrossRefEntity(productId = id, tagName = tag) }
    )
    recordProductActivity(
      productId = id,
      sharedListId = targetList?.id,
      summarySuffix = "added ${draft.title}",
      eventType = "PRODUCT_ADDED",
    )
    remoteDataSource.notifyLocalMutation(
      table = "products",
      action = "INSERT",
      recordPayload =
        buildJsonObject {
          put("id", id)
          put("title", draft.title)
          put("store", draft.store)
          put("current_price_huf", draft.priceHuf)
        },
    )
    return id
  }

  override suspend fun setPurchased(productId: String, purchased: Boolean) {
    productDao.updatePurchased(productId, purchased)
    val product = productDao.getProductById(productId)
    recordProductActivity(
      productId = productId,
      sharedListId = product?.sharedListId,
      summarySuffix =
        if (purchased) "marked item purchased" else "reopened ${product?.title ?: "item"}",
      eventType = "PURCHASE_STATUS",
    )
    remoteDataSource.notifyLocalMutation("products", "UPDATE")
  }

  override suspend fun setArchived(productId: String, archived: Boolean) {
    productDao.updateArchived(productId, archived)
    remoteDataSource.notifyLocalMutation("products", "ARCHIVE")
  }

  override suspend fun deleteProduct(productId: String) {
    productDao.deleteProduct(productId)
    remoteDataSource.notifyLocalMutation("products", "DELETE")
  }

  override suspend fun togglePriceAlert(productId: String) {
    productDao.togglePriceAlert(productId)
  }

  override suspend fun assignRecipient(productId: String, recipientId: String?, confidence: Int) {
    val recipient = recipientId?.let { recipientDao.getRecipientById(it) }
    productDao.updateRecipient(
      productId = productId,
      recipientId = recipient?.id,
      recipientName = recipient?.name,
      confidence = if (recipient != null) confidence else 0,
    )
    val product = productDao.getProductById(productId)
    recordProductActivity(
      productId = productId,
      sharedListId = product?.sharedListId,
      summarySuffix = "assigned gift recipient to ${recipient?.name ?: "Personal"}",
      eventType = "RECIPIENT_CHANGED",
    )
    remoteDataSource.notifyLocalMutation("products", "UPDATE")
  }

  override suspend fun assignProductToSharedList(productId: String, sharedListId: String?) {
    val targetList = sharedListId?.let { sharedListDao.getSharedListById(it) }
    productDao.updateSharedListAssignment(
      productId = productId,
      sharedListId = targetList?.id,
      hiddenFromMemberId = targetList?.hiddenFromMemberId,
    )
    recordProductActivity(
      productId = productId,
      sharedListId = targetList?.id,
      summarySuffix =
        if (targetList != null) "moved item to ${targetList.emoji} ${targetList.title}"
        else "removed item from shared list",
      eventType = "LIST_ASSIGNED",
    )
    remoteDataSource.notifyLocalMutation("products", "UPDATE")
  }

  override suspend fun addProductPhoto(productId: String, photoUrl: String) {
    val current = productDao.getProductById(productId) ?: return
    val existing = runCatching {
      repositoryJson.decodeFromString<List<String>>(current.additionalImagesJson)
    }.getOrDefault(emptyList())
    val updatedList = (existing + photoUrl).distinct()
    val updated = current.copy(
      imageKey = if (current.imageKey.isBlank() || current.imageKey == "default") photoUrl else current.imageKey,
      additionalImagesJson = repositoryJson.encodeToString(updatedList),
    )
    productDao.upsertProduct(updated)
    remoteDataSource.notifyLocalMutation("products", "UPDATE")
  }

  private suspend fun recordProductActivity(
    productId: String,
    sharedListId: String?,
    summarySuffix: String,
    eventType: String,
  ) {
    val prefs = preferencesRepository.preferencesFlow.first()
    val member = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
    val actorName = member?.name ?: prefs.displayName.substringBefore(" ")
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = productId,
        sharedListId = sharedListId,
        actorMemberId = prefs.activeMemberId,
        actorName = actorName,
        actorAccentKey = member?.accentKey ?: "indigo",
        actionSummary = "$actorName $summarySuffix",
        eventType = eventType,
        createdAtMillis = System.currentTimeMillis(),
      )
    )
  }
}

@Singleton
class RoomGiftRepository
@Inject
constructor(
  private val initializer: DatabaseInitializer,
  private val recipientDao: GiftRecipientDao,
  private val productDao: ProductDao,
  private val priceHistoryDao: PriceHistoryDao,
  private val recommendationDao: RecommendationDao,
  private val preferencesRepository: UserPreferencesRepository,
) : GiftRepository {

  override fun observeRecipients(): Flow<List<GiftRecipient>> =
    recipientDao.observeRecipients().map { list -> list.map { it.toDomain() } }

  override fun observeRecipientById(recipientId: String): Flow<GiftRecipient?> =
    recipientDao.observeRecipientById(recipientId).map { it?.toDomain() }

  override fun observeSavedIdeasForRecipient(recipientId: String): Flow<List<Product>> =
    combine(
      productDao.observeProductsForRecipient(recipientId),
      preferencesRepository.preferencesFlow.map { it.activeMemberId }.distinctUntilChanged(),
    ) { list, viewerId ->
      list.filter { it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
    }

  override fun observeRecommendationsForRecipient(
    recipientId: String
  ): Flow<List<AiRecommendation>> =
    combine(
      recommendationDao.observeRecommendationsForRecipient(recipientId),
      preferencesRepository.preferencesFlow.map { it.dismissedRecommendationIds }.distinctUntilChanged(),
    ) { list, dismissed ->
      list.filterNot { rec ->
        rec.id in dismissed ||
          rec.title in dismissed ||
          rec.title.lowercase().trim() in dismissed ||
          dismissed.any { d -> d.length >= 4 && rec.title.contains(d, ignoreCase = true) }
      }.map { it.toDomain() }
    }

  override suspend fun updateGiftStage(recipientId: String, stage: GiftStage) {
    recipientDao.updateGiftStage(recipientId, stage.name)
  }

  override suspend fun addInterest(recipientId: String, interest: String) {
    val clean = interest.trim()
    if (clean.isEmpty()) return
    val current = recipientDao.getRecipientById(recipientId)?.toDomain() ?: return
    if (current.interests.any { it.equals(clean, ignoreCase = true) }) return
    val updated = current.interests + clean
    recipientDao.updateInterests(recipientId, repositoryJson.encodeToString(updated))
  }

  override suspend fun removeInterest(recipientId: String, interest: String) {
    val current = recipientDao.getRecipientById(recipientId)?.toDomain() ?: return
    val updated = current.interests.filterNot { it.equals(interest, ignoreCase = true) }
    recipientDao.updateInterests(recipientId, repositoryJson.encodeToString(updated))
  }

  override suspend fun addRecipient(
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    budgetHuf: Int,
    interests: List<String>,
    avatarUrl: String?,
  ): String {
    initializer.ensureSeeded()
    val id = "rec_${UUID.randomUUID().toString().take(6)}"
    val accents = listOf("coral", "mint", "lavender", "indigo")
    val avatars = listOf("avatar_dalma", "avatar_olivia", "avatar_armin", "avatar_attila")
    val index = name.hashCode().let { if (it < 0) -it else it } % 4
    val entity =
      GiftRecipientEntity(
        id = id,
        name = name.trim(),
        relationship = relationship.trim().ifBlank { "Family" },
        avatarKey = avatars[index],
        upcomingEventName = eventName.trim().ifBlank { "${name.trim()} Celebration" },
        daysUntilEvent = daysUntilEvent.coerceAtLeast(1),
        budgetHuf = budgetHuf.coerceAtLeast(5_000),
        interestsJson =
          repositoryJson.encodeToString(interests.ifEmpty { listOf("Gift", "Home Decor") }),
        giftStageName = GiftStage.IDEA.name,
        accentKey = accents[index],
        avatarUrl = avatarUrl,
      )
    recipientDao.upsertRecipient(entity)
    recommendationDao.upsertRecommendation(
      RecommendationEntity(
        id = "ai_rec_${UUID.randomUUID().toString().take(6)}",
        recipientId = id,
        title = "LEGO Bonsai",
        store = "LEGO Store",
        priceHuf = 17_990,
        imageKey = "lego_bonsai",
        matchPercentage = 91,
        reason = "Matches saved interests.",
        categoriesJson = repositoryJson.encodeToString(listOf("Gift", "Home")),
        tagsJson = repositoryJson.encodeToString(interests.take(3) + "Gift"),
        linkedProductId = null,
      )
    )
    return id
  }

  override suspend fun updateRecipient(
    recipientId: String,
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    interests: List<String>,
    avatarUrl: String?,
  ) {
    val current = recipientDao.getRecipientById(recipientId)
    if (current != null) {
      val updated =
        current.copy(
          name = name.trim(),
          relationship = relationship.trim().ifBlank { "Family" },
          upcomingEventName = eventName.trim().ifBlank { "Celebration" },
          daysUntilEvent = daysUntilEvent.coerceAtLeast(0),
          interestsJson = repositoryJson.encodeToString(interests),
          avatarUrl = avatarUrl ?: current.avatarUrl,
        )
      recipientDao.upsertRecipient(updated)
    } else {
      recipientDao.updateRecipientDetails(
        recipientId = recipientId,
        name = name.trim(),
        relationship = relationship.trim().ifBlank { "Family" },
        eventName = eventName.trim().ifBlank { "Celebration" },
        daysUntilEvent = daysUntilEvent.coerceAtLeast(0),
        interestsJson = repositoryJson.encodeToString(interests),
      )
    }
  }

  override suspend fun deleteRecipient(recipientId: String) {
    recipientDao.deleteRecipient(recipientId)
    productDao.clearRecipientFromProducts(recipientId)
    recommendationDao.deleteRecommendationsForRecipient(recipientId)
  }

  override suspend fun dismissRecommendation(recommendationId: String) {
    val existing = recommendationDao.getRecommendationById(recommendationId)
    val tokensToDismiss = mutableSetOf(recommendationId)
    if (existing != null) {
      tokensToDismiss.add(existing.title)
      tokensToDismiss.add(existing.title.lowercase().trim())
      val normalized = existing.title.lowercase().replace(Regex("[^a-z0-9áéíóöőúüű ]"), " ")
      tokensToDismiss.add(normalized.trim())
      normalized.split(" ")
        .map { it.trim() }
        .filter { it.length >= 4 }
        .forEach { tokensToDismiss.add(it) }
    }
    preferencesRepository.addDismissedRecommendations(tokensToDismiss)
    recommendationDao.deleteRecommendation(recommendationId)
  }

  private fun isGiftDismissedOrSaved(
    title: String,
    dismissedTokens: Set<String>,
    savedProductTitles: List<String>,
  ): Boolean {
    val cleanTitle = title.trim().lowercase()

    // 1. Direct or substring match with saved wishlist products
    for (saved in savedProductTitles) {
      val cleanSaved = saved.trim().lowercase()
      if (cleanSaved.isEmpty()) continue
      if (cleanTitle == cleanSaved) return true
      if (cleanSaved.length >= 6 && cleanTitle.contains(cleanSaved)) return true
      if (cleanTitle.length >= 6 && cleanSaved.contains(cleanTitle)) return true
    }

    // 2. Direct match with dismissed tokens
    if (dismissedTokens.any { it.equals(cleanTitle, ignoreCase = true) }) return true

    // 3. Keyword / substring match with dismissed items
    for (d in dismissedTokens) {
      val cleanD = d.trim().lowercase()
      if (cleanD.length < 4) continue
      if (cleanTitle.contains(cleanD) || cleanD.contains(cleanTitle)) return true

      val dWords = cleanD.split(Regex("[\\s\\-_/]+")).filter { it.length >= 4 }
      val titleWords = cleanTitle.split(Regex("[\\s\\-_/]+")).filter { it.length >= 4 }
      val common = dWords.intersect(titleWords.toSet())
      if (common.size >= 2) return true
      if (dWords.size == 1 && common.size == 1) {
        val kw = dWords.first()
        if (kw in listOf("bonsai", "szárítógép", "stagg", "kinto", "kindle", "phaidon", "airfryer", "ikebana", "pixel", "airdrop", "wh1000xm5")) {
          return true
        }
      }
    }
    return false
  }

  private data class GiftCandidate(
    val title: String,
    val store: String,
    val priceHuf: Int,
    val imageKey: String,
    val categories: List<String>,
    val tags: List<String>,
    val primaryInterest: String,
  )

  override suspend fun generateAiRecommendations(recipientId: String) {
    val recipientEntity = recipientDao.getRecipientById(recipientId) ?: return
    val recipient = recipientEntity.toDomain()
    val interests = recipient.interests
    val budget = recipient.budgetHuf.let { if (it <= 0) 35_000 else it }

    val prefs = preferencesRepository.preferencesFlow.first()
    val dismissed = prefs.dismissedRecommendationIds

    val savedProducts = productDao.getProductsForRecipientSnapshot(recipientId)
    val savedProductTitles = savedProducts.map { it.title }

    // Clear previous un-saved recommendations so they don't linger or duplicate
    recommendationDao.deleteUnsavedRecommendationsForRecipient(recipientId)

    val candidateCatalog = listOf(
      // Flowers & Botanical
      GiftCandidate("LEGO Icons Vadvirág Csokor", "LEGO Store", 19_990, "lego_bonsai", listOf("Flowers", "Decor", "LEGO"), listOf("Flowers", "Home Decor", "Creative Hobbies"), "Flowers"),
      GiftCandidate("Phaidon Flower Color Guide Botanikai Díszkönyv", "Libri", 14_990, "art_book", listOf("Flowers", "Books", "Art"), listOf("Flowers", "Books", "Art"), "Flowers"),
      GiftCandidate("Japán Ikebana Virágrendező Kerámia Készlet", "Kézműves Studio", 12_490, "art_book", listOf("Flowers", "Home Decor", "Creative Hobbies"), listOf("Flowers", "Home Decor", "Creative Hobbies"), "Flowers"),
      GiftCandidate("Smart Garden 3 Intelligens Beltéri Fűszerkert", "Alza", 34_990, "tech_accessory", listOf("Flowers", "Home", "Tech"), listOf("Flowers", "Home Decor", "Technology"), "Flowers"),
      GiftCandidate("Kézműves Minimalista Kerámia Virágváza", "Design Market", 11_990, "coffee_kettle", listOf("Home Decor", "Flowers"), listOf("Flowers", "Home Decor", "Art"), "Flowers"),

      // Coffee & Specialty
      GiftCandidate("Fellow Stagg EKG Hőfokszabályzós Kávékiöntő", "Specialty Roast", 42_990, "coffee_kettle", listOf("Coffee", "Kitchen", "Design"), listOf("Coffee", "Home Decor", "Technology"), "Coffee"),
      GiftCandidate("Kinto Sepia Kerámia Coffee Dripper & Kancsó", "Specialty Roast", 14_890, "coffee_kettle", listOf("Coffee", "Home Decor", "Gift"), listOf("Coffee", "Home Decor", "Gift"), "Coffee"),
      GiftCandidate("Timemore Chestnut C3 Precíziós Kézi Kávéőrlő", "Barista Tools", 27_990, "coffee_kettle", listOf("Coffee", "Kitchen"), listOf("Coffee", "Creative Hobbies", "Technology"), "Coffee"),
      GiftCandidate("Specialty Kávéválogatás 4 Földrészről (4x250g)", "Madal Café", 13_990, "coffee_kettle", listOf("Coffee", "Gourmet", "Gift"), listOf("Coffee", "Gift"), "Coffee"),
      GiftCandidate("Kézzel Készített Kerámia Flat White Csésze Szett", "Kézműves Kerámia", 10_990, "coffee_kettle", listOf("Coffee", "Home Decor", "Art"), listOf("Coffee", "Home Decor", "Creative Hobbies"), "Coffee"),

      // Books & Reading
      GiftCandidate("Kindle Paperwhite 16GB Vízálló E-könyv Olvasó", "Amazon Store", 59_990, "art_book", listOf("Books", "Technology", "Reading"), listOf("Books", "Technology", "Gift"), "Books"),
      GiftCandidate("Taschen Great Painters Művészeti Díszalbum", "Taschen", 22_500, "art_book", listOf("Books", "Art", "Culture"), listOf("Books", "Creative Hobbies", "Home Decor"), "Books"),
      GiftCandidate("Glocusent Prémium LED Olvasólámpa", "Book Haven", 9_990, "tech_accessory", listOf("Books", "Accessories"), listOf("Books", "Technology", "Home Decor"), "Books"),
      GiftCandidate("Természetes Tölgyfa Könyvtámasz Szett", "WoodCraft", 14_490, "art_book", listOf("Books", "Home Decor"), listOf("Books", "Home Decor", "Creative Hobbies"), "Books"),
      GiftCandidate("Klasszikus Bőrkötéses Világirodalmi Válogatás", "Helikon", 16_900, "art_book", listOf("Books", "Literature"), listOf("Books", "Gift"), "Books"),

      // Home Decor & Atmosphere
      GiftCandidate("Smart Ambient Intelligens Szabályozható Asztali Lámpa", "Philips Hue", 26_990, "tech_accessory", listOf("Home Decor", "Technology", "Lighting"), listOf("Home Decor", "Technology", "Gift"), "Home Decor"),
      GiftCandidate("Aromaterápiás Ultrahangos Kerámia Párásító", "Design Home", 16_990, "tech_accessory", listOf("Home Decor", "Wellness"), listOf("Home Decor", "Wellness", "Technology"), "Home Decor"),
      GiftCandidate("Természetes Szójaviasz Illatgyertya Szett Fa Kanóccal", "Candle Co.", 11_990, "coffee_kettle", listOf("Home Decor", "Wellness", "Gift"), listOf("Home Decor", "Creative Hobbies", "Gift"), "Home Decor"),
      GiftCandidate("Skandináv Meleg Gyapjú Kézzel Szőtt Díszpléd", "Nordic Living", 18_990, "wool_cap", listOf("Home Decor", "Fashion"), listOf("Home Decor", "Gift"), "Home Decor"),
      GiftCandidate("Marshall Emberton II Vezeték Nélküli Hangszóró", "Marshall Store", 49_990, "tech_accessory", listOf("Audio", "Home Decor", "Tech"), listOf("Home Decor", "Technology", "Audio"), "Home Decor"),

      // Creative Hobbies & Art
      GiftCandidate("Winsor & Newton Prémium Akvarell Festőkészlet", "Művészellátó", 18_900, "art_book", listOf("Creative Hobbies", "Art"), listOf("Creative Hobbies", "Art", "Gift"), "Creative Hobbies"),
      GiftCandidate("Kézműves Bőrkötéses Vázlatfüzet & Napló", "Paper & Co.", 12_990, "art_book", listOf("Creative Hobbies", "Books"), listOf("Creative Hobbies", "Books", "Home Decor"), "Creative Hobbies"),
      GiftCandidate("Szója Gyertyaöntő Kreatív Kezdőcsomag", "Craft Box", 13_990, "coffee_kettle", listOf("Creative Hobbies", "Home Decor"), listOf("Creative Hobbies", "Home Decor", "Gift"), "Creative Hobbies"),
      GiftCandidate("Lamy Safari Prémium Kaligráfia Készlet", "Lamy", 15_990, "art_book", listOf("Creative Hobbies", "Writing"), listOf("Creative Hobbies", "Books", "Office"), "Creative Hobbies"),
      GiftCandidate("Makramé Fali Dísz Készítő Komplett Csomag", "Kreatív Ház", 9_990, "art_book", listOf("Creative Hobbies", "Home Decor"), listOf("Creative Hobbies", "Home Decor"), "Creative Hobbies"),

      // Technology & Audio
      GiftCandidate("Google Pixel Buds Pro 2 Vezeték Nélküli Fülhallgató", "Google Store", 78_900, "pixel_buds", listOf("Technology", "Audio"), listOf("Technology", "Audio", "Pixel"), "Technology"),
      GiftCandidate("Anker MagGo Qi2 Mágneses Vezeték Nélküli Töltőállomás", "Anker Store", 19_990, "tech_accessory", listOf("Technology", "Accessories"), listOf("Technology", "Home Decor"), "Technology"),
      GiftCandidate("Xiaomi Smart Air Purifier 4 Légtisztító", "Xiaomi", 32_990, "tech_accessory", listOf("Technology", "Home"), listOf("Technology", "Home Decor", "Appliances"), "Technology"),
      GiftCandidate("Keychron K2 Vezeték Nélküli Mechanikus Billentyűzet", "Keychron", 36_990, "tech_accessory", listOf("Technology", "Work"), listOf("Technology", "Creative Hobbies"), "Technology"),
      GiftCandidate("Sony WH-1000XM5 Prémium Zajszűrős Fejhallgató", "Sony Center", 119_900, "tech_accessory", listOf("Technology", "Audio"), listOf("Technology", "Audio"), "Technology"),

      // Gaming
      GiftCandidate("8BitDo Ultimate Bluetooth Kontroller Dokkolóval", "Alza", 24_990, "tech_accessory", listOf("Gaming", "Tech"), listOf("Gaming", "Technology"), "Gaming"),
      GiftCandidate("Wingspan (Fesztáv) Prémium Madaras Társasjáték", "Gémklub", 19_990, "art_book", listOf("Gaming", "Board Games"), listOf("Gaming", "Creative Hobbies", "Gift"), "Gaming"),
      GiftCandidate("Catan Telepesei 3D Jubileumi Társasjáték", "Piatnik", 24_990, "art_book", listOf("Gaming", "Board Games"), listOf("Gaming", "Gift"), "Gaming"),
      GiftCandidate("SteelSeries Arctis Nova 7 Gamer Headset", "SteelSeries", 54_990, "tech_accessory", listOf("Gaming", "Audio"), listOf("Gaming", "Technology", "Audio"), "Gaming"),

      // Fashion & Accessories
      GiftCandidate("Valódi Olasz Bőr Kártyatartó & Pénztárca", "Leather Goods", 13_990, "art_book", listOf("Fashion", "Accessories"), listOf("Fashion", "Gift"), "Fashion"),
      GiftCandidate("100% Kasmír Finomkötött Téli Sál", "Cashmere Boutique", 22_990, "wool_cap", listOf("Fashion", "Clothing"), listOf("Fashion", "Gift"), "Fashion"),
      GiftCandidate("Ray-Ban Classic Polarizált Napszemüveg", "Optic World", 44_990, "tech_accessory", listOf("Fashion", "Accessories"), listOf("Fashion", "Gift"), "Fashion"),

      // Wellness & Self Care
      GiftCandidate("Theragun Mini Ütögetős Masszázspisztoly", "Therabody", 48_900, "tech_accessory", listOf("Wellness", "Fitness"), listOf("Wellness", "Technology", "Health"), "Wellness"),
      GiftCandidate("Rituals The Ritual of Sakura Prémium Ajándékszett", "Rituals", 17_990, "coffee_kettle", listOf("Wellness", "Beauty"), listOf("Wellness", "Gift"), "Wellness"),
      GiftCandidate("100% Eperfaselyem Alvómaszk & Párnahuzat", "Silk Dreams", 14_990, "art_book", listOf("Wellness", "Home Decor"), listOf("Wellness", "Home Decor", "Gift"), "Wellness"),

      // Cooking & Kitchen
      GiftCandidate("Le Creuset Zománcozott Öntöttvas Sütőtál", "Le Creuset", 49_990, "coffee_kettle", listOf("Cooking", "Kitchen"), listOf("Cooking", "Kitchen", "Home Decor"), "Cooking"),
      GiftCandidate("Wüsthof Classic Prémium Japán Szakácskés", "Késmánia", 34_990, "art_book", listOf("Cooking", "Kitchen"), listOf("Cooking", "Kitchen"), "Cooking"),
      GiftCandidate("Microplane Professzionális Reszelő Szett", "Chef Shop", 13_990, "coffee_kettle", listOf("Cooking", "Kitchen"), listOf("Cooking", "Gift"), "Cooking"),

      // Fitness & Sports
      GiftCandidate("Manduka PROlite Prémium Jógaszőnyeg", "Yoga Corner", 24_900, "art_book", listOf("Fitness", "Wellness"), listOf("Fitness", "Wellness"), "Fitness"),
      GiftCandidate("Hydro Flask Hőtartó Nemesacél Kulacs 946ml", "Outdoor Life", 15_990, "coffee_kettle", listOf("Fitness", "Accessories"), listOf("Fitness", "Gift"), "Fitness"),
      GiftCandidate("Garmin Forerunner Futó és Sportóra", "Garmin", 69_900, "pixel_watch", listOf("Fitness", "Technology"), listOf("Fitness", "Technology"), "Fitness"),
    )

    val availableCandidates = candidateCatalog.filter {
      !isGiftDismissedOrSaved(it.title, dismissed, savedProductTitles)
    }.shuffled().toMutableList()

    val selected = mutableListOf<GiftCandidate>()
    val shuffledInterests = interests.shuffled()

    val eventLower = recipient.upcomingEventName.lowercase()
    val isBirthday = eventLower.contains("szüli") || eventLower.contains("születés") || eventLower.contains("birthday")
    val isAnniversary = eventLower.contains("évforduló") || eventLower.contains("anniversary") || eventLower.contains("házasság")
    val isChristmas = eventLower.contains("karácsony") || eventLower.contains("christmas")
    val isNameday = eventLower.contains("névnap") || eventLower.contains("nameday")

    val eventPreferredCategories = when {
      isAnniversary -> listOf("Coffee", "Home Decor", "Fashion", "Wellness", "Art")
      isChristmas -> listOf("Home Decor", "Gaming", "Books", "Wellness", "Cooking")
      isNameday -> listOf("Flowers", "Coffee", "Books", "Wellness")
      isBirthday -> listOf("Technology", "Creative Hobbies", "Gaming", "Books", "Coffee", "Flowers")
      else -> emptyList()
    }

    // 1. Pick candidates covering different interests from the recipient's profile, prioritizing event-appropriate items
    for (interest in shuffledInterests) {
      if (selected.size >= 3) break
      val match = availableCandidates.firstOrNull { cand ->
        val matchesInterest = cand.primaryInterest.equals(interest, ignoreCase = true) ||
          cand.tags.any { t -> t.contains(interest, ignoreCase = true) || interest.contains(t, ignoreCase = true) } ||
          cand.categories.any { c -> c.contains(interest, ignoreCase = true) || interest.contains(c, ignoreCase = true) }
        val matchesEvent = eventPreferredCategories.isEmpty() || cand.categories.any { it in eventPreferredCategories }
        matchesInterest && matchesEvent
      } ?: availableCandidates.firstOrNull { cand ->
        cand.primaryInterest.equals(interest, ignoreCase = true) ||
          cand.tags.any { t -> t.contains(interest, ignoreCase = true) || interest.contains(t, ignoreCase = true) } ||
          cand.categories.any { c -> c.contains(interest, ignoreCase = true) || interest.contains(c, ignoreCase = true) }
      }
      if (match != null) {
        selected.add(match)
        availableCandidates.remove(match)
      }
    }

    // 2. If fewer than 3, pick from remaining available candidates matching any interest
    if (selected.size < 3) {
      val anyMatch = availableCandidates.filter { cand ->
        interests.isEmpty() || cand.tags.any { t -> interests.any { i -> i.contains(t, ignoreCase = true) || t.contains(i, ignoreCase = true) } }
      }
      for (cand in anyMatch) {
        if (selected.size >= 3) break
        selected.add(cand)
        availableCandidates.remove(cand)
      }
    }

    // 3. If still fewer than 3, pick from whatever is available in catalog that is not dismissed or saved
    if (selected.size < 3) {
      for (cand in availableCandidates) {
        if (selected.size >= 3) break
        selected.add(cand)
      }
    }

    // 4. If catalog ran out for this person's custom interests, synthesize dynamic custom recommendations
    if (selected.size < 3 && interests.isNotEmpty()) {
      for (interest in shuffledInterests) {
        if (selected.size >= 3) break
        val synthTitle = "${interest.replaceFirstChar { it.uppercase() }} Prémium Ajándékválogatás"
        if (!isGiftDismissedOrSaved(synthTitle, dismissed, savedProductTitles) && selected.none { it.title == synthTitle }) {
          selected.add(
            GiftCandidate(
              title = synthTitle,
              store = "WishDrop AI Verified",
              priceHuf = 16_990,
              imageKey = "art_book",
              categories = listOf(interest, "Gift"),
              tags = listOf(interest, "Personalized", "Gift"),
              primaryInterest = interest,
            )
          )
        }
      }
    }

    selected.forEach { item ->
      val recId = "ai_rec_${UUID.randomUUID().toString().take(6)}"
      val matchingLabel = if (item.primaryInterest.isNotBlank()) item.primaryInterest else interests.joinToString(", ").ifBlank { "Személyes preferenciák" }
      val eventReason = when {
        isBirthday -> "${recipient.name} születésnapjára, a(z) $matchingLabel iránti érdeklődése alapján személyre szabott meglepetésként ajánlva."
        isAnniversary -> "${recipient.name} évfordulójára különleges, elegáns meglepetésként ($matchingLabel) AI mélyelemzéssel ajánlva."
        isChristmas -> "${recipient.name} karácsonyi meglepetéseként a fa alá, a(z) $matchingLabel témakörben AI által válogatva."
        isNameday -> "${recipient.name} névnapjára figyelmes meglepetésként ($matchingLabel) AI által ajánlva."
        else -> "${recipient.name} részére a(z) ${recipient.upcomingEventName} alkalmából, $matchingLabel érdeklődése alapján ajánlva."
      }
      recommendationDao.upsertRecommendation(
        RecommendationEntity(
          id = recId,
          recipientId = recipientId,
          title = item.title,
          store = item.store,
          priceHuf = item.priceHuf.coerceAtMost(budget),
          imageKey = item.imageKey,
          matchPercentage = (91..99).random(),
          reason = eventReason,
          categoriesJson = repositoryJson.encodeToString(item.categories),
          tagsJson = repositoryJson.encodeToString(interests + item.tags),
          linkedProductId = null,
        )
      )
    }
  }

  override suspend fun saveRecommendationToWishlist(recommendation: AiRecommendation): String {
    recommendation.linkedProductId?.let { existingId ->
      if (productDao.getProductById(existingId) != null) return existingId
    }
    val recipient = recipientDao.getRecipientById(recommendation.recipientId)
    val newId = "prod_${UUID.randomUUID().toString().take(8)}"
    val entity =
      ProductEntity(
        id = newId,
        title = recommendation.title,
        store = recommendation.store,
        currentPriceHuf = recommendation.priceHuf,
        originalPriceHuf = recommendation.priceHuf,
        priceChangePercent = 0,
        savingsHuf = 0,
        imageKey = recommendation.imageKey,
        categoriesJson = repositoryJson.encodeToString(recommendation.categories),
        tagsJson = repositoryJson.encodeToString(recommendation.tags),
        recipientId = recommendation.recipientId,
        recipientName = recipient?.name,
        recipientConfidence = recommendation.matchPercentage,
        collectionName = recipient?.name ?: "Gift",
        isPurchased = false,
        isArchived = false,
        isPriceAlertEnabled = true,
        isFeaturedDeal = false,
        createdAtMillis = System.currentTimeMillis(),
      )
    productDao.upsertProduct(entity)
    priceHistoryDao.insertHistory(WishDropSeedCatalog.generatePriceHistoryForProducts(listOf(entity)))
    recommendationDao.upsertRecommendation(
      RecommendationEntity(
        id = recommendation.id,
        recipientId = recommendation.recipientId,
        title = recommendation.title,
        store = recommendation.store,
        priceHuf = recommendation.priceHuf,
        imageKey = recommendation.imageKey,
        matchPercentage = recommendation.matchPercentage,
        reason = recommendation.reason,
        categoriesJson = repositoryJson.encodeToString(recommendation.categories),
        tagsJson = repositoryJson.encodeToString(recommendation.tags),
        linkedProductId = newId,
      )
    )
    return newId
  }
}

@Singleton
class RoomInsightsRepository
@Inject
constructor(
  private val productDao: ProductDao,
  private val preferencesRepository: UserPreferencesRepository,
) : InsightsRepository {

  override fun observeInsightsReport(): Flow<InsightsReport> =
    combine(
      productDao.observeAllProducts(),
      preferencesRepository.preferencesFlow.map { it.activeMemberId }.distinctUntilChanged(),
    ) { entities, viewerId ->
      val allProducts =
        entities.filter { !it.isArchived && it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
      val yearlySaved = allProducts.sumOf { it.savingsHuf }
      val activePotential = allProducts.filter { !it.isPurchased }.sumOf { it.savingsHuf }

      val topPurchase =
        allProducts.find { it.id == "prod_pixel_buds_2a" }
          ?: allProducts.maxByOrNull { it.savingsHuf }

      val totalCount = allProducts.size.coerceAtLeast(1)
      val electronicsItems = allProducts.filter { "Electronics" in it.categories }
      val giftItems =
        allProducts.filter { "Electronics" !in it.categories && "Gift" in it.categories }
      val homeItems =
        allProducts.filter {
          "Electronics" !in it.categories && "Gift" !in it.categories && "Home" in it.categories
        }

      val rawElectronics = electronicsItems.size
      val rawGift = allProducts.count { "Gift" in it.categories }
      val rawHome = allProducts.count { "Home" in it.categories }
      val rawOther =
        allProducts
          .count {
            "Electronics" !in it.categories && "Gift" !in it.categories && "Home" !in it.categories
          }
          .coerceAtLeast(2)
      val bucketSum = (rawElectronics + rawGift + rawHome + rawOther).coerceAtLeast(1)

      val pElec = ((rawElectronics * 100.0) / bucketSum).roundToInt()
      val pGift = ((rawGift * 100.0) / bucketSum).roundToInt()
      val pHome = ((rawHome * 100.0) / bucketSum).roundToInt()
      val pOther = (100 - pElec - pGift - pHome).coerceAtLeast(4)

      val distribution =
        listOf(
          CategoryDistribution(
            categoryName = "Electronics",
            percentage = pElec,
            itemCount = rawElectronics,
            totalValueHuf = electronicsItems.sumOf { it.currentPriceHuf },
            colorIndex = 0,
          ),
          CategoryDistribution(
            categoryName = "Gift",
            percentage = pGift,
            itemCount = rawGift,
            totalValueHuf = giftItems.sumOf { it.currentPriceHuf },
            colorIndex = 1,
          ),
          CategoryDistribution(
            categoryName = "Home",
            percentage = pHome,
            itemCount = rawHome,
            totalValueHuf = homeItems.sumOf { it.currentPriceHuf },
            colorIndex = 2,
          ),
          CategoryDistribution(
            categoryName = "Other",
            percentage = pOther,
            itemCount = totalCount.coerceAtLeast(1),
            totalValueHuf = allProducts.sumOf { it.currentPriceHuf } / 10,
            colorIndex = 3,
          ),
        )

      val tagCounts = mutableMapOf<String, Int>()
      allProducts.forEach { product ->
        product.tags.forEach { tag -> tagCounts[tag] = (tagCounts[tag] ?: 0) + 1 }
      }
      val commonTags =
        tagCounts.entries
          .sortedByDescending { it.value }
          .take(12)
          .map { TagStat(it.key, it.value) }

      val biggestDrops =
        allProducts
          .filter { !it.isPurchased && it.savingsHuf > 0 }
          .sortedWith(
            compareBy<Product> { it.priceChangePercent }.thenByDescending { it.savingsHuf }
          )

      InsightsReport(
        yearlySavedHuf = yearlySaved,
        activePotentialSavingsHuf = activePotential,
        topSavedProduct = topPurchase,
        categoryDistribution = distribution,
        commonTags = commonTags,
        biggestPriceDrops = biggestDrops,
      )
    }
}

@Singleton
class RoomBackupRepository
@Inject
constructor(
  private val database: WishDropDatabase,
  private val initializer: DatabaseInitializer,
  private val productDao: ProductDao,
  private val sharedListDao: SharedListDao,
  private val collaborationDao: CollaborationDao,
  private val wishlistDao: WishlistDao,
  private val priceHistoryDao: PriceHistoryDao,
  private val recipientDao: GiftRecipientDao,
  private val taxonomyDao: TaxonomyDao,
  private val recommendationDao: RecommendationDao,
) : BackupRepository {

  override suspend fun exportJsonBackup(): String {
    initializer.ensureSeeded()
    val payload =
      WishDropBackupPayload(
        exportedAtMillis = System.currentTimeMillis(),
        products = productDao.getAllSnapshot(),
        wishlists = wishlistDao.getAllSnapshot(),
        priceHistory = priceHistoryDao.getAllSnapshot(),
        recipients = recipientDao.getAllSnapshot(),
        categories = taxonomyDao.getCategoriesSnapshot(),
        tags = taxonomyDao.getTagsSnapshot(),
        recommendations = recommendationDao.getAllSnapshot(),
        sharedLists = sharedListDao.getAllSharedListsSnapshot(),
        listMembers = sharedListDao.getAllMembersSnapshot(),
        comments = collaborationDao.getAllCommentsSnapshot(),
        activityEvents = collaborationDao.getAllActivitySnapshot(),
        invitations = collaborationDao.getAllInvitationsSnapshot(),
      )
    return repositoryJson.encodeToString(payload)
  }

  override suspend fun importJsonBackup(jsonPayload: String): Result<Int> =
    runCatching {
      val payload = repositoryJson.decodeFromString<WishDropBackupPayload>(jsonPayload)
      database.withTransaction {
        collaborationDao.clearComments()
        collaborationDao.clearActivityEvents()
        collaborationDao.clearInvitations()
        sharedListDao.clearMembers()
        sharedListDao.clearLists()
        productDao.clearAll()
        wishlistDao.clearAll()
        priceHistoryDao.clearAll()
        recipientDao.clearAll()
        taxonomyDao.clearCategories()
        taxonomyDao.clearTags()
        recommendationDao.clearAll()

        recipientDao.upsertRecipients(payload.recipients)
        wishlistDao.upsertWishlists(payload.wishlists)
        productDao.upsertProducts(payload.products)
        priceHistoryDao.insertHistory(payload.priceHistory)
        taxonomyDao.upsertCategories(payload.categories)
        taxonomyDao.upsertTags(payload.tags)
        recommendationDao.upsertRecommendations(payload.recommendations)
        if (payload.sharedLists.isNotEmpty()) {
          sharedListDao.upsertSharedLists(payload.sharedLists)
        } else {
          sharedListDao.upsertSharedLists(WishDropSeedCatalog.defaultSharedLists())
        }
        if (payload.listMembers.isNotEmpty()) {
          sharedListDao.upsertListMembers(payload.listMembers)
        } else {
          sharedListDao.upsertListMembers(WishDropSeedCatalog.defaultListMembers())
        }
        collaborationDao.upsertComments(payload.comments)
        collaborationDao.insertActivityEvents(payload.activityEvents)
        collaborationDao.upsertInvitations(payload.invitations)
      }
      payload.products.size
    }

  override suspend fun resetToDefaultCatalog() {
    initializer.forceReseed()
  }
}
