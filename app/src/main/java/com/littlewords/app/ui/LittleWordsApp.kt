package com.littlewords.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable fun LittleWordsApp(model: ReadingViewModel, onReadingChanged: (Boolean) -> Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("home") }
    var paused by rememberSaveable { mutableStateOf(false) }
    var showingPrivacy by rememberSaveable { mutableStateOf(false) }
    var openSentenceChoices by rememberSaveable { mutableStateOf(false) }
    var pendingProfileId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.profileId, pendingProfileId) {
        if (pendingProfileId != null && state.profileId == pendingProfileId) {
            page = "home"
            pendingProfileId = null
        }
    }
    val reading = page == "reading"
    SideEffect { onReadingChanged(reading) }
    val activity = LocalContext.current.activity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(reading, paused, foreground, state.card?.id) {
        val cardId = state.card?.id
        if (reading && !paused && foreground && cardId != null) model.startTiming(cardId)
        onDispose { if (cardId != null) model.stopTiming(cardId) }
    }
    DisposableEffect(reading, paused, activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (reading && !paused) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BackHandler(page != "home" && !showingPrivacy) { if (reading) paused = true else page = "home" }
    BackHandler(showingPrivacy) { showingPrivacy = false }
    LittleWordsTheme(state.settings.theme) {
        val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!state.loaded) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            } else when (page) {
                "home" -> HomeScreen(state, busy,
                    onStart = { mode -> model.start(mode) { page = "reading"; paused = false } },
                    onSettings = { openSentenceChoices = false; page = "settings" },
                    onAdjustSentences = { openSentenceChoices = true; page = "settings" },
                    onProgress = { page = "progress" },
                    onProfiles = { page = "profiles" })
                "profiles" -> ProfilesScreen(state.profiles, state.profileId, busy,
                    onSelect = { id -> model.selectProfile(id) { pendingProfileId = id } },
                    onCreate = { name -> model.createProfile(name) { pendingProfileId = it } },
                    onBack = { page = "home" })
                "settings" -> Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().then(if (showingPrivacy) Modifier.clearAndSetSemantics { } else Modifier)) {
                        SettingsScreen(state.settings, state.session != null, busy,
                            onTheme = model::theme,
                            onSave = { model.saveSettings(it) { page = "home" } },
                            onLearningStages = { page = "progress" },
                            onPrivacy = { showingPrivacy = true }, onBack = { page = "home" },
                            openSentenceChoices = openSentenceChoices)
                    }
                    if (showingPrivacy) Surface(
                        Modifier.fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AboutPrivacyScreen(onBack = { showingPrivacy = false })
                    }
                }
                "progress" -> ProgressScreen(state, busy,
                    onSaveChoices = { config -> model.saveSettings(state.settings.copy(config = config)) {} },
                    onBack = { page = "home" })
                "reading" -> state.card?.let { card ->
                    PracticeScreen(card, state.settings,
                        itemNumber = 1 + state.validHistory.count { it.sessionId == card.sessionId },
                        busy = busy || paused,
                        onScore = { model.score(card.id, it) }, onPause = { paused = true },
                        onToggleButtons = { model.showScoringButtons(!state.settings.showButtons) })
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            }
        }
        if (paused && reading) AlertDialog(
            onDismissRequest = { paused = false },
            title = { Text("Take your time") },
            text = {
                Column {
                    Text("Your place is saved. A little practice is enough.")
                    if (state.session?.lastAttemptId != null) TextButton(enabled = !busy, onClick = { model.undo { paused = false } }) { Text("Undo last swipe") }
                    TextButton(enabled = !busy, onClick = { page = "home"; paused = false }) { Text("Save & go home") }
                }
            },
            confirmButton = { Button(enabled = !busy, onClick = { paused = false }) { Text("Keep reading") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { model.end { page = "progress"; paused = false } }) { Text("End practice") } },
        )
        error?.let { message -> AlertDialog(onDismissRequest = model::clearError,
            title = { Text("Let's try that again") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = model::clearError) { Text("OK") } }) }
    }
}
