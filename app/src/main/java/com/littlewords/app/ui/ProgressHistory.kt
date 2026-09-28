package com.littlewords.app.ui

import com.littlewords.app.data.HistoryItem
import com.littlewords.app.data.SessionEntity

fun historySessions(sessions: List<SessionEntity>, selectedAttempts: List<HistoryItem>, filter: String): List<SessionEntity> {
    if (filter == "All") return sessions
    val matchingIds = selectedAttempts.map { it.sessionId }.toSet()
    return sessions.filter { it.id in matchingIds }
}

fun formatSessionDuration(startedAt: Long, endedAt: Long): String {
    val seconds = ((endedAt - startedAt).coerceAtLeast(0)) / 1000
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val remainder = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 && remainder > 0 -> "${minutes}m ${remainder}s"
        minutes > 0 -> "${minutes}m"
        else -> "${remainder}s"
    }
}
