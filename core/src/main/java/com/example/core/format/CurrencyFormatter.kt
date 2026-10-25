package com.example.core.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

  fun formatHuf(amount: Int): String {
    val negative = amount < 0
    val digits = abs(amount).toString()
    val grouped = StringBuilder()
    for (i in digits.indices) {
      if (i > 0 && (digits.length - i) % 3 == 0) {
        grouped.append(' ')
      }
      grouped.append(digits[i])
    }
    val prefix = if (negative) "-" else ""
    return "$prefix$grouped Ft"
  }

  fun formatPercentDrop(percentChange: Int): String {
    return when {
      percentChange < 0 -> "▼${abs(percentChange)}%"
      percentChange > 0 -> "▲$percentChange%"
      else -> "0%"
    }
  }

  fun formatShortDate(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.US)
    return formatter.format(Date(timestampMillis))
  }
}
