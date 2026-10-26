package com.example.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.ai.GeminiReadyProductExtractor
import com.example.data.ai.OnDeviceAiAlternativeRecommender
import com.example.data.ai.OnDeviceAiCategoryGenerator
import com.example.data.ai.OnDeviceAiRecipientDetector
import com.example.data.ai.OnDeviceAiTagGenerator
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
import com.example.data.repository.OfflineFirstCollaborationRepository
import com.example.data.repository.OfflineFirstSharedListRepository
import com.example.data.repository.RoomBackupRepository
import com.example.data.repository.RoomGiftRepository
import com.example.data.repository.RoomInsightsRepository
import com.example.data.repository.RoomProductRepository
import com.example.data.repository.SupabaseFamilyAuthRepository
import com.example.domain.ai.AiAlternativeRecommender
import com.example.domain.ai.AiCategoryGenerator
import com.example.domain.ai.AiProductExtractor
import com.example.domain.ai.AiRecipientDetector
import com.example.domain.ai.AiTagGenerator
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BackupRepository
import com.example.domain.repository.CollaborationRepository
import com.example.domain.repository.GiftRepository
import com.example.domain.repository.InsightsRepository
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.SharedListRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json

private val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE gift_recipients ADD COLUMN avatarUrl TEXT DEFAULT NULL")
  }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE products ADD COLUMN additionalImagesJson TEXT NOT NULL DEFAULT '[]'")
  }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // Schema v4 -> v5 migration
  }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideWishDropDatabase(@ApplicationContext context: Context): WishDropDatabase =
    Room.databaseBuilder(context, WishDropDatabase::class.java, "wishdrop.db")
      .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()

  @Provides
  @Singleton
  fun provideKtorHttpClient(): HttpClient =
    HttpClient(CIO) {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
          }
        )
      }
      install(WebSockets)
    }

  @Provides fun provideProductDao(db: WishDropDatabase): ProductDao = db.productDao()

  @Provides fun provideSharedListDao(db: WishDropDatabase): SharedListDao = db.sharedListDao()

  @Provides
  fun provideCollaborationDao(db: WishDropDatabase): CollaborationDao = db.collaborationDao()

  @Provides
  fun provideUserAndFamilyDao(db: WishDropDatabase): UserAndFamilyDao = db.userAndFamilyDao()

  @Provides fun provideWishlistDao(db: WishDropDatabase): WishlistDao = db.wishlistDao()

  @Provides fun providePriceHistoryDao(db: WishDropDatabase): PriceHistoryDao = db.priceHistoryDao()

  @Provides
  fun provideGiftRecipientDao(db: WishDropDatabase): GiftRecipientDao = db.giftRecipientDao()

  @Provides fun provideTaxonomyDao(db: WishDropDatabase): TaxonomyDao = db.taxonomyDao()

  @Provides
  fun provideRecommendationDao(db: WishDropDatabase): RecommendationDao = db.recommendationDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

  @Binds
  @Singleton
  abstract fun bindProductRepository(impl: RoomProductRepository): ProductRepository

  @Binds
  @Singleton
  abstract fun bindSharedListRepository(
    impl: OfflineFirstSharedListRepository
  ): SharedListRepository

  @Binds
  @Singleton
  abstract fun bindCollaborationRepository(
    impl: OfflineFirstCollaborationRepository
  ): CollaborationRepository

  @Binds
  @Singleton
  abstract fun bindAuthRepository(impl: SupabaseFamilyAuthRepository): AuthRepository

  @Binds
  @Singleton
  abstract fun bindGiftRepository(impl: RoomGiftRepository): GiftRepository

  @Binds
  @Singleton
  abstract fun bindInsightsRepository(impl: RoomInsightsRepository): InsightsRepository

  @Binds
  @Singleton
  abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository

  @Binds
  @Singleton
  abstract fun bindAiCategoryGenerator(impl: OnDeviceAiCategoryGenerator): AiCategoryGenerator

  @Binds
  @Singleton
  abstract fun bindAiTagGenerator(impl: OnDeviceAiTagGenerator): AiTagGenerator

  @Binds
  @Singleton
  abstract fun bindAiRecipientDetector(impl: OnDeviceAiRecipientDetector): AiRecipientDetector

  @Binds
  @Singleton
  abstract fun bindAiAlternativeRecommender(
    impl: OnDeviceAiAlternativeRecommender
  ): AiAlternativeRecommender

  @Binds
  @Singleton
  abstract fun bindAiProductExtractor(impl: GeminiReadyProductExtractor): AiProductExtractor
}
