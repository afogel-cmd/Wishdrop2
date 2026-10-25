package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ActivityEventEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GiftRecipientEntity
import com.example.data.local.entity.ListInvitationEntity
import com.example.data.local.entity.ListMemberEntity
import com.example.data.local.entity.PriceHistoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductTagCrossRefEntity
import com.example.data.local.entity.RecommendationEntity
import com.example.data.local.entity.SharedListEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
  @Query("SELECT COUNT(*) FROM products")
  suspend fun countAll(): Int

  @Query("SELECT * FROM products ORDER BY isFeaturedDeal DESC, savingsHuf DESC, createdAtMillis DESC")
  fun observeAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE isPurchased = 0 AND isArchived = 0 ORDER BY isFeaturedDeal DESC, savingsHuf DESC, createdAtMillis DESC")
  fun observeActiveProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE sharedListId = :listId AND isArchived = 0 ORDER BY isPurchased ASC, createdAtMillis DESC")
  fun observeProductsForSharedList(listId: String): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
  fun observeProductById(productId: String): Flow<ProductEntity?>

  @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
  suspend fun getProductById(productId: String): ProductEntity?

  @Query("SELECT * FROM products WHERE recipientId = :recipientId AND isArchived = 0 ORDER BY createdAtMillis DESC")
  fun observeProductsForRecipient(recipientId: String): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products")
  suspend fun getAllSnapshot(): List<ProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProduct(product: ProductEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProducts(products: List<ProductEntity>)

  @Query("UPDATE products SET isPurchased = :purchased, isArchived = CASE WHEN :purchased = 1 THEN 0 ELSE isArchived END WHERE id = :productId")
  suspend fun updatePurchased(productId: String, purchased: Boolean)

  @Query("UPDATE products SET isArchived = :archived WHERE id = :productId")
  suspend fun updateArchived(productId: String, archived: Boolean)

  @Query("UPDATE products SET isPriceAlertEnabled = NOT isPriceAlertEnabled WHERE id = :productId")
  suspend fun togglePriceAlert(productId: String)

  @Query("UPDATE products SET recipientId = :recipientId, recipientName = :recipientName, recipientConfidence = :confidence WHERE id = :productId")
  suspend fun updateRecipient(
    productId: String,
    recipientId: String?,
    recipientName: String?,
    confidence: Int,
  )

  @Query("UPDATE products SET sharedListId = :sharedListId, hiddenFromMemberId = :hiddenFromMemberId WHERE id = :productId")
  suspend fun updateSharedListAssignment(
    productId: String,
    sharedListId: String?,
    hiddenFromMemberId: String?,
  )

  @Query("UPDATE products SET hiddenFromMemberId = :hiddenFromMemberId WHERE sharedListId = :sharedListId")
  suspend fun updateHiddenRecipientForListProducts(
    sharedListId: String,
    hiddenFromMemberId: String?,
  )

  @Query("DELETE FROM products")
  suspend fun clearAll()
}

@Dao
interface SharedListDao {
  @Query("SELECT * FROM shared_lists ORDER BY updatedAtMillis DESC")
  fun observeAllSharedLists(): Flow<List<SharedListEntity>>

  @Query("SELECT * FROM shared_lists WHERE id = :listId LIMIT 1")
  fun observeSharedListById(listId: String): Flow<SharedListEntity?>

  @Query("SELECT * FROM shared_lists WHERE id = :listId LIMIT 1")
  suspend fun getSharedListById(listId: String): SharedListEntity?

  @Query("SELECT * FROM shared_lists")
  suspend fun getAllSharedListsSnapshot(): List<SharedListEntity>

  @Query("SELECT * FROM list_members ORDER BY joinedAtMillis ASC")
  fun observeAllListMembers(): Flow<List<ListMemberEntity>>

  @Query("SELECT * FROM list_members WHERE listId = :listId ORDER BY joinedAtMillis ASC")
  fun observeMembersForList(listId: String): Flow<List<ListMemberEntity>>

  @Query("SELECT * FROM list_members")
  suspend fun getAllMembersSnapshot(): List<ListMemberEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSharedList(list: SharedListEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSharedLists(lists: List<SharedListEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertListMember(member: ListMemberEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertListMembers(members: List<ListMemberEntity>)

  @Query("UPDATE shared_lists SET visibility = :visibility, hiddenFromMemberId = :hiddenFromMemberId, hiddenFromMemberName = :hiddenFromMemberName, updatedAtMillis = :updatedAtMillis WHERE id = :listId")
  suspend fun updateVisibility(
    listId: String,
    visibility: String,
    hiddenFromMemberId: String?,
    hiddenFromMemberName: String?,
    updatedAtMillis: Long,
  )

  @Query("UPDATE list_members SET role = :role WHERE listId = :listId AND memberId = :memberId")
  suspend fun updateMemberRole(listId: String, memberId: String, role: String)

  @Query("DELETE FROM list_members")
  suspend fun clearMembers()

  @Query("DELETE FROM shared_lists")
  suspend fun clearLists()
}

@Dao
interface CollaborationDao {
  @Query("SELECT * FROM comments WHERE productId = :productId ORDER BY createdAtMillis ASC")
  fun observeCommentsForProduct(productId: String): Flow<List<CommentEntity>>

  @Query("SELECT * FROM comments")
  suspend fun getAllCommentsSnapshot(): List<CommentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertComment(comment: CommentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertComments(comments: List<CommentEntity>)

  @Query("SELECT * FROM activity_events WHERE productId = :productId ORDER BY createdAtMillis DESC")
  fun observeActivityForProduct(productId: String): Flow<List<ActivityEventEntity>>

  @Query("SELECT * FROM activity_events WHERE sharedListId = :listId ORDER BY createdAtMillis DESC")
  fun observeActivityForList(listId: String): Flow<List<ActivityEventEntity>>

  @Query("SELECT * FROM activity_events ORDER BY createdAtMillis DESC")
  suspend fun getAllActivitySnapshot(): List<ActivityEventEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityEvent(event: ActivityEventEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityEvents(events: List<ActivityEventEntity>)

  @Query("SELECT * FROM list_invitations WHERE status = 'PENDING' ORDER BY createdAtMillis DESC")
  fun observePendingInvitations(): Flow<List<ListInvitationEntity>>

  @Query("SELECT * FROM list_invitations WHERE listId = :listId AND status = 'PENDING' ORDER BY createdAtMillis DESC")
  fun observePendingInvitationsForList(listId: String): Flow<List<ListInvitationEntity>>

  @Query("SELECT * FROM list_invitations WHERE id = :invitationId LIMIT 1")
  suspend fun getInvitationById(invitationId: String): ListInvitationEntity?

  @Query("SELECT * FROM list_invitations")
  suspend fun getAllInvitationsSnapshot(): List<ListInvitationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInvitation(invitation: ListInvitationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInvitations(invitations: List<ListInvitationEntity>)

  @Query("UPDATE list_invitations SET status = :status WHERE id = :invitationId")
  suspend fun updateInvitationStatus(invitationId: String, status: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProductTags(refs: List<ProductTagCrossRefEntity>)

  @Query("DELETE FROM product_tags")
  suspend fun clearProductTags()

  @Query("DELETE FROM comments")
  suspend fun clearComments()

  @Query("DELETE FROM activity_events")
  suspend fun clearActivityEvents()

  @Query("DELETE FROM list_invitations")
  suspend fun clearInvitations()
}

@Dao
interface UserAndFamilyDao {
  @Query("SELECT * FROM family_members ORDER BY createdAtMillis ASC")
  fun observeFamilyMembers(): Flow<List<FamilyMemberEntity>>

  @Query("SELECT * FROM family_members WHERE id = :memberId LIMIT 1")
  suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity?

  @Query("SELECT * FROM family_members WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun getFamilyMemberByEmail(email: String): FamilyMemberEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUsers(users: List<UserEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUser(user: UserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFamilyMembers(members: List<FamilyMemberEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFamilyMember(member: FamilyMemberEntity)

  @Query("DELETE FROM family_members")
  suspend fun clearFamilyMembers()

  @Query("DELETE FROM users")
  suspend fun clearUsers()
}

@Dao
interface WishlistDao {
  @Query("SELECT * FROM wishlists ORDER BY sortOrder ASC")
  fun observeWishlists(): Flow<List<WishlistEntity>>

  @Query("SELECT * FROM wishlists")
  suspend fun getAllSnapshot(): List<WishlistEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWishlists(items: List<WishlistEntity>)

  @Query("DELETE FROM wishlists")
  suspend fun clearAll()
}

@Dao
interface PriceHistoryDao {
  @Query("SELECT * FROM price_history WHERE productId = :productId AND timestampMillis >= :sinceMillis ORDER BY timestampMillis ASC")
  fun observeHistorySince(productId: String, sinceMillis: Long): Flow<List<PriceHistoryEntity>>

  @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestampMillis ASC")
  fun observeAllHistory(productId: String): Flow<List<PriceHistoryEntity>>

  @Query("SELECT * FROM price_history")
  suspend fun getAllSnapshot(): List<PriceHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(points: List<PriceHistoryEntity>)

  @Query("DELETE FROM price_history")
  suspend fun clearAll()
}

@Dao
interface GiftRecipientDao {
  @Query("SELECT * FROM gift_recipients ORDER BY daysUntilEvent ASC")
  fun observeRecipients(): Flow<List<GiftRecipientEntity>>

  @Query("SELECT * FROM gift_recipients WHERE id = :recipientId LIMIT 1")
  fun observeRecipientById(recipientId: String): Flow<GiftRecipientEntity?>

  @Query("SELECT * FROM gift_recipients WHERE id = :recipientId LIMIT 1")
  suspend fun getRecipientById(recipientId: String): GiftRecipientEntity?

  @Query("SELECT * FROM gift_recipients ORDER BY daysUntilEvent ASC")
  suspend fun getAllSnapshot(): List<GiftRecipientEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecipient(recipient: GiftRecipientEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecipients(recipients: List<GiftRecipientEntity>)

  @Query("UPDATE gift_recipients SET giftStageName = :stageName WHERE id = :recipientId")
  suspend fun updateGiftStage(recipientId: String, stageName: String)

  @Query("UPDATE gift_recipients SET interestsJson = :interestsJson WHERE id = :recipientId")
  suspend fun updateInterests(recipientId: String, interestsJson: String)

  @Query("DELETE FROM gift_recipients")
  suspend fun clearAll()
}

@Dao
interface TaxonomyDao {
  @Query("SELECT * FROM categories ORDER BY colorIndex ASC")
  fun observeCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM tags ORDER BY usageCount DESC")
  fun observeTags(): Flow<List<TagEntity>>

  @Query("SELECT * FROM categories")
  suspend fun getCategoriesSnapshot(): List<CategoryEntity>

  @Query("SELECT * FROM tags")
  suspend fun getTagsSnapshot(): List<TagEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategories(categories: List<CategoryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTags(tags: List<TagEntity>)

  @Query("DELETE FROM categories")
  suspend fun clearCategories()

  @Query("DELETE FROM tags")
  suspend fun clearTags()
}

@Dao
interface RecommendationDao {
  @Query("SELECT * FROM recommendations WHERE recipientId = :recipientId ORDER BY matchPercentage DESC")
  fun observeRecommendationsForRecipient(recipientId: String): Flow<List<RecommendationEntity>>

  @Query("SELECT * FROM recommendations")
  suspend fun getAllSnapshot(): List<RecommendationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecommendations(items: List<RecommendationEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecommendation(item: RecommendationEntity)

  @Query("DELETE FROM recommendations")
  suspend fun clearAll()
}
