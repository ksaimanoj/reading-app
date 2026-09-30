package com.littlewords.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutPrivacyScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        PageHeader("About & privacy", onBack)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Privacy at a glance", style = MaterialTheme.typography.titleLarge)
            Text("Little Words keeps reading profiles and progress in private app storage on this device.")
            PrivacyPoint("Profiles, settings, reading cards, attempts, session times, and milestones stay on the device.")
            PrivacyPoint("There is no account, advertising, analytics, or microphone recording. The app does not request internet access.")
            PrivacyPoint("Android backup and device transfer are disabled. Uninstalling or clearing app data removes all progress.")
            Text("No in-app export or profile deletion is available.")
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrivacyPoint(text: String) {
    Text("•  $text", style = MaterialTheme.typography.bodyLarge)
}
