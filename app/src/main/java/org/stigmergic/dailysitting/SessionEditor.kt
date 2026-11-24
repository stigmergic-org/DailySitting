// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun sessionFromForm(
    original: SittingSession?,
    name: String,
    endedDate: LocalDate,
    endedTime: LocalTime,
    durationMinutes: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): SittingSession {
    val minutes = durationMinutes.coerceIn(1, 24 * 60)
    // Keep sub-minute precision when the user only changes the name or end time.
    val seconds = if (original != null && minutes == original.durationMinutes.coerceIn(1, 24 * 60)) {
        original.durationSeconds
    } else {
        minutes * 60
    }
    val end = endedDate.atTime(endedTime).atZone(zone).toInstant().toEpochMilli()
    val base = original ?: SittingSession(
        presetId = "manual",
        presetName = "Manual session",
        durationMinutes = minutes,
        startedAtMillis = end - seconds * 1_000L,
        endedAtMillis = end,
    )
    return base.copy(
        presetName = name.trim().ifBlank { base.presetName },
        durationMinutes = seconds / 60,
        durationSeconds = seconds,
        startedAtMillis = end - seconds * 1_000L,
        endedAtMillis = end,
    )
}
