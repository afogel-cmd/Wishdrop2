package com.example.domain.ai

import com.example.domain.model.AlternativeProduct
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.GiftRecipient
import com.example.domain.model.RecipientPrediction
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer for AI product categorization.
 * Ready to swap from on-device heuristic classification to Gemini structured output.
 */
interface AiCategoryGenerator {
  suspend fun generateCategories(
    productTitle: String,
    storeName: String = "",
    rawHint: String = "",
  ): List<String>
}

/**
 * Abstraction layer for AI semantic tag extraction.
 */
interface AiTagGenerator {
  suspend fun generateTags(
    productTitle: String,
    categories: List<String>,
    storeName: String = "",
  ): List<String>
}

/**
 * Abstraction layer for AI gift recipient matching based on interest profiles.
 */
interface AiRecipientDetector {
  suspend fun detectRecipient(
    productTitle: String,
    categories: List<String>,
    tags: List<String>,
    recipients: List<GiftRecipient>,
  ): RecipientPrediction?
}

/**
 * Abstraction layer for AI alternative product recommendations & competitive comparisons.
 */
interface AiAlternativeRecommender {
  suspend fun recommendAlternatives(
    productTitle: String,
    primaryCategory: String,
    currentPriceHuf: Int,
  ): List<AlternativeProduct>
}

sealed interface ExtractionProgress {
  data class Analyzing(val stepLabel: String, val progressFraction: Float) : ExtractionProgress

  data class Completed(val draft: ExtractedProductDraft) : ExtractionProgress
}

/**
 * High-level orchestrator for URL, Manual, and Barcode product extraction.
 */
interface AiProductExtractor {
  fun extractFromUrl(url: String): Flow<ExtractionProgress>

  fun extractFromManual(
    name: String,
    store: String,
    priceHuf: Int,
    notes: String,
  ): Flow<ExtractionProgress>

  fun extractFromBarcode(barcode: String): Flow<ExtractionProgress>
}
