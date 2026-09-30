package com.littlewords.app.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlewords.app.data.AppSettings
import com.littlewords.app.data.CardEntity
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.SENTENCE_CATEGORY
import kotlin.math.abs

@Composable fun PracticeScreen(
    card: CardEntity,
    settings: AppSettings,
    busy: Boolean,
    onScore: (Boolean) -> Unit,
    onPause: () -> Unit,
    onToggleButtons: () -> Unit,
) {
    var drag by remember(card.id) { mutableFloatStateOf(0f) }
    var vertical by remember(card.id) { mutableFloatStateOf(0f) }
    var scoringHintSeen by rememberSaveable { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val latestScore by rememberUpdatedState(onScore)
    val score: (Boolean) -> Unit = { success ->
        scoringHintSeen = true
        latestScore(success)
    }
    val isSentence = card.category == SENTENCE_CATEGORY
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (settings.showSillyMarker && card.category == Category.THREE_SILLY.name)
                    Text("✦  silly word", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else if (isSentence)
                    Text("short sentence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onToggleButtons, enabled = !busy) {
                Text(if (settings.showButtons) "Hide buttons" else "Show buttons")
            }
            TextButton(onClick = onPause, enabled = !busy) { Text("Pause") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).testTag("readingSurface")
            .pointerInput(card.id, busy) {
                if (!busy) detectHorizontalDragGestures(
                    onDragStart = { drag = 0f; vertical = 0f },
                    onHorizontalDrag = { change, amount -> drag += amount; vertical += change.positionChange().y; change.consume() },
                    onDragCancel = { drag = 0f; vertical = 0f },
                    onDragEnd = {
                        if (abs(drag) >= threshold && abs(drag) > abs(vertical) * 1.4f) score(drag > 0f)
                        drag = 0f; vertical = 0f
                    },
                )
            }, contentAlignment = Alignment.Center) {
            val fontScale = LocalDensity.current.fontScale
            val heightScale = if (isSentence) .34f else .62f
            val widthScale = if (isSentence) .52f else .68f
            val maximum = if (isSentence) 88f else 240f
            val minimum = if (isSentence) 30f else 28f
            val size = (minOf(maxHeight.value * heightScale, (maxWidth.value - 72f) / (card.word.length * widthScale)) / fontScale).coerceIn(minimum, maximum)
            Text(card.word, fontSize = size.sp, lineHeight = (size * 1.18f).sp, fontWeight = FontWeight.Medium, maxLines = if (isSentence) 2 else 1,
                textAlign = TextAlign.Center, modifier = Modifier.testTag("readingWord").graphicsLayer { translationX = drag * .15f })
        }
        if (settings.showButtons) Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(enabled = !busy, onClick = { score(false) }) { Text("←  Needs practice") }
            Button(enabled = !busy, onClick = { score(true) }) { Text("Read it  →") }
        } else if (!scoringHintSeen) {
            Text(
                "Swipe right for Read it; swipe left for Needs practice. Or show scoring buttons.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        } else Spacer(Modifier.height(24.dp))
    }
}
