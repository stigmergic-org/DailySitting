// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController

private const val VolumeChangedAction = "android.media.VOLUME_CHANGED_ACTION"
private const val VolumeStreamTypeExtra = "android.media.EXTRA_VOLUME_STREAM_TYPE"

class MainActivity : ComponentActivity() {
    private val viewModel: DailySittingViewModel by viewModels()
    private var isVolumeReceiverRegistered = false

    private val volumeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != VolumeChangedAction) return

            val streamType = intent.getIntExtra(VolumeStreamTypeExtra, AudioManager.STREAM_MUSIC)
            if (streamType == AudioManager.STREAM_MUSIC) {
                viewModel.refreshBellVolumeWarning()
            }
        }
    }

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        viewModel.onHealthPermissionResult(
            grantedPermissions.containsAll(HealthConnectPermissions),
        )
    }

    private val importLogs = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importInsightTimerLogs(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setVolumeControlStream(AudioManager.STREAM_MUSIC)
        setContent {
            DailySittingApp(
                viewModel = viewModel,
                onRequestHealthPermissions = {
                    requestHealthPermissions.launch(HealthConnectPermissions)
                },
                onImportLogs = {
                    importLogs.launch(
                        arrayOf(
                            "text/*",
                            "text/csv",
                            "application/csv",
                            "application/vnd.ms-excel",
                        ),
                    )
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        registerVolumeReceiver()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBellVolumeWarning()
    }

    override fun onStop() {
        unregisterVolumeReceiver()
        super.onStop()
    }

    private fun registerVolumeReceiver() {
        if (isVolumeReceiverRegistered) return

        val filter = IntentFilter(VolumeChangedAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeChangedReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(volumeChangedReceiver, filter)
        }
        isVolumeReceiverRegistered = true
    }

    private fun unregisterVolumeReceiver() {
        if (!isVolumeReceiverRegistered) return

        unregisterReceiver(volumeChangedReceiver)
        isVolumeReceiverRegistered = false
    }
}
