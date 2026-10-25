package com.example

import com.example.core.config.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseConfigModule {

  @Provides
  @Singleton
  fun provideSupabaseConfig(): SupabaseConfig =
    SupabaseConfig(
      url = BuildConfig.SUPABASE_URL,
      anonKey = BuildConfig.SUPABASE_ANON_KEY,
    )
}
