package com.littlewords.app.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.PracticeConfig
import com.littlewords.app.domain.PracticeMode
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Entity(tableName = "profiles")
data class ProfileEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "active_profile")
data class ActiveProfileEntity(@PrimaryKey val id: Int = 1, val profileId: Long)

data class AppSettings(
    val config: PracticeConfig = PracticeConfig(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val showButtons: Boolean = false,
    val showSillyMarker: Boolean = true,
)

object ConfigCodec {
    fun encode(config: PracticeConfig): String = JSONObject().apply {
        put("enabledCategories", JSONArray(
            config.enabledCategories.sortedBy { it.ordinal }.map { it.name },
        ))
        put("letters", config.letters)
        put("patterns", JSONArray(config.patterns.sorted()))
        put("mode", config.mode.name)
        config.selectedSubskills?.let { put("selectedSubskills", JSONArray(it.sorted())) }
        put("catalogueVersion", config.catalogueVersion)
    }.toString()

    fun decode(value: String): PracticeConfig = JSONObject(value).let { json ->
        val patterns = json.getJSONArray("patterns")
        val enabledCategories = if (json.has("enabledCategories")) {
            val categories = json.getJSONArray("enabledCategories")
            (0 until categories.length()).map { Category.valueOf(categories.getString(it)) }.toSet()
        } else {
            val weights = json.getJSONArray("weights")
            require(weights.length() == Category.entries.size) { "Legacy settings must contain five category weights." }
            Category.entries.filterIndexed { index, _ -> weights.getInt(index) > 0 }.toSet()
        }
        PracticeConfig(
            enabledCategories = enabledCategories,
            letters = json.getString("letters"),
            patterns = (0 until patterns.length()).map { patterns.getString(it) }.toSet(),
            mode = if (json.has("mode")) PracticeMode.valueOf(json.getString("mode")) else PracticeMode.WORDS,
            selectedSubskills = if (json.has("selectedSubskills")) json.getJSONArray("selectedSubskills").let { values ->
                (0 until values.length()).map { values.getString(it) }.toSet()
            } else null,
            catalogueVersion = json.optInt("catalogueVersion", 1),
        )
    }
}

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1,
    val config: String,
    val theme: String,
    val showButtons: Boolean,
    val showSillyMarker: Boolean,
) {
    fun toSettings() = AppSettings(ConfigCodec.decode(config), ThemeMode.valueOf(theme), showButtons, showSillyMarker)
    companion object {
        fun from(settings: AppSettings, profileId: Long = 1) = SettingsEntity(
            id = profileId,
            config = ConfigCodec.encode(settings.config), theme = settings.theme.name,
            showButtons = settings.showButtons, showSillyMarker = settings.showSillyMarker,
        )
    }
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1,
    val startedAt: Long,
    val endedAt: Long? = null,
    val config: String,
    val currentCardId: Long? = null,
    val remainingBag: String = "",
    val lastAttemptId: Long? = null,
    val contentVersion: Int = 1,
)

@Entity(tableName = "cards", foreignKeys = [ForeignKey(
    entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
)], indices = [Index("sessionId")])
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val word: String,
    val category: String,
    val patterns: String,
    val shownAt: Long,
    val contentVersion: Int = 1,
)

@Entity(tableName = "attempts", foreignKeys = [
    ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"]),
    ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"]),
], indices = [Index("cardId"), Index("sessionId")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val sessionId: Long,
    val direction: String,
    val success: Boolean,
    val respondedAt: Long,
    val voidedAt: Long? = null,
    val durationMs: Long? = null,
)

data class HistoryItem(
    val attemptId: Long,
    val sessionId: Long,
    val word: String,
    val category: String,
    val success: Boolean,
    val respondedAt: Long,
    val voidedAt: Long?,
    val durationMs: Long?,
)

@Entity(tableName = "stage_achievements", primaryKeys = ["profileId", "stageId", "catalogueVersion"])
data class StageAchievementEntity(
    val profileId: Long,
    val stageId: String,
    val catalogueVersion: Int,
    val earnedAt: Long,
)
