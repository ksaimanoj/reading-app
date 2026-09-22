package com.littlewords.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM profiles ORDER BY id") fun observeProfiles(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles ORDER BY id") suspend fun profiles(): List<ProfileEntity>
    @Insert suspend fun insertProfile(value: ProfileEntity): Long
    @Query("SELECT profileId FROM active_profile WHERE id = 1") fun observeProfileId(): Flow<Long>
    @Query("SELECT profileId FROM active_profile WHERE id = 1") suspend fun profileId(): Long
    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE id = :id)") suspend fun hasProfile(id: Long): Boolean
    @Upsert suspend fun selectProfile(value: ActiveProfileEntity)
    @Query("SELECT * FROM settings WHERE id = :profileId") fun observeSettings(profileId: Long): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id = :profileId") suspend fun settings(profileId: Long): SettingsEntity?
    @Upsert suspend fun putSettings(value: SettingsEntity)
    @Query("SELECT * FROM sessions WHERE profileId = :profileId AND endedAt IS NULL ORDER BY id DESC LIMIT 1") fun observeActiveSession(profileId: Long): Flow<SessionEntity?>
    @Query("SELECT * FROM sessions WHERE profileId = :profileId AND endedAt IS NULL ORDER BY id DESC LIMIT 1") suspend fun activeSession(profileId: Long): SessionEntity?
    @Query("SELECT * FROM sessions WHERE profileId = :profileId ORDER BY id DESC") fun observeSessions(profileId: Long): Flow<List<SessionEntity>>
    @Insert suspend fun insertSession(value: SessionEntity): Long
    @Update suspend fun updateSession(value: SessionEntity)
    @Query("SELECT cards.* FROM cards INNER JOIN sessions ON cards.id = sessions.currentCardId WHERE sessions.profileId = :profileId AND sessions.endedAt IS NULL LIMIT 1") fun observeCurrentCard(profileId: Long): Flow<CardEntity?>
    @Query("SELECT * FROM cards WHERE id = :id") suspend fun card(id: Long): CardEntity?
    @Insert suspend fun insertCard(value: CardEntity): Long
    @Insert suspend fun insertAttempt(value: AttemptEntity): Long
    @Update suspend fun updateAttempt(value: AttemptEntity)
    @Query("SELECT * FROM attempts WHERE id = :id") suspend fun attempt(id: Long): AttemptEntity?
    @Query("SELECT attempts.id AS attemptId, attempts.sessionId, cards.word, cards.category, attempts.success, attempts.respondedAt, attempts.voidedAt, attempts.durationMs FROM attempts INNER JOIN cards ON attempts.cardId = cards.id INNER JOIN sessions ON attempts.sessionId = sessions.id WHERE sessions.profileId = :profileId ORDER BY attempts.id DESC")
    fun observeHistory(profileId: Long): Flow<List<HistoryItem>>
    @Query("SELECT attempts.id AS attemptId, attempts.sessionId, cards.word, cards.category, attempts.success, attempts.respondedAt, attempts.voidedAt, attempts.durationMs FROM attempts INNER JOIN cards ON attempts.cardId = cards.id INNER JOIN sessions ON attempts.sessionId = sessions.id WHERE sessions.profileId = :profileId AND attempts.voidedAt IS NULL ORDER BY attempts.id DESC")
    suspend fun validHistory(profileId: Long): List<HistoryItem>
    @Query("""
        SELECT cards.word FROM cards
        INNER JOIN sessions ON cards.sessionId = sessions.id
        WHERE sessions.profileId = :profileId AND (
        NOT EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id)
           OR EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id AND attempts.voidedAt IS NULL)
        )
        ORDER BY cards.id DESC LIMIT :limit
    """)
    suspend fun recentPresentedWords(profileId: Long, limit: Int): List<String>
    @Query("""
        SELECT cards.word FROM cards
        INNER JOIN sessions ON cards.sessionId = sessions.id
        WHERE sessions.profileId = :profileId AND cards.category = 'SENTENCE'
          AND (NOT EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id)
            OR EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id AND attempts.voidedAt IS NULL))
        ORDER BY cards.id DESC
    """)
    suspend fun presentedSentences(profileId: Long): List<String>
}

@Database(entities = [ProfileEntity::class, ActiveProfileEntity::class, SettingsEntity::class, SessionEntity::class, CardEntity::class, AttemptEntity::class], version = 3, exportSchema = true)
abstract class ReadingDatabase : RoomDatabase() {
    abstract fun dao(): ReadingDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Room validates the declared schema before onOpen. These legacy
                // expression/partial indices are restored immediately afterward.
                db.execSQL("DROP INDEX IF EXISTS one_active_attempt")
                db.execSQL("DROP INDEX IF EXISTS one_active_session")
                db.execSQL("ALTER TABLE attempts ADD COLUMN durationMs INTEGER")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS active_profile (id INTEGER NOT NULL PRIMARY KEY, profileId INTEGER NOT NULL)")
                db.execSQL("INSERT INTO profiles (id, name) VALUES (1, 'Gagan')")
                db.execSQL("INSERT INTO active_profile (id, profileId) VALUES (1, 1)")
                db.execSQL("ALTER TABLE sessions ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("DROP INDEX IF EXISTS one_active_attempt")
                db.execSQL("DROP INDEX IF EXISTS one_active_session")
            }
        }

        fun open(context: Context, name: String = "little-words.db"): ReadingDatabase =
            Room.databaseBuilder(context.applicationContext, ReadingDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("INSERT INTO profiles (id, name) VALUES (1, 'Gagan')")
                        db.execSQL("INSERT INTO active_profile (id, profileId) VALUES (1, 1)")
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS one_active_attempt ON attempts(cardId) WHERE voidedAt IS NULL")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS one_active_session_per_profile ON sessions(profileId) WHERE endedAt IS NULL")
                    }
                }).build()
    }
}
