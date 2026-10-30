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
    val rawText = "$productTitle $storeName $rawHint"
    val lower = rawText.lowercase()
    val normalized = lower
      .replace("á", "a")
      .replace("é", "e")
      .replace("í", "i")
      .replace("ó", "o")
      .replace("ö", "o")
      .replace("ő", "o")
      .replace("ú", "u")
      .replace("ü", "u")
      .replace("ű", "u")

    val categories = mutableListOf<String>()

    // 1. Specific product appliance / item type
    if (normalized.contains("szaritogep") || normalized.contains("dryer") || normalized.contains("hoszivattyus")) {
      categories.add("Szárítógép")
      categories.add("Elektronika")
      categories.add("Háztartási gép")
    } else if (normalized.contains("mosogep") || normalized.contains("washer") || normalized.contains("mosas")) {
      categories.add("Mosógép")
      categories.add("Elektronika")
      categories.add("Háztartási gép")
    } else if (normalized.contains("mosogatogep") || normalized.contains("dishwasher")) {
      categories.add("Mosogatógép")
      categories.add("Elektronika")
      categories.add("Háztartási gép")
    } else if (normalized.contains("huto") || normalized.contains("fagyaszto") || normalized.contains("fridge") || normalized.contains("refrigerator")) {
      categories.add("Hűtőszekrény")
      categories.add("Elektronika")
      categories.add("Háztartási gép")
    } else if (normalized.contains("porszivo") || normalized.contains("vacuum")) {
      categories.add("Porszívó")
      categories.add("Elektronika")
      categories.add("Háztartási gép")
    } else if (normalized.contains("kave") || normalized.contains("coffee") || normalized.contains("espresso") || normalized.contains("kettle")) {
      categories.add("Kávéfőző")
      categories.add("Háztartási gép")
    } else if (listOf("buds", "ear", "headphone", "speaker", "audio", "xm5", "anker", "jbl", "fulhallgato", "fejhallgato", "hangszoro").any { normalized.contains(it) }) {
      categories.add("Audio")
      categories.add("Elektronika")
    } else if (listOf("watch", "band", "ring", "wear", "ora", "okosora").any { normalized.contains(it) }) {
      categories.add("Okosóra")
      categories.add("Elektronika")
    } else if (listOf("pixel", "phone", "mobil", "telefon", "smartphone").any { normalized.contains(it) }) {
      categories.add("Okostelefon")
      categories.add("Elektronika")
    } else if (listOf("tv", "televizio", "oled", "qled").any { normalized.contains(it) }) {
      categories.add("Televízió")
      categories.add("Elektronika")
    } else if (listOf("laptop", "notebook", "monitor", "macbook", "szamitogep", "pc").any { normalized.contains(it) }) {
      categories.add("Számítástechnika")
      categories.add("Elektronika")
    } else if (listOf("lego", "orchid", "bonsai", "jatek").any { normalized.contains(it) }) {
      categories.add("LEGO")
      categories.add("Játék")
    } else if (listOf("cap", "wool", "jacket", "scarf", "bag", "sling", "nadrag", "ing", "cipo", "ruha").any { normalized.contains(it) }) {
      categories.add("Divat")
      categories.add("Ruházat")
    } else if (listOf("book", "taschen", "novel", "atlas", "konyv").any { normalized.contains(it) }) {
      categories.add("Könyv")
    }

    // 2. Brand detection
    val knownBrands = listOf(
      "Bosch", "Samsung", "Apple", "Google", "Sony", "LG", "Philips",
      "Whirlpool", "Miele", "Electrolux", "Dyson", "Xiaomi", "JBL", "Anker", "LEGO"
    )
    for (brand in knownBrands) {
      if (normalized.contains(brand.lowercase())) {
        if (!categories.contains(brand)) {
          categories.add(brand)
        }
      }
    }

    // 3. Breadcrumb / site category hints
    if (normalized.contains("haztartasi nagygep") || normalized.contains("haztartasi gep")) {
      if (!categories.contains("Háztartási gép")) categories.add("Háztartási gép")
    }

    if (categories.isEmpty()) {
      categories.add("Gift")
      categories.add("Lifestyle")
    }

    return categories.distinct().take(5)
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
    val normalized = text
      .replace("á", "a")
      .replace("é", "e")
      .replace("í", "i")
      .replace("ó", "o")
      .replace("ö", "o")
      .replace("ő", "o")
      .replace("ú", "u")
      .replace("ü", "u")
      .replace("ű", "u")

    val tags = mutableSetOf<String>()

    if (normalized.contains("szaritogep") || normalized.contains("dryer")) {
      tags.add("Szárítógép")
      tags.add("Hőszivattyús")
    }
    if (normalized.contains("mosogep") || normalized.contains("washer")) {
      tags.add("Mosógép")
    }
    if (normalized.contains("bosch")) {
      tags.add("Bosch")
    }
    if (normalized.contains("samsung")) {
      tags.add("Samsung")
    }
    if (normalized.contains("google") || normalized.contains("pixel")) {
      tags.add("Google")
      tags.add("Pixel")
    }
    if (normalized.contains("haztartasi")) {
      tags.add("HáztartásiGép")
    }
    if (normalized.contains("buds") || normalized.contains("ear") || normalized.contains("audio")) {
      tags.add("Bluetooth")
      tags.add("Wireless")
      tags.add("Audio")
    }
    if (normalized.contains("lego") || normalized.contains("bonsai") || normalized.contains("orchid")) {
      tags.add("Flowers")
      tags.add("Home Decor")
      tags.add("LEGO")
    }
    if (tags.size < 3) {
      categories.forEach { cat ->
        tags.add(cat.replace(" ", ""))
      }
    }
    tags.add("Smart Pick")
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
  val siteCategories: List<String> = emptyList(),
  val brand: String? = null,
)

private fun normalizeImageUrl(rawUrl: String?, baseUrl: String): String? {
  if (rawUrl.isNullOrBlank()) return null
  val unescaped = rawUrl.trim()
    .replace("&amp;", "&")
    .replace("\\u002F", "/")
    .replace("\\/", "/")

  val normalized = when {
    unescaped.startsWith("http://") || unescaped.startsWith("https://") || unescaped.startsWith("content://") -> unescaped
    unescaped.startsWith("//") -> "https:$unescaped"
    unescaped.startsWith("/") -> {
      val host = runCatching { URI(baseUrl).host }.getOrNull() ?: ""
      val scheme = if (baseUrl.startsWith("http://")) "http" else "https"
      if (host.isNotBlank()) "$scheme://$host$unescaped" else null
    }
    else -> "https://$unescaped"
  } ?: return null

  val lower = normalized.lowercase()
  if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".php") || lower.endsWith(".aspx") ||
      lower.contains("/category/") || lower.contains("/service/") || lower.endsWith(".svg") ||
      lower.contains("favicon") || lower.contains("pixel_trans") || lower.contains("1x1")) {
    return null
  }
  return normalized
}

private fun extractMetadataFromHtml(html: String, baseUrl: String): ExtractedWebMetadata {
  val ogTitle1 = Regex("""<meta[^>]+(?:property|name)=["'](?:og:title|twitter:title)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val ogTitle2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:title|twitter:title)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val productNameMatch = Regex(""""productName"\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val htmlTitle = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val jsonLdProductMatch = Regex(""""@type"\s*:\s*"Product"[\s\S]*?"name"\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)

  fun cleanTitleString(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return raw.replace("&amp;", "&")
      .replace("&#39;", "'")
      .replace("&quot;", "\"")
      .substringBefore('|')
      .substringBefore("—")
      .substringBefore(" – MediaMarkt")
      .substringBefore(" - MediaMarkt")
      .substringBefore(" – Alza")
      .substringBefore(" - Alza")
      .substringBefore(" – Euronics")
      .substringBefore(" - Euronics")
      .substringBefore(" vásárlás ")
      .trim()
      .takeIf { it.isNotBlank() }
  }

  val cleanProductName = cleanTitleString(productNameMatch)
  val cleanOgTitle = cleanTitleString(ogTitle1 ?: ogTitle2)
  val cleanHtmlTitle = cleanTitleString(htmlTitle)
  val cleanJsonLd = cleanTitleString(jsonLdProductMatch)

  // Pick the most complete title, avoiding single-brand words like "BOSCH" when full product name is available
  val titleCandidates = listOfNotNull(cleanProductName, cleanOgTitle, cleanHtmlTitle, cleanJsonLd)
  val cleanTitle = titleCandidates.maxByOrNull { it.split(' ').size }

  val jsonLdOfferPrice = Regex(""""offers"[\s\S]*?"price"\s*:\s*"?(\d+)""").find(html)?.groupValues?.get(1)
  val jsonLdPriceSpec = Regex(""""priceSpecification"[\s\S]*?"price"\s*:\s*"?(\d+)""").find(html)?.groupValues?.get(1)
  val jsonLdPricePattern = Regex(""""price"\s*:\s*"?(\d{4,8})""").find(html)?.groupValues?.get(1)
  val ogPricePattern1 = Regex("""<meta[^>]+(?:property|name)=["'](?:og:price:amount|product:price:amount|price)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val ogPricePattern2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:price:amount|product:price:amount|price)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
  val textPricePattern = Regex("""(\d{1,3}(?:[\s.]\d{3}){1,2})\s*(?:Ft|HUF)""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.replace(" ", "")?.replace(".", "")

  val rawPriceStr = jsonLdOfferPrice
    ?: jsonLdPriceSpec
    ?: jsonLdPricePattern
    ?: ogPricePattern1
    ?: ogPricePattern2
    ?: textPricePattern

  val extractedPrice = rawPriceStr?.toDoubleOrNull()?.roundToInt()

  val siteCategories = mutableListOf<String>()
  Regex(""""categoryName"\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).findAll(html).forEach {
    val cat = it.groupValues[1].replace("\\/", "/").trim()
    if (cat.isNotBlank() && cat.length < 50) siteCategories.add(cat)
  }
  Regex("""<meta[^>]+(?:property|name)=["'](?:product:category|category)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).findAll(html).forEach {
    val cat = it.groupValues[1].trim()
    if (cat.isNotBlank()) siteCategories.add(cat)
  }

  val brandMatch = Regex(""""(?:manufacturerName|brandName)"\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
    ?: Regex(""""brand"\s*:\s*\{[^}]*?"name"\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)

  val candidateImages = mutableListOf<String>()

  // 1. Meta og:image and twitter:image
  Regex("""<meta[^>]+(?:property|name)=["'](?:og:image|twitter:image|image)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    .findAll(html).forEach { candidateImages.add(it.groupValues[1]) }
  Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:image|twitter:image|image)["']""", RegexOption.IGNORE_CASE)
    .findAll(html).forEach { candidateImages.add(it.groupValues[1]) }

  // 2. JSON-LD "image": ["..."]
  Regex(""""image"\s*:\s*\[([^]]+)\]""", RegexOption.IGNORE_CASE).findAll(html).forEach { match ->
    Regex("""["'](https?:[^"']+)["']""").findAll(match.groupValues[1]).forEach {
      candidateImages.add(it.groupValues[1])
    }
  }

  // 3. JSON-LD "image": "..."
  Regex(""""image"\s*:\s*["'](https?:[^"']+)["']""", RegexOption.IGNORE_CASE).findAll(html).forEach {
    candidateImages.add(it.groupValues[1])
  }

  // 4. GraphQL productImages in MediaMarkt / retailers
  Regex(""""productImages"\s*:\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE).findAll(html).forEach { match ->
    Regex("""["']link["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).findAll(match.groupValues[1]).forEach {
      val link = it.groupValues[1].replace("\\/", "/")
      candidateImages.add(if (link.contains("/fee_") || link.endsWith(".png") || link.endsWith(".webp") || link.endsWith(".jpg")) link else "$link/fee_786_587_png")
    }
  }

  // 5. Preload LCP image links
  Regex("""<link[^>]+(?:rel=["']preload["'][^>]+as=["']image["']|as=["']image["'][^>]+rel=["']preload["'])[^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    .findAll(html).forEach { candidateImages.add(it.groupValues[1]) }
  Regex("""<link[^>]+href=["']([^"']+)["'][^>]+(?:rel=["']preload["'][^>]+as=["']image["']|as=["']image["'][^>]+rel=["']preload["'])""", RegexOption.IGNORE_CASE)
    .findAll(html).forEach { candidateImages.add(it.groupValues[1]) }
  Regex("""<link[^>]+rel=["']image_src["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    .findAll(html).forEach { candidateImages.add(it.groupValues[1]) }

  // 6. MediaMarkt asset URLs with resolution /fee_
  Regex("""(https://assets\.mmsrg\.com/isr/\d+/c1/-/[A-Za-z0-9_-]+/fee_[A-Za-z0-9_]+)""").findAll(html).forEach {
    candidateImages.add(it.groupValues[1])
  }

  val normalizedImages = candidateImages
    .mapNotNull { normalizeImageUrl(it, baseUrl) }
    .filter { url ->
      val lower = url.lowercase()
      (lower.contains("assets.mmsrg.com") || lower.contains("fee_") ||
        lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp") ||
        lower.contains("product") || lower.contains("image")) &&
      !lower.contains("warranty") && !lower.contains("service") && !lower.contains("logo")
    }
    .distinct()

  return ExtractedWebMetadata(
    title = cleanTitle,
    priceHuf = extractedPrice,
    images = normalizedImages,
    siteCategories = siteCategories.distinct(),
    brand = brandMatch?.trim(),
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
    val encodedUrl = runCatching {
      val parsedUri = URI(if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) cleanUrl else "https://$cleanUrl")
      URI(
        parsedUri.scheme,
        parsedUri.authority,
        parsedUri.path,
        parsedUri.query,
        parsedUri.fragment
      ).toASCIIString()
    }.getOrDefault(cleanUrl)

    val htmlContent = runCatching {
      httpClient.get(encodedUrl) {
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
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
    val categoryHint = (webMeta?.siteCategories.orEmpty() + listOfNotNull(webMeta?.brand) + listOf(cleanUrl)).joinToString(" ")
    val categories = categoryGenerator.generateCategories(finalTitle, parsed.store, categoryHint)
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

    val additionalImages = (listOf(finalMainImage) + extractedImages).distinct().filter { it.isNotBlank() }

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
      lower.contains("bosch") && (lower.contains("szaritogep") || lower.contains("dryer") || lower.contains("wth83256by") || lower.contains("1430932") || lower.contains("1474420")) ->
        ParsedSeed(
          "BOSCH WTH83256BY Hőszivattyús szárítógép",
          "MediaMarkt",
          196_999,
          "https://assets.mmsrg.com/isr/166325/c1/-/ASSET_MMS_149111125/fee_786_587_png",
        )
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
          lowerTitle.contains("szaritogep") || lowerTitle.contains("mosogep") || lowerTitle.contains("bosch") -> 196_999
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
      lower.contains("szaritogep") || lower.contains("mosogep") || lower.contains("dryer") || lower.contains("washer") || lower.contains("bosch") -> "dryer"
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
