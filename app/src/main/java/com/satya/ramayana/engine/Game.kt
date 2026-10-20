package com.satya.ramayana.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import com.satya.ramayana.game.SaveData
import com.satya.ramayana.game.Stages
import com.satya.ramayana.screens.DiwaliScreen
import com.satya.ramayana.screens.MapScreen
import com.satya.ramayana.screens.MenuScreen
import com.satya.ramayana.screens.ResultScreen
import com.satya.ramayana.screens.SplashScreen
import com.satya.ramayana.screens.StoryScreen
import com.satya.ramayana.screens.VivahScreen
import com.satya.ramayana.stages.BaseStage

class Game(context: Context) {

    val save = SaveData(context)
    val audio = AudioEngine().also {
        it.enabled = save.soundOn
        it.start()
    }
    val narrator = Narrator(context, audio).also { it.enabled = save.voiceOn }

    private var screen: Screen = SplashScreen(this)
    private var pending: Screen? = null
    private var fade = 1f          // 1 = fully black overlay
    private var fadingOut = false

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f

    init {
        screen.enter()
    }

    fun setViewport(w: Int, h: Int) {
        scale = minOf(w / V.W, h / V.H)
        offX = (w - V.W * scale) / 2f
        offY = (h - V.H * scale) / 2f
    }

    /** Switch to another screen with a short fade-through-black. */
    fun go(next: Screen) {
        pending = next
        fadingOut = true
    }

    fun update(dt: Float) {
        if (fadingOut) {
            fade += dt * 3.5f
            if (fade >= 1f) {
                fade = 1f
                fadingOut = false
                pending?.let {
                    narrator.stop()
                    screen.exit()
                    screen = it
                    pending = null
                    it.enter()
                }
            }
        } else if (fade > 0f) {
            fade = (fade - dt * 2.5f).coerceAtLeast(0f)
        }
        screen.time += dt
        screen.update(dt)
        (screen as? BaseStage)?.let {
            if (debugProgress > it.progress && it.state == BaseStage.State.PLAY) it.progress = debugProgress
        }
    }

    fun render(c: Canvas) {
        c.drawColor(Color.BLACK)
        c.save()
        c.translate(offX, offY)
        c.scale(scale, scale)
        c.clipRect(0f, 0f, V.W, V.H)
        screen.draw(c)
        if (fade > 0f) {
            c.drawColor(Color.argb((fade * 255).toInt(), 8, 4, 2))
        }
        c.restore()
    }

    fun touch(action: Int, rx: Float, ry: Float) {
        if (fadingOut) return
        val x = (rx - offX) / scale
        val y = (ry - offY) / scale
        screen.handleTouch(action, x, y)
    }

    fun onPause() {
        narrator.stop()
        screen.onPause()
    }

    fun release() {
        audio.stop()
        narrator.release()
    }

    fun onBack(): Boolean = if (fadingOut) true else screen.onBack()

    /** Debug only: fill a stage's progress so screenshots show a game in full swing. */
    var debugProgress = -1

    /** Jump straight to a screen; used for debugging/screenshots. */
    fun debugOpen(name: String) {
        // e.g. "stage7", "stage7_3" (stage 7, level 3), "story2_2", "win3_3"
        val nums = Regex("\\d+").findAll(name).map { it.value.toInt() }.toList()
        val n = nums.getOrElse(0) { 1 }
        val l = nums.getOrElse(1) { 1 }
        go(
            when {
                name.startsWith("stage") -> Stages.create(this, n, l)
                name.startsWith("story") -> StoryScreen(this, n, l)
                name == "map" -> MapScreen(this)
                name == "menu" -> MenuScreen(this)
                name.startsWith("vivah") -> VivahScreen(this, nums.getOrElse(0) { 1 }, 3, 1000)
                name.startsWith("diwali") -> DiwaliScreen(this, nums.getOrElse(0) { 3 }, 3, 5000)
                name.startsWith("win") -> ResultScreen(this, n, l, true, 3, 1200)
                name.startsWith("lose") -> ResultScreen(this, n, l, false, 0, 300)
                else -> MenuScreen(this)
            }
        )
    }

    fun toggleSound() {
        save.soundOn = !save.soundOn
        audio.enabled = save.soundOn
    }

    fun toggleVoice() {
        save.voiceOn = !save.voiceOn
        narrator.enabled = save.voiceOn
    }
}
