@file:OptIn(androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi::class)

package org.stigmergic.dailysitting

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

val HealthConnectPermissions: Set<String> = setOf(
    HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    HealthPermission.getWritePermission(MindfulnessSessionRecord::class),
)

class HealthConnectWriter(private val context: Context) {
    suspend fun refreshState(): HealthConnectUi {
        val sdkStatus = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)
        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return HealthConnectUi(
                status = HealthConnectStatus.Unavailable,
                message = "Health Connect is not available on this device",
            )
        }

        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            return HealthConnectUi(
                status = HealthConnectStatus.Unavailable,
                message = "Install or update Health Connect to sync mindfulness minutes",
            )
        }

        val client = HealthConnectClient.getOrCreate(context)
        if (
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION) !=
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        ) {
            return HealthConnectUi(
                status = HealthConnectStatus.Unavailable,
                message = "Health Connect is installed, but mindfulness sessions are unavailable",
            )
        }

        val granted = client.permissionController.getGrantedPermissions()
        return if (granted.containsAll(HealthConnectPermissions)) {
            HealthConnectUi(
                status = HealthConnectStatus.Ready,
                message = "Health Connect is ready",
            )
        } else {
            HealthConnectUi(
                status = HealthConnectStatus.NeedsPermission,
                message = "Connect Health Connect to record sessions and load stats",
            )
        }
    }

    suspend fun readSessions(): List<SittingSession> {
        val currentState = refreshState()
        if (currentState.status != HealthConnectStatus.Ready) return emptyList()

        val client = HealthConnectClient.getOrCreate(context)
        val start = LocalDate.now().minusYears(10).atStartOfDay()
        val end = LocalDate.now().plusDays(1).atStartOfDay()
        val records = try {
            readMindfulnessRecords(client, start, end)
        } catch (error: Exception) {
            if (!error.isOtherAppsReadPermissionError()) throw error
            readMindfulnessRecords(
                client = client,
                start = start,
                end = end,
                dataOriginFilter = setOf(DataOrigin(context.packageName)),
            )
        }

        return records.map { record ->
            val durationMinutes = Duration.between(record.startTime, record.endTime)
                .toMinutes()
                .toInt()
                .coerceAtLeast(1)
            SittingSession(
                id = record.metadata.id.ifBlank { record.metadata.clientRecordId ?: newId() },
                presetId = record.metadata.clientRecordId ?: "health-connect",
                presetName = record.title ?: "Mindfulness session",
                durationMinutes = durationMinutes,
                startedAtMillis = record.startTime.toEpochMilli(),
                endedAtMillis = record.endTime.toEpochMilli(),
                isOwnedByApp = record.metadata.dataOrigin.packageName == context.packageName,
            )
        }
    }

    private suspend fun readMindfulnessRecords(
        client: HealthConnectClient,
        start: LocalDateTime,
        end: LocalDateTime,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
    ): List<MindfulnessSessionRecord> {
        val records = mutableListOf<MindfulnessSessionRecord>()
        var pageToken: String? = null

        do {
            val response = client.readRecords(
                ReadRecordsRequest<MindfulnessSessionRecord>(
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    dataOriginFilter = dataOriginFilter,
                    ascendingOrder = false,
                    pageSize = 1_000,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)

        return records
    }

    private fun Throwable.isOtherAppsReadPermissionError(): Boolean =
        generateSequence(this) { it.cause }
            .any { error ->
                error.message?.contains("from other applications", ignoreCase = true) == true
            }

    suspend fun writeSession(session: SittingSession): HealthConnectUi {
        val currentState = refreshState()
        if (currentState.status != HealthConnectStatus.Ready) {
            return currentState.copy(message = "Not recorded. ${currentState.message}")
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            client.insertRecords(listOf(session.toMindfulnessSessionRecord()))
            HealthConnectUi(
                status = HealthConnectStatus.Synced,
                message = "Saved to Health Connect",
            )
        } catch (error: Exception) {
            HealthConnectUi(
                status = HealthConnectStatus.Error,
                message = "Not recorded. Health Connect write failed: ${error.message ?: "unknown error"}",
            )
        }
    }

    suspend fun writeSessions(sessions: List<SittingSession>): HealthConnectUi {
        if (sessions.isEmpty()) {
            return HealthConnectUi(
                status = HealthConnectStatus.Synced,
                message = "No new sessions to import",
            )
        }

        val currentState = refreshState()
        if (currentState.status != HealthConnectStatus.Ready) {
            return currentState.copy(message = "Not imported. ${currentState.message}")
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            sessions
                .map { it.toMindfulnessSessionRecord() }
                .chunked(500)
                .forEach { records -> client.insertRecords(records) }
            HealthConnectUi(
                status = HealthConnectStatus.Synced,
                message = "Imported ${sessions.size} session${if (sessions.size == 1) "" else "s"}",
            )
        } catch (error: Exception) {
            HealthConnectUi(
                status = HealthConnectStatus.Error,
                message = "Not imported. Health Connect import failed: ${error.message ?: "unknown error"}",
            )
        }
    }

    suspend fun deleteSession(session: SittingSession): HealthConnectUi {
        if (!session.isOwnedByApp) {
            return HealthConnectUi(
                status = HealthConnectStatus.Error,
                message = "Not deleted. This session was recorded by another app.",
            )
        }

        val currentState = refreshState()
        if (currentState.status != HealthConnectStatus.Ready) {
            return currentState.copy(message = "Not deleted. ${currentState.message}")
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            client.deleteRecords(
                recordType = MindfulnessSessionRecord::class,
                recordIdsList = listOf(session.id),
                clientRecordIdsList = emptyList(),
            )
            HealthConnectUi(
                status = HealthConnectStatus.Synced,
                message = "Deleted from Health Connect",
            )
        } catch (error: Exception) {
            HealthConnectUi(
                status = HealthConnectStatus.Error,
                message = "Not deleted. Health Connect delete failed: ${error.message ?: "unknown error"}",
            )
        }
    }

    private fun SittingSession.toMindfulnessSessionRecord(): MindfulnessSessionRecord {
        val start = Instant.ofEpochMilli(startedAtMillis)
        val end = Instant.ofEpochMilli(endedAtMillis)
        val zoneRules = ZoneId.systemDefault().rules
        return MindfulnessSessionRecord(
            startTime = start,
            startZoneOffset = zoneRules.getOffset(start),
            endTime = end,
            endZoneOffset = zoneRules.getOffset(end),
            metadata = Metadata.manualEntry(clientRecordId = "daily-sitting-$id"),
            mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
            title = presetName,
        )
    }
}
