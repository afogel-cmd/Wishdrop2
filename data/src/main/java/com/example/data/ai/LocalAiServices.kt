package com.example.data.ai

import com.example.domain.ai.AiAlternativeRecommender
import com.example.domain.ai.AiCategoryGenerator
import com.example.domain.ai.AiProductExtractor
import com.example.domain.ai.AiRecipientDetector
import com.example.domain.ai.AiTagGenerator
import com.example.domain.ai.ExtractionProgress
import com.example.domain.model.AlternativeProduct
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.GiftRecipient
import com.example.domain.model.RecipientPrediction
import com.example.domain.model.StoreOffer
import com.example.domain.repository.GiftRepository
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class OnDeviceAiCategoryGenerator @Inject constructor() : AiCategoryGenerator {
  override suspend fun generateCategories(
    productTitle: String,
    storeName: String,
    rawHint: String,
  ): List<String> {
    val text = "$productTitle $storeName $rawHint".lowercase()
    val categories = mutableListOf<String>()
    if (
      listOf("buds", "ear", "headphone", "speaker", "audio", "xm5", "anker", "jbl").any {
        text.contains(it)
      }
    ) {
      categories.add("Electronics")
      categories.add("Audio")
    }
    if (
      listOf("pixel", "watch", "stand", "kindle", "camera", "mouse", "streamer", "phone", "tech")
        .any { text.contains(it) }
    ) {
      if ("Electronics" !in categories) categories.add("Electronics")
    }
    if (listOf("watch", "band", "ring", "wear").any { text.contains(it) }) {
      categories.add("Wearables")
    }
    if (
      listOf("lego", "orchid", "bonsai", "kettle", "coffee", "lamp", "throw", "ceramic", "mug")
        .any { text.contains(it) }
    ) {
      categories.add("Home")
      categories.add("Gift")
    }
    if (listOf("cap", "wool", "jacket", "scarf", "bag", "sling").any { text.contains(it) }) {
      categories.add("Fashion")
      categories.add("Gift")
    }
    if (listOf("book", "taschen", "novel", "atlas").any { text.contains(it) }) {
      categories.add("Gift")
    }
    if (categories.isEmpty()) {
      categories.add("Gift")
      categories.add("Lifestyle")
    }
    return categories.distinct().take(3)
  }
}

@Singleton
class OnDeviceAiTagGenerator @Inject constructor() : AiTagGenerator {
  override suspend fun generateTags(
    productTitle: String,
    categories: List<String>,
    storeName: String,
  ): List<String> {
    val text = "$productTitle ${categories.joinToString(" ")} $storeName".lowercase()
    val tags = mutableSetOf<String>()
    if (text.contains("google") || text.contains("pixel")) {
      tags.add("Google")
      tags.add("Pixel")
      tags.add("Android")
    }
    if (text.contains("buds") || text.contains("ear") || text.contains("audio")) {
      tags.add("Bluetooth")
      tags.add("Wireless")
      tags.add("Audio")
    }
    if (text.contains("lego") || text.contains("bonsai") || text.contains("orchid")) {
      tags.add("Flowers")
      tags.add("Home Decor")
      tags.add("Creative Hobbies")
      tags.add("LEGO")
    }
    if (text.contains("coffee") || text.contains("kettle") || text.contains("dripper")) {
      tags.add("Coffee")
      tags.add("Home Decor")
    }
    if (text.contains("book") || text.contains("kindle")) {
      tags.add("Books")
    }
    if (text.contains("camera") || text.contains("polaroid") || text.contains("kodak")) {
      tags.add("Photography")
      tags.add("Design")
    }
    if (tags.size < 3) {
      tags.addAll(categories.take(2))
      tags.add("Smart Pick")
    }
    return tags.take(5).toList()
  }
}

@Singleton
class OnDeviceAiRecipientDetector @Inject constructor() : AiRecipientDetector {
  override suspend fun detectRecipient(
    productTitle: String,
    categories: List<String>,
    tags: List<String>,
    recipients: List<GiftRecipient>,
  ): RecipientPrediction? {
    if (recipients.isEmpty()) return null
    val tokens =
      (productTitle.split(" ") + categories + tags)
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }

    var bestRecipient: GiftRecipient = recipients.first()
    var bestScore = -1
    var matchedInterests = emptyList<String>()

    for (recipient in recipients) {
      val overlaps =
        recipient.interests.filter { interest ->
          val lowerInterest = interest.lowercase()
          tokens.any { token ->
            lowerInterest.contains(token) || token.contains(lowerInterest)
          }
        }
      val score = overlaps.size * 22 + (if (recipient.daysUntilEvent <= 30) 10 else 4)
      if (score > bestScore) {
        bestScore = score
        bestRecipient = recipient
        matchedInterests = overlaps
      }
    }

    val confidence = (78 + bestScore.coerceAtMost(19)).coerceIn(79, 97)
    val reason =
      if (matchedInterests.isNotEmpty()) {
        "Strong overlap with ${bestRecipient.name}'s interests in ${matchedInterests.joinToString(" & ")}."
      } else {
        "Fits ${bestRecipient.name}'s upcoming ${bestRecipient.upcomingEventName} budget."
      }
    return RecipientPrediction(
      recipientId = bestRecipient.id,
      recipientName = bestRecipient.name,
      confidencePercent = confidence,
      explanation = reason,
    )
  }
}

@Singleton
class OnDeviceAiAlternativeRecommender @Inject constructor() : AiAlternativeRecommender {
  override suspend fun recommendAlternatives(
    productTitle: String,
    primaryCategory: String,
    currentPriceHuf: Int,
  ): List<AlternativeProduct> {
    val lower = "$productTitle $primaryCategory".lowercase()
    return when {
      lower.contains("audio") || lower.contains("buds") || lower.contains("ear") ->
        listOf(
          AlternativeProduct(
            id = "prod_nothing_ear",
            title = "Nothing Ear",
            store = "Alza",
            priceHuf = 48_990,
            imageKey = "nothing_ear",
            highlight = "Hi-Res LDAC",
          ),
          AlternativeProduct(
            id = "prod_samsung_buds_fe",
            title = "Samsung Buds FE",
            store = "MediaMarkt",
            priceHuf = 26_990,
            imageKey = "samsung_buds",
            highlight = "Value ANC",
          ),
          AlternativeProduct(
            id = "prod_anker_liberty_4",
            title = "Anker Liberty 4",
            store = "Alza",
            priceHuf = 34_990,
            imageKey = "anker_buds",
            highlight = "Spatial Audio",
          ),
        )
      lower.contains("home") || lower.contains("lego") || lower.contains("flower") ->
        listOf(
          AlternativeProduct(
            id = "prod_lego_orchid",
            title = "LEGO Orchid",
            store = "Alza",
            priceHuf = 16_490,
            imageKey = "lego_orchid",
            highlight = "▼13% Deal",
          ),
          AlternativeProduct(
            id = "ai_rec_lego_bonsai",
            title = "LEGO Bonsai",
            store = "LEGO Store",
            priceHuf = 17_990,
            imageKey = "lego_bonsai",
            highlight = "Botanical Set",
          ),
        )
      else ->
        listOf(
          AlternativeProduct(
            id = "prod_pixel_buds_2a",
            title = "Google Pixel Buds 2a",
            store = "Yettel",
            priceHuf = 28_490,
            imageKey = "pixel_buds",
            highlight = "Top Deal",
          ),
          AlternativeProduct(
            id = "prod_pixel_stand",
            title = "Pixel Stand",
            store = "Alza",
            priceHuf = (currentPriceHuf * 0.9).roundToInt().coerceAtLeast(14_990),
            imageKey = "pixel_stand",
            highlight = "Smart Pick",
          ),
        )
    }
  }
}

@Singleton
class GeminiReadyProductExtractor
@Inject
constructor(
  private val categoryGenerator: AiCategoryGenerator,
  private val tagGenerator: AiTagGenerator,
  private val recipientDetector: AiRecipientDetector,
  private val alternativeRecommender: AiAlternativeRecommender,
  private val giftRepository: GiftRepository,
) : AiProductExtractor {

  override fun extractFromUrl(url: String): Flow<ExtractionProgress> = flow {
    emit(ExtractionProgress.Analyzing("Connecting to store & parsing metadata...", 0.25f))
    delay(320)
    val parsed = parseUrlHeuristics(url)
    emit(ExtractionProgress.Analyzing("Generating AI categories & semantic tags...", 0.55f))
    delay(320)
    val categories = categoryGenerator.generateCategories(parsed.title, parsed.store, url)
    val tags = tagGenerator.generateTags(parsed.title, categories, parsed.store)
    emit(ExtractionProgress.Analyzing("Detecting gift recipient & comparing stores...", 0.85f))
    delay(300)
    val recipients = giftRepository.observeRecipients().first()
    val prediction = recipientDetector.detectRecipient(parsed.title, categories, tags, recipients)
    val alternatives =
      alternativeRecommender.recommendAlternatives(
        parsed.title,
        categories.firstOrNull() ?: "Gift",
        parsed.priceHuf,
      )
    emit(
      ExtractionProgress.Completed(
        ExtractedProductDraft(
          title = parsed.title,
          imageKey = parsed.imageKey,
          store = parsed.store,
          priceHuf = parsed.priceHuf,
          originalPriceHuf = (parsed.priceHuf * 1.12).roundToInt(),
          sourceUrl = url.trim(),
          categories = categories,
          tags = tags,
          recipientPrediction = prediction,
          alternatives = alternatives,
          storeOffers = buildOffers(parsed.store, parsed.priceHuf),
        )
      )
    )
  }

  override fun extractFromManual(
    name: String,
    store: String,
    priceHuf: Int,
    notes: String,
  ): Flow<ExtractionProgress> = flow {
    emit(ExtractionProgress.Analyzing("Structuring product details with AI...", 0.35f))
    delay(280)
    val cleanTitle = name.trim().ifBlank { "Smart Gift Idea" }
    val cleanStore = store.trim().ifBlank { "Alza" }
    val cleanPrice = if (priceHuf > 0) priceHuf else 24_990
    val categories = categoryGenerator.generateCategories(cleanTitle, cleanStore, notes)
    val tags = tagGenerator.generateTags(cleanTitle, categories, cleanStore)
    emit(ExtractionProgress.Analyzing("Matching family interest profiles...", 0.78f))
    delay(280)
    val recipients = giftRepository.observeRecipients().first()
    val prediction = recipientDetector.detectRecipient(cleanTitle, categories, tags, recipients)
    val alternatives =
      alternativeRecommender.recommendAlternatives(
        cleanTitle,
        categories.firstOrNull() ?: "Gift",
        cleanPrice,
      )
    emit(
      ExtractionProgress.Completed(
        ExtractedProductDraft(
          title = cleanTitle,
          imageKey = resolveArtworkKey(cleanTitle),
          store = cleanStore,
          priceHuf = cleanPrice,
          originalPriceHuf = cleanPrice,
          categories = categories,
          tags = tags,
          recipientPrediction = prediction,
          alternatives = alternatives,
          storeOffers = buildOffers(cleanStore, cleanPrice),
        )
      )
    )
  }

  override fun extractFromBarcode(barcode: String): Flow<ExtractionProgress> = flow {
    emit(ExtractionProgress.Analyzing("Looking up EAN/UPC barcode $barcode...", 0.30f))
    delay(320)
    val catalogMatch = lookupBarcode(barcode.trim())
    emit(ExtractionProgress.Analyzing("Enriching with AI categories & store prices...", 0.72f))
    delay(320)
    val categories = categoryGenerator.generateCategories(catalogMatch.title, catalogMatch.store, barcode)
    val tags = tagGenerator.generateTags(catalogMatch.title, categories, catalogMatch.store)
    val recipients = giftRepository.observeRecipients().first()
    val prediction = recipientDetector.detectRecipient(catalogMatch.title, categories, tags, recipients)
    val alternatives =
      alternativeRecommender.recommendAlternatives(
        catalogMatch.title,
        categories.firstOrNull() ?: "Electronics",
        catalogMatch.priceHuf,
      )
    emit(
      ExtractionProgress.Completed(
        ExtractedProductDraft(
          title = catalogMatch.title,
          imageKey = catalogMatch.imageKey,
          store = catalogMatch.store,
          priceHuf = catalogMatch.priceHuf,
          originalPriceHuf = (catalogMatch.priceHuf * 1.10).roundToInt(),
          barcode = barcode.trim(),
          categories = categories,
          tags = tags,
          recipientPrediction = prediction,
          alternatives = alternatives,
          storeOffers = buildOffers(catalogMatch.store, catalogMatch.priceHuf),
        )
      )
    )
  }

  private data class ParsedSeed(
    val title: String,
    val store: String,
    val priceHuf: Int,
    val imageKey: String,
  )

  private fun parseUrlHeuristics(rawUrl: String): ParsedSeed {
    val lower = rawUrl.lowercase()
    return when {
      lower.contains("bonsai") ->
        ParsedSeed("LEGO Bonsai Tree", "LEGO Store", 17_990, "lego_bonsai")
      lower.contains("orchid") ->
        ParsedSeed("LEGO Orchid Botanical", "Alza", 16_490, "lego_orchid")
      lower.contains("buds") ->
        ParsedSeed("Google Pixel Buds 2a", "Google Store", 28_490, "pixel_buds")
      lower.contains("watch") ->
        ParsedSeed("Google Pixel Watch 3", "Google Store", 114_990, "pixel_watch")
      lower.contains("nothing") ->
        ParsedSeed("Nothing Ear Wireless", "Alza", 48_990, "nothing_ear")
      lower.contains("kettle") || lower.contains("coffee") ->
        ParsedSeed("Fellow Stagg Pour-Over Kettle", "Specialty Roast", 64_900, "coffee_kettle")
      else -> {
        val host =
          runCatching {
              URI(if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl").host ?: "alza.hu"
            }
            .getOrDefault("alza.hu")
            .removePrefix("www.")
        val storeLabel =
          host.substringBefore('.').replaceFirstChar { it.uppercaseChar() }.ifBlank { "Alza" }
        val slug =
          rawUrl
            .substringAfterLast('/')
            .substringBefore('?')
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
        val prettyTitle =
          if (slug.length >= 3 && !slug.contains('.')) {
            slug.split(' ').joinToString(" ") { part ->
              part.replaceFirstChar { it.uppercaseChar() }
            }
          } else {
            "Pixel Ecosystem Smart Accessory"
          }
        ParsedSeed(prettyTitle, storeLabel, 29_990, resolveArtworkKey(prettyTitle))
      }
    }
  }

  private fun lookupBarcode(barcode: String): ParsedSeed =
    when (barcode) {
      "0840353900240" -> ParsedSeed("Google Pixel Buds 2a", "Yettel", 28_490, "pixel_buds")
      "5702017153162" -> ParsedSeed("LEGO Bonsai Tree 10281", "LEGO Store", 17_990, "lego_bonsai")
      "5060696460512" -> ParsedSeed("Nothing Ear (2026)", "Alza", 48_990, "nothing_ear")
      else -> ParsedSeed("Anker Soundcore Liberty 4 NC", "MediaMarkt", 33_990, "anker_buds")
    }

  private fun resolveArtworkKey(title: String): String {
    val lower = title.lowercase()
    return when {
      lower.contains("buds") && lower.contains("samsung") -> "samsung_buds"
      lower.contains("buds") || lower.contains("pixel") && lower.contains("audio") -> "pixel_buds"
      lower.contains("watch") -> "pixel_watch"
      lower.contains("bonsai") -> "lego_bonsai"
      lower.contains("orchid") || lower.contains("lego") -> "lego_orchid"
      lower.contains("nothing") -> "nothing_ear"
      lower.contains("anker") || lower.contains("liberty") || lower.contains("speaker") -> "anker_buds"
      lower.contains("cap") || lower.contains("wool") -> "wool_cap"
      lower.contains("stand") || lower.contains("dock") -> "pixel_stand"
      lower.contains("coffee") || lower.contains("kettle") -> "coffee_kettle"
      lower.contains("book") -> "art_book"
      else -> "pixel_buds"
    }
  }

  private fun buildOffers(primaryStore: String, priceHuf: Int): List<StoreOffer> =
    listOf(
      StoreOffer(primaryStore, priceHuf, "In stock • Best tracked price", true, true),
      StoreOffer(
        if (primaryStore == "Alza") "Yettel" else "Alza",
        ((priceHuf * 1.05) / 100.0).roundToInt() * 100 + 90,
        "Express locker pickup",
        true,
        false,
      ),
      StoreOffer(
        "MediaMarkt",
        ((priceHuf * 1.11) / 100.0).roundToInt() * 100 + 90,
        "Store pickup & courier",
        true,
        false,
      ),
    )
}
