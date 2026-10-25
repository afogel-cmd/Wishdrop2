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
    recommendationDao.observeRecommendationsForRecipient(recipientId).map { list ->
      list.map { it.toDomain() }
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
