package com.littlewords.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.littlewords.app.data.ReadingDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun versionThreeHistoryWithActiveIndicesSurvivesUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            legacy.execSQL("CREATE TABLE profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
            legacy.execSQL("CREATE TABLE active_profile (id INTEGER NOT NULL PRIMARY KEY, profileId INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY, config TEXT NOT NULL, theme TEXT NOT NULL, showButtons INTEGER NOT NULL, showSillyMarker INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL DEFAULT 1, startedAt INTEGER NOT NULL, endedAt INTEGER, config TEXT NOT NULL, currentCardId INTEGER, remainingBag TEXT NOT NULL, lastAttemptId INTEGER, contentVersion INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE cards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, word TEXT NOT NULL, category TEXT NOT NULL, patterns TEXT NOT NULL, shownAt INTEGER NOT NULL, contentVersion INTEGER NOT NULL, FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            legacy.execSQL("CREATE TABLE attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cardId INTEGER NOT NULL, sessionId INTEGER NOT NULL, direction TEXT NOT NULL, success INTEGER NOT NULL, respondedAt INTEGER NOT NULL, voidedAt INTEGER, durationMs INTEGER, FOREIGN KEY(cardId) REFERENCES cards(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            legacy.execSQL("CREATE INDEX index_cards_sessionId ON cards(sessionId)")
            legacy.execSQL("CREATE INDEX index_attempts_cardId ON attempts(cardId)")
            legacy.execSQL("CREATE INDEX index_attempts_sessionId ON attempts(sessionId)")
            legacy.execSQL("CREATE UNIQUE INDEX one_active_attempt ON attempts(cardId) WHERE voidedAt IS NULL")
            legacy.execSQL("CREATE UNIQUE INDEX one_active_session_per_profile ON sessions(profileId) WHERE endedAt IS NULL")
            legacy.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            legacy.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '61f800ba433ca67af8eb6b227f49f453')")
            legacy.execSQL("INSERT INTO profiles (id, name) VALUES (1, 'Gagan')")
            legacy.execSQL("INSERT INTO active_profile (id, profileId) VALUES (1, 1)")
            legacy.execSQL("INSERT INTO sessions (id, profileId, startedAt, endedAt, config, remainingBag, contentVersion) VALUES (1, 1, 1000, 3000, '{}', '', 1)")
            legacy.execSQL("INSERT INTO cards (id, sessionId, word, category, patterns, shownAt, contentVersion) VALUES (1, 1, 'cat', 'THREE_REAL', 'short_a', 1100, 1)")
            legacy.execSQL("INSERT INTO attempts (id, cardId, sessionId, direction, success, respondedAt) VALUES (1, 1, 1, 'right', 1, 2100)")
            legacy.version = 3
        } finally {
            legacy.close()
        }

        val upgraded = ReadingDatabase.open(context, name)
        try {
            val history = upgraded.dao().observeHistory(1).first()
            assertEquals("cat", history.single().word)
            assertEquals(1, upgraded.dao().observeSessions(1).first().size)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun versionOneHistorySurvivesTimingColumnMigration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            legacy.execSQL("CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY, config TEXT NOT NULL, theme TEXT NOT NULL, showButtons INTEGER NOT NULL, showSillyMarker INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, config TEXT NOT NULL, currentCardId INTEGER, remainingBag TEXT NOT NULL, lastAttemptId INTEGER, contentVersion INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE cards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, word TEXT NOT NULL, category TEXT NOT NULL, patterns TEXT NOT NULL, shownAt INTEGER NOT NULL, contentVersion INTEGER NOT NULL, FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            legacy.execSQL("CREATE TABLE attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cardId INTEGER NOT NULL, sessionId INTEGER NOT NULL, direction TEXT NOT NULL, success INTEGER NOT NULL, respondedAt INTEGER NOT NULL, voidedAt INTEGER, FOREIGN KEY(cardId) REFERENCES cards(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            legacy.execSQL("CREATE INDEX index_cards_sessionId ON cards(sessionId)")
            legacy.execSQL("CREATE INDEX index_attempts_cardId ON attempts(cardId)")
            legacy.execSQL("CREATE INDEX index_attempts_sessionId ON attempts(sessionId)")
            legacy.execSQL("CREATE UNIQUE INDEX one_active_attempt ON attempts(cardId) WHERE voidedAt IS NULL")
            legacy.execSQL("CREATE UNIQUE INDEX one_active_session ON sessions((1)) WHERE endedAt IS NULL")
            legacy.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            legacy.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '09073ffd789b68bcd7a339f29f959310')")
            legacy.execSQL("INSERT INTO sessions (id, startedAt, endedAt, config, remainingBag, contentVersion) VALUES (1, 1000, 3000, '{\"enabledCategories\":[\"THREE_REAL\"],\"letters\":\"abcdefghijklmnopqrstuvwxyz\",\"patterns\":[\"short_a\"]}', '', 1)")
            legacy.execSQL("INSERT INTO cards (id, sessionId, word, category, patterns, shownAt, contentVersion) VALUES (1, 1, 'cat', 'THREE_REAL', 'short_a', 1100, 1)")
            legacy.execSQL("INSERT INTO attempts (id, cardId, sessionId, direction, success, respondedAt) VALUES (1, 1, 1, 'right', 1, 2100)")
            legacy.version = 1
        } finally {
            legacy.close()
        }

        val upgraded = ReadingDatabase.open(context, name)
        try {
            val history = upgraded.dao().observeHistory(1).first()
            assertEquals(1, history.size)
            assertEquals("cat", history.single().word)
            assertNull(history.single().durationMs)
            assertEquals(1, upgraded.dao().observeSessions(1).first().size)
            assertEquals("Gagan", upgraded.dao().observeProfiles().first().single().name)
            assertEquals(emptyList<com.littlewords.app.data.StageAchievementEntity>(), upgraded.dao().achievements(1))
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
