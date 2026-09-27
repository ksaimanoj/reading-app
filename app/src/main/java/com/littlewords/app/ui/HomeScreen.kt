package com.littlewords.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlewords.app.domain.PracticeMode
import com.littlewords.app.domain.SENTENCE_CATEGORY

@Composable fun HomeScreen(state: ReadingState, busy: Boolean, onStart: (PracticeMode) -> Unit, onSettings: () -> Unit, onProgress: () -> Unit, onProfiles: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 600.dp
        val compact = maxHeight < 500.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 20.dp, vertical = 8.dp)) {
            HomeHeader(state.profileName, wide, onProfiles, onProgress, onSettings)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (wide) {
                    Row(Modifier.fillMaxWidth().widthIn(max = 960.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                        HomeTitle(compact, Modifier.weight(1f))
                        HomeActions(state, busy, compact, onStart, Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.fillMaxWidth().widthIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp)) {
                        HomeTitle(compact, Modifier.fillMaxWidth())
                        HomeActions(state, busy, compact, onStart, Modifier.fillMaxWidth())
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

@Composable private fun HomeActions(state: ReadingState, busy: Boolean, compact: Boolean, onStart: (PracticeMode) -> Unit, modifier: Modifier) {
    val activeSession = state.session != null
    val sentenceSession = state.card?.category == SENTENCE_CATEGORY
    val practiced = state.validHistory.count { it.sessionId == state.session?.id }
    val mode = if (sentenceSession) PracticeMode.SENTENCES else PracticeMode.WORDS
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(if (compact) 16.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        if (activeSession) {
            Text("$practiced ${if (sentenceSession) "sentences" else "words"} completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onStart(mode) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp)) {
                Text("Resume reading", fontSize = 17.sp)
            }
        } else {
            Button(onClick = { onStart(PracticeMode.WORDS) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp)) {
                Text("Start reading", fontSize = 17.sp)
            }
            OutlinedButton(onClick = { onStart(PracticeMode.SENTENCES) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp)) {
                Text("Try short sentences", fontSize = 17.sp)
            }
        }
    }
}
