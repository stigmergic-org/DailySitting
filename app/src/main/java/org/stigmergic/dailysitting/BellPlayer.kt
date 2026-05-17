// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.content.Context
import android.media.MediaPlayer

class BellPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(bellSoundId: String?) {
        try {
            release()
            player = MediaPlayer.create(context, bellSoundForId(bellSoundId).rawResId)?.apply {
                setOnCompletionListener { completedPlayer ->
                    if (player === completedPlayer) {
                        player = null
                    }
                    completedPlayer.release()
                }
                start()
            }
        } catch (_: RuntimeException) {
            // Audio allocation can fail on some devices. The timer should still complete.
        }
    }

    fun release() {
        try {
            player?.release()
            player = null
        } catch (_: RuntimeException) {
            player = null
        }
    }
}
