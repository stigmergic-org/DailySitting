// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.app.Application
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val LowBellVolumeFraction = 0.20f

class DailySittingViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SittingStore(application)
    private val healthConnectWriter = HealthConnectWriter(application)
    private val bellPlayer = BellPlayer(application)
    private val audioManager: AudioManager? = application.getSystemService(AudioManager::class.java)
    private val timerMediaNotification = TimerMediaNotification(
        application,
        object : TimerMediaNotificationControls {
            override fun onPauseTimer() {
                pauseTimer()
            }

            override fun onResumeTimer() {
                resumeTimer()
            }

            override fun onBackToTimers() {
                showTimerList()
            }

            override fun onAddCompletedTime() {
                saveCompletedSessionWithAdditionalTime()
            }
        },
    )

    private var tickerJob: Job? = null
    private var sessionSyncJob: Job? = null
    private var endRealtimeMillis: Long = 0L
    private var sessionPausedAtMillis: Long = 0L
    private var completionRealtimeMillis: Long = 0L
    private var intervalSeconds: Int = 0
    private var nextIntervalBellSecond: Int = 0

    var uiState by mutableStateOf(DailySittingUiState())
        private set

    init {
        reloadLocalState()
        refreshBellVolumeWarning()
        refreshHealthConnect()
    }

    fun refreshBellVolumeWarning() {
        uiState = uiState.copy(bellVolumeWarning = currentBellVolumeWarning())
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            refreshHealthConnectNow()
        }
    }

    fun onHealthPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            refreshHealthConnectNow()
            if (granted) {
                uiState.completedSession?.let { completedSession ->
                    syncSessionToHealthConnect(completedSession)
                } ?: refreshStatsFromHealthConnect()
            }
        }
    }

    fun showTimerList() {
        tickerJob?.cancel()
        timerMediaNotification.cancel()
        completionRealtimeMillis = 0L
        uiState = uiState.copy(
            screen = AppScreen.TimerList,
            editingPreset = null,
            selectedPreset = null,
            completedSession = null,
            remainingSeconds = 0,
            totalSeconds = 0,
            isTimerRunning = false,
            completionExtraSeconds = 0,
            canExtendCompletedSession = false,
            bellVolumeWarning = currentBellVolumeWarning(),
        )
    }

    fun showEditor(preset: TimerPreset?) {
        uiState = uiState.copy(
            screen = AppScreen.Editor,
            editingPreset = preset,
        )
    }

    fun showManualSession() {
        uiState = uiState.copy(
            screen = AppScreen.ManualSession,
            editingPreset = null,
            selectedPreset = null,
            completedSession = null,
        )
    }

    fun showMeditationLog() {
        uiState = uiState.copy(
            screen = AppScreen.MeditationLog,
            editingPreset = null,
            selectedPreset = null,
            completedSession = null,
        )
        refreshHealthConnect()
    }

    fun savePreset(name: String, durationMinutes: Int, intervalMinutes: Int?, bellSoundId: String) {
        val existingPreset = uiState.editingPreset
        val cleanDuration = durationMinutes.coerceIn(1, 24 * 60)
        val cleanInterval = intervalMinutes
            ?.takeIf { it > 0 && it < cleanDuration }
        val cleanName = name.trim().ifBlank { "$cleanDuration minutes" }

        store.savePreset(
            TimerPreset(
                id = existingPreset?.id ?: newId(),
                name = cleanName,
                durationMinutes = cleanDuration,
                intervalMinutes = cleanInterval,
                bellSoundId = cleanBellSoundId(bellSoundId),
            ),
        )
        reloadLocalState()
        showTimerList()
    }

    fun deletePreset(preset: TimerPreset) {
        store.deletePreset(preset.id)
        reloadLocalState()
        showTimerList()
    }

    fun deleteSession(session: SittingSession) {
        viewModelScope.launch {
            val healthConnect = healthConnectWriter.deleteSession(session)
            uiState = uiState.copy(healthConnect = healthConnect)
            if (healthConnect.status == HealthConnectStatus.Synced) {
                refreshStatsFromHealthConnect()
            }
        }
    }

    fun importInsightTimerLogs(uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(
                healthConnect = uiState.healthConnect.copy(message = "Importing Insight Timer logs"),
            )

            val result = try {
                val csv = withContext(Dispatchers.IO) {
                    getApplication<Application>()
                        .contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalArgumentException("Could not open selected file")
                }
                parseInsightTimerLogs(csv)
            } catch (error: Exception) {
                uiState = uiState.copy(
                    healthConnect = HealthConnectUi(
                        status = HealthConnectStatus.Error,
                        message = "Could not import logs: ${error.message ?: "unknown error"}",
                    ),
                )
                return@launch
            }

            if (result.sessions.isEmpty()) {
                uiState = uiState.copy(
                    healthConnect = HealthConnectUi(
                        status = HealthConnectStatus.Error,
                        message = "No Insight Timer sessions found in the selected file",
                    ),
                )
                return@launch
            }

            val existingSessionKeys = uiState.sessions
                .map { it.startedAtMillis to it.endedAtMillis }
                .toSet()
            val importSessions = result.sessions
                .distinctBy { it.startedAtMillis to it.endedAtMillis }
                .filterNot { (it.startedAtMillis to it.endedAtMillis) in existingSessionKeys }

            val healthConnect = healthConnectWriter.writeSessions(importSessions)
            val skippedCount = result.skippedRows + result.sessions.size - importSessions.size
            val message = if (healthConnect.status == HealthConnectStatus.Synced) {
                buildImportMessage(importSessions.size, skippedCount)
            } else {
                healthConnect.message
            }
            uiState = uiState.copy(healthConnect = healthConnect.copy(message = message))
            if (healthConnect.status == HealthConnectStatus.Synced) {
                refreshStatsFromHealthConnect()
            }
        }
    }

    fun addManualSession(endedDate: LocalDate, endedTime: LocalTime, durationMinutes: Int) {
        val cleanDuration = durationMinutes.coerceIn(1, 24 * 60)
        val endedAtMillis = endedDate
            .atTime(endedTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val startedAtMillis = endedAtMillis - cleanDuration * 60_000L
        val session = SittingSession(
            presetId = "manual",
            presetName = "Manual session",
            durationMinutes = cleanDuration,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )

        uiState = uiState.copy(
            screen = AppScreen.TimerList,
            completedSession = null,
            healthConnect = uiState.healthConnect.copy(message = "Recording manual session"),
            bellVolumeWarning = currentBellVolumeWarning(),
        )

        viewModelScope.launch {
            syncSessionToHealthConnect(session)
        }
    }

    fun startTimer(preset: TimerPreset) {
        tickerJob?.cancel()
        val totalSeconds = preset.durationMinutes * 60
        intervalSeconds = (preset.intervalMinutes ?: 0) * 60
        nextIntervalBellSecond = intervalSeconds
        sessionPausedAtMillis = 0L
        completionRealtimeMillis = 0L
        endRealtimeMillis = SystemClock.elapsedRealtime() + totalSeconds * 1_000L

        uiState = uiState.copy(
            screen = AppScreen.Timer,
            selectedPreset = preset,
            completedSession = null,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isTimerRunning = true,
            completionExtraSeconds = 0,
            canExtendCompletedSession = false,
        )
        updateTimerNotification()
        launchTicker()
    }

    fun pauseTimer() {
        if (!uiState.isTimerRunning) return
        updateRemainingFromClock(playBells = false)
        tickerJob?.cancel()
        sessionPausedAtMillis = System.currentTimeMillis()
        uiState = uiState.copy(isTimerRunning = false)
        updateTimerNotification()
    }

    fun resumeTimer() {
        if (uiState.remainingSeconds <= 0) return
        sessionPausedAtMillis = 0L
        endRealtimeMillis = SystemClock.elapsedRealtime() + uiState.remainingSeconds * 1_000L
        uiState = uiState.copy(isTimerRunning = true)
        updateTimerNotification()
        launchTicker()
    }

    fun savePausedSession() {
        if (uiState.isTimerRunning) return
        val preset = uiState.selectedPreset ?: return
        val elapsedSeconds = (uiState.totalSeconds - uiState.remainingSeconds).coerceAtLeast(0)
        if (elapsedSeconds <= 0) return

        tickerJob?.cancel()
        timerMediaNotification.cancel()
        completionRealtimeMillis = 0L
        val endedAtMillis = sessionPausedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val session = timerSession(
            preset = preset,
            activeSeconds = elapsedSeconds,
            endedAtMillis = endedAtMillis,
        )

        uiState = uiState.copy(
            screen = AppScreen.PartialComplete,
            completedSession = session,
            remainingSeconds = 0,
            isTimerRunning = false,
            healthConnect = HealthConnectUi(
                status = HealthConnectStatus.Checking,
                message = "Recording partial session",
            ),
        )

        sessionSyncJob = viewModelScope.launch {
            syncSessionToHealthConnect(session)
        }
    }

    fun finishPartialCompletion() {
        if (uiState.screen == AppScreen.PartialComplete) {
            showTimerList()
        }
    }

    fun showCompletedSessionConfirmation() {
        if (uiState.screen != AppScreen.Complete) return

        tickerJob?.cancel()
        timerMediaNotification.cancel()
        completionRealtimeMillis = 0L
        uiState = uiState.copy(
            screen = AppScreen.PartialComplete,
            completionExtraSeconds = 0,
            canExtendCompletedSession = false,
        )
    }

    fun saveCompletedSessionWithAdditionalTime() {
        if (!uiState.canExtendCompletedSession) return
        val completedSession = uiState.completedSession ?: return
        val extraSeconds = currentCompletionExtraSeconds()
        if (extraSeconds <= 0) return

        val activeSeconds = (uiState.totalSeconds + extraSeconds).coerceAtLeast(1)
        val endedAtMillis = System.currentTimeMillis()
        val updatedSession = completedSession.copy(
            durationMinutes = durationMinutesForSeconds(activeSeconds),
            durationSeconds = activeSeconds,
            startedAtMillis = endedAtMillis - activeSeconds * 1_000L,
            endedAtMillis = endedAtMillis,
        )

        tickerJob?.cancel()
        timerMediaNotification.cancel()
        completionRealtimeMillis = 0L
        uiState = uiState.copy(
            screen = AppScreen.PartialComplete,
            completedSession = updatedSession,
            completionExtraSeconds = extraSeconds,
            canExtendCompletedSession = false,
            healthConnect = HealthConnectUi(
                status = HealthConnectStatus.Checking,
                message = "Recording extended session",
            ),
        )

        val previousSyncJob = sessionSyncJob
        sessionSyncJob = viewModelScope.launch {
            previousSyncJob?.join()
            syncSessionToHealthConnect(updatedSession)
        }
    }

    fun discardCompletedSession() {
        val completedSession = uiState.completedSession ?: return
        val previousSyncJob = sessionSyncJob
        showTimerList()

        sessionSyncJob = viewModelScope.launch {
            previousSyncJob?.join()
            val healthConnect = healthConnectWriter.deleteSession(completedSession)
            uiState = uiState.copy(healthConnect = healthConnect)
            if (healthConnect.status == HealthConnectStatus.Synced) {
                refreshStatsFromHealthConnect()
            }
        }
    }

    fun cancelTimer() {
        tickerJob?.cancel()
        timerMediaNotification.cancel()
        sessionPausedAtMillis = 0L
        completionRealtimeMillis = 0L
        uiState = uiState.copy(
            screen = AppScreen.TimerList,
            selectedPreset = null,
            completedSession = null,
            totalSeconds = 0,
            remainingSeconds = 0,
            isTimerRunning = false,
            completionExtraSeconds = 0,
            canExtendCompletedSession = false,
            bellVolumeWarning = currentBellVolumeWarning(),
        )
    }

    fun previewBellSound(bellSoundId: String) {
        bellPlayer.play(cleanBellSoundId(bellSoundId))
    }

    override fun onCleared() {
        tickerJob?.cancel()
        timerMediaNotification.release()
        bellPlayer.release()
        super.onCleared()
    }

    private fun reloadLocalState() {
        uiState = uiState.copy(
            presets = store.loadPresets(),
        )
    }

    private fun currentBellVolumeWarning(): BellVolumeWarning? {
        val audioManager = audioManager ?: return null
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume <= 0) return null

        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val lowVolume = (maxVolume * LowBellVolumeFraction).toInt().coerceAtLeast(1)
        return when {
            volume <= 0 -> BellVolumeWarning.Muted
            volume <= lowVolume -> BellVolumeWarning.Low
            else -> null
        }
    }

    private suspend fun refreshHealthConnectNow() {
        uiState = uiState.copy(healthConnect = HealthConnectUi())
        val healthConnect = healthConnectWriter.refreshState()
        uiState = uiState.copy(healthConnect = healthConnect)
        if (healthConnect.status == HealthConnectStatus.Ready) {
            refreshStatsFromHealthConnect()
        } else {
            clearHealthConnectStats()
        }
    }

    private suspend fun refreshStatsFromHealthConnect() {
        try {
            val sessions = healthConnectWriter.readSessions()
            uiState = uiState.copy(
                sessions = sessions,
                streakDays = currentStreakDays(sessions),
                todayMinutes = todayCompletedMinutes(sessions),
                sevenDayAverageMinutes = sevenDayAverageCompletedMinutes(sessions),
                thirtyDayAverageMinutes = thirtyDayAverageCompletedMinutes(sessions),
                totalMinutes = totalCompletedMinutes(sessions),
            )
        } catch (error: Exception) {
            clearHealthConnectStats()
            uiState = uiState.copy(
                healthConnect = HealthConnectUi(
                    status = HealthConnectStatus.Error,
                    message = "Could not read Health Connect stats: ${error.message ?: "unknown error"}",
                ),
            )
        }
    }

    private fun clearHealthConnectStats() {
        uiState = uiState.copy(
            sessions = emptyList(),
            streakDays = 0,
            todayMinutes = 0,
            sevenDayAverageMinutes = 0,
            thirtyDayAverageMinutes = 0,
            totalMinutes = 0,
        )
    }

    private fun launchTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val remainingSeconds = updateRemainingFromClock(playBells = true)
                if (remainingSeconds <= 0) {
                    completeTimer()
                    break
                }
                delay(250L)
            }
        }
    }

    private fun updateRemainingFromClock(playBells: Boolean): Int {
        val millisRemaining = (endRealtimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val remainingSeconds = ((millisRemaining + 999L) / 1_000L).toInt()
        if (remainingSeconds != uiState.remainingSeconds) {
            uiState = uiState.copy(remainingSeconds = remainingSeconds)
            updateTimerNotification()
        }

        if (playBells) {
            playIntervalBellIfNeeded(uiState.totalSeconds - remainingSeconds)
        }

        return remainingSeconds
    }

    private fun playIntervalBellIfNeeded(elapsedSeconds: Int) {
        if (intervalSeconds <= 0) return
        if (nextIntervalBellSecond <= 0) return
        if (elapsedSeconds >= uiState.totalSeconds) return
        if (elapsedSeconds < nextIntervalBellSecond) return

        bellPlayer.play(uiState.selectedPreset?.bellSoundId)
        while (nextIntervalBellSecond <= elapsedSeconds) {
            nextIntervalBellSecond += intervalSeconds
        }
    }

    private fun completeTimer() {
        val preset = uiState.selectedPreset ?: return
        val endedAtMillis = System.currentTimeMillis()
        completionRealtimeMillis = SystemClock.elapsedRealtime()
        val session = timerSession(
            preset = preset,
            activeSeconds = uiState.totalSeconds,
            endedAtMillis = endedAtMillis,
        )

        bellPlayer.play(preset.bellSoundId)
        uiState = uiState.copy(
            screen = AppScreen.Complete,
            completedSession = session,
            remainingSeconds = 0,
            isTimerRunning = false,
            completionExtraSeconds = 0,
            canExtendCompletedSession = true,
            healthConnect = HealthConnectUi(
                status = HealthConnectStatus.Checking,
                message = "Recording to Health Connect",
            ),
        )

        launchCompletionTicker()
        viewModelScope.launch {
            delay(300L)
            updateCompletedNotification()
        }
        sessionSyncJob = viewModelScope.launch {
            syncSessionToHealthConnect(session)
        }
    }

    private fun timerSession(
        preset: TimerPreset,
        activeSeconds: Int,
        endedAtMillis: Long,
    ): SittingSession {
        val boundedActiveSeconds = activeSeconds.coerceAtLeast(1)
        return SittingSession(
            presetId = preset.id,
            presetName = preset.name,
            durationMinutes = durationMinutesForSeconds(boundedActiveSeconds),
            durationSeconds = boundedActiveSeconds,
            startedAtMillis = endedAtMillis - boundedActiveSeconds * 1_000L,
            endedAtMillis = endedAtMillis,
        )
    }

    private fun durationMinutesForSeconds(seconds: Int): Int = seconds / 60

    private fun launchCompletionTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val extraSeconds = currentCompletionExtraSeconds()
                if (extraSeconds != uiState.completionExtraSeconds) {
                    uiState = uiState.copy(completionExtraSeconds = extraSeconds)
                    updateCompletedNotification()
                }
                delay(250L)
            }
        }
    }

    private fun currentCompletionExtraSeconds(): Int {
        if (completionRealtimeMillis <= 0L) return uiState.completionExtraSeconds
        return ((SystemClock.elapsedRealtime() - completionRealtimeMillis) / 1_000L)
            .toInt()
            .coerceAtLeast(0)
    }

    private suspend fun syncSessionToHealthConnect(session: SittingSession) {
        val healthConnect = healthConnectWriter.writeSession(session)
        uiState = uiState.copy(healthConnect = healthConnect)
        if (healthConnect.status == HealthConnectStatus.Synced) {
            refreshStatsFromHealthConnect()
        }
    }

    private fun buildImportMessage(importedCount: Int, skippedCount: Int): String {
        val importedText = if (importedCount == 0) {
            "No new sessions to import"
        } else {
            "Imported $importedCount Insight Timer session${if (importedCount == 1) "" else "s"}"
        }
        return if (skippedCount > 0) {
            "$importedText. Skipped $skippedCount duplicate or invalid row${if (skippedCount == 1) "" else "s"}."
        } else {
            importedText
        }
    }

    private fun updateTimerNotification() {
        val preset = uiState.selectedPreset ?: return
        if (uiState.totalSeconds <= 0) {
            timerMediaNotification.cancel()
            return
        }
        if (uiState.remainingSeconds <= 0) return

        timerMediaNotification.show(
            preset = preset,
            totalSeconds = uiState.totalSeconds,
            remainingSeconds = uiState.remainingSeconds,
            isRunning = uiState.isTimerRunning,
        )
    }

    private fun updateCompletedNotification() {
        val preset = uiState.selectedPreset ?: return
        if (uiState.screen != AppScreen.Complete) return

        timerMediaNotification.showCompleted(
            preset = preset,
            extraSeconds = uiState.completionExtraSeconds,
        )
    }
}
