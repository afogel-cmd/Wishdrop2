package com.example.data.repository

import com.example.core.preferences.UserPreferencesRepository
import com.example.data.local.dao.CollaborationDao
import com.example.data.local.dao.GiftRecipientDao
import com.example.data.local.dao.PriceHistoryDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SharedListDao
import com.example.data.local.dao.UserAndFamilyDao
import com.example.data.local.entity.ActivityEventEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.ListInvitationEntity
import com.example.data.local.entity.ListMemberEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SharedListEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.SupabaseRemoteDataSource
import com.example.data.seed.WishDropSeedCatalog
import com.example.domain.model.ActivityEvent
import com.example.domain.model.FamilyPersona
import com.example.domain.model.InvitationStatus
import com.example.domain.model.ListInvitation
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.ProductComment
import com.example.domain.model.RealtimeSyncStatus
import com.example.domain.model.SharedListDetails
import com.example.domain.model.SharedListMember
import com.example.domain.model.SharedListSummary
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CollaborationRepository
import com.example.domain.repository.SharedListRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun ListMemberEntity.toDomain(): SharedListMember =
  SharedListMember(
    listId = listId,
    memberId = memberId,
    memberName = memberName,
    memberEmail = memberEmail,
    avatarKey = avatarKey,
    accentKey = accentKey,
    role = runCatching { ListMemberRole.valueOf(role) }.getOrDefault(ListMemberRole.VIEWER),
  )

private fun CommentEntity.toDomain(): ProductComment =
  ProductComment(
    id = id,
    productId = productId,
    sharedListId = sharedListId,
    authorMemberId = authorMemberId,
    authorName = authorName,
    authorAccentKey = authorAccentKey,
    message = message,
    createdAtMillis = createdAtMillis,
  )

private fun ActivityEventEntity.toDomain(): ActivityEvent =
  ActivityEvent(
    id = id,
    productId = productId,
    sharedListId = sharedListId,
    actorMemberId = actorMemberId,
    actorName = actorName,
    actorAccentKey = actorAccentKey,
    actionSummary = actionSummary,
    eventType = eventType,
    createdAtMillis = createdAtMillis,
  )

private fun ListInvitationEntity.toDomain(): ListInvitation =
  ListInvitation(
    id = id,
    listId = listId,
    listTitle = listTitle,
    inviterName = inviterName,
    inviteeEmail = inviteeEmail,
    proposedRole =
      runCatching { ListMemberRole.valueOf(proposedRole) }.getOrDefault(ListMemberRole.EDITOR),
    status = runCatching { InvitationStatus.valueOf(status) }.getOrDefault(InvitationStatus.PENDING),
    createdAtMillis = createdAtMillis,
  )

@Singleton
class OfflineFirstSharedListRepository
@Inject
constructor(
  private val initializer: DatabaseInitializer,
  private val sharedListDao: SharedListDao,
  private val productDao: ProductDao,
  private val priceHistoryDao: PriceHistoryDao,
  private val collaborationDao: CollaborationDao,
  private val userAndFamilyDao: UserAndFamilyDao,
  private val recipientDao: GiftRecipientDao,
  private val preferencesRepository: UserPreferencesRepository,
  private val remoteDataSource: SupabaseRemoteDataSource,
) : SharedListRepository {

  private val activeViewerFlow: Flow<String> =
    preferencesRepository.preferencesFlow.map { it.activeMemberId }.distinctUntilChanged()

  override fun observeVisibleSharedLists(): Flow<List<SharedListSummary>> =
    combine(
      sharedListDao.observeAllSharedLists(),
      sharedListDao.observeAllListMembers(),
      productDao.observeActiveProducts(),
      activeViewerFlow,
    ) { lists, members, activeProducts, viewerId ->
      val membersByList = members.groupBy { it.listId }
      lists
        .filter { list ->
          val ids = membersByList[list.id].orEmpty().map { it.memberId }.toSet()
          list.isVisibleToViewer(viewerId, ids)
        }
        .map { list ->
          val visibleLinkedCount =
            activeProducts.count {
              it.sharedListId == list.id && it.isVisibleToViewer(viewerId)
            }
          list.toSummaryDomain(membersByList[list.id].orEmpty(), visibleLinkedCount)
        }
    }

  override fun observeSharedListDetails(listId: String): Flow<SharedListDetails?> {
    val listAndMembers =
      combine(
        sharedListDao.observeSharedListById(listId),
        sharedListDao.observeMembersForList(listId),
        productDao.observeProductsForSharedList(listId),
      ) { list, members, products ->
        Triple(list, members, products)
      }
    val collabAndViewer =
      combine(
        collaborationDao.observeActivityForList(listId),
        collaborationDao.observePendingInvitationsForList(listId),
        activeViewerFlow,
        remoteDataSource.syncStatus,
      ) { activity, invites, viewerId, sync ->
        listOf(activity, invites, viewerId, sync)
      }

    return combine(listAndMembers, collabAndViewer) { (listEntity, memberEntities, productEntities), meta ->
      @Suppress("UNCHECKED_CAST")
      val activityEntities = meta[0] as List<ActivityEventEntity>
      @Suppress("UNCHECKED_CAST")
      val inviteEntities = meta[1] as List<ListInvitationEntity>
      val viewerId = meta[2] as String
      val syncStatus = meta[3] as RealtimeSyncStatus

      val list = listEntity ?: return@combine null
      val memberIds = memberEntities.map { it.memberId }.toSet()
      if (!list.isVisibleToViewer(viewerId, memberIds)) {
        return@combine null
      }

      val visibleProducts =
        productEntities.filter { it.isVisibleToViewer(viewerId) }.map { it.toDomain() }
      val summary = list.toSummaryDomain(memberEntities, visibleProducts.count { !it.isPurchased })
      val currentRole =
        memberEntities
          .find { it.memberId == viewerId }
          ?.let { runCatching { ListMemberRole.valueOf(it.role) }.getOrNull() }
      val canEdit =
        list.ownerMemberId == viewerId ||
          currentRole == ListMemberRole.OWNER ||
          currentRole == ListMemberRole.EDITOR ||
          currentRole == ListMemberRole.HIDDEN_GIFT_MANAGER

      SharedListDetails(
        summary = summary,
        members = memberEntities.map { it.toDomain() },
        products = visibleProducts,
        activityFeed = activityEntities.map { it.toDomain() },
        pendingInvitations = inviteEntities.map { it.toDomain() },
        canEdit = canEdit,
        isRealtimeConnected = syncStatus.isRealtimeSocketActive,
      )
    }
  }

  override suspend fun createSharedList(
    title: String,
    emoji: String,
    subtitle: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ): String {
    initializer.ensureSeeded()
    val prefs = preferencesRepository.preferencesFlow.first()
    val owner = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
    val hiddenRecipient = hiddenFromMemberId?.let { recipientDao.getRecipientById(it) }
    val now = System.currentTimeMillis()
    val listId = "sl_${UUID.randomUUID().toString().take(8)}"

    val listEntity =
      SharedListEntity(
        id = listId,
        title = title.trim().ifBlank { "Shared Wishlist" },
        emoji = emoji.trim().ifBlank { "🎁" },
        subtitle = subtitle.trim().ifBlank { "Collaborative family wishlist" },
        ownerMemberId = prefs.activeMemberId,
        visibility = visibility.name,
        hiddenFromMemberId = hiddenRecipient?.id,
        hiddenFromMemberName = hiddenRecipient?.name,
        baselineItemCount = 0,
        eventDateLabel =
          if (hiddenRecipient != null) "Secret • Hidden from ${hiddenRecipient.name}"
          else visibility.label,
        updatedAtMillis = now,
      )
    sharedListDao.upsertSharedList(listEntity)
    sharedListDao.upsertListMember(
      ListMemberEntity(
        listId = listId,
        memberId = prefs.activeMemberId,
        memberName = owner?.name ?: prefs.displayName.substringBefore(" "),
        memberEmail = owner?.email ?: prefs.email,
        avatarKey = owner?.avatarKey ?: "avatar_attila",
        accentKey = owner?.accentKey ?: "indigo",
        role = ListMemberRole.OWNER.name,
        joinedAtMillis = now,
      )
    )
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = null,
        sharedListId = listId,
        actorMemberId = prefs.activeMemberId,
        actorName = owner?.name ?: "Attila",
        actorAccentKey = owner?.accentKey ?: "indigo",
        actionSummary =
          "${owner?.name ?: "Attila"} created ${listEntity.emoji} ${listEntity.title}",
        eventType = "LIST_CREATED",
        createdAtMillis = now,
      )
    )
    remoteDataSource.notifyLocalMutation(
      table = "shared_lists",
      action = "INSERT",
      recordPayload =
        buildJsonObject {
          put("id", listId)
          put("title", listEntity.title)
          put("emoji", listEntity.emoji)
          put("visibility", visibility.name)
        },
    )
    return listId
  }

  override suspend fun updateListVisibility(
    listId: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ) {
    val resolvedHiddenId =
      if (visibility == ListVisibility.HIDDEN) hiddenFromMemberId ?: "rec_dalma"
      else hiddenFromMemberId
    val hiddenRecipient = resolvedHiddenId?.let { recipientDao.getRecipientById(it) }
    val now = System.currentTimeMillis()
    sharedListDao.updateVisibility(
      listId = listId,
      visibility = visibility.name,
      hiddenFromMemberId = hiddenRecipient?.id,
      hiddenFromMemberName = hiddenRecipient?.name,
      updatedAtMillis = now,
    )
    productDao.updateHiddenRecipientForListProducts(
      sharedListId = listId,
      hiddenFromMemberId = hiddenRecipient?.id,
    )

    val prefs = preferencesRepository.preferencesFlow.first()
    val actor = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
    val actorName = actor?.name ?: "Attila"
    val modeSummary =
      if (hiddenRecipient != null) {
        "$actorName enabled Secret Gift protection (hidden from ${hiddenRecipient.name})"
      } else {
        "$actorName changed list visibility to ${visibility.label}"
      }
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = null,
        sharedListId = listId,
        actorMemberId = prefs.activeMemberId,
        actorName = actorName,
        actorAccentKey = actor?.accentKey ?: "indigo",
        actionSummary = modeSummary,
        eventType = "VISIBILITY_CHANGED",
        createdAtMillis = now,
      )
    )
    remoteDataSource.notifyLocalMutation("shared_lists", "UPDATE")
  }

  override suspend fun updateMemberRole(listId: String, memberId: String, role: ListMemberRole) {
    sharedListDao.updateMemberRole(listId = listId, memberId = memberId, role = role.name)
    val targetMember = userAndFamilyDao.getFamilyMemberById(memberId)
    val prefs = preferencesRepository.preferencesFlow.first()
    val actor = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = null,
        sharedListId = listId,
        actorMemberId = prefs.activeMemberId,
        actorName = actor?.name ?: "Attila",
        actorAccentKey = actor?.accentKey ?: "indigo",
        actionSummary =
          "${actor?.name ?: "Attila"} updated ${targetMember?.name ?: "member"} role to ${role.label}",
        eventType = "ROLE_UPDATED",
        createdAtMillis = System.currentTimeMillis(),
      )
    )
    remoteDataSource.notifyLocalMutation("list_members", "UPDATE")
  }

  override suspend fun addProductIdeaToList(
    listId: String,
    title: String,
    store: String,
    priceHuf: Int,
  ): String {
    initializer.ensureSeeded()
    val list = sharedListDao.getSharedListById(listId)
    val cleanTitle = title.trim().ifBlank { "Shared Gift Idea" }
    val cleanStore = store.trim().ifBlank { "Alza" }
    val cleanPrice = priceHuf.coerceAtLeast(1_990)
    val id = "prod_${UUID.randomUUID().toString().take(8)}"
    val now = System.currentTimeMillis()
    val prefs = preferencesRepository.preferencesFlow.first()
    val actor = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)

    val entity =
      ProductEntity(
        id = id,
        title = cleanTitle,
        store = cleanStore,
        currentPriceHuf = cleanPrice,
        originalPriceHuf = cleanPrice,
        priceChangePercent = 0,
        savingsHuf = 0,
        imageKey = resolveQuickImageKey(cleanTitle),
        categoriesJson = repositoryJson.encodeToString(listOf("Gift", "Shared")),
        tagsJson =
          repositoryJson.encodeToString(
            listOfNotNull("Shared", list?.title?.substringBefore(" "))
          ),
        recipientId = list?.hiddenFromMemberId,
        recipientName = list?.hiddenFromMemberName,
        recipientConfidence = if (list?.hiddenFromMemberId != null) 95 else 0,
        sharedListId = listId,
        hiddenFromMemberId = list?.hiddenFromMemberId,
        collectionName = list?.title ?: "Shared",
        isPurchased = false,
        isArchived = false,
        isPriceAlertEnabled = true,
        isFeaturedDeal = false,
        createdAtMillis = now,
      )
    productDao.upsertProduct(entity)
    priceHistoryDao.insertHistory(WishDropSeedCatalog.generatePriceHistoryForProducts(listOf(entity)))
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = id,
        sharedListId = listId,
        actorMemberId = prefs.activeMemberId,
        actorName = actor?.name ?: "Attila",
        actorAccentKey = actor?.accentKey ?: "indigo",
        actionSummary = "${actor?.name ?: "Attila"} added $cleanTitle",
        eventType = "PRODUCT_ADDED",
        createdAtMillis = now,
      )
    )
    remoteDataSource.notifyLocalMutation("products", "INSERT")
    return id
  }

  private fun resolveQuickImageKey(title: String): String {
    val lower = title.lowercase()
    return when {
      lower.contains("buds") || lower.contains("pixel") -> "pixel_buds"
      lower.contains("watch") -> "pixel_watch"
      lower.contains("bonsai") -> "lego_bonsai"
      lower.contains("lego") || lower.contains("orchid") -> "lego_orchid"
      lower.contains("nothing") || lower.contains("ear") -> "nothing_ear"
      lower.contains("coffee") || lower.contains("kettle") -> "coffee_kettle"
      lower.contains("book") -> "art_book"
      else -> "home_decor"
    }
  }
}

@Singleton
class OfflineFirstCollaborationRepository
@Inject
constructor(
  private val initializer: DatabaseInitializer,
  private val collaborationDao: CollaborationDao,
  private val sharedListDao: SharedListDao,
  private val productDao: ProductDao,
  private val userAndFamilyDao: UserAndFamilyDao,
  private val preferencesRepository: UserPreferencesRepository,
  private val remoteDataSource: SupabaseRemoteDataSource,
) : CollaborationRepository {

  override fun observeCommentsForProduct(productId: String): Flow<List<ProductComment>> =
    collaborationDao.observeCommentsForProduct(productId).map { list ->
      list.map { it.toDomain() }
    }

  override fun observeActivityForProduct(productId: String): Flow<List<ActivityEvent>> =
    collaborationDao.observeActivityForProduct(productId).map { list ->
      list.map { it.toDomain() }
    }

  override fun observePendingInvitations(): Flow<List<ListInvitation>> =
    combine(
      collaborationDao.observePendingInvitations(),
      preferencesRepository.preferencesFlow.map { it.activeMemberId }.distinctUntilChanged(),
    ) { invites, viewerId ->
      invites
        .filter { invite ->
          val list = sharedListDao.getSharedListById(invite.listId)
          list?.hiddenFromMemberId != viewerId
        }
        .map { it.toDomain() }
    }

  override fun observeRealtimeSyncStatus(): Flow<RealtimeSyncStatus> = remoteDataSource.syncStatus

  override suspend fun addComment(productId: String, message: String) {
    val clean = message.trim()
    if (clean.isEmpty()) return
    initializer.ensureSeeded()
    val prefs = preferencesRepository.preferencesFlow.first()
    val member = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
    val authorName = member?.name ?: prefs.displayName.substringBefore(" ")
    val product = productDao.getProductById(productId)
    val now = System.currentTimeMillis()
    val commentId = "cmt_${UUID.randomUUID().toString().take(8)}"

    collaborationDao.upsertComment(
      CommentEntity(
        id = commentId,
        productId = productId,
        sharedListId = product?.sharedListId,
        authorMemberId = prefs.activeMemberId,
        authorName = authorName,
        authorAccentKey = member?.accentKey ?: "indigo",
        message = clean,
        createdAtMillis = now,
      )
    )
    collaborationDao.insertActivityEvent(
      ActivityEventEntity(
        id = "act_${UUID.randomUUID().toString().take(8)}",
        productId = productId,
        sharedListId = product?.sharedListId,
        actorMemberId = prefs.activeMemberId,
        actorName = authorName,
        actorAccentKey = member?.accentKey ?: "indigo",
        actionSummary = "$authorName commented: \"$clean\"",
        eventType = "COMMENT_ADDED",
        createdAtMillis = now,
      )
    )
    remoteDataSource.notifyLocalMutation(
      table = "comments",
      action = "INSERT",
      recordPayload =
        buildJsonObject {
          put("id", commentId)
          put("product_id", productId)
          put("author_name", authorName)
          put("message", clean)
        },
    )
  }

  override suspend fun sendInvitation(
    listId: String,
    inviteeEmail: String,
    proposedRole: ListMemberRole,
  ): Result<ListInvitation> =
    runCatching {
      val cleanEmail = inviteeEmail.trim()
      require(cleanEmail.contains("@") && cleanEmail.contains(".")) {
        "Please enter a valid collaborator email address"
      }
      initializer.ensureSeeded()
      val list =
        sharedListDao.getSharedListById(listId)
          ?: throw IllegalArgumentException("Shared list not found")
      val prefs = preferencesRepository.preferencesFlow.first()
      val actor = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
      val inviterName = actor?.name ?: prefs.displayName.substringBefore(" ")
      val now = System.currentTimeMillis()

      val invitation =
        ListInvitationEntity(
          id = "inv_${UUID.randomUUID().toString().take(8)}",
          listId = listId,
          listTitle = "${list.emoji} ${list.title}",
          inviterName = inviterName,
          inviteeEmail = cleanEmail,
          proposedRole = proposedRole.name,
          status = InvitationStatus.PENDING.name,
          createdAtMillis = now,
        )
      collaborationDao.upsertInvitation(invitation)
      collaborationDao.insertActivityEvent(
        ActivityEventEntity(
          id = "act_${UUID.randomUUID().toString().take(8)}",
          productId = null,
          sharedListId = listId,
          actorMemberId = prefs.activeMemberId,
          actorName = inviterName,
          actorAccentKey = actor?.accentKey ?: "indigo",
          actionSummary = "$inviterName invited $cleanEmail as ${proposedRole.label}",
          eventType = "INVITE_SENT",
          createdAtMillis = now,
        )
      )
      remoteDataSource.notifyLocalMutation("list_invitations", "INSERT")
      invitation.toDomain()
    }

  override suspend fun acceptInvitation(invitationId: String): Result<String> =
    runCatching {
      val invite =
        collaborationDao.getInvitationById(invitationId)
          ?: throw IllegalArgumentException("Invitation not found")
      val matchedMember = userAndFamilyDao.getFamilyMemberByEmail(invite.inviteeEmail)
      val prefs = preferencesRepository.preferencesFlow.first()
      val activeMember = userAndFamilyDao.getFamilyMemberById(prefs.activeMemberId)
      val joiningMemberId = matchedMember?.id ?: prefs.activeMemberId
      val joiningName =
        matchedMember?.name
          ?: invite.inviteeEmail
            .substringBefore("@")
            .substringBefore(".")
            .replaceFirstChar { it.uppercaseChar() }
      val now = System.currentTimeMillis()

      collaborationDao.updateInvitationStatus(invitationId, InvitationStatus.ACCEPTED.name)
      sharedListDao.upsertListMember(
        ListMemberEntity(
          listId = invite.listId,
          memberId = joiningMemberId,
          memberName = joiningName,
          memberEmail = invite.inviteeEmail,
          avatarKey = matchedMember?.avatarKey ?: activeMember?.avatarKey ?: "avatar_olivia",
          accentKey = matchedMember?.accentKey ?: activeMember?.accentKey ?: "mint",
          role = invite.proposedRole,
          joinedAtMillis = now,
        )
      )
      collaborationDao.insertActivityEvent(
        ActivityEventEntity(
          id = "act_${UUID.randomUUID().toString().take(8)}",
          productId = null,
          sharedListId = invite.listId,
          actorMemberId = joiningMemberId,
          actorName = joiningName,
          actorAccentKey = matchedMember?.accentKey ?: "mint",
          actionSummary = "$joiningName accepted invitation and joined ${invite.listTitle}",
          eventType = "MEMBER_JOINED",
          createdAtMillis = now,
        )
      )
      remoteDataSource.notifyLocalMutation("list_members", "INSERT")
      invite.listId
    }
}

@Singleton
class SupabaseFamilyAuthRepository
@Inject
constructor(
  private val initializer: DatabaseInitializer,
  private val userAndFamilyDao: UserAndFamilyDao,
  private val preferencesRepository: UserPreferencesRepository,
  private val remoteDataSource: SupabaseRemoteDataSource,
) : AuthRepository {

  override fun observeFamilyPersonas(): Flow<List<FamilyPersona>> =
    userAndFamilyDao.observeFamilyMembers().map { members ->
      members.map {
        FamilyPersona(
          memberId = it.id,
          displayName =
            when (it.id) {
              "rec_attila" -> "Attila Kovács"
              "rec_dalma" -> "Dalma Szabó"
              "rec_olivia" -> "Olívia Kovács"
              "rec_armin" -> "Ármin Kovács"
              else -> it.name
            },
          email = it.email,
          relationship = it.relationship,
          accentKey = it.accentKey,
        )
      }
    }

  override suspend fun signInWithSupabase(email: String, password: String): Result<String> {
    initializer.ensureSeeded()
    return remoteDataSource.signInWithEmailPassword(email, password).map { session ->
      val cleanEmail = email.trim()
      val matchedMember = userAndFamilyDao.getFamilyMemberByEmail(cleanEmail)
      val displayName =
        matchedMember?.name
          ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
      if (matchedMember != null) {
        preferencesRepository.switchActiveFamilyMember(
          memberId = matchedMember.id,
          displayName = displayName,
          email = cleanEmail,
        )
      }
      preferencesRepository.setCloudSession(
        userId = session.user?.id ?: "usr_${cleanEmail.hashCode()}",
        accessToken = session.accessToken,
        email = cleanEmail,
        displayName = displayName,
      )
      displayName
    }
  }

  override suspend fun signUpWithSupabase(
    displayName: String,
    email: String,
    password: String,
  ): Result<String> {
    initializer.ensureSeeded()
    return remoteDataSource.signUpWithEmailPassword(displayName, email, password).map { session ->
      val cleanName = displayName.trim().ifBlank { "Family Member" }
      val cleanEmail = email.trim()
      val userId = session.user?.id ?: "usr_${UUID.randomUUID().toString().take(8)}"
      userAndFamilyDao.upsertUser(
        UserEntity(
          id = userId,
          authUid = userId,
          email = cleanEmail,
          displayName = cleanName,
          linkedMemberId = "rec_attila",
          createdAtMillis = System.currentTimeMillis(),
        )
      )
      preferencesRepository.setCloudSession(
        userId = userId,
        accessToken = session.accessToken,
        email = cleanEmail,
        displayName = cleanName,
      )
      cleanName
    }
  }

  override suspend fun signOutCloudSession() {
    preferencesRepository.setCloudSession(
      userId = null,
      accessToken = null,
      email = null,
      displayName = null,
    )
  }

  override suspend fun switchActivePersona(memberId: String) {
    initializer.ensureSeeded()
    val member = userAndFamilyDao.getFamilyMemberById(memberId) ?: return
    val fullName =
      when (member.id) {
        "rec_attila" -> "Attila Kovács"
        "rec_dalma" -> "Dalma Szabó"
        "rec_olivia" -> "Olívia Kovács"
        "rec_armin" -> "Ármin Kovács"
        else -> member.name
      }
    preferencesRepository.switchActiveFamilyMember(
      memberId = member.id,
      displayName = fullName,
      email = member.email,
    )
  }
}
