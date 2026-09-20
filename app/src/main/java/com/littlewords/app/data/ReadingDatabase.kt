package com.littlewords.app.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM settings WHERE id = 1") fun observeSettings(): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id = 1") suspend fun settings(): SettingsEntity?
    @Upsert suspend fun putSettings(value: SettingsEntity)
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1") fun observeActiveSession(): Flow<SessionEntity?>
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1") suspend fun activeSession(): SessionEntity?
    @Query("SELECT * FROM sessions ORDER BY id DESC") fun observeSessions(): Flow<List<SessionEntity>>
    @Insert suspend fun insertSession(value: SessionEntity): Long
    @Update suspend fun updateSession(value: SessionEntity)
    @Query("SELECT cards.* FROM cards INNER JOIN sessions ON cards.id = sessions.currentCardId WHERE sessions.endedAt IS NULL LIMIT 1") fun observeCurrentCard(): Flow<CardEntity?>
    @Query("SELECT * FROM cards WHERE id = :id") suspend fun card(id: Long): CardEntity?
    @Insert suspend fun insertCard(value: CardEntity): Long
    @Insert suspend fun insertAttempt(value: AttemptEntity): Long
    @Update suspend fun updateAttempt(value: AttemptEntity)
    @Query("SELECT * FROM attempts WHERE id = :id") suspend fun attempt(id: Long): AttemptEntity?
    @Query("SELECT attempts.id AS attemptId, attempts.sessionId, cards.word, cards.category, attempts.success, attempts.respondedAt, attempts.voidedAt FROM attempts INNER JOIN cards ON attempts.cardId = cards.id ORDER BY attempts.id DESC")
    fun observeHistory(): Flow<List<HistoryItem>>
    @Query("SELECT attempts.id AS attemptId, attempts.sessionId, cards.word, cards.category, attempts.success, attempts.respondedAt, attempts.voidedAt FROM attempts INNER JOIN cards ON attempts.cardId = cards.id WHERE attempts.voidedAt IS NULL ORDER BY attempts.id DESC")
    suspend fun validHistory(): List<HistoryItem>
    @Query("""
        SELECT cards.word FROM cards
        WHERE NOT EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id)
           OR EXISTS (SELECT 1 FROM attempts WHERE attempts.cardId = cards.id AND attempts.voidedAt IS NULL)
        ORDER BY cards.id DESC LIMIT :limit
    """)
    suspend fun recentPresentedWords(limit: Int): List<String>
}

@Database(entities = [SettingsEntity::class, SessionEntity::class, CardEntity::class, AttemptEntity::class], version = 1, exportSchema = true)
abstract class ReadingDatabase : RoomDatabase() {
    abstract fun dao(): ReadingDao
    companion object {
        fun open(context: Context, name: String = "little-words.db"): ReadingDatabase =
            Room.databaseBuilder(context.applicationContext, ReadingDatabase::class.java, name)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE UNIQUE INDEX one_active_attempt ON attempts(cardId) WHERE voidedAt IS NULL")
                        db.execSQL("CREATE UNIQUE INDEX one_active_session ON sessions((1)) WHERE endedAt IS NULL")
                    }
                }).build()
    }
}
