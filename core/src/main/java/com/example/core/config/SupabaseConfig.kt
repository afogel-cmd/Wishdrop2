package com.example.core.config

/**
 * Runtime Supabase endpoint configuration injected from BuildConfig via the Secrets Gradle Plugin.
 * Falls back gracefully to offline-first Room sync when placeholder values are present.
 */
data class SupabaseConfig(
  val url: String,
  val anonKey: String,
) {
  val normalizedUrl: String = url.trim().trimEnd('/')

  val isConfigured: Boolean =
    normalizedUrl.startsWith("https://") &&
      !normalizedUrl.contains("your-project-ref", ignoreCase = true) &&
      anonKey.isNotBlank() &&
      !anonKey.equals("YOUR_SUPABASE_ANON_KEY", ignoreCase = true)

  val realtimeWebSocketUrl: String
    get() =
      normalizedUrl.replaceFirst("https://", "wss://") +
        "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
}
