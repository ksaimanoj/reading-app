package com.littlewords.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.littlewords.app.data.*
import com.littlewords.app.domain.*

fun toggleCategory(config: PracticeConfig, category: Category): PracticeConfig = config.copy(
    enabledCategories = if (category in config.enabledCategories) {
        config.enabledCategories - category
    } else {
        config.enabledCategories + category
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(settings: AppSettings, hasSession: Boolean, busy: Boolean,
    onTheme: (ThemeMode) -> Unit, onSave: (AppSettings) -> Unit,
    onLearningStages: () -> Unit, onBack: () -> Unit) {
    var enabledCategories by remember(settings.config) { mutableStateOf(settings.config.enabledCategories) }
    var patterns by remember(settings.config) { mutableStateOf(settings.config.patterns) }
    var letters by remember(settings.config) { mutableStateOf(settings.config.letters) }
    var showLetters by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var buttons by remember(settings.showButtons) { mutableStateOf(settings.showButtons) }
    var sillyMarker by remember(settings.showSillyMarker) { mutableStateOf(settings.showSillyMarker) }
    val config = settings.config.copy(enabledCategories = enabledCategories, letters = letters, patterns = patterns)
    val errors = Selector.validate(config)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        PageHeader("Make it their own", onBack) {
            Button(enabled = !busy && errors.isEmpty(), onClick = { onSave(AppSettings(config, settings.theme, buttons, sillyMarker)) }) { Text("Save & back") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeMode.entries.forEach { mode -> FilterChip(
                        selected = settings.theme == mode, enabled = !busy, onClick = { onTheme(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) }) }
                }
                Text("Light, dark, or follow your device. Saved as soon as you choose.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (settings.config.selectedSubskills != null) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Practice content", style = MaterialTheme.typography.titleMedium)
                Text("Choose stages and see word milestones for this child in Learning stages.")
                TextButton(onClick = onLearningStages) { Text("Open Learning stages") }
            }
            if (settings.config.selectedSubskills == null) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Word groups", style = MaterialTheme.typography.titleMedium)
                Text("Current custom selection. You can switch to Learning stages from Progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onLearningStages) { Text("Open Learning stages") }
                Category.entries.forEach { category ->
                    val count = Selector.eligible(config, category).size
                    SettingSwitch(
                        category.displayName(),
                        "$count available with these sounds" + if (count in 1..5) " · a small pool, so expect repeats" else "",
                        category in enabledCategories,
                        Modifier.testTag("category_${category.name}"),
                    ) {
                        enabledCategories = toggleCategory(config, category).enabledCategories
                    }
                }
            }
            if (settings.config.selectedSubskills != null) TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide advanced options" else "Advanced options")
            }
            if (settings.config.selectedSubskills == null || showAdvanced) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sound restrictions", style = MaterialTheme.typography.titleMedium)
                Text("Only words using these sounds and patterns will appear.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Catalog.patternLabels.forEach { (key, label) -> FilterChip(selected = key in patterns,
                        onClick = { patterns = if (key in patterns) patterns - key else patterns + key }, label = { Text(label) }) }
                }
                val sentenceCount = SentenceSelector.eligible(config).size
                Text(
                    if (sentenceCount == 0) "No reviewed sentences are available with these sounds and letters. Adjust the restrictions to restore sentence practice."
                    else "$sentenceCount reviewed short sentences available with these sounds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sentenceCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = { showLetters = !showLetters }) { Text(if (showLetters) "Hide individual letters" else "Choose individual letters") }
                if (showLetters) {
                    Text("Keep only the letters they have learned. Sound patterns above also apply.", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ('a'..'z').forEach { letter -> FilterChip(selected = letter in letters,
                            onClick = { letters = if (letter in letters) letters.replace(letter.toString(), "") else (letters + letter).toList().sorted().joinToString("") },
                            label = { Text(letter.toString()) }) }
                    }
                }
            }
            if (settings.config.selectedSubskills != null && !showAdvanced) {
                val sentenceCount = SentenceSelector.eligible(config).size
                Text(if (sentenceCount == 0) "No reviewed sentences are available with the current choices. Adjust advanced restrictions or stage choices."
                    else "$sentenceCount reviewed short sentences available with the current choices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sentenceCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            if (errors.isNotEmpty()) SoftPanel(Modifier.fillMaxWidth()) {
                Text("Adjust the word choices before saving", style = MaterialTheme.typography.titleMedium)
                errors.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSwitch("Show scoring buttons", "An alternative to swiping, at the bottom of the word screen.", buttons) { buttons = it }
            SettingSwitch("Mark silly words", "A small label reminds him these words have no meaning.", sillyMarker) { sillyMarker = it }
            if (hasSession) Text("Your current session keeps its word choices. Changes to groups and sounds apply to the next session.", color = MaterialTheme.colorScheme.primary)
            Text("Reading history stays on this device. Uninstalling or clearing app data removes it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun SettingSwitch(title: String, detail: String, value: Boolean,
    modifier: Modifier = Modifier, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = value, onCheckedChange = change, modifier = modifier)
    }
}
