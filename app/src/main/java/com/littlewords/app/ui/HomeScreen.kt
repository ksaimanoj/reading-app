package com.littlewords.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.SENTENCE_CATEGORY
import com.littlewords.app.domain.StageCatalog

@Composable fun HomeScreen(state: ReadingState, busy: Boolean, onStart: (PracticeMode) -> Unit, onSettings: () -> Unit, onProgress: () -> Unit, onProfiles: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 600.dp
        val compact = maxHeight < 500.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 20.dp, vertical = 8.dp)) {
            HomeHeader(state.profileName, wide, onProfiles, onProgress, onSettings)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (wide || compact) {
                    Row(Modifier.fillMaxWidth().widthIn(max = 960.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 32.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (compact) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeTitle(true, Modifier.fillMaxWidth())
                            Text("${stageMessage(state)} · View stages", Modifier.clickable(onClick = onProgress),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        } else HomeTitle(false, Modifier.weight(1f))
                        HomeActions(state, busy, compact, onStart, onProgress, Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.fillMaxWidth().widthIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp)) {
                        HomeTitle(compact, Modifier.fillMaxWidth())
                        HomeActions(state, busy, compact, onStart, onProgress, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable private fun HomeHeader(profileName: String, wide: Boolean, onProfiles: () -> Unit, onProgress: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text("L", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Serif, fontSize = 22.sp)
        }
        Text("little words", Modifier.weight(1f).padding(start = 10.dp), fontSize = 22.sp, fontFamily = FontFamily.Serif, maxLines = 1)
        TextButton(onClick = onProfiles, modifier = Modifier.widthIn(max = 140.dp)) {
            Text("$profileName ▾", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (wide) HomeNavigation(onProgress, onSettings)
    }
    if (!wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        HomeNavigation(onProgress, onSettings)
    }
}

@Composable private fun HomeNavigation(onProgress: () -> Unit, onSettings: () -> Unit) {
    TextButton(onClick = onProgress) { Text("Progress") }
    TextButton(onClick = onSettings) { Text("Settings") }
}

@Composable private fun HomeTitle(compact: Boolean, modifier: Modifier) {
    Text(
        "Let's read.",
        modifier = modifier,
        fontFamily = FontFamily.Serif,
        fontSize = if (compact) 36.sp else 48.sp,
        lineHeight = if (compact) 40.sp else 52.sp,
        maxLines = 1,
    )
}

@Composable private fun HomeActions(state: ReadingState, busy: Boolean, compact: Boolean,
    onStart: (PracticeMode) -> Unit, onProgress: () -> Unit, modifier: Modifier) {
    val activeSession = state.session != null
    val sentenceSession = state.card?.category == SENTENCE_CATEGORY
    val practiced = state.validHistory.count { it.sessionId == state.session?.id }
    val mode = if (sentenceSession) PracticeMode.SENTENCES else PracticeMode.WORDS
    val largeText = LocalDensity.current.fontScale > 1.2f
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(if (compact) 12.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp),
    ) {
        if (!compact) {
            if (state.settings.config.selectedSubskills != null) Text(stageMessage(state), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onProgress) { Text("View Learning stages") }
        }
        if (activeSession) {
            Text("$practiced ${if (sentenceSession) "sentences" else "words"} completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onStart(mode) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (compact) 44.dp else 54.dp)) {
                Text("Resume reading", fontSize = 17.sp)
            }
        } else {
            Button(onClick = { onStart(PracticeMode.WORDS) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (compact) 44.dp else 54.dp)) {
                Text("Start reading", fontSize = 17.sp)
            }
            OutlinedButton(onClick = { onStart(PracticeMode.SENTENCES) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(if (compact) 44.dp else 54.dp)
                    .semantics { contentDescription = "Try short sentences" }) {
                Text(if (compact && largeText) "Sentences" else "Try short sentences", fontSize = if (compact) 13.sp else 17.sp,
                    maxLines = 1)
            }
        }
    }
}

private fun stageMessage(state: ReadingState): String {
    val selected = state.settings.config.selectedSubskills ?: return "Set up Learning stages"
    val stages = StageCatalog.stages.filter { stage ->
        stage.subskills.any { StageCatalog.key(stage.id, it.id) in selected }
    }
    val next = stages.firstOrNull { stage -> state.stageSummaries[stage.id]?.collectionComplete != true }
    val summary = next?.let { state.stageSummaries[it.id] }
    return when {
        stages.isEmpty() -> "Silly words included · choose a real-word stage when ready."
        next == null -> "All included collections are ready for review."
        else -> "${next.label} · ${summary?.confident ?: 0} of ${summary?.total ?: 0} confident" +
            if (stages.size > 1) " · ${stages.size - 1} other groups included" else ""
    }
}
