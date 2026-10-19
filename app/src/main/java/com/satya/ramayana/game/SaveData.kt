package com.satya.ramayana.game

import android.content.Context

/**
 * Persistent progress, like the stage continues of classic arcade games.
 * Every stage has 3 levels played strictly in order: Level 1 unlocks Level 2,
 * Level 3 unlocks the next stage. Progress is stored as one linear index
 * (stage 1 level 1 = 0 … stage 10 level 3 = 29).
 */
class SaveData(context: Context) {
    private val prefs = context.getSharedPreferences("ramayana_save", Context.MODE_PRIVATE)

    private val maxIndex = Stages.COUNT * Stages.LEVELS - 1

    fun index(stage: Int, level: Int) = (stage - 1) * Stages.LEVELS + (level - 1)

    /** Highest playable level as a linear index. Older saves only knew stages, so they map to level 1. */
    private var unlockedIndex: Int
        get() = prefs.getInt("uidx", (prefs.getInt("unlocked", 1) - 1) * Stages.LEVELS).coerceIn(0, maxIndex)
        set(v) = prefs.edit().putInt("uidx", v.coerceIn(0, maxIndex)).apply()

    /** Highest stage the player may enter (1..Stages.COUNT). */
    val unlocked: Int get() = unlockedIndex / Stages.LEVELS + 1

    fun isUnlocked(stage: Int, level: Int) = index(stage, level) <= unlockedIndex

    /** Highest playable level inside a stage (1..3). */
    fun unlockedLevel(stage: Int) = when {
        stage < unlocked -> Stages.LEVELS
        stage == unlocked -> unlockedIndex % Stages.LEVELS + 1
        else -> 1
    }

    /** Stage and level the player was last playing; "Continue" resumes here. */
    var lastStage: Int
        get() = prefs.getInt("last", 0)
        set(v) = prefs.edit().putInt("last", v).apply()

    var lastLevel: Int
        get() = prefs.getInt("last_level", 1).coerceIn(1, Stages.LEVELS)
        set(v) = prefs.edit().putInt("last_level", v).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.edit().putBoolean("sound", v).apply()

    /** Story narration (text-to-speech). */
    var voiceOn: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) = prefs.edit().putBoolean("voice", v).apply()

    var gameCompleted: Boolean
        get() = prefs.getBoolean("completed", false)
        set(v) = prefs.edit().putBoolean("completed", v).apply()

    val hasProgress: Boolean get() = lastStage > 0 || unlockedIndex > 0

    /** Hanuman's blessing is earned by completing the first level of "Meeting Hanuman". */
    val hanumanShakti: Boolean get() = stars(Stages.HANUMAN_STAGE, 1) > 0

    fun stars(stage: Int, level: Int): Int =
        prefs.getInt("stars_${stage}_$level", if (level == 1) prefs.getInt("stars_$stage", 0) else 0)

    fun levelDone(stage: Int, level: Int) = stars(stage, level) > 0

    fun bestScore(stage: Int, level: Int) = prefs.getInt("score_${stage}_$level", 0)

    val totalStars: Int
        get() = (1..Stages.COUNT).sumOf { s -> (1..Stages.LEVELS).sumOf { l -> stars(s, l) } }

    val maxStars get() = Stages.COUNT * Stages.LEVELS * 3

    fun completeLevel(stage: Int, level: Int, stars: Int, score: Int) {
        val e = prefs.edit()
        if (stars > stars(stage, level)) e.putInt("stars_${stage}_$level", stars)
        if (score > bestScore(stage, level)) e.putInt("score_${stage}_$level", score)
        val idx = index(stage, level)
        if (idx < maxIndex) {
            if (idx + 1 > unlockedIndex) e.putInt("uidx", idx + 1)
            val next = idx + 1
            e.putInt("last", next / Stages.LEVELS + 1)
            e.putInt("last_level", next % Stages.LEVELS + 1)
        } else {
            e.putBoolean("completed", true)
            e.putInt("last", Stages.COUNT)
            e.putInt("last_level", Stages.LEVELS)
        }
        e.apply()
    }

    fun reset() {
        val sound = soundOn
        prefs.edit().clear().putBoolean("sound", sound).apply()
    }
}
