package com.littlewords.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlewords.app.domain.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun ProgressScreen(state: ReadingState, onBack: () -> Unit) {
    var filter by remember { mutableStateOf("All words") }
    val valid = state.validHistory
    val selected = valid.filter { when (filter) {
        "Real words" -> it.category != Category.THREE_SILLY.name
        "Silly words" -> it.category == Category.THREE_SILLY.name
        else -> true
    } }
    val successes = selected.count { it.success }
    val revisit = selected.distinctBy { it.word }.filterNot { it.success }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        PageHeader("Every little step", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("A quiet record for you", style = MaterialTheme.typography.headlineLarge)
            Text("These are your observations, not a test score. Look for confidence growing across sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All words", "Real words", "Silly words").forEach { label -> FilterChip(selected = label == filter, onClick = { filter = label }, label = { Text(label) }) }
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
                Text("After your first swipes, the words you practised will appear here. There is no hurry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Words to revisit", style = MaterialTheme.typography.titleLarge)
                Text(if (revisit.isEmpty()) "No words waiting for another try in this group."
                    else revisit.take(40).joinToString("   ·   ") { it.word }, style = MaterialTheme.typography.bodyLarge)
                Text("Based on the latest attempt at each word. Undone swipes are excluded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Recent words", style = MaterialTheme.typography.titleLarge)
                selected.take(20).forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.word + if (item.category == Category.THREE_SILLY.name) "  ·  silly" else "")
                        Text(if (item.success) "Read independently" else "Needs practice", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Practice sessions", style = MaterialTheme.typography.titleLarge)
            if (state.sessions.isEmpty()) Text("Your first session is waiting for you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.sessions.take(30).forEach { session ->
                val attempts = valid.filter { it.sessionId == session.id }
                val date = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(session.startedAt))
                Column {
                    Text(date + if (session.endedAt == null) "  ·  In progress" else "")
                    Text("${attempts.size} attempts · ${attempts.count { it.success }} independent reads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
