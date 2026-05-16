package app.dailysitting

import android.app.Application
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

class DailySittingViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SittingStore(application)
    private val healthConnectWriter = HealthConnectWriter(application)
    private val bellPlayer = BellPlayer(application)

    private var tickerJob: Job? = null
    private var endRealtimeMillis: Long = 0L
    private var sessionStartedAtMillis: Long = 0L
    private var intervalSeconds: Int = 0
    private var nextIntervalBellSecond: Int = 0

    var uiState by mutableStateOf(DailySittingUiState())
        private set

    init {
        reloadLocalState()
        refreshHealthConnect()
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
        uiState = uiState.copy(
            screen = AppScreen.TimerList,
            editingPreset = null,
            selectedPreset = null,
            completedSession = null,
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
        sessionStartedAtMillis = System.currentTimeMillis()
        endRealtimeMillis = SystemClock.elapsedRealtime() + totalSeconds * 1_000L

        uiState = uiState.copy(
            screen = AppScreen.Timer,
            selectedPreset = preset,
            completedSession = null,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isTimerRunning = true,
        )
        launchTicker()
    }

    fun pauseTimer() {
        updateRemainingFromClock(playBells = false)
        tickerJob?.cancel()
        uiState = uiState.copy(isTimerRunning = false)
    }

    fun resumeTimer() {
        if (uiState.remainingSeconds <= 0) return
        endRealtimeMillis = SystemClock.elapsedRealtime() + uiState.remainingSeconds * 1_000L
        uiState = uiState.copy(isTimerRunning = true)
        launchTicker()
    }

    fun cancelTimer() {
        tickerJob?.cancel()
        uiState = uiState.copy(
            screen = AppScreen.TimerList,
            selectedPreset = null,
            completedSession = null,
            totalSeconds = 0,
            remainingSeconds = 0,
            isTimerRunning = false,
        )
    }

    fun previewBellSound(bellSoundId: String) {
        bellPlayer.play(cleanBellSoundId(bellSoundId))
    }

    override fun onCleared() {
        tickerJob?.cancel()
        bellPlayer.release()
        super.onCleared()
    }

    private fun reloadLocalState() {
        uiState = uiState.copy(
            presets = store.loadPresets(),
        )
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
                weekMinutes = weekCompletedMinutes(sessions),
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
            weekMinutes = 0,
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
        tickerJob?.cancel()
        val preset = uiState.selectedPreset ?: return
        val endedAtMillis = System.currentTimeMillis()
        val startedAtMillis = sessionStartedAtMillis.takeIf { it > 0L }
            ?: (endedAtMillis - uiState.totalSeconds * 1_000L)
        val session = SittingSession(
            presetId = preset.id,
            presetName = preset.name,
            durationMinutes = preset.durationMinutes,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )

        bellPlayer.play(preset.bellSoundId)
        uiState = uiState.copy(
            screen = AppScreen.Complete,
            completedSession = session,
            remainingSeconds = 0,
            isTimerRunning = false,
            healthConnect = uiState.healthConnect.copy(message = "Recording to Health Connect"),
        )

        viewModelScope.launch {
            syncSessionToHealthConnect(session)
        }
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
}
