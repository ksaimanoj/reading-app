package com.littlewords.app.ui

import android.os.SystemClock

/** Measures only time for which the current card is visible in the foreground. */
class ReadingTimer(private val clock: () -> Long = SystemClock::elapsedRealtime) {
    private var cardId: Long? = null
    private var activeSince: Long? = null
    private var elapsedMs: Long = 0

    fun start(id: Long) {
        if (cardId != id) {
            cardId = id
            activeSince = null
            elapsedMs = 0
        }
        if (activeSince == null) activeSince = clock()
    }

    fun stop(id: Long) {
        if (cardId != id) return
        activeSince?.let { started ->
            elapsedMs += (clock() - started).coerceAtLeast(0)
            activeSince = null
        }
    }

    fun elapsed(id: Long): Long? {
        if (cardId != id) return null
        return elapsedMs + (activeSince?.let { (clock() - it).coerceAtLeast(0) } ?: 0)
    }

    fun clear(id: Long) {
        if (cardId == id) {
            cardId = null
            activeSince = null
            elapsedMs = 0
        }
    }
}
