package org.stigmergic.dailysitting

import java.util.UUID

enum class AppScreen {
    TimerList,
    Editor,
    ManualSession,
    MeditationLog,
    Timer,
    Complete,
}

enum class HealthConnectStatus {
    Checking,
    Unavailable,
    NeedsPermission,
    Ready,
    Synced,
    Error,
}

data class HealthConnectUi(
    val status: HealthConnectStatus = HealthConnectStatus.Checking,
    val message: String = "Checking Health Connect",
)

data class TimerPreset(
    val id: String = newId(),
    val name: String,
    val durationMinutes: Int,
    val intervalMinutes: Int?,
    val bellSoundId: String = DefaultBellSoundId,
)

data class SittingSession(
    val id: String = newId(),
    val presetId: String,
    val presetName: String,
    val durationMinutes: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
)

data class DailySittingUiState(
    val screen: AppScreen = AppScreen.TimerList,
    val presets: List<TimerPreset> = emptyList(),
    val sessions: List<SittingSession> = emptyList(),
    val editingPreset: TimerPreset? = null,
    val selectedPreset: TimerPreset? = null,
    val completedSession: SittingSession? = null,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val streakDays: Int = 0,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val healthConnect: HealthConnectUi = HealthConnectUi(),
)

fun newId(): String = UUID.randomUUID().toString()
