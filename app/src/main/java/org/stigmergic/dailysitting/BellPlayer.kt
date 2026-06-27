// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class BellPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(audioAttributes)
        .build()

    fun play(
        bellSoundId: String?,
        onStarted: () -> Unit = {},
        onCompletion: () -> Unit = {},
    ) {
        try {
            release()
            audioManager?.requestAudioFocus(focusRequest)
            player = MediaPlayer.create(context, bellSoundForId(bellSoundId).rawResId, audioAttributes, 0)?.apply {
                setOnCompletionListener { completedPlayer ->
                    if (player === completedPlayer) {
                        player = null
                    }
                    completedPlayer.release()
                    audioManager?.abandonAudioFocusRequest(focusRequest)
                    onCompletion()
                }
                setOnErrorListener { erroredPlayer, _, _ ->
                    if (player === erroredPlayer) {
                        player = null
                    }
                    erroredPlayer.release()
                    audioManager?.abandonAudioFocusRequest(focusRequest)
                    onCompletion()
                    true
                }
                start()
                onStarted()
            }
            if (player == null) {
                audioManager?.abandonAudioFocusRequest(focusRequest)
                onCompletion()
            }
        } catch (_: RuntimeException) {
            audioManager?.abandonAudioFocusRequest(focusRequest)
            onCompletion()
            // Audio allocation can fail on some devices. The timer should still complete.
        }
    }

    fun release() {
        try {
            player?.release()
            player = null
            audioManager?.abandonAudioFocusRequest(focusRequest)
        } catch (_: RuntimeException) {
            player = null
            audioManager?.abandonAudioFocusRequest(focusRequest)
        }
    }
}
