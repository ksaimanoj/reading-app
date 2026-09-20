package com.littlewords.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlewords.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReadingState(
    val settings: AppSettings = AppSettings(),
    val session: SessionEntity? = null,
    val card: CardEntity? = null,
    val history: List<HistoryItem> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val loaded: Boolean = false,
) { val validHistory get() = history.filter { it.voidedAt == null } }

class ReadingViewModel(private val repository: ReadingRepository) : ViewModel() {
    val state = combine(repository.settings, repository.activeSession, repository.currentCard, repository.history, repository.sessions) {
        settings, session, card, history, sessions -> ReadingState(settings, session, card, history, sessions, true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingState())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { action(); onSuccess() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                _error.value = if (failure is IllegalArgumentException) failure.message
                    else "Couldn't save on this device. Your current word is unchanged. Please try again."
            } finally { _busy.value = false }
        }
    }
    fun start(onSuccess: () -> Unit) = write(onSuccess) { repository.startSession() }
    fun score(cardId: Long, success: Boolean) = write { repository.score(cardId, success) }
    fun undo(onSuccess: () -> Unit) = write(onSuccess) { repository.undo() }
    fun end(onSuccess: () -> Unit) = write(onSuccess) { repository.endSession() }
    fun saveSettings(settings: AppSettings, onSuccess: () -> Unit) = write(onSuccess) { repository.saveSettings(settings) }
    fun theme(mode: ThemeMode) = write { repository.setTheme(mode) }
}
