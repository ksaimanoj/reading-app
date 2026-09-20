package com.littlewords.app.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.abs

@Composable fun PracticeScreen(card: CardEntity, settings: AppSettings, busy: Boolean, onScore: (Boolean) -> Unit, onPause: () -> Unit) {
    var drag by remember(card.id) { mutableFloatStateOf(0f) }
    var vertical by remember(card.id) { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val latestScore by rememberUpdatedState(onScore)
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (settings.showSillyMarker && card.category == Category.THREE_SILLY.name)
                    Text("✦  silly word", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        if (abs(drag) >= threshold && abs(drag) > abs(vertical) * 1.4f) latestScore(drag > 0f)
                        drag = 0f; vertical = 0f
                    },
                )
            }, contentAlignment = Alignment.Center) {
            val fontScale = LocalDensity.current.fontScale
            val size = (minOf(maxHeight.value * .62f, (maxWidth.value - 72f) / (card.word.length * .68f)) / fontScale).coerceIn(28f, 240f)
            Text(card.word, fontSize = size.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                textAlign = TextAlign.Center, modifier = Modifier.testTag("readingWord").graphicsLayer { translationX = drag * .15f })
        }
        if (settings.showButtons) Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(enabled = !busy, onClick = { onScore(false) }) { Text("←  Needs practice") }
            Button(enabled = !busy, onClick = { onScore(true) }) { Text("Read it  →") }
        } else Spacer(Modifier.height(24.dp))
    }
}
