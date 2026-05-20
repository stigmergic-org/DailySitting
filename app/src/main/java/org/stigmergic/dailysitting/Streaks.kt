// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

fun currentStreakDays(
    sessions: List<SittingSession>,
    clock: Clock = Clock.systemDefaultZone(),
): Int {
    val completedDays = sessions
        .map { Instant.ofEpochMilli(it.endedAtMillis).atZone(clock.zone).toLocalDate() }
        .toSet()

    var cursor = LocalDate.now(clock)
    var streak = 0
    while (completedDays.contains(cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun todayCompletedMinutes(
    sessions: List<SittingSession>,
    clock: Clock = Clock.systemDefaultZone(),
): Int {
    val today = LocalDate.now(clock)
    return sessions
        .filter { Instant.ofEpochMilli(it.endedAtMillis).atZone(clock.zone).toLocalDate() == today }
        .sumOf { it.durationSeconds } / 60
}

fun sevenDayAverageCompletedMinutes(
    sessions: List<SittingSession>,
    clock: Clock = Clock.systemDefaultZone(),
): Int = averageCompletedMinutes(sessions = sessions, days = 7, clock = clock)

fun thirtyDayAverageCompletedMinutes(
    sessions: List<SittingSession>,
    clock: Clock = Clock.systemDefaultZone(),
): Int = averageCompletedMinutes(sessions = sessions, days = 30, clock = clock)

private fun averageCompletedMinutes(
    sessions: List<SittingSession>,
    days: Long,
    clock: Clock,
): Int {
    val today = LocalDate.now(clock)
    val windowStart = today.minusDays(days - 1)
    val totalSeconds = sessions
        .filter {
            val date = Instant.ofEpochMilli(it.endedAtMillis).atZone(clock.zone).toLocalDate()
            date in windowStart..today
        }
        .sumOf { it.durationSeconds }
    return (totalSeconds / 60f / days.toFloat()).roundToInt()
}

fun totalCompletedMinutes(sessions: List<SittingSession>): Int = sessions.sumOf { it.durationSeconds } / 60
