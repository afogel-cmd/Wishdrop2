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
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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
      lower.contains("nadrag") || lower.contains("chino") || lower.contains("pants") ||
        lower.contains("shirt") || lower.contains("clothing") || lower.contains("fashion") ||
        lower.contains("sapka") || lower.contains("kabát") || lower.contains("dress") || lower.contains("reserved") ->
        listOf(
          AlternativeProduct(
            id = "prod_fashion_chino",
            title = "Slim Fit Chino Nadrág",
            store = "Zara",
            priceHuf = (currentPriceHuf * 0.88).roundToInt().coerceAtLeast(12_990),
            imageKey = "art_book",
            highlight = "Prémium Pamut",
          ),
          AlternativeProduct(
            id = "prod_fashion_belt",
            title = "Klasszikus Bőr Öv",
            store = "Reserved",
            priceHuf = 12_990,
            imageKey = "art_book",
            highlight = "Valódi Bőr",
          ),
          AlternativeProduct(
            id = "prod_fashion_shirt",
            title = "Elegáns Pamut Ing",
            store = "H&M",
            priceHuf = 14_990,
            imageKey = "art_book",
            highlight = "Könnyű Vasalás",
          ),
        )
      lower.contains("szaritogep") || lower.contains("mosogep") || lower.contains("bosch") ||
        lower.contains("whirlpool") || lower.contains("miele") || lower.contains("dryer") ->
        listOf(
          AlternativeProduct(
            id = "prod_appliance_miele",
            title = "Miele Hőszivattyús Szárítógép",
            store = "MediaMarkt",
            priceHuf = (currentPriceHuf * 1.08).roundToInt().coerceAtLeast(249_990),
            imageKey = "art_book",
            highlight = "A+++ Miele Eco",
          ),
          AlternativeProduct(
            id = "prod_appliance_samsung",
            title = "Samsung AI Control Szárítógép",
            store = "Alza",
            priceHuf = (currentPriceHuf * 0.92).roundToInt().coerceAtLeast(189_990),
            imageKey = "art_book",
            highlight = "AI Auto Dry",
          ),
          AlternativeProduct(
            id = "prod_appliance_whirlpool",
            title = "Whirlpool FreshCare Szárítógép",
            store = "Euronics",
            priceHuf = 179_990,
            imageKey = "art_book",
            highlight = "FreshCare+",
          ),
        )
      else ->
        listOf(
          AlternativeProduct(
            id = "prod_gift_idea_1",
            title = "Handcrafted Bőr Mappa",
            store = "Reserved",
            priceHuf = (currentPriceHuf * 0.9).roundToInt().coerceAtLeast(11_990),
            imageKey = "art_book",
            highlight = "Személyes Ajándék",
          ),
          AlternativeProduct(
            id = "prod_gift_idea_2",
            title = "Smart Desk Organizer",
            store = "Alza",
            priceHuf = 16_990,
            imageKey = "pixel_stand",
            highlight = "Smart Pick",
          ),
        )
    }
  }
}

private data class ExtractedWebMetadata(
  val title: String?,
  val priceHuf: Int?,
  val images: List<String>,
)

private fun normalizeImageUrl(rawUrl: String?, baseUrl: String): String? {
  if (rawUrl.isNullOrBlank()) return null
  val trimmed = rawUrl.trim()
  return when {
    trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") -> trimmed
    trimmed.startsWith("//") -> "https:$trimmed"
    trimmed.startsWith("/") -> {
      val host = runCatching { URI(baseUrl).host }.getOrNull() ?: ""
      val scheme = if (baseUrl.startsWith("http://")) "http" else "https"
      if (host.isNotBlank()) "$scheme://$host$trimmed" else null
    }
    else -> "https://$trimmed"
  }
}

private fun extractMetadataFromHtml(html: String, baseUrl: String): ExtractedWebMetadata {
  val jsonLdNamePattern = """"@type"\s*:\s*"Product"[^}]*?"name"\s*:\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
  val ogTitlePattern = """<meta\s+(?:property|name)=["'](?:og:title|twitter:title)["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
  val htmlTitlePattern = """<title>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE)

  val rawTitle = jsonLdNamePattern.find(html)?.groupValues?.get(1)
    ?: ogTitlePattern.find(html)?.groupValues?.get(1)
    ?: htmlTitlePattern.find(html)?.groupValues?.get(1)

  val cleanTitle = rawTitle?.let { t ->
    t.substringBefore('|').substringBefore('-').substringBefore("—").trim()
  }?.takeIf { it.isNotBlank() }

  val jsonLdOfferPricePattern = """"offers"\s*:\s*\{[^}]*?"price"\s*:\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
  val jsonLdPricePattern = """"price"\s*:\s*(\d{5,7})""".toRegex(RegexOption.IGNORE_CASE)
  val ogPricePattern = """<meta\s+(?:property|name)=["'](?:og:price:amount|product:price:amount|price)["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
  val textPricePattern = """(\d{1,3}(?:\s?\d{3}){1,2})\s*(?:Ft|HUF)""".toRegex(RegexOption.IGNORE_CASE)

  val rawPriceStr = jsonLdOfferPricePattern.find(html)?.groupValues?.get(1)
    ?: jsonLdPricePattern.find(html)?.groupValues?.get(1)
    ?: ogPricePattern.find(html)?.groupValues?.get(1)
    ?: textPricePattern.find(html)?.groupValues?.get(1)?.replace(" ", "")

  val extractedPrice = rawPriceStr?.toDoubleOrNull()?.roundToInt()

  val ogImagePattern = """<meta\s+(?:property|name)=["'](?:og:image|twitter:image|image)["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
  val assetLinkPattern = """"link"\s*:\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
  val jsonLdImagePattern = """"image"\s*:\s*\[\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)

  val foundImages = (ogImagePattern.findAll(html).map { it.groupValues[1] } +
    assetLinkPattern.findAll(html).map { it.groupValues[1] } +
    jsonLdImagePattern.findAll(html).map { it.groupValues[1] }).toList()

  val normalizedImages = foundImages
    .mapNotNull { normalizeImageUrl(it, baseUrl) }
    .filter { it.contains("ASSET") || it.contains("product") || it.contains("media") || it.contains("image") || it.contains("mmsrg") }
    .distinct()

  return ExtractedWebMetadata(
    title = cleanTitle,
    priceHuf = extractedPrice,
    images = normalizedImages,
  )
}

@Singleton
class GeminiReadyProductExtractor
@Inject
constructor(
  private val httpClient: HttpClient,
  private val categoryGenerator: AiCategoryGenerator,
  private val tagGenerator: AiTagGenerator,
  private val recipientDetector: AiRecipientDetector,
  private val alternativeRecommender: AiAlternativeRecommender,
  private val giftRepository: GiftRepository,
) : AiProductExtractor {

  override fun extractFromUrl(url: String): Flow<ExtractionProgress> = flow {
    emit(ExtractionProgress.Analyzing("Connecting to store & parsing metadata...", 0.25f))
    val cleanUrl = url.trim()
    val htmlContent = runCatching {
      httpClient.get(cleanUrl) {
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
      }.bodyAsText()
    }.getOrNull()

    val parsed = parseUrlHeuristics(cleanUrl)
    val webMeta = htmlContent?.let { extractMetadataFromHtml(it, cleanUrl) }

    val finalTitle = webMeta?.title ?: parsed.title
    val finalPrice = webMeta?.priceHuf ?: parsed.priceHuf
    val extractedImages = webMeta?.images.orEmpty()
    val finalMainImage = extractedImages.firstOrNull() ?: parsed.imageKey

    emit(ExtractionProgress.Analyzing("Generating AI categories & semantic tags...", 0.55f))
    delay(280)
    val categories = categoryGenerator.generateCategories(finalTitle, parsed.store, cleanUrl)
    val tags = tagGenerator.generateTags(finalTitle, categories, parsed.store)
    emit(ExtractionProgress.Analyzing("Detecting gift recipient & comparing stores...", 0.85f))
    delay(280)
    val recipients = giftRepository.observeRecipients().first()
    val prediction = recipientDetector.detectRecipient(finalTitle, categories, tags, recipients)
    val alternatives =
      alternativeRecommender.recommendAlternatives(
        finalTitle,
        categories.firstOrNull() ?: "Gift",
        finalPrice,
      )

    val additionalImages = (listOf(finalMainImage) + extractedImages).distinct()

    emit(
      ExtractionProgress.Completed(
        ExtractedProductDraft(
          title = finalTitle,
          imageKey = finalMainImage,
          store = parsed.store,
          priceHuf = finalPrice,
          originalPriceHuf = (finalPrice * 1.12).roundToInt(),
          sourceUrl = cleanUrl,
          categories = categories,
          tags = tags,
          recipientPrediction = prediction,
          alternatives = alternatives,
          storeOffers = buildOffers(parsed.store, finalPrice),
          additionalImages = additionalImages,
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
              URI(if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl").host ?: "mediamarkt.hu"
            }
            .getOrDefault("mediamarkt.hu")
            .removePrefix("www.")
        val storeLabel =
          host.substringBefore('.').replaceFirstChar { it.uppercaseChar() }.ifBlank { "MediaMarkt" }

        val cleanPath = rawUrl
          .substringBefore('?')
          .removeSuffix(".html")
          .removeSuffix(".htm")
          .removeSuffix(".php")
          .removeSuffix(".aspx")
        val rawSlug = cleanPath.substringAfterLast('/').removePrefix("_").removePrefix("-")
        val slugWithoutId = rawSlug.replace(Regex("""-\d+$"""), "")

        val prettyTitle = slugWithoutId
          .split('-', '_', ' ')
          .filter { it.isNotBlank() }
          .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
          .ifBlank { "Termék A Weboldalról" }

        val lowerTitle = prettyTitle.lowercase()
        val defaultPrice = when {
          lowerTitle.contains("szaritogep") || lowerTitle.contains("mosogep") || lowerTitle.contains("bosch") -> 219_990
          lowerTitle.contains("tv") || lowerTitle.contains("televizio") -> 189_990
          lowerTitle.contains("nadrag") || lowerTitle.contains("chino") || lowerTitle.contains("sapka") || lowerTitle.contains("ing") -> 19_990
          else -> 24_990
        }

        ParsedSeed(prettyTitle, storeLabel, defaultPrice, resolveArtworkKey(prettyTitle))
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
      else -> "art_book"
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
