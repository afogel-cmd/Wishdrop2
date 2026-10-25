package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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

@Database(
  entities =
    [
      UserEntity::class,
      FamilyMemberEntity::class,
      SharedListEntity::class,
      ListMemberEntity::class,
      ListInvitationEntity::class,
      ProductEntity::class,
      ProductTagCrossRefEntity::class,
      CommentEntity::class,
      ActivityEventEntity::class,
      WishlistEntity::class,
      PriceHistoryEntity::class,
      GiftRecipientEntity::class,
      CategoryEntity::class,
      TagEntity::class,
      RecommendationEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
abstract class WishDropDatabase : RoomDatabase() {
  abstract fun productDao(): ProductDao

  abstract fun sharedListDao(): SharedListDao

  abstract fun collaborationDao(): CollaborationDao

  abstract fun userAndFamilyDao(): UserAndFamilyDao

  abstract fun wishlistDao(): WishlistDao

  abstract fun priceHistoryDao(): PriceHistoryDao

  abstract fun giftRecipientDao(): GiftRecipientDao

  abstract fun taxonomyDao(): TaxonomyDao

  abstract fun recommendationDao(): RecommendationDao
}
