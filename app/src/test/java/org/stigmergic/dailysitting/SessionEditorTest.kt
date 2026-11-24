// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionEditorTest {
    private val date = LocalDate.of(2026, 11, 23)
    private val time = LocalTime.of(18, 30, 12)
    private val zone = ZoneId.of("UTC")

    @Test
    fun blankManualNameUsesDefault() {
        val session = sessionFromForm(null, "  ", date, time, 10, zone)
        assertEquals("Manual session", session.presetName)
        assertEquals("manual", session.presetId)
        assertEquals(600, session.durationSeconds)
        assertEquals(600_000L, session.endedAtMillis - session.startedAtMillis)
    }

    @Test
    fun customNameIsTrimmed() {
        assertEquals(
            "Evening sit",
            sessionFromForm(null, "  Evening sit  ", date, time, 10, zone).presetName,
        )
    }

    @Test
    fun editingPreservesIdentityAndBlankNameKeepsOriginal() {
        val original = sessionFromForm(null, "Guided meditation", date, time, 10, zone)
        val edited = sessionFromForm(original, " ", date.plusDays(1), time, 20, zone)
        assertEquals(original.id, edited.id)
        assertEquals(original.presetId, edited.presetId)
        assertEquals(original.presetName, edited.presetName)
        assertEquals(1200, edited.durationSeconds)
        assertEquals(86_400_000L, edited.endedAtMillis - original.endedAtMillis)
        assertEquals(1_200_000L, edited.endedAtMillis - edited.startedAtMillis)
    }

    @Test
    fun renamingDoesNotRoundDurationOrTimestamps() {
        val original = sessionFromForm(null, "", date, time, 10, zone).copy(
            durationSeconds = 623,
            startedAtMillis = date.atTime(time).atZone(zone).toInstant().toEpochMilli() - 623_000L,
        )
        val edited = sessionFromForm(original, "Renamed", date, time, 10, zone)
        assertEquals(original.copy(presetName = "Renamed"), edited)
    }

    @Test
    fun renamingShortSessionPreservesSeconds() {
        val original = sessionFromForm(null, "", date, time, 1, zone).copy(
            durationMinutes = 0,
            durationSeconds = 25,
        )
        val edited = sessionFromForm(original, "Short sit", date, time, 1, zone)
        assertEquals(25, edited.durationSeconds)
        assertEquals(25_000L, edited.endedAtMillis - edited.startedAtMillis)
    }

    @Test
    fun durationIsBoundedAndSessionsCanCrossMidnight() {
        val session = sessionFromForm(null, "", date, LocalTime.of(0, 5), 0, zone)
        assertEquals(1, session.durationMinutes)
        val overnight = sessionFromForm(null, "", date, LocalTime.of(0, 5), 10, zone)
        assertEquals(
            date.minusDays(1).atTime(23, 55).atZone(zone).toInstant().toEpochMilli(),
            overnight.startedAtMillis,
        )
        assertEquals(1440, sessionFromForm(null, "", date, time, 2000, zone).durationMinutes)
    }
}
