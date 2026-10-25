package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

data class UserPreferences(
  val activeMemberId: String = "rec_attila",
  val displayName: String = "Attila Kovács",
  val email: String = "attila.kovacs@pixelmail.hu",
  val isCloudAuthenticated: Boolean = false,
  val supabaseUserId: String? = null,
  val supabaseAccessToken: String? = null,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = true,
  val priceDropNotifications: Boolean = true,
  val eventReminders: Boolean = true,
  val aiAutoCategorization: Boolean = true,
  val aiRecipientMatching: Boolean = true,
  val aiConfidenceThreshold: Float = 0.80f,
)

interface UserPreferencesRepository {
  val preferencesFlow: Flow<UserPreferences>

  suspend fun setThemeMode(mode: ThemeMode)

  suspend fun setUseDynamicColor(enabled: Boolean)

  suspend fun setPriceDropNotifications(enabled: Boolean)

  suspend fun setEventReminders(enabled: Boolean)

  suspend fun setAiAutoCategorization(enabled: Boolean)

  suspend fun setAiRecipientMatching(enabled: Boolean)

  suspend fun setAiConfidenceThreshold(threshold: Float)

  suspend fun updateProfile(displayName: String, email: String)

  suspend fun switchActiveFamilyMember(memberId: String, displayName: String, email: String)

  suspend fun setCloudSession(
    userId: String?,
    accessToken: String?,
    email: String?,
    displayName: String?,
  )
}

private val Context.wishDropDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "wishdrop_preferences")

@Singleton
class DataStoreUserPreferencesRepository
@Inject
constructor(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {

  private object Keys {
    val ACTIVE_MEMBER_ID = stringPreferencesKey("active_member_id")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val EMAIL = stringPreferencesKey("email")
    val IS_CLOUD_AUTH = booleanPreferencesKey("is_cloud_authenticated")
    val SUPABASE_USER_ID = stringPreferencesKey("supabase_user_id")
    val SUPABASE_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val PRICE_DROP_NOTIFICATIONS = booleanPreferencesKey("price_drop_notifications")
    val EVENT_REMINDERS = booleanPreferencesKey("event_reminders")
    val AI_AUTO_CATEGORIZE = booleanPreferencesKey("ai_auto_categorize")
    val AI_RECIPIENT_MATCH = booleanPreferencesKey("ai_recipient_match")
    val AI_CONFIDENCE = floatPreferencesKey("ai_confidence")
  }

  override val preferencesFlow: Flow<UserPreferences> =
    dataStore.data.map { prefs ->
      val modeString = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
      val mode =
        runCatching { ThemeMode.valueOf(modeString) }.getOrDefault(ThemeMode.SYSTEM)
      UserPreferences(
        activeMemberId = prefs[Keys.ACTIVE_MEMBER_ID] ?: "rec_attila",
        displayName = prefs[Keys.DISPLAY_NAME] ?: "Attila Kovács",
        email = prefs[Keys.EMAIL] ?: "attila.kovacs@pixelmail.hu",
        isCloudAuthenticated = prefs[Keys.IS_CLOUD_AUTH] ?: false,
        supabaseUserId = prefs[Keys.SUPABASE_USER_ID],
        supabaseAccessToken = prefs[Keys.SUPABASE_ACCESS_TOKEN],
        themeMode = mode,
        useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
        priceDropNotifications = prefs[Keys.PRICE_DROP_NOTIFICATIONS] ?: true,
        eventReminders = prefs[Keys.EVENT_REMINDERS] ?: true,
        aiAutoCategorization = prefs[Keys.AI_AUTO_CATEGORIZE] ?: true,
        aiRecipientMatching = prefs[Keys.AI_RECIPIENT_MATCH] ?: true,
        aiConfidenceThreshold = prefs[Keys.AI_CONFIDENCE] ?: 0.80f,
      )
    }

  override suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  override suspend fun setUseDynamicColor(enabled: Boolean) {
    dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  override suspend fun setPriceDropNotifications(enabled: Boolean) {
    dataStore.edit { it[Keys.PRICE_DROP_NOTIFICATIONS] = enabled }
  }

  override suspend fun setEventReminders(enabled: Boolean) {
    dataStore.edit { it[Keys.EVENT_REMINDERS] = enabled }
  }

  override suspend fun setAiAutoCategorization(enabled: Boolean) {
    dataStore.edit { it[Keys.AI_AUTO_CATEGORIZE] = enabled }
  }

  override suspend fun setAiRecipientMatching(enabled: Boolean) {
    dataStore.edit { it[Keys.AI_RECIPIENT_MATCH] = enabled }
  }

  override suspend fun setAiConfidenceThreshold(threshold: Float) {
    dataStore.edit { it[Keys.AI_CONFIDENCE] = threshold.coerceIn(0.50f, 0.99f) }
  }

  override suspend fun updateProfile(displayName: String, email: String) {
    dataStore.edit {
      it[Keys.DISPLAY_NAME] = displayName.trim().ifBlank { "Attila Kovács" }
      it[Keys.EMAIL] = email.trim().ifBlank { "attila.kovacs@pixelmail.hu" }
    }
  }

  override suspend fun switchActiveFamilyMember(
    memberId: String,
    displayName: String,
    email: String,
  ) {
    dataStore.edit {
      it[Keys.ACTIVE_MEMBER_ID] = memberId
      it[Keys.DISPLAY_NAME] = displayName
      it[Keys.EMAIL] = email
    }
  }

  override suspend fun setCloudSession(
    userId: String?,
    accessToken: String?,
    email: String?,
    displayName: String?,
  ) {
    dataStore.edit {
      it[Keys.IS_CLOUD_AUTH] = userId != null
      if (userId != null) {
        it[Keys.SUPABASE_USER_ID] = userId
      } else {
        it.remove(Keys.SUPABASE_USER_ID)
      }
      if (accessToken != null) {
        it[Keys.SUPABASE_ACCESS_TOKEN] = accessToken
      } else {
        it.remove(Keys.SUPABASE_ACCESS_TOKEN)
      }
      if (!email.isNullOrBlank()) {
        it[Keys.EMAIL] = email.trim()
      }
      if (!displayName.isNullOrBlank()) {
        it[Keys.DISPLAY_NAME] = displayName.trim()
      }
    }
  }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
  @Provides
  @Singleton
  fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    context.wishDropDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindingModule {
  @Binds
  @Singleton
  abstract fun bindUserPreferencesRepository(
    impl: DataStoreUserPreferencesRepository
  ): UserPreferencesRepository
}
