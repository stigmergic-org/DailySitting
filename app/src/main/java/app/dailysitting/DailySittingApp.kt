package app.dailysitting

import android.annotation.SuppressLint
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LightDailySittingColorScheme = lightColorScheme(
    primary = Color(0xFF4F6F52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E8CE),
    onPrimaryContainer = Color(0xFF0C1F10),
    inversePrimary = Color(0xFFB7CCB2),
    secondary = Color(0xFF625F41),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E3BD),
    onSecondaryContainer = Color(0xFF1E1C08),
    tertiary = Color(0xFF456A64),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8ECE3),
    onTertiaryContainer = Color(0xFF00201C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFE2E4D7),
    onSurfaceVariant = Color(0xFF45483F),
    surfaceTint = Color(0xFF4F6F52),
    inverseSurface = Color(0xFF34302A),
    inverseOnSurface = Color(0xFFF7EFE6),
    outline = Color(0xFF76786D),
    outlineVariant = Color(0xFFC5C8BA),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceDim = Color(0xFFE3DED6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F2E9),
    surfaceContainer = Color(0xFFF3ECE3),
    surfaceContainerHigh = Color(0xFFEDE7DE),
    surfaceContainerHighest = Color(0xFFE8E1D8),
    primaryFixed = Color(0xFFD2E8CE),
    primaryFixedDim = Color(0xFFB7CCB2),
    onPrimaryFixed = Color(0xFF0C1F10),
    onPrimaryFixedVariant = Color(0xFF38513B),
    secondaryFixed = Color(0xFFE8E3BD),
    secondaryFixedDim = Color(0xFFCCC6A1),
    onSecondaryFixed = Color(0xFF1E1C08),
    onSecondaryFixedVariant = Color(0xFF4A482B),
    tertiaryFixed = Color(0xFFC8ECE3),
    tertiaryFixedDim = Color(0xFFACCFC7),
    onTertiaryFixed = Color(0xFF00201C),
    onTertiaryFixedVariant = Color(0xFF2E514B),
)

private val DarkDailySittingColorScheme = darkColorScheme(
    primary = Color(0xFFB7CCB2),
    onPrimary = Color(0xFF213526),
    primaryContainer = Color(0xFF38513B),
    onPrimaryContainer = Color(0xFFD2E8CE),
    inversePrimary = Color(0xFF4F6F52),
    secondary = Color(0xFFCCC6A1),
    onSecondary = Color(0xFF333117),
    secondaryContainer = Color(0xFF4A482B),
    onSecondaryContainer = Color(0xFFE8E3BD),
    tertiary = Color(0xFFACCFC7),
    onTertiary = Color(0xFF173731),
    tertiaryContainer = Color(0xFF2E514B),
    onTertiaryContainer = Color(0xFFC8ECE3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF14130E),
    onBackground = Color(0xFFE8E2D9),
    surface = Color(0xFF14130E),
    onSurface = Color(0xFFE8E2D9),
    surfaceVariant = Color(0xFF45483F),
    onSurfaceVariant = Color(0xFFC5C8BA),
    surfaceTint = Color(0xFFB7CCB2),
    inverseSurface = Color(0xFFE8E2D9),
    inverseOnSurface = Color(0xFF34302A),
    outline = Color(0xFF8F9286),
    outlineVariant = Color(0xFF45483F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3831),
    surfaceDim = Color(0xFF14130E),
    surfaceContainerLowest = Color(0xFF0F0E0A),
    surfaceContainerLow = Color(0xFF1F1D17),
    surfaceContainer = Color(0xFF23211B),
    surfaceContainerHigh = Color(0xFF2D2B25),
    surfaceContainerHighest = Color(0xFF38362F),
    primaryFixed = Color(0xFFD2E8CE),
    primaryFixedDim = Color(0xFFB7CCB2),
    onPrimaryFixed = Color(0xFF0C1F10),
    onPrimaryFixedVariant = Color(0xFF38513B),
    secondaryFixed = Color(0xFFE8E3BD),
    secondaryFixedDim = Color(0xFFCCC6A1),
    onSecondaryFixed = Color(0xFF1E1C08),
    onSecondaryFixedVariant = Color(0xFF4A482B),
    tertiaryFixed = Color(0xFFC8ECE3),
    tertiaryFixedDim = Color(0xFFACCFC7),
    onTertiaryFixed = Color(0xFF00201C),
    onTertiaryFixedVariant = Color(0xFF2E514B),
)

private val AppBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val AppPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val AppText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val AppMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val AppCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
private val AppProgressTrack: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun DailySittingApp(
    viewModel: DailySittingViewModel,
    onRequestHealthPermissions: () -> Unit,
    onImportLogs: () -> Unit,
) {
    DailySittingTheme {
        BackHandler(
            enabled = viewModel.uiState.screen == AppScreen.Editor ||
                viewModel.uiState.screen == AppScreen.ManualSession ||
                viewModel.uiState.screen == AppScreen.MeditationLog,
            onBack = viewModel::showTimerList,
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground,
        ) {
            when (viewModel.uiState.screen) {
                AppScreen.TimerList -> TimerListScreen(
                    state = viewModel.uiState,
                    onStart = viewModel::startTimer,
                    onEdit = viewModel::showEditor,
                    onAdd = { viewModel.showEditor(null) },
                    onAddSession = viewModel::showManualSession,
                    onShowLog = viewModel::showMeditationLog,
                    onRequestHealthPermissions = onRequestHealthPermissions,
                )

                AppScreen.Editor -> TimerEditorScreen(
                    preset = viewModel.uiState.editingPreset,
                    onSave = viewModel::savePreset,
                    onDelete = viewModel::deletePreset,
                    onCancel = viewModel::showTimerList,
                    onPreviewBellSound = viewModel::previewBellSound,
                )

                AppScreen.ManualSession -> ManualSessionScreen(
                    onSave = viewModel::addManualSession,
                    onCancel = viewModel::showTimerList,
                )

                AppScreen.MeditationLog -> MeditationLogScreen(
                    state = viewModel.uiState,
                    onBack = viewModel::showTimerList,
                    onDeleteSession = viewModel::deleteSession,
                    onImportLogs = onImportLogs,
                    onRequestHealthPermissions = onRequestHealthPermissions,
                )

                AppScreen.Timer -> ActiveTimerScreen(
                    state = viewModel.uiState,
                    onPause = viewModel::pauseTimer,
                    onResume = viewModel::resumeTimer,
                    onCancel = viewModel::cancelTimer,
                )

                AppScreen.Complete -> CompletionScreen(
                    state = viewModel.uiState,
                    onBack = viewModel::showTimerList,
                )
            }
        }
    }
}

@Composable
private fun DailySittingTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkDailySittingColorScheme else LightDailySittingColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailySittingTopBar(
    title: String,
    navigationText: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            if (navigationText != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = if (navigationText == "Back") Icons.Default.ArrowBack else Icons.Default.Close,
                        contentDescription = navigationText,
                    )
                }
            }
        },
        actions = { actions() },
    )
}

@Composable
private fun AddFabMenu(
    onAddTimer: () -> Unit,
    onAddSession: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (expanded) {
            FabMenuAction(
                label = "New timer",
                icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onAddTimer()
                },
            )
            FabMenuAction(
                label = "Log session",
                icon = { Icon(Icons.Default.Event, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onAddSession()
                },
            )
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close add menu" else "Add",
            )
        }
    }
}

@Composable
private fun FabMenuAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = icon,
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun TimerListScreen(
    state: DailySittingUiState,
    onStart: (TimerPreset) -> Unit,
    onEdit: (TimerPreset) -> Unit,
    onAdd: () -> Unit,
    onAddSession: () -> Unit,
    onShowLog: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DailySittingTopBar(
                    title = "Daily Sitting",
                    actions = {
                        IconButton(onClick = onShowLog) {
                            Icon(Icons.Default.History, contentDescription = "View meditation log")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    StatsRow(state = state)
                }

                item {
                    Text(
                        text = "Pick a timer and sit.",
                        color = AppMuted,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (shouldShowHealthConnectCard(state.healthConnect)) {
                    item {
                        HealthConnectCard(
                            healthConnect = state.healthConnect,
                            onRequestHealthPermissions = onRequestHealthPermissions,
                        )
                    }
                }

                items(state.presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onStart = { onStart(preset) },
                        onEdit = { onEdit(preset) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { fabExpanded = false },
                    ),
            ) {}
        }

        AddFabMenu(
            onAddTimer = onAdd,
            onAddSession = onAddSession,
            expanded = fabExpanded,
            onExpandedChange = { fabExpanded = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
        )
    }
}

private fun shouldShowHealthConnectCard(healthConnect: HealthConnectUi): Boolean =
    healthConnect.status != HealthConnectStatus.Ready &&
        healthConnect.status != HealthConnectStatus.Synced

@Composable
private fun StatsRow(state: DailySittingUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            label = "Today",
            value = "${state.todayMinutes}m",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Streak",
            value = "${state.streakDays}d",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Total",
            value = "${state.totalMinutes}m",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                color = AppMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = AppText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    healthConnect: HealthConnectUi,
    onRequestHealthPermissions: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Health Connect",
                color = AppText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = healthConnect.message,
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (healthConnect.status == HealthConnectStatus.NeedsPermission) {
                Button(
                    onClick = onRequestHealthPermissions,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Connect")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeditationLogScreen(
    state: DailySittingUiState,
    onBack: () -> Unit,
    onDeleteSession: (SittingSession) -> Unit,
    onImportLogs: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val use24HourTime = DateFormat.is24HourFormat(context)
    val sessions = state.sessions.sortedByDescending { it.endedAtMillis }
    var sessionPendingDelete by remember { mutableStateOf<SittingSession?>(null) }
    var showImportExplanation by remember { mutableStateOf(false) }

    if (showImportExplanation) {
        AlertDialog(
            onDismissRequest = { showImportExplanation = false },
            title = { Text("Import Insight Timer logs") },
            text = {
                Text(
                    "Choose a CSV export from Insight Timer. Daily Sitting will import meditation sessions into Health Connect and skip entries already in your log.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportExplanation = false
                        onImportLogs()
                    },
                ) {
                    Text("Choose CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportExplanation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete log entry?") },
            text = {
                Text(
                    "This will remove ${session.durationMinutes} minutes from your Health Connect meditation log.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionPendingDelete = null
                        onDeleteSession(session)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            DailySittingTopBar(
                title = "Meditation Log",
                navigationText = "Back",
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = { showImportExplanation = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import logs")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sessions.isNotEmpty() && shouldShowMeditationLogStatus(state.healthConnect)) {
                item {
                    MeditationLogStatusCard(
                        healthConnect = state.healthConnect,
                        onRequestHealthPermissions = onRequestHealthPermissions,
                    )
                }
            }

            if (sessions.isEmpty()) {
                item {
                    MeditationLogEmptyCard(
                        healthConnect = state.healthConnect,
                        onRequestHealthPermissions = onRequestHealthPermissions,
                    )
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    MeditationLogSessionCard(
                        session = session,
                        use24HourTime = use24HourTime,
                        onDelete = { sessionPendingDelete = session },
                    )
                }
            }
        }
    }
}

private fun shouldShowMeditationLogStatus(healthConnect: HealthConnectUi): Boolean =
    healthConnect.status != HealthConnectStatus.Ready

@Composable
private fun MeditationLogStatusCard(
    healthConnect: HealthConnectUi,
    onRequestHealthPermissions: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = healthConnect.message,
                modifier = Modifier.weight(1f),
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (healthConnect.status == HealthConnectStatus.NeedsPermission) {
                TextButton(onClick = onRequestHealthPermissions) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun MeditationLogEmptyCard(
    healthConnect: HealthConnectUi,
    onRequestHealthPermissions: () -> Unit,
) {
    val message = when (healthConnect.status) {
        HealthConnectStatus.Checking -> "Loading meditation sessions."
        HealthConnectStatus.NeedsPermission -> "Connect Health Connect to view your meditation log."
        HealthConnectStatus.Unavailable -> healthConnect.message
        HealthConnectStatus.Error -> healthConnect.message
        HealthConnectStatus.Ready,
        HealthConnectStatus.Synced -> "No meditation sessions found."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Meditation log",
                color = AppText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (healthConnect.status == HealthConnectStatus.NeedsPermission) {
                Button(
                    onClick = onRequestHealthPermissions,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun MeditationLogSessionCard(
    session: SittingSession,
    use24HourTime: Boolean,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.presetName,
                    color = AppText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatSessionDateTime(session, use24HourTime),
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "${session.durationMinutes}m",
                color = AppText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete log entry",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: TimerPreset,
    onStart: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    color = AppText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = presetDescription(preset),
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledTonalIconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit timer")
            }
        }
    }
}

@Composable
private fun MinuteInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    val boundedValue = value.coerceIn(range)
    var text by remember { mutableStateOf(boundedValue.toString()) }

    LaunchedEffect(boundedValue) {
        if (text.toIntOrNull() != boundedValue) {
            text = boundedValue.toString()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(4)
            text = digits
            digits.toIntOrNull()?.let { minutes ->
                onValueChange(minutes.coerceIn(range))
            }
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        suffix = { Text("min") },
        supportingText = { Text("${range.first}-${range.last} minutes") },
    )
}

@Composable
private fun TimerEditorScreen(
    preset: TimerPreset?,
    onSave: (String, Int, Int?, String) -> Unit,
    onDelete: (TimerPreset) -> Unit,
    onCancel: () -> Unit,
    onPreviewBellSound: (String) -> Unit,
) {
    var name by remember(preset?.id) { mutableStateOf(preset?.name.orEmpty()) }
    var durationMinutes by remember(preset?.id) { mutableStateOf(preset?.durationMinutes ?: 10) }
    var hasInterval by remember(preset?.id) { mutableStateOf(preset?.intervalMinutes != null) }
    var intervalMinutes by remember(preset?.id) { mutableStateOf(preset?.intervalMinutes ?: 5) }
    var selectedBellSoundId by remember(preset?.id) {
        mutableStateOf(cleanBellSoundId(preset?.bellSoundId))
    }
    var presetPendingDelete by remember(preset?.id) { mutableStateOf<TimerPreset?>(null) }
    val maxIntervalMinutes = (durationMinutes - 1).coerceAtLeast(1)
    val cleanIntervalMinutes = intervalMinutes.coerceIn(1, maxIntervalMinutes)
    val intervalEnabled = durationMinutes > 1

    presetPendingDelete?.let { timerPreset ->
        AlertDialog(
            onDismissRequest = { presetPendingDelete = null },
            title = { Text("Delete timer?") },
            text = { Text("This will remove ${timerPreset.name} from your timer list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        presetPendingDelete = null
                        onDelete(timerPreset)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            DailySittingTopBar(
                title = if (preset == null) "Add Timer" else "Edit Timer",
                navigationText = "Cancel",
                onNavigationClick = onCancel,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            MinuteInput(
                label = "Duration",
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                range = 1..(24 * 60),
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Interval bell", color = AppText, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (hasInterval && intervalEnabled) {
                                    "Bell during the session"
                                } else {
                                    "No interval bell"
                                },
                                color = AppMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = hasInterval && intervalEnabled,
                            onCheckedChange = { checked ->
                                hasInterval = checked
                                if (checked) {
                                    intervalMinutes = cleanIntervalMinutes
                                }
                            },
                            enabled = intervalEnabled,
                        )
                    }
                    if (hasInterval && intervalEnabled) {
                        MinuteInput(
                            label = "Every",
                            value = cleanIntervalMinutes,
                            onValueChange = { intervalMinutes = it },
                            range = 1..maxIntervalMinutes,
                        )
                    }
                }
            }

            BellSoundSelector(
                selectedBellSoundId = selectedBellSoundId,
                onBellSoundSelected = { bellSoundId ->
                    selectedBellSoundId = bellSoundId
                    onPreviewBellSound(bellSoundId)
                },
            )

            Button(
                onClick = {
                    onSave(
                        name,
                        durationMinutes,
                        if (hasInterval && intervalEnabled) cleanIntervalMinutes else null,
                        selectedBellSoundId,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
            if (preset != null) {
                FilledTonalButton(
                    onClick = { presetPendingDelete = preset },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("Delete Timer")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BellSoundSelector(
    selectedBellSoundId: String,
    onBellSoundSelected: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Ending bell", color = AppText, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Tap a sound to preview it",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(BellSoundOptions, key = { it.id }) { option ->
                    BellSoundCard(
                        option = option,
                        selected = option.id == selectedBellSoundId,
                        onClick = { onBellSoundSelected(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BellSoundCard(
    option: BellSoundOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .width(120.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BellSoundIconGraphic(
                icon = option.icon,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = option.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else AppMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BellSoundIconGraphic(
    icon: BellSoundIcon,
    color: Color,
    modifier: Modifier = Modifier.size(28.dp),
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.6f)
        when (icon) {
            BellSoundIcon.Bowl -> {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
                    size = Size(size.width * 0.76f, size.height * 0.56f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.22f, size.height * 0.55f),
                    end = Offset(size.width * 0.78f, size.height * 0.55f),
                    strokeWidth = stroke.width,
                )
                drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.5f, size.height * 0.22f))
            }

            BellSoundIcon.Ring -> {
                drawCircle(color, radius = size.minDimension * 0.3f, style = stroke)
                drawCircle(color, radius = size.minDimension * 0.17f, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.1f),
                    end = Offset(size.width * 0.5f, size.height * 0.28f),
                    strokeWidth = stroke.width,
                )
            }

            BellSoundIcon.Bell -> {
                val path = Path().apply {
                    moveTo(size.width * 0.26f, size.height * 0.64f)
                    quadraticBezierTo(size.width * 0.28f, size.height * 0.32f, size.width * 0.5f, size.height * 0.24f)
                    quadraticBezierTo(size.width * 0.72f, size.height * 0.32f, size.width * 0.74f, size.height * 0.64f)
                    lineTo(size.width * 0.82f, size.height * 0.76f)
                    lineTo(size.width * 0.18f, size.height * 0.76f)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawCircle(color, radius = size.minDimension * 0.06f, center = Offset(size.width * 0.5f, size.height * 0.84f))
            }

            BellSoundIcon.Temple -> {
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.28f), Offset(size.width * 0.82f, size.height * 0.28f), stroke.width)
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.2f, size.height * 0.7f), stroke.width)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.8f, size.height * 0.7f), stroke.width)
                drawLine(color, Offset(size.width * 0.2f, size.height * 0.7f), Offset(size.width * 0.8f, size.height * 0.7f), stroke.width)
                drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.5f, size.height * 0.48f))
            }

            BellSoundIcon.Harmony -> {
                drawCircle(color, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.36f, size.height * 0.45f), style = stroke)
                drawCircle(color, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.62f, size.height * 0.55f), style = stroke)
                drawLine(color, Offset(size.width * 0.16f, size.height * 0.78f), Offset(size.width * 0.86f, size.height * 0.78f), stroke.width)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualSessionScreen(
    onSave: (LocalDate, LocalTime, Int) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var sessionDate by remember { mutableStateOf(LocalDate.now()) }
    var sessionTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var durationMinutes by remember { mutableStateOf(10) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val use24HourTime = DateFormat.is24HourFormat(context)
    val isFutureSession = LocalDateTime.of(sessionDate, sessionTime).isAfter(LocalDateTime.now())
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = sessionDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    val timePickerState = rememberTimePickerState(
        initialHour = sessionTime.hour,
        initialMinute = sessionTime.minute,
        is24Hour = use24HourTime,
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            sessionDate = Instant
                                .ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    Scaffold(
        topBar = {
            DailySittingTopBar(
                title = "Add Session",
                navigationText = "Cancel",
                onNavigationClick = onCancel,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Use the time the session ended.",
                color = AppMuted,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatDate(sessionDate))
                }
                FilledTonalButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatTime(sessionTime, use24HourTime))
                }
            }

            MinuteInput(
                label = "Length",
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                range = 1..(24 * 60),
            )

            if (isFutureSession) {
                Text(
                    text = "Session time cannot be in the future.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { onSave(sessionDate, sessionTime, durationMinutes) },
                enabled = !isFutureSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Session")
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ExpressiveTimerProgress(
    progress: Float,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val indicatorSize = 284.dp
    val trackSize = 264.dp
    val strokeWidth = 26.dp
    val progressStroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Box(
        modifier = modifier.size(indicatorSize),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { boundedProgress },
            modifier = Modifier
                .requiredSize(trackSize)
                .clearAndSetSemantics { },
            color = Color.Transparent,
            strokeWidth = strokeWidth,
            trackColor = AppProgressTrack,
            strokeCap = StrokeCap.Round,
        )
        CircularWavyProgressIndicator(
            progress = { boundedProgress },
            modifier = Modifier.requiredSize(indicatorSize),
            color = AppPrimary,
            trackColor = Color.Transparent,
            stroke = progressStroke,
            trackStroke = progressStroke,
            amplitude = { 1f },
            wavelength = 134.dp,
            waveSpeed = 12.dp,
        )
        Text(
            text = formatSeconds(remainingSeconds),
            color = AppText,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActiveTimerScreen(
    state: DailySittingUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val preset = state.selectedPreset
    val progress = if (state.totalSeconds <= 0) {
        0f
    } else {
        1f - (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())
    }

    Scaffold(
        topBar = { DailySittingTopBar(preset?.name ?: "Sitting") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = preset?.let(::presetDescription).orEmpty(),
                color = AppMuted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            ExpressiveTimerProgress(
                progress = progress,
                remainingSeconds = state.remainingSeconds,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = if (state.isTimerRunning) onPause else onResume,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isTimerRunning) "Pause" else "Resume")
                }
                FilledTonalButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    state: DailySittingUiState,
    onBack: () -> Unit,
) {
    val completedSession = state.completedSession
    val completedPreset = state.presets.firstOrNull { it.id == completedSession?.presetId }
    val completedBell = bellSoundForId(completedPreset?.bellSoundId)

    Scaffold(
        topBar = { DailySittingTopBar("Session Complete") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(1.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BellSoundIconGraphic(
                            icon = completedBell.icon,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = completionRecordText(
                        durationMinutes = completedSession?.durationMinutes ?: 0,
                        healthConnect = state.healthConnect,
                    ),
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Current streak", color = AppMuted, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${state.streakDays} day${if (state.streakDays == 1) "" else "s"}",
                            color = AppText,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Timers")
            }
        }
    }
}

private fun presetDescription(preset: TimerPreset): String {
    val interval = preset.intervalMinutes
    return if (interval == null) {
        "${preset.durationMinutes} min, ending bell"
    } else {
        "${preset.durationMinutes} min, bell every $interval min"
    }
}

private fun formatSessionDateTime(session: SittingSession, use24HourTime: Boolean): String {
    val endedAt = Instant
        .ofEpochMilli(session.endedAtMillis)
        .atZone(ZoneId.systemDefault())
    val date = endedAt.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val time = endedAt.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()))
    return "$date, $time"
}

private fun completionRecordText(
    durationMinutes: Int,
    healthConnect: HealthConnectUi,
): String = when (healthConnect.status) {
    HealthConnectStatus.Synced -> "$durationMinutes minutes recorded"
    HealthConnectStatus.Checking,
    HealthConnectStatus.Ready -> "Recording to Health Connect"
    HealthConnectStatus.NeedsPermission,
    HealthConnectStatus.Unavailable,
    HealthConnectStatus.Error -> "Not recorded"
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))

private fun formatTime(time: LocalTime, use24HourTime: Boolean): String {
    val pattern = if (use24HourTime) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
