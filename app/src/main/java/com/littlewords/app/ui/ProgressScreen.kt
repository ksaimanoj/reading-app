package com.littlewords.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.SENTENCE_CATEGORY
import com.littlewords.app.data.ConfigCodec
import com.littlewords.app.data.HistoryItem
import com.littlewords.app.data.SessionEntity
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.PracticeConfig
import androidx.compose.ui.platform.testTag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProgressScreen(state: ReadingState, busy: Boolean,
    onSaveChoices: (PracticeConfig) -> Unit, onBack: () -> Unit) {
    var tab by rememberSaveable(state.profileId) { mutableIntStateOf(0) }
    var expandedStageId by rememberSaveable(state.profileId) { mutableStateOf<String?>(null) }
    val stageScroll = rememberScrollState()
    val historyScroll = rememberScrollState()
    if (tab == 0) {
        LearningStagesScreen(state, busy, onSaveChoices, expandedStageId,
            onExpandedStageChange = { expandedStageId = it }, stageScroll,
            onHistory = { tab = 1 }, onBack = onBack)
        return
    }
    var filter by rememberSaveable(state.profileId) { mutableStateOf("All") }
    var expandedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val valid = state.validHistory
    val selected = valid.filter { when (filter) {
        "Real words" -> it.category != Category.THREE_SILLY.name && it.category != SENTENCE_CATEGORY
        "Silly words" -> it.category == Category.THREE_SILLY.name
        "Sentences" -> it.category == SENTENCE_CATEGORY
        else -> true
    } }
    val successes = selected.count { it.success }
    val revisit = selected.distinctBy { it.word }.filterNot { it.success }
    val visibleSessions = historySessions(state.sessions, selected, filter)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        PageHeader("Every little step", onBack)
        Text(state.profileName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
        PrimaryTabRow(selectedTabIndex = 1) {
            Tab(selected = false, onClick = { tab = 0 }, text = { Text("Learning stages") })
            Tab(selected = true, onClick = {}, text = { Text("History") })
        }
        Column(Modifier.weight(1f).verticalScroll(historyScroll).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("A quiet record for you", style = MaterialTheme.typography.headlineLarge)
            Text("These are your observations, not a test score. Look for confidence growing across sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Real words", "Silly words", "Sentences").forEach { label -> FilterChip(selected = label == filter,
                    onClick = { filter = label; expandedSessionId = null }, label = { Text(label) }) }
            }
            SoftPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Stat(selected.size.toString(), "attempts", Modifier.weight(1f))
                    Stat(successes.toString(), "read independently", Modifier.weight(1f))
                    Stat((selected.size - successes).toString(), "needed practice", Modifier.weight(1f))
                    Stat(if (selected.isEmpty()) "—" else "${successes * 100 / selected.size}%", "independent reads", Modifier.weight(1f))
                }
            }
            if (valid.isEmpty()) {
                Text("A fresh page", style = MaterialTheme.typography.titleLarge)
                Text("After your first swipes, the reading you practised will appear here. There is no hurry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Practice sessions", style = MaterialTheme.typography.titleLarge)
            if (state.sessions.isEmpty()) Text("Your first session is waiting for you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (visibleSessions.isEmpty()) Text("No sessions in this group yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            visibleSessions.forEach { session ->
                SessionHistory(
                    session = session,
                    attempts = selected.filter { it.sessionId == session.id },
                    expanded = expandedSessionId == session.id,
                    onToggle = {
                        expandedSessionId = if (expandedSessionId == session.id) null else session.id
                    },
                )
            }
            if (selected.isNotEmpty()) {
                Text("Reading to revisit", style = MaterialTheme.typography.titleLarge)
                Text(if (revisit.isEmpty()) "Nothing is waiting for another try in this group."
                    else revisit.take(40).joinToString("   ·   ") { it.word }, style = MaterialTheme.typography.bodyLarge)
                Text("Based on the latest attempt at each word or sentence. Undone swipes are excluded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Recent reading", style = MaterialTheme.typography.titleLarge)
                selected.take(20).forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.word + when (item.category) {
                            Category.THREE_SILLY.name -> "  ·  silly"
                            SENTENCE_CATEGORY -> "  ·  sentence"
                            else -> ""
                        })
                        Text((if (item.success) "Read independently" else "Needs practice") + " · " + formatReadingTime(item.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (valid.isNotEmpty()) {
                Text("No reading in this group yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

fun formatReadingTime(durationMs: Long?): String = when {
    durationMs == null -> "time not recorded"
    durationMs < 60_000 -> String.format(Locale.getDefault(), "%.1f s", durationMs / 1000.0)
    else -> "${durationMs / 60_000}m ${(durationMs % 60_000) / 1000}s"
}

@Composable private fun SessionHistory(
    session: SessionEntity,
    attempts: List<HistoryItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val date = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(session.startedAt))
    val ended = session.endedAt?.let { SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(it)) }
    val mode = if (ConfigCodec.decode(session.config).mode == PracticeMode.SENTENCES) "Sentences" else "Words"
    SoftPanel(Modifier.fillMaxWidth()) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().testTag("session_${session.id}")) {
            Column(Modifier.fillMaxWidth()) {
                Text("${if (expanded) "▾" else "▸"}  Started $date · $mode")
                Text(if (ended == null) "In progress · End time and duration available when the session ends"
                    else "Ended $ended · Duration ${formatSessionDuration(session.startedAt, session.endedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${attempts.size} attempts · ${attempts.count { it.success }} independent reads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (attempts.isEmpty()) {
                Text("No completed swipes in this session yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                attempts.sortedBy { it.respondedAt }.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().testTag("session_attempt_${item.attemptId}"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${index + 1}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(item.word)
                            Text(
                                (if (item.success) "Read independently" else "Needs practice") +
                                    " · " + formatReadingTime(item.durationMs),
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
