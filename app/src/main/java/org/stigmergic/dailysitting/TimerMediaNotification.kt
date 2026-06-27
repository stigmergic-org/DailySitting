// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Locale
import kotlin.random.Random

private const val TimerNotificationChannelId = "timer"
private const val TimerNotificationId = 1001
private const val PauseTimerAction = "org.stigmergic.dailysitting.action.PAUSE_TIMER"
private const val ResumeTimerAction = "org.stigmergic.dailysitting.action.RESUME_TIMER"
private const val BackToTimersAction = "org.stigmergic.dailysitting.action.BACK_TO_TIMERS"
private const val AddCompletedTimeAction = "org.stigmergic.dailysitting.action.ADD_COMPLETED_TIME"
private const val PauseTimerRequestCode = 1
private const val ResumeTimerRequestCode = 2
private const val ContentRequestCode = 3
private const val BackToTimersRequestCode = 4
private const val AddCompletedTimeRequestCode = 5
private const val TimerArtworkSizePx = 512

interface TimerMediaNotificationControls {
    fun onPauseTimer()
    fun onResumeTimer()
    fun onBackToTimers()
    fun onAddCompletedTime()
}

class TimerMediaNotification(
    context: Context,
    private val controls: TimerMediaNotificationControls,
) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val artworkBitmap: Bitmap by lazy { createTimerArtworkBitmap() }
    private val mediaSession = MediaSession(appContext, "Daily Sitting Timer").apply {
        setCallback(
            object : MediaSession.Callback() {
                override fun onPlay() {
                    controls.onResumeTimer()
                }

                override fun onPause() {
                    controls.onPauseTimer()
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        BackToTimersAction -> controls.onBackToTimers()
                        AddCompletedTimeAction -> controls.onAddCompletedTime()
                    }
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    init {
        TimerMediaNotificationCommands.register(controls)
    }

    fun show(
        preset: TimerPreset,
        totalSeconds: Int,
        remainingSeconds: Int,
        isRunning: Boolean,
    ) {
        val boundedTotalSeconds = totalSeconds.coerceAtLeast(1)
        val boundedRemainingSeconds = remainingSeconds.coerceIn(0, boundedTotalSeconds)
        val elapsedSeconds = boundedTotalSeconds - boundedRemainingSeconds

        ensureNotificationChannel()
        updateMediaSession(
            title = preset.name,
            totalSeconds = boundedTotalSeconds,
            elapsedSeconds = elapsedSeconds,
            isRunning = isRunning,
        )

        try {
            notificationManager.notify(
                TimerNotificationId,
                buildNotification(
                    title = preset.name,
                    remainingSeconds = boundedRemainingSeconds,
                    isRunning = isRunning,
                ),
            )
        } catch (_: SecurityException) {
            // Notification permission can be denied; the timer should continue normally.
        }
    }

    fun cancel() {
        notificationManager.cancel(TimerNotificationId)
        stopMediaSession()
    }

    fun showCompleted(
        preset: TimerPreset,
        extraSeconds: Int,
    ) {
        try {
            ensureNotificationChannel()
            updateCompletedMediaSession(
                title = preset.name,
                extraSeconds = extraSeconds,
            )
            notificationManager.notify(
                TimerNotificationId,
                buildCompletedNotification(
                    title = preset.name,
                    extraSeconds = extraSeconds,
                ),
            )
        } catch (_: SecurityException) {
            // Notification permission can be denied; the timer completion still counts.
        } catch (_: RuntimeException) {
            // Notification rendering can vary by device; completion and bell playback should continue.
        }
    }

    fun release() {
        cancel()
        TimerMediaNotificationCommands.unregister(controls)
        mediaSession.release()
    }

    private fun buildNotification(
        title: String,
        remainingSeconds: Int,
        isRunning: Boolean,
    ): Notification {
        val action = if (isRunning) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                actionIntent(PauseTimerAction, PauseTimerRequestCode),
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                actionIntent(ResumeTimerAction, ResumeTimerRequestCode),
            ).build()
        }

        return Notification.Builder(appContext, TimerNotificationChannelId)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setLargeIcon(Icon.createWithBitmap(artworkBitmap))
            .setContentTitle(title)
            .setContentText("${formatTimerSeconds(remainingSeconds)} remaining")
            .setSubText(appContext.getString(R.string.app_name))
            .setContentIntent(contentIntent())
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .addAction(action)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0),
            )
            .build()
    }

    private fun buildCompletedNotification(
        title: String,
        extraSeconds: Int,
    ): Notification {
        val backToTimersAction = Notification.Action.Builder(
            R.drawable.ic_notification_back,
            "Back to Timers",
            actionIntent(BackToTimersAction, BackToTimersRequestCode),
        ).build()
        val addTimeTitle = if (extraSeconds > 0) {
            "Add +${formatTimerSeconds(extraSeconds)}"
        } else {
            "Add time"
        }
        val addTimeAction = Notification.Action.Builder(
            R.drawable.ic_notification_add,
            addTimeTitle,
            actionIntent(AddCompletedTimeAction, AddCompletedTimeRequestCode),
        ).build()

        return Notification.Builder(appContext, TimerNotificationChannelId)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setLargeIcon(Icon.createWithBitmap(artworkBitmap))
            .setContentTitle(title)
            .setContentText("Session complete")
            .setSubText(appContext.getString(R.string.app_name))
            .setContentIntent(contentIntent())
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .addAction(backToTimersAction)
            .addAction(addTimeAction)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun updateMediaSession(
        title: String,
        totalSeconds: Int,
        elapsedSeconds: Int,
        isRunning: Boolean,
    ) {
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, appContext.getString(R.string.app_name))
                .putBitmap(MediaMetadata.METADATA_KEY_ART, artworkBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artworkBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artworkBitmap)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, totalSeconds * 1_000L)
                .build(),
        )

        val playbackState = if (isRunning) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackSpeed = if (isRunning) 1f else 0f
        val playbackAction = if (isRunning) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
        val playbackActions = PlaybackState.ACTION_PLAY_PAUSE or playbackAction
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(playbackActions)
                .setState(playbackState, elapsedSeconds * 1_000L, playbackSpeed)
                .build(),
        )
        mediaSession.isActive = true
    }

    private fun updateCompletedMediaSession(
        title: String,
        extraSeconds: Int,
    ) {
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Session complete")
                .putBitmap(MediaMetadata.METADATA_KEY_ART, artworkBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artworkBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artworkBitmap)
                .build(),
        )
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .addCustomAction(
                    PlaybackState.CustomAction.Builder(
                        BackToTimersAction,
                        "Back to Timers",
                        R.drawable.ic_notification_back,
                    ).build(),
                )
                .addCustomAction(
                    PlaybackState.CustomAction.Builder(
                        AddCompletedTimeAction,
                        if (extraSeconds > 0) "Add +${formatTimerSeconds(extraSeconds)}" else "Add time",
                        R.drawable.ic_notification_add,
                    ).build(),
                )
                .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
                .build(),
        )
        mediaSession.isActive = true
    }

    private fun stopMediaSession() {
        mediaSession.isActive = false
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, 0L, 0f)
                .build(),
        )
    }

    private fun ensureNotificationChannel() {
        if (notificationManager.getNotificationChannel(TimerNotificationChannelId) != null) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                TimerNotificationChannelId,
                "Timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active timer controls"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            },
        )
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            Intent(appContext, TimerMediaNotificationActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun contentIntent(): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            ContentRequestCode,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class TimerMediaNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerMediaNotificationCommands.dispatch(context, intent.action)
    }
}

private object TimerMediaNotificationCommands {
    @Volatile
    private var controls: TimerMediaNotificationControls? = null

    fun register(controls: TimerMediaNotificationControls) {
        this.controls = controls
    }

    fun unregister(controls: TimerMediaNotificationControls) {
        if (this.controls === controls) {
            this.controls = null
        }
    }

    fun dispatch(context: Context, action: String?) {
        val currentControls = controls
        if (currentControls == null) {
            context.getSystemService(NotificationManager::class.java).cancel(TimerNotificationId)
            return
        }

        when (action) {
            PauseTimerAction -> currentControls.onPauseTimer()
            ResumeTimerAction -> currentControls.onResumeTimer()
            BackToTimersAction -> currentControls.onBackToTimers()
            AddCompletedTimeAction -> currentControls.onAddCompletedTime()
        }
    }
}

private fun formatTimerSeconds(totalSeconds: Int): String {
    val boundedSeconds = totalSeconds.coerceAtLeast(0)
    val hours = boundedSeconds / 3_600
    val minutes = (boundedSeconds % 3_600) / 60
    val seconds = boundedSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun createTimerArtworkBitmap(): Bitmap {
    val size = TimerArtworkSizePx.toFloat()
    val bitmap = Bitmap.createBitmap(TimerArtworkSizePx, TimerArtworkSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawColor(Color.rgb(255, 248, 240))

    paint.shader = LinearGradient(
        0f,
        0f,
        size,
        size,
        intArrayOf(
            Color.rgb(255, 248, 240),
            Color.rgb(235, 232, 207),
            Color.rgb(218, 234, 217),
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, size, size, paint)

    paint.shader = RadialGradient(
        size * 0.36f,
        size * 0.30f,
        size * 0.72f,
        intArrayOf(
            Color.argb(140, 210, 232, 206),
            Color.argb(58, 232, 227, 189),
            Color.TRANSPARENT,
        ),
        floatArrayOf(0f, 0.54f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, size, size, paint)

    paint.shader = RadialGradient(
        size * 0.78f,
        size * 0.76f,
        size * 0.62f,
        intArrayOf(
            Color.argb(96, 200, 236, 227),
            Color.argb(42, 210, 232, 206),
            Color.TRANSPARENT,
        ),
        floatArrayOf(0f, 0.50f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, size, size, paint)

    paint.shader = null
    val random = Random(29)
    repeat(360) {
        paint.color = Color.argb(random.nextInt(8, 18), 42, 54, 38)
        canvas.drawCircle(
            random.nextFloat() * size,
            random.nextFloat() * size,
            random.nextFloat() * 0.75f + 0.25f,
            paint,
        )
    }

    return bitmap
}
