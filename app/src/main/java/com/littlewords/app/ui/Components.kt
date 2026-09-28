package com.littlewords.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlewords.app.domain.Category

fun Category.displayName(): String = when(this) {
    Category.TWO_REAL -> "2-letter words"
    Category.THREE_REAL -> "3-letter words"
    Category.THREE_SILLY -> "3-letter silly words"
    Category.FOUR_REAL -> "4-letter words"
    Category.FIVE_REAL -> "5-letter words"
}

@Composable fun PageHeader(title: String, onBack: () -> Unit, action: @Composable RowScope.() -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        val compact = maxWidth < 420.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = if (compact) 4.dp else 12.dp)) { Text("← Back") }
            Text(title, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(horizontal = if (compact) 8.dp else 12.dp))
            action()
        }
    }
}

@Composable fun Eyebrow(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, color = MaterialTheme.colorScheme.primary)
}

@Composable fun SoftPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
