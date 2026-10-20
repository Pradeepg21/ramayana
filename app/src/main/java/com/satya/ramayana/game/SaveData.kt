package com.satya.ramayana.game

import android.content.Context

/**
 * Persistent progress, like the stage continues of classic arcade games.
 * The journey is played level by level: all 10 stages of Level 1 in order, then all
 * 10 stages of Level 2, then Level 3. Progress is stored as one linear index
 * (Level 1 stage 1 = 0 … Level 3 stage 10 = 29). Once everything is finished, all
 * levels stay open, even after "New Journey", until the app is reinstalled.
 */
class SaveData(context: Context) {
    private val prefs = context.getSharedPreferences("ramayana_save", Context.MODE_PRIVATE)

    private val maxIndex = Stages.COUNT * Stages.LEVELS - 1

    fun index(stage: Int, level: Int) = (level - 1) * Stages.COUNT + (stage - 1)
    private fun stageOf(i: Int) = i % Stages.COUNT + 1
    private fun levelOf(i: Int) = i / Stages.COUNT + 1

    /** Furthest playable point as a linear index. */
    private var unlockedIndex: Int
        get() = prefs.getInt("uidx2", -1).let { if (it >= 0) it else migrate() }.coerceIn(0, maxIndex)
        set(v) = prefs.edit().putInt("uidx2", v.coerceIn(0, maxIndex)).apply()

    /**
     * Older saves counted stage by stage (Stage 1 levels 1-3, then Stage 2…). Keep every
     * stage they had reached open on Level 1, and anything further they had already finished.
     */
    private fun migrate(): Int {
        val oldStage = prefs.getInt("uidx", (prefs.getInt("unlocked", 1) - 1) * Stages.LEVELS) / Stages.LEVELS + 1
        var i = 0
        while (i < maxIndex && levelDone(stageOf(i), levelOf(i))) i++
        return maxOf(i, oldStage - 1).coerceAtMost(maxIndex).also { unlockedIndex = it }
    }

    fun isUnlocked(stage: Int, level: Int) = index(stage, level) <= unlockedIndex

    /** Highest open level (1..3): Level 2 opens when all of Level 1 is done, and so on. */
    val unlockedLevel: Int get() = levelOf(unlockedIndex)

    /** The next stage and level to play. */
    val frontier: Pair<Int, Int> get() = unlockedIndex.let { stageOf(it) to levelOf(it) }

    fun isFrontier(stage: Int, level: Int) = !gameCompleted && index(stage, level) == unlockedIndex

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

    /** Hanuman's blessing is earned by completing "Meeting Hanuman" on Level 1. */
    val hanumanShakti: Boolean get() = stars(Stages.HANUMAN_STAGE, 1) > 0

    fun stars(stage: Int, level: Int): Int =
        prefs.getInt("stars_${stage}_$level", if (level == 1) prefs.getInt("stars_$stage", 0) else 0)

    fun levelDone(stage: Int, level: Int) = stars(stage, level) > 0

    fun bestScore(stage: Int, level: Int) = prefs.getInt("score_${stage}_$level", 0)

    val totalStars: Int
        get() = (1..Stages.COUNT).sumOf { s -> (1..Stages.LEVELS).sumOf { l -> stars(s, l) } }

    val maxStars get() = Stages.COUNT * Stages.LEVELS * 3

    fun completeLevel(stage: Int, level: Int, stars: Int, score: Int) {
        val idx = index(stage, level)
        if (idx >= unlockedIndex) unlockedIndex = minOf(idx + 1, maxIndex)
        val e = prefs.edit()
        if (stars > stars(stage, level)) e.putInt("stars_${stage}_$level", stars)
        if (score > bestScore(stage, level)) e.putInt("score_${stage}_$level", score)
        if (idx < maxIndex) {
            val next = idx + 1
            e.putInt("last", stageOf(next))
            e.putInt("last_level", levelOf(next))
        } else {
            e.putBoolean("completed", true)
            e.putInt("last", Stages.COUNT)
            e.putInt("last_level", Stages.LEVELS)
        }
        e.apply()
    }

    /** "New Journey": clears stars and scores. After a full completion every level stays open. */
    fun reset() {
        val sound = soundOn
        val voice = voiceOn
        val completed = gameCompleted
        val e = prefs.edit().clear().putBoolean("sound", sound).putBoolean("voice", voice)
        if (completed) e.putBoolean("completed", true).putInt("uidx2", maxIndex)
        e.apply()
    }
}
