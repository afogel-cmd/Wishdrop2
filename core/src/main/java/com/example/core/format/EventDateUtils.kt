package com.example.core.format

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object EventDateUtils {

  val POPULAR_GIFT_INTERESTS: List<String> = listOf(
    "Könyvek",
    "Kávé",
    "Virágok",
    "Lakberendezés",
    "Technológia",
    "Kreatív Hobbik",
    "Gasztronómia",
    "Divat",
    "Gaming",
    "Wellness",
    "Sport",
    "Zene",
  )

  fun calculateDaysUntilRecurringEvent(targetDateMillis: Long): Int {
    val calendarToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val calendarTarget = Calendar.getInstance().apply {
      timeInMillis = targetDateMillis
    }

    val todayYear = calendarToday.get(Calendar.YEAR)
    val targetMonth = calendarTarget.get(Calendar.MONTH)
    val targetDay = calendarTarget.get(Calendar.DAY_OF_MONTH)

    val nextOccurrence = Calendar.getInstance().apply {
      set(Calendar.YEAR, todayYear)
      set(Calendar.MONTH, targetMonth)
      set(Calendar.DAY_OF_MONTH, targetDay)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (nextOccurrence.before(calendarToday)) {
      nextOccurrence.add(Calendar.YEAR, 1)
    }

    val diffMillis = nextOccurrence.timeInMillis - calendarToday.timeInMillis
    return (diffMillis / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt()
  }

  fun formatEventDate(dateMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return formatter.format(Date(dateMillis))
  }

  fun formatDaysRemaining(days: Int): String {
    return when (days) {
      0 -> "Ma van!"
      1 -> "Holnap!"
      else -> "$days nap múlva"
    }
  }

  fun determineEventType(eventName: String): String {
    val lower = eventName.lowercase()
    return when {
      lower.contains("szüli") || lower.contains("születés") || lower.contains("birthday") -> "Születésnap"
      lower.contains("évforduló") || lower.contains("anniversary") || lower.contains("házasság") -> "Évforduló"
      lower.contains("karácsony") || lower.contains("christmas") -> "Karácsony"
      lower.contains("névnap") || lower.contains("nameday") -> "Névnap"
      else -> "Egyéb"
    }
  }
}
