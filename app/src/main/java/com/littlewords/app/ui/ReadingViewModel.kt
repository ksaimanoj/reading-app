package com.littlewords.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlewords.app.data.*
import com.littlewords.app.domain.PracticeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReadingState(
    val profileId: Long = 0,
    val profiles: List<ProfileEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val session: SessionEntity? = null,
    val card: CardEntity? = null,
    val history: List<HistoryItem> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val loaded: Boolean = false,
) {
    val validHistory get() = history.filter { it.voidedAt == null }
    val profileName get() = profiles.firstOrNull { it.id == profileId }?.name ?: ""
}

class ReadingViewModel(private val repository: ReadingRepository) : ViewModel() {
    private val readingTimer = ReadingTimer()
    val state = combine(repository.reading, repository.profiles) { reading, profiles ->
        ReadingState(reading.profileId, profiles, reading.settings, reading.activeSession,
            reading.currentCard, reading.history, reading.sessions, true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingState())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    fun startTiming(cardId: Long) = readingTimer.start(cardId)
    fun stopTiming(cardId: Long) = readingTimer.stop(cardId)
    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { action(); onSuccess() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                _error.value = if (failure is IllegalArgumentException) failure.message
                    else "Couldn't save on this device. Your current reading card is unchanged. Please try again."
            } finally { _busy.value = false }
        }
    }
    fun start(mode: PracticeMode, onSuccess: () -> Unit) = write(onSuccess) { repository.startSession(mode) }
    fun score(cardId: Long, success: Boolean) = write {
        if (repository.score(cardId, success, readingTimer.elapsed(cardId))) readingTimer.clear(cardId)
    }
    fun undo(onSuccess: () -> Unit) = write(onSuccess) { repository.undo() }
    fun end(onSuccess: () -> Unit) = write(onSuccess) { repository.endSession() }
    fun saveSettings(settings: AppSettings, onSuccess: () -> Unit) = write(onSuccess) { repository.saveSettings(settings) }
    fun theme(mode: ThemeMode) = write { repository.setTheme(mode) }
    fun selectProfile(id: Long, onSuccess: () -> Unit) = write(onSuccess) { repository.selectProfile(id) }
    fun createProfile(name: String, onSuccess: (Long) -> Unit) {
        var id = 0L
        write(onSuccess = { onSuccess(id) }) { id = repository.createProfile(name) }
    }
}
