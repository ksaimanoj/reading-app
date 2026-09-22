package com.littlewords.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlewords.app.data.ProfileEntity

@Composable fun ProfilesScreen(
    profiles: List<ProfileEntity>, selectedId: Long, busy: Boolean,
    onSelect: (Long) -> Unit, onCreate: (String) -> Unit, onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val cleanName = name.trim()
    val duplicate = profiles.any { it.name.equals(cleanName, ignoreCase = true) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        PageHeader("Reading profiles", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Choose who is reading", style = MaterialTheme.typography.headlineLarge)
            Text("Each profile has its own progress, saved place, and reading settings.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            profiles.forEach { profile ->
                OutlinedButton(onClick = { onSelect(profile.id) }, enabled = !busy && profile.id != selectedId,
                    modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(profile.name + if (profile.id == selectedId) " · Current" else "")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Add a profile", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Child's name or Testing") }, modifier = Modifier.fillMaxWidth(),
                isError = duplicate || cleanName.length > 40)
            if (duplicate) Text("That name is already in use.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { onCreate(cleanName) },
                enabled = !busy && cleanName.isNotEmpty() && cleanName.length <= 40 && !duplicate,
                modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Create & switch") }
            Text("Your existing reading is saved under Gagan. Add Testing to try features without changing a child's progress.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
