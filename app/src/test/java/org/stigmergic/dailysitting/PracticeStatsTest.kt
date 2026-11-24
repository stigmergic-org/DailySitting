// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeStatsTest {
    @Test
    fun rollingWindowIncludesTodayAndCrossesYearBoundary() {
        val range = practiceRange(LocalDate.of(2026, 1, 5), null)
        assertEquals(LocalDate.of(2025, 12, 7), range.start)
        assertEquals(LocalDate.of(2026, 1, 5), range.end)
        assertEquals(30, range.dayCount)
    }

    @Test
    fun historicalMonthIncludesLeapDay() {
        val range = practiceRange(LocalDate.of(2026, 11, 24), YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 1), range.start)
        assertEquals(LocalDate.of(2024, 2, 29), range.end)
        assertEquals(29, range.dayCount)
    }

    @Test
    fun currentMonthOnlyCountsElapsedDays() {
        val today = LocalDate.of(2026, 11, 3)
        val range = practiceRange(today, YearMonth.from(today))
        assertEquals(today, range.end)
        assertEquals(3, range.dayCount)
    }

    @Test
    fun averageIncludesDaysWithoutSittingAndExcludesSessionsOutsideRange() {
        val start = LocalDate.of(2026, 11, 1)
        val summary = practiceSummary(
            mapOf(
                start.minusDays(1) to 60_000L,
                start to 600L,
                start.plusDays(1) to 1200L,
                start.plusDays(4) to 60_000L,
            ),
            PracticeRange(start, start.plusDays(2)),
        )
        assertEquals(2, summary.daysPracticed)
        assertEquals(1800L, summary.totalSeconds)
        assertEquals(10, summary.dailyAverageMinutes)
        assertEquals(2, summary.longestStreak)
    }

    @Test
    fun longestStreakIsClippedToPeriodAndResetsOnEmptyDays() {
        val start = LocalDate.of(2026, 11, 1)
        val dailySeconds = listOf(-2L, -1L, 0L, 1L, 3L, 4L, 5L)
            .associate { start.plusDays(it) to 60L }
        val summary = practiceSummary(dailySeconds, PracticeRange(start, start.plusDays(5)))
        assertEquals(5, summary.daysPracticed)
        assertEquals(3, summary.longestStreak)
    }

    @Test
    fun emptyHistoryHasZeroStats() {
        assertEquals(
            PracticeSummary(0, 0, 0, 0),
            practiceSummary(emptyMap(), practiceRange(LocalDate.of(2026, 11, 24), null)),
        )
    }

    @Test
    fun multipleSessionsAreSummedByLocalCompletionDate() {
        val zone = ZoneId.of("America/Los_Angeles")
        val date = LocalDate.of(2026, 11, 1)
        val end = date.atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            session(end, 600),
            session(end + 60_000, 1200),
            session(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), 30),
        )
        assertEquals(mapOf(date to 1800L, date.plusDays(1) to 30L), dailyPracticeSeconds(sessions, zone))
    }

    @Test
    fun shortSittingIsNotCountedAsAnEmptyDay() {
        val date = LocalDate.of(2026, 11, 24)
        val summary = practiceSummary(mapOf(date to 1L), PracticeRange(date, date))
        assertEquals(1, summary.daysPracticed)
        assertEquals(1, practiceIntensity(1))
        assertEquals("<1m", practiceDuration(1))
    }

    @Test
    fun intensityThresholdsIncludeStrictlyOverTwoHours() {
        val cases = mapOf(
            0L to 0,
            1L to 1,
            599L to 1,
            600L to 2,
            1199L to 2,
            1200L to 3,
            2399L to 3,
            2400L to 4,
            7200L to 4,
            7201L to 5,
        )
        cases.forEach { (seconds, expected) -> assertEquals("$seconds seconds", expected, practiceIntensity(seconds)) }
    }

    @Test
    fun durationUsesHoursForLongTotals() {
        assertEquals("0m", practiceDuration(0))
        assertEquals("25m", practiceDuration(1500))
        assertEquals("2h 5m", practiceDuration(7500))
    }

    private fun session(endMillis: Long, seconds: Int) = SittingSession(
        presetId = "test",
        presetName = "Test",
        durationMinutes = seconds / 60,
        durationSeconds = seconds,
        startedAtMillis = endMillis - seconds * 1000L,
        endedAtMillis = endMillis,
    )
}
