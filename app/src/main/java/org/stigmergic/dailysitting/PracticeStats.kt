// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

internal data class PracticeRange(val start: LocalDate, val end: LocalDate) {
    val dayCount: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

// Null means the rolling window; calendar history starts with the last completed month.
internal fun practiceRange(today: LocalDate, month: YearMonth?): PracticeRange =
    if (month == null) {
        PracticeRange(today.minusDays(29), today)
    } else {
        PracticeRange(month.atDay(1), minOf(month.atEndOfMonth(), today))
    }

internal fun dailyPracticeSeconds(
    sessions: List<SittingSession>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Long> = sessions.groupBy {
    // Match the existing home-screen stats: attribute a session to its local completion date.
    Instant.ofEpochMilli(it.endedAtMillis).atZone(zone).toLocalDate()
}.mapValues { (_, sessionsOnDay) -> sessionsOnDay.sumOf { it.durationSeconds.toLong() } }

internal data class PracticeSummary(
    val daysPracticed: Int,
    val totalSeconds: Long,
    val dailyAverageMinutes: Int,
    val longestStreak: Int,
)

internal fun practiceSummary(
    dailySeconds: Map<LocalDate, Long>,
    range: PracticeRange,
): PracticeSummary {
    var date = range.start
    var days = 0
    var total = 0L
    var streak = 0
    var longest = 0
    while (date <= range.end) {
        val seconds = dailySeconds[date] ?: 0L
        total += seconds
        if (seconds > 0) {
            days++
            streak++
            longest = maxOf(longest, streak)
        } else {
            streak = 0
        }
        date = date.plusDays(1)
    }
    return PracticeSummary(days, total, (total / 60.0 / range.dayCount).roundToInt(), longest)
}

internal fun practiceIntensity(seconds: Long): Int = when {
    seconds <= 0 -> 0
    seconds < 10 * 60 -> 1
    seconds < 20 * 60 -> 2
    seconds < 40 * 60 -> 3
    seconds <= 120 * 60 -> 4
    else -> 5
}

internal fun practiceDuration(seconds: Long): String {
    if (seconds in 1..59) return "<1m"
    val minutes = seconds / 60
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}
