package app.dailysitting

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

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
        .sumOf { it.durationMinutes }
}

fun weekCompletedMinutes(
    sessions: List<SittingSession>,
    clock: Clock = Clock.systemDefaultZone(),
): Int {
    val today = LocalDate.now(clock)
    val weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    return sessions
        .filter { Instant.ofEpochMilli(it.endedAtMillis).atZone(clock.zone).toLocalDate() >= weekStart }
        .sumOf { it.durationMinutes }
}

fun totalCompletedMinutes(sessions: List<SittingSession>): Int = sessions.sumOf { it.durationMinutes }
