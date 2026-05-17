// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class InsightTimerImportResult(
    val sessions: List<SittingSession>,
    val skippedRows: Int,
)

private val InsightTimerDateFormatters = listOf(
    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss", Locale.US),
    DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US),
)

fun parseInsightTimerLogs(csv: String, zoneId: ZoneId = ZoneId.systemDefault()): InsightTimerImportResult {
    val lines = csv
        .lineSequence()
        .map { it.trimEnd('\r') }
        .filter { it.isNotBlank() }
        .toList()
    if (lines.isEmpty()) return InsightTimerImportResult(emptyList(), skippedRows = 0)

    val header = parseCsvLine(lines.first()).map { it.trim().lowercase(Locale.US) }
    val startedAtIndex = header.indexOf("started at")
    val durationIndex = header.indexOf("duration")
    val presetIndex = header.indexOf("preset")
    val activityIndex = header.indexOf("activity")
    if (startedAtIndex == -1 || durationIndex == -1) {
        return InsightTimerImportResult(emptyList(), skippedRows = lines.drop(1).size)
    }

    val sessions = mutableListOf<SittingSession>()
    var skippedRows = 0

    lines.drop(1).forEach { line ->
        val columns = parseCsvLine(line)
        val startedAtText = columns.getOrNull(startedAtIndex).orEmpty().trim()
        val durationText = columns.getOrNull(durationIndex).orEmpty().trim()
        val startedAt = parseInsightTimerStartedAt(startedAtText)
        val durationSeconds = parseInsightTimerDurationSeconds(durationText)

        if (startedAt == null || durationSeconds == null || durationSeconds <= 0L) {
            skippedRows += 1
            return@forEach
        }

        val activity = columns.getOrNull(activityIndex).orEmpty().trim()
        if (activity.isNotBlank() && !activity.equals("Meditation", ignoreCase = true)) {
            skippedRows += 1
            return@forEach
        }

        val preset = columns.getOrNull(presetIndex).orEmpty().trim()
        val title = preset.ifBlank { "Insight Timer" }
        val startedAtMillis = startedAt
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val endedAtMillis = startedAtMillis + durationSeconds * 1_000L
        val durationSecondsInt = durationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        sessions += SittingSession(
            id = "insight-timer-$startedAtMillis-$durationSeconds",
            presetId = "insight-timer",
            presetName = title,
            durationMinutes = durationSecondsInt / 60,
            durationSeconds = durationSecondsInt,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )
    }

    return InsightTimerImportResult(sessions = sessions, skippedRows = skippedRows)
}

private fun parseInsightTimerStartedAt(value: String): LocalDateTime? {
    for (formatter in InsightTimerDateFormatters) {
        try {
            return LocalDateTime.parse(value, formatter)
        } catch (_: Exception) {
        }
    }
    return null
}

private fun parseInsightTimerDurationSeconds(value: String): Long? {
    val parts = value.split(':').map { it.trim().toLongOrNull() ?: return null }
    return when (parts.size) {
        3 -> parts[0] * 3_600L + parts[1] * 60L + parts[2]
        2 -> parts[0] * 60L + parts[1]
        1 -> parts[0]
        else -> null
    }
}

private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0

    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
        index += 1
    }

    values += current.toString()
    return values
}
