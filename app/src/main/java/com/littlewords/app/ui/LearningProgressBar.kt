package com.littlewords.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable fun LearningProgressBar(confident: Int, total: Int, modifier: Modifier = Modifier, itemLabel: String = "words") {
    val progress = if (total > 0) (confident.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).roundToInt()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$confident of $total $itemLabel confident", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    stateDescription = "$percentage percent, $confident of $total $itemLabel confident"
                }) {
                if (progress > 0f) Box(Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Text("$percentage%", modifier = Modifier.widthIn(min = 48.dp),
                textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
