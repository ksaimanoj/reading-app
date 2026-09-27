package com.littlewords.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.littlewords.app.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LearningStagesScreen(state: ReadingState, busy: Boolean,
    onSaveChoices: (PracticeConfig) -> Unit, expanded: String?,
    onExpandedStageChange: (String?) -> Unit, scrollState: ScrollState,
    onHistory: () -> Unit, onBack: () -> Unit) {
    val base = state.settings.config
    var draft by remember(state.profileId, base.selectedSubskills) { mutableStateOf(base.selectedSubskills.orEmpty()) }
    var listFilter by remember(state.profileId) { mutableStateOf("Not tried") }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val dirty = if (base.selectedSubskills == null) draft.isNotEmpty() else draft != base.selectedSubskills
    fun leave(action: () -> Unit) { if (dirty) pendingLeave = action else action() }
    BackHandler(dirty) { pendingLeave = onBack }
    val saveConfig = StageCatalog.saveChoices(base, draft)
    val saveErrors = Selector.validate(saveConfig)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        PageHeader("Every little step", onBack = { leave(onBack) })
        Text(state.profileName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
        PrimaryTabRow(selectedTabIndex = 0) {
            Tab(selected = true, onClick = {}, text = { Text("Learning stages") })
            Tab(selected = false, onClick = { leave(onHistory) }, text = { Text("History") })
        }
        Column(Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Confident means read independently in two different sessions. These are your observations, not a test or reading level.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (base.selectedSubskills == null) Text(
                "Custom selection active. Saving stage choices changes future sessions; current sessions keep their settings.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            StageCatalog.stages.forEach { stage ->
                val words = StageCatalog.words(stage.id)
                val summary = state.stageSummaries[stage.id] ?: WordProgressCalculator.summarize(stage.id, state.wordMilestones)
                val keys = stage.subskills.map { StageCatalog.key(stage.id, it.id) }.toSet()
                val selectedCount = keys.count { it in draft }
                SoftPanel(Modifier.fillMaxWidth()) {
                    Text(stage.label, style = MaterialTheme.typography.titleLarge)
                    Text("For example: ${words.take(3).joinToString(", ") { it.text }}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary.confident} of ${summary.total} words confident")
                    LinearProgressIndicator(progress = { if (summary.total == 0) 0f else summary.confident.toFloat() / summary.total },
                        modifier = Modifier.fillMaxWidth())
                    Text("${summary.practising} practising · ${summary.notTried} not tried" +
                        if (summary.needsReview > 0) " · ${summary.needsReview} needs review" else "")
                    val available = Selector.eligible(StageCatalog.saveChoices(base, keys)).size
                    if (available < summary.total) Text("$available available for current practice with advanced restrictions",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Include in practice")
                            if (selectedCount in 1 until keys.size) Text("Some included", color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(checked = selectedCount == keys.size, onCheckedChange = { checked ->
                            draft = if (checked) draft + keys else draft - keys
                        }, modifier = Modifier.semantics { contentDescription =
                            "Include ${stage.label} in practice: ${if (selectedCount == keys.size) "all included" else if (selectedCount == 0) "none included" else "some included"}" })
                    }
                    TextButton(onClick = { onExpandedStageChange(if (expanded == stage.id) null else stage.id) },
                        modifier = Modifier.testTag("stage_${stage.id}_expand")) {
                        Text(if (expanded == stage.id) "Hide skills and words" else "Show skills and words")
                    }
                    if (expanded == stage.id) {
                        HorizontalDivider()
                        stage.subskills.forEach { skill ->
                            val key = StageCatalog.key(stage.id, skill.id)
                            val skillWords = StageCatalog.words(stage.id, skill.id)
                            val confident = skillWords.count { state.wordMilestones[it.text]?.status == WordStatus.CONFIDENT }
                            val practising = skillWords.count { state.wordMilestones[it.text]?.status == WordStatus.PRACTISING }
                            val review = skillWords.count { state.wordMilestones[it.text]?.needsReview == true }
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(skill.label, style = MaterialTheme.typography.titleMedium)
                                    Text("$confident of ${skillWords.size} confident · $practising practising · ${skillWords.size - confident - practising} not tried" +
                                        if (review > 0) " · $review needs review" else "",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("For example: ${skillWords.take(3).joinToString(", ") { it.text }}",
                                        style = MaterialTheme.typography.bodySmall)
                                    if (skill.explanation.isNotEmpty()) Text(skill.explanation, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${Selector.eligible(StageCatalog.saveChoices(base, setOf(key))).size} available for current practice",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = key in draft, onCheckedChange = { checked ->
                                    draft = if (checked) draft + key else draft - key
                                }, modifier = Modifier.testTag("skill_$key").semantics { contentDescription = "Include ${skill.label} in practice" })
                            }
                        }
                        Text("Word lists", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Not tried", "Practising", "Confident", "Needs review").forEach { label ->
                                FilterChip(selected = listFilter == label, onClick = { listFilter = label }, label = { Text(label) })
                            }
                        }
                        val listed = words.filter { word ->
                            val progress = state.wordMilestones[word.text]
                            if (listFilter == "Needs review") progress?.needsReview == true
                            else progress?.status?.name == listFilter.uppercase().replace(' ', '_')
                        }
                        Text(if (listed.isEmpty()) "No words in this list yet." else listed.joinToString(" · ") { word ->
                            if (word.subskillId == "combined") "${word.text} (${word.patterns.sorted().joinToString(", ")})" else word.text
                        })
                    }
                    if (summary.firstIndependent) Text("Milestone: first independent word")
                    if (summary.firstTenConfident) Text("Milestone: 10 confident words")
                    if (summary.collectionComplete) Text("Collection completed · catalogue version $CATALOGUE_VERSION")
                    if (!summary.collectionComplete) state.achievements.filter {
                        it.stageId == stage.id && it.catalogueVersion < CATALOGUE_VERSION
                    }.maxByOrNull { it.catalogueVersion }?.let { previous ->
                        Text("Completed the version ${previous.catalogueVersion} collection. New reviewed words were added to this version.",
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SoftPanel(Modifier.fillMaxWidth()) {
                Text("Silly words", style = MaterialTheme.typography.titleLarge)
                Text("${StageCatalog.words("silly").size} reviewed silly words. Separate practice; these do not count toward real-word milestones.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Include in practice", modifier = Modifier.weight(1f))
                    Switch(checked = StageCatalog.SILLY_KEY in draft, onCheckedChange = {
                        draft = if (it) draft + StageCatalog.SILLY_KEY else draft - StageCatalog.SILLY_KEY
                    }, modifier = Modifier.semantics { contentDescription = "Include silly words in practice" })
                }
            }
            Text("More sound patterns will appear when reviewed words are available. They cannot be selected yet.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Catalogue version $CATALOGUE_VERSION · totals count each reviewed real word once and do not change with practice choices.",
                style = MaterialTheme.typography.bodySmall)
            if (state.session != null) Text("Your current session keeps its saved choices. Changes affect the next session.",
                color = MaterialTheme.colorScheme.primary)
            if (saveErrors.isNotEmpty()) Text(saveErrors.joinToString("\n"), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
        }
        Button(onClick = { if (saveErrors.isEmpty()) onSaveChoices(saveConfig) }, enabled = !busy && dirty,
            modifier = Modifier.fillMaxWidth()) { Text("Save choices") }
    }
    pendingLeave?.let { action -> AlertDialog(
        onDismissRequest = { pendingLeave = null }, title = { Text("Leave without saving?") },
        text = { Text("Your practice choices have not been saved.") },
        confirmButton = { TextButton(onClick = { pendingLeave = null; action() }) { Text("Discard changes") } },
        dismissButton = { TextButton(onClick = { pendingLeave = null }) { Text("Keep editing") } },
    ) }
}
