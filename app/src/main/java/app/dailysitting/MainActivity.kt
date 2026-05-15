package app.dailysitting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.health.connect.client.PermissionController

class MainActivity : ComponentActivity() {
    private val viewModel: DailySittingViewModel by viewModels()

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        viewModel.onHealthPermissionResult(
            grantedPermissions.containsAll(HealthConnectPermissions),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailySittingApp(
                viewModel = viewModel,
                onRequestHealthPermissions = {
                    requestHealthPermissions.launch(HealthConnectPermissions)
                },
            )
        }
    }
}
