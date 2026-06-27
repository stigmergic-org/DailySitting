// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat

private const val ActionShowActiveTimer = "org.stigmergic.dailysitting.service.SHOW_ACTIVE_TIMER"
private const val ActionShowCompletedTimer = "org.stigmergic.dailysitting.service.SHOW_COMPLETED_TIMER"
private const val ActionCancelTimerService = "org.stigmergic.dailysitting.service.CANCEL_TIMER_SERVICE"
private const val ExtraPresetName = "presetName"
private const val ExtraBellSoundId = "bellSoundId"
private const val ExtraTotalSeconds = "totalSeconds"
private const val ExtraRemainingSeconds = "remainingSeconds"
private const val ExtraIsRunning = "isRunning"
private const val ExtraExtraSeconds = "extraSeconds"
private const val ExtraPlayBell = "playBell"

class TimerForegroundService : Service() {
    private lateinit var timerNotification: TimerMediaNotification
    private lateinit var bellPlayer: BellPlayer
    private val mainHandler = Handler(Looper.getMainLooper())
    private var completedPreset: TimerPreset? = null
    private var completedExtraSeconds: Int = 0
    private var isBellPlaying: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        timerNotification = TimerMediaNotification(this)
        bellPlayer = BellPlayer(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionShowActiveTimer -> showActiveTimer(intent)
            ActionShowCompletedTimer -> showCompletedTimer(intent)
            ActionCancelTimerService -> stopTimerService()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bellPlayer.release()
        timerNotification.release()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun showActiveTimer(intent: Intent) {
        val preset = intent.timerPreset()
        bellPlayer.release()
        completedPreset = null
        completedExtraSeconds = 0
        isBellPlaying = false
        startTimerForeground(
            timerNotification.activeNotification(
                preset = preset,
                totalSeconds = intent.getIntExtra(ExtraTotalSeconds, preset.durationMinutes * 60),
                remainingSeconds = intent.getIntExtra(ExtraRemainingSeconds, preset.durationMinutes * 60),
                isRunning = intent.getBooleanExtra(ExtraIsRunning, false),
            ),
        )
    }

    private fun showCompletedTimer(intent: Intent) {
        completedPreset = intent.timerPreset()
        completedExtraSeconds = intent.getIntExtra(ExtraExtraSeconds, completedExtraSeconds)
        val playBell = intent.getBooleanExtra(ExtraPlayBell, false)
        if (playBell) {
            isBellPlaying = false
        }
        showCompletedForeground()

        if (playBell) {
            bellPlayer.play(
                bellSoundId = completedPreset?.bellSoundId,
                onStarted = {
                    mainHandler.post {
                        isBellPlaying = true
                        showCompletedForeground()
                    }
                },
                onCompletion = {
                    mainHandler.post {
                        isBellPlaying = false
                        showCompletedForeground()
                    }
                },
            )
        }
    }

    private fun showCompletedForeground() {
        val preset = completedPreset ?: return
        startTimerForeground(
            timerNotification.completedNotification(
                preset = preset,
                extraSeconds = completedExtraSeconds,
                isBellPlaying = isBellPlaying,
            ),
        )
    }

    private fun stopTimerService() {
        bellPlayer.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startTimerForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(TimerNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(TimerNotificationId, notification)
        }
    }

    private fun Intent.timerPreset(): TimerPreset {
        val totalSeconds = getIntExtra(ExtraTotalSeconds, 60).coerceAtLeast(1)
        return TimerPreset(
            name = getStringExtra(ExtraPresetName).orEmpty().ifBlank { "Timer" },
            durationMinutes = (totalSeconds / 60).coerceAtLeast(1),
            intervalMinutes = null,
            bellSoundId = getStringExtra(ExtraBellSoundId) ?: DefaultBellSoundId,
        )
    }

    companion object {
        private var instance: TimerForegroundService? = null

        fun showActive(
            context: Context,
            preset: TimerPreset,
            totalSeconds: Int,
            remainingSeconds: Int,
            isRunning: Boolean,
        ) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ActionShowActiveTimer
                putTimerPreset(preset, totalSeconds)
                putExtra(ExtraRemainingSeconds, remainingSeconds)
                putExtra(ExtraIsRunning, isRunning)
            }
            instance?.showActiveTimer(intent) ?: startForegroundCommand(context, intent)
        }

        fun showCompleted(
            context: Context,
            preset: TimerPreset,
            extraSeconds: Int,
            playBell: Boolean,
        ) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ActionShowCompletedTimer
                putTimerPreset(preset, preset.durationMinutes * 60)
                putExtra(ExtraExtraSeconds, extraSeconds)
                putExtra(ExtraPlayBell, playBell)
            }
            instance?.showCompletedTimer(intent) ?: startForegroundCommand(context, intent)
        }

        fun cancel(context: Context) {
            instance?.let { service ->
                service.stopTimerService()
                return
            }

            try {
                context.startService(
                    Intent(context, TimerForegroundService::class.java).apply {
                        action = ActionCancelTimerService
                    },
                )
            } catch (_: RuntimeException) {
                // The service may already be stopped; nothing else is required.
            }
        }

        private fun Intent.putTimerPreset(preset: TimerPreset, totalSeconds: Int) {
            putExtra(ExtraPresetName, preset.name)
            putExtra(ExtraBellSoundId, preset.bellSoundId)
            putExtra(ExtraTotalSeconds, totalSeconds)
        }

        private fun startForegroundCommand(context: Context, intent: Intent) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: RuntimeException) {
                try {
                    context.startService(intent)
                } catch (_: RuntimeException) {
                    // If Android rejects the command, keep the app state consistent and avoid crashing.
                }
            }
        }
    }
}
