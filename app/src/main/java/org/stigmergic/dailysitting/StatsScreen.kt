// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(
    state: DailySittingUiState,
    onBack: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
) {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    val currentMonth = YearMonth.from(today)
    var monthKey by rememberSaveable { mutableStateOf<String?>(null) }
    var yearView by rememberSaveable { mutableStateOf(false) }
    var returnToYear by rememberSaveable { mutableStateOf(false) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    var selectedDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    val detailScrollState = rememberLazyListState()
    val yearScrollState = rememberLazyListState()
    val navigateBack: () -> Unit = {
        if (returnToYear && !yearView) {
            yearView = true
            returnToYear = false
            selectedDayKey = null
        } else {
            onBack()
        }
    }
    BackHandler(enabled = returnToYear && !yearView, onBack = navigateBack)
    val month = monthKey?.let(YearMonth::parse)
    val range = if (yearView) {
        PracticeRange(LocalDate.of(year, 1, 1), minOf(LocalDate.of(year, 12, 31), today))
    } else {
        practiceRange(today, month)
    }
    val zone = ZoneId.systemDefault()
    val dailySeconds = remember(state.sessions, zone) { dailyPracticeSeconds(state.sessions, zone) }
    val summary = remember(dailySeconds, range) { practiceSummary(dailySeconds, range) }
    val colors = practiceColors()
    val earliestYear = minOf(today.year, dailySeconds.keys.minOfOrNull { it.year } ?: today.year)
    val earliestMonth = minOf(
        currentMonth.minusMonths(1),
        dailySeconds.keys.minOfOrNull { YearMonth.from(it) } ?: currentMonth,
    )
    val ready = state.healthConnect.status == HealthConnectStatus.Ready ||
        state.healthConnect.status == HealthConnectStatus.Synced

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your practice") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = if (yearView) yearScrollState else detailScrollState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!ready) {
                item {
                    Card {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(state.healthConnect.message)
                            Text("Practice history is read from Health Connect.")
                            if (state.healthConnect.status == HealthConnectStatus.NeedsPermission) {
                                Button(onClick = onRequestHealthPermissions) { Text("Connect") }
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !yearView,
                            onClick = {
                                if (yearView) returnToYear = false
                                yearView = false
                                selectedDayKey = null
                            },
                            label = { Text("Detail") },
                        )
                        FilterChip(
                            selected = yearView,
                            onClick = {
                                yearView = true
                                returnToYear = false
                                selectedDayKey = null
                            },
                            label = { Text("Year") },
                        )
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                enabled = if (yearView) year > earliestYear else month == null || month > earliestMonth,
                                onClick = {
                                    selectedDayKey = null
                                    if (yearView) year-- else {
                                        monthKey = if (month == currentMonth) {
                                            null
                                        } else {
                                            (month?.minusMonths(1) ?: currentMonth.minusMonths(1)).toString()
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = if (yearView) "Previous year" else "Previous month")
                            }
                            Text(
                                text = if (yearView) "$year" else month?.format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy", locale),
                                ) ?: "Last 30 days",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            IconButton(
                                enabled = if (yearView) year < today.year else month != currentMonth,
                                onClick = {
                                    selectedDayKey = null
                                    if (yearView) year++ else {
                                        monthKey = if (month == null) {
                                            currentMonth.toString()
                                        } else {
                                            month.plusMonths(1).takeIf { it < currentMonth }?.toString()
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = if (yearView) "Next year" else "Next period")
                            }
                        }
                        val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
                        Text(
                            "${range.start.format(dateFormat)} – ${range.end.format(dateFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (yearView || month != null) {
                            TextButton(onClick = {
                                monthKey = null
                                yearView = false
                                returnToYear = false
                                selectedDayKey = null
                            }) { Text("Back to last 30 days") }
                        }
                    }
                }
                if (yearView) {
                    items(4) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(3) { column ->
                                val miniMonth = YearMonth.of(year, row * 3 + column + 1)
                                MiniPracticeMonth(
                                    month = miniMonth,
                                    today = today,
                                    dailySeconds = dailySeconds,
                                    colors = colors,
                                    locale = locale,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        monthKey = miniMonth.toString()
                                        yearView = false
                                        returnToYear = true
                                        selectedDayKey = null
                                    },
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PracticeCalendar(
                                range = range,
                                dailySeconds = dailySeconds,
                                selectedDate = selectedDayKey?.let(LocalDate::parse),
                                colors = colors,
                                locale = locale,
                                onSelect = { selectedDayKey = it.toString() },
                            )
                            Text(
                                "Each square is one day. Tap a day to see its sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { PracticeLegend(colors) }
                if (!yearView && selectedDayKey != null) {
                    item {
                        PracticeDayDetails(
                            date = LocalDate.parse(selectedDayKey),
                            sessions = state.sessions,
                            zone = zone,
                            locale = locale,
                        )
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                "${summary.daysPracticed} of ${range.dayCount} days",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("with a sitting", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                PracticeMetric("Total time", practiceDuration(summary.totalSeconds), Modifier.weight(1f))
                                PracticeMetric("Daily average", "${summary.dailyAverageMinutes}m", Modifier.weight(1f))
                            }
                            Row {
                                PracticeMetric("Current streak", "${state.streakDays} days", Modifier.weight(1f))
                                PracticeMetric("Longest in period", "${summary.longestStreak} days", Modifier.weight(1f))
                            }
                            Text(
                                "Average includes days without sitting. Current streak is as of today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun practiceColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        Color.Transparent,
        lerp(scheme.surfaceContainer, scheme.primary, 0.22f),
        lerp(scheme.surfaceContainer, scheme.primary, 0.40f),
        lerp(scheme.surfaceContainer, scheme.primary, 0.60f),
        lerp(scheme.surfaceContainer, scheme.primary, 0.80f),
        scheme.primary,
    )
}

private fun weekDays(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return List(7) { first.plus(it.toLong()) }
}

@Composable
private fun PracticeCalendar(
    range: PracticeRange,
    dailySeconds: Map<LocalDate, Long>,
    selectedDate: LocalDate?,
    colors: List<Color>,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
) {
    val weekdays = weekDays(locale)
    val offset = (range.start.dayOfWeek.value - weekdays.first().value + 7) % 7
    val rows = (offset + range.dayCount + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            weekdays.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { column ->
                    val dayIndex = row * 7 + column - offset
                    val modifier = Modifier.weight(1f).aspectRatio(1f)
                    if (dayIndex !in 0 until range.dayCount) {
                        Box(modifier)
                    } else {
                        val date = range.start.plusDays(dayIndex.toLong())
                        val seconds = dailySeconds[date] ?: 0L
                        val selected = date == selectedDate
                        Surface(
                            onClick = { onSelect(date) },
                            modifier = modifier.semantics {
                                this.selected = selected
                                contentDescription = "${date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))}: " +
                                    if (seconds > 0) practiceDuration(seconds) else "No sitting"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = colors[practiceIntensity(seconds)],
                            border = when {
                                selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                                seconds == 0L -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                else -> null
                            },
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPracticeMonth(
    month: YearMonth,
    today: LocalDate,
    dailySeconds: Map<LocalDate, Long>,
    colors: List<Color>,
    locale: Locale,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val firstDay = weekDays(locale).first()
    val offset = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val outline = MaterialTheme.colorScheme.outline
    val future = month > YearMonth.from(today)
    val description = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    Card(
        onClick = onClick,
        enabled = !future,
        modifier = modifier.semantics { contentDescription = "Open $description" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                month.month.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
            )
            Canvas(Modifier.fillMaxWidth().aspectRatio(7f / 6f)) {
                val cell = size.width / 7
                val side = cell * 0.72f
                for (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    if (date > today) continue
                    val index = offset + day - 1
                    val position = Offset(index % 7 * cell + cell * 0.14f, index / 7 * cell + cell * 0.14f)
                    val intensity = practiceIntensity(dailySeconds[date] ?: 0L)
                    if (intensity == 0) {
                        drawRoundRect(
                            color = outline,
                            topLeft = position,
                            size = Size(side, side),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                            style = Stroke(1.dp.toPx()),
                        )
                    } else {
                        drawRoundRect(
                            color = colors[intensity],
                            topLeft = position,
                            size = Size(side, side),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeLegend(colors: List<Color>) {
    val labels = listOf("No sitting", "<10m", "10–19m", "20–39m", "40–120m", ">2h")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.indices.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { index ->
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = RoundedCornerShape(3.dp),
                            color = colors[index],
                            border = if (index == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        ) {}
                        Text(labels[index], style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun PracticeDayDetails(
    date: LocalDate,
    sessions: List<SittingSession>,
    zone: ZoneId,
    locale: Locale,
) {
    val daySessions = sessions.filter {
        Instant.ofEpochMilli(it.endedAtMillis).atZone(zone).toLocalDate() == date
    }.sortedBy { it.startedAtMillis }
    val timePattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"
    val timeFormat = DateTimeFormatter.ofPattern(timePattern, locale)
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (daySessions.isEmpty()) "No sitting" else {
                    "${practiceDuration(daySessions.sumOf { it.durationSeconds.toLong() })} · " +
                        "${daySessions.size} ${if (daySessions.size == 1) "session" else "sessions"}"
                },
            )
            daySessions.forEach {
                Text(
                    "${Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).format(timeFormat)} · " +
                        "${it.presetName} · ${practiceDuration(it.durationSeconds.toLong())}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
