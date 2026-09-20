package com.littlewords.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun HomeScreen(state: ReadingState, busy: Boolean, onStart: () -> Unit, onSettings: () -> Unit, onProgress: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("L", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Serif, fontSize = 22.sp)
            }
            Text("little words", Modifier.weight(1f).padding(start = 10.dp), fontSize = 24.sp, fontFamily = FontFamily.Serif)
            TextButton(onClick = onProgress) { Text("Progress") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        Spacer(Modifier.height(18.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth > 650.dp
            if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                HomeIntro(Modifier.weight(1.08f).verticalScroll(rememberScrollState()))
                HomePractice(state, busy, onStart, Modifier.weight(1f).verticalScroll(rememberScrollState()))
            } else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                HomeIntro(Modifier.fillMaxWidth())
                HomePractice(state, busy, onStart, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable private fun HomeIntro(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("Small steps, together")
        Text("Let's read\na little.", fontFamily = FontFamily.Serif, fontSize = 49.sp, lineHeight = 51.sp)
        Text("One word at a time. At your own pace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 4.dp)) {
            Column { Text("←  Needs practice", fontWeight = FontWeight.SemiBold); Text("Swipe left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column { Text("Read it  →", fontWeight = FontWeight.SemiBold); Text("Swipe right", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text("For you: count an independent read, even if he sounds it out first. Silly words have no meaning—just blend the sounds.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable private fun HomePractice(state: ReadingState, busy: Boolean, onStart: () -> Unit, modifier: Modifier) {
    val practiced = state.validHistory.count { it.sessionId == state.session?.id }
    SoftPanel(modifier) {
        Eyebrow(if (state.session != null) "Your place is saved" else "Ready when you are")
        Text(if (state.session != null) "A little more?" else "Make room for a small win.", style = MaterialTheme.typography.titleLarge)
        Text(if (state.session != null) "$practiced words practised in this session. Pick up where you left off."
            else "Real words and silly words, chosen from familiar sounds.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(if (state.session != null) "Resume reading" else "Start reading", fontSize = 17.sp)
        }
        Text("Stop whenever you like. Every swipe is saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Just you two. Everything stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}
