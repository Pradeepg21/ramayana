package com.satya.ramayana.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Button
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.Music
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Particles
import com.satya.ramayana.engine.Screen
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.Stages
import kotlin.math.hypot
import kotlin.math.sin

class SplashScreen(game: Game) : Screen(game) {
    private var done = false

    override fun update(dt: Float) {
        if (time > 3.2f) next()
    }

    private fun next() {
        if (done) return
        done = true
        game.go(MenuScreen(game))
    }

    override fun touch(action: Int, x: Float, y: Float) {
        if (action == Touch.DOWN && time > 0.8f) next()
    }

    override fun draw(c: Canvas) {
        c.drawRect(0f, 0f, V.W, V.H, P.vgrad(0f, V.H, 0xFF2A0508.toInt(), 0xFF120204.toInt()))
        val k = (time / 1.2f).coerceAtMost(1f)
        c.drawCircle(V.W / 2, 380f, 360f, P.glow(V.W / 2, 380f, 360f, 0xFFFF9800.toInt(), 0.6f * k))
        Ui.title(c, "ॐ", V.W / 2, 470f, 260f, k)
        Ui.text(c, "|| श्री राम जय राम जय जय राम ||", V.W / 2, 640f, 54f, Ui.GOLD, alpha = ((time - 0.8f) / 1f).coerceIn(0f, 1f))
        Ui.text(c, "presents", V.W / 2, 720f, 32f, Ui.CREAM, face = P.sans, alpha = ((time - 1.4f) / 1f).coerceIn(0f, 1f))
    }
}

class MenuScreen(game: Game) : Screen(game) {
    private val petals = Particles()
    private var confirm = false

    private val continueBtn = Button("", 1050f, 480f, 470f, 96f, Button.PRIMARY) { continueGame() }
    private val newBtn = Button("New Journey", 1050f, 600f, 470f, 90f, Button.GREEN) { newJourney() }
    private val mapBtn = Button("Select Stage", 1050f, 715f, 470f, 90f, Button.SECONDARY) { game.go(MapScreen(game)) }
    private val soundBtn = Button("", V.W - 110f, 60f, 180f, 70f, Button.SECONDARY) {
        game.toggleSound()
        updateLabels()
    }
    private val voiceBtn = Button("", V.W - 330f, 60f, 230f, 70f, Button.SECONDARY) {
        game.toggleVoice()
        updateLabels()
    }
    private val yesBtn = Button("Yes, start again", V.W / 2 - 200f, 600f, 360f, 90f, Button.PRIMARY) {
        game.save.reset()
        confirm = false
        game.go(StoryScreen(game, 1))
    }
    private val noBtn = Button("No", V.W / 2 + 200f, 600f, 280f, 90f, Button.SECONDARY) { confirm = false }

    init {
        buttons.addAll(listOf(continueBtn, newBtn, mapBtn, soundBtn, voiceBtn, yesBtn, noBtn))
        updateLabels()
    }

    private fun updateLabels() {
        val save = game.save
        continueBtn.visible = save.hasProgress
        val (s, l) = resumePoint()
        continueBtn.label = "Continue – Stage $s · Level $l"
        continueBtn.icon = "▶"
        newBtn.label = if (save.hasProgress) "New Journey" else "Start Journey"
        newBtn.y = if (save.hasProgress) 600f else 520f
        mapBtn.y = if (save.hasProgress) 715f else 640f
        soundBtn.label = if (save.soundOn) "♪ On" else "♪ Off"
        voiceBtn.label = if (save.voiceOn) "Voice: On" else "Voice: Off"
    }

    override fun enter() {
        game.audio.music(Music.BHAJAN)
    }

    override fun update(dt: Float) {
        yesBtn.visible = confirm
        noBtn.visible = confirm
        continueBtn.enabled = !confirm
        newBtn.enabled = !confirm
        mapBtn.enabled = !confirm
        if ((time * 10).toInt() % 4 == 0) petals.petals(V.W / 2, -20f, 1)
        petals.update(dt)
    }

    private fun continueGame() {
        val s = game.save
        val (st, l) = resumePoint()
        game.go(StoryScreen(game, st, l))
    }

    /** Where "Continue" picks up: the last stage played, if it is still open, else the next one to play. */
    private fun resumePoint(): Pair<Int, Int> {
        val s = game.save
        return when {
            s.lastStage in 1..Stages.COUNT && s.isUnlocked(s.lastStage, s.lastLevel) -> s.lastStage to s.lastLevel
            s.gameCompleted -> 1 to 1
            else -> s.frontier
        }
    }

    private fun newJourney() {
        if (game.save.hasProgress) confirm = true else game.go(StoryScreen(game, 1))
    }

    override fun onBack(): Boolean {
        if (confirm) {
            confirm = false
            return true
        }
        return false
    }

    override fun draw(c: Canvas) {
        Scenery.sky(c, 0xFF2B1B5A.toInt(), 0xFFC2477A.toInt(), 0xFFFFB46B.toInt())
        Scenery.stars(c, time, 50, 300f)
        Scenery.sun(c, 1050f, 300f, 90f, 0xFFFFC46B.toInt())
        Scenery.hills(c, 640f, 70f, 0xFF5B2E5E.toInt(), 1f, color2 = 0xFF3B1A40.toInt())
        Scenery.palace(c, 520f, 760f, 0.95f, time, 0.6f + 0.2f * sin(time * 2f))
        Scenery.ground(c, 760f, 0xFF6B3A2A.toInt(), 0xFF2E1610.toInt())
        for (k in 0 until 12) Ui.diya(c, 70f + k * 135f, 845f, 0.9f, time + k)

        // Shri Ram
        c.drawCircle(1440f, 420f, 280f, P.glow(1440f, 420f, 280f, 0xFFFFE082.toInt(), 0.35f))
        val p = Characters.pose()
        p.aim = -0.35f
        p.drawn = true
        Characters.person(c, 1400f, 805f, 1.45f, Characters.RAM, time, p)

        petals.draw(c)

        // title
        c.drawCircle(V.W / 2 - 280f, 200f, 260f, P.glow(V.W / 2 - 280f, 200f, 260f, 0xFFFFC107.toInt(), 0.35f))
        Ui.title(c, "रामायण", V.W / 2 - 280f, 230f, 170f)
        Ui.text(c, "RAMAYANA", V.W / 2 - 280f, 300f, 60f, Ui.CREAM, outline = Ui.MAROON_DARK)
        Ui.text(c, "The Journey of Shri Ram", V.W / 2 - 280f, 350f, 38f, Ui.GOLD, outline = Ui.MAROON_DARK)
        if (game.save.hasProgress) {
            Ui.text(c, "⭐ ${game.save.totalStars} / ${game.save.maxStars}", V.W / 2 - 280f, 410f, 34f, Color.WHITE, outline = Color.BLACK)
        }

        drawButtons(c)
        if (confirm) {
            c.drawRect(0f, 0f, V.W, V.H, P.f(0xAA000000.toInt()))
            Ui.panel(c, V.W / 2 - 440f, 300f, V.W / 2 + 440f, 690f)
            Ui.title(c, "Start a new journey?", V.W / 2, 410f, 64f)
            Ui.text(c, "Your saved stages and stars will be reset.", V.W / 2, 490f, 36f, Ui.CREAM, face = P.sans)
            yesBtn.draw(c, time)
            noBtn.draw(c, time)
        }
    }
}

/**
 * Stage map: a winding path of 10 medallions, like a classic level map, with a tab for
 * each level. Level 2 opens after all of Level 1, Level 3 after all of Level 2.
 */
class MapScreen(game: Game, level: Int = 0) : Screen(game) {
    private var level = if (level > 0) level else defaultLevel()
    private var lockedMsg = -1f

    private val path = Path()
    private val shortNames = arrayOf(
        "Gurukul", "Yagna", "Swayamvar", "Vanvas", "Find Sita",
        "Hanuman", "Sugriv & Bali", "Vanar Sena", "Ram Setu", "Ravan"
    )
    private fun pos(i: Int): Pair<Float, Float> = (140f + i * 147f) to (500f + sin(i * 1.15f) * 170f)

    private val levelBtns = (1..Stages.LEVELS).map { l ->
        Button("Level $l", V.W / 2 + (l - 2) * 260f, 178f, 240f, 64f, Button.PRIMARY) { pickLevel(l) }
    }

    init {
        buttons.add(Button("Menu", 130f, 70f, 200f, 76f, Button.SECONDARY, "◀") { game.go(MenuScreen(game)) })
        buttons.addAll(levelBtns)
        updateTabs()
    }

    /** The level being played now: the last one played if open, else the highest open level. */
    private fun defaultLevel(): Int {
        val s = game.save
        return if (s.lastLevel <= s.unlockedLevel) s.lastLevel else s.unlockedLevel
    }

    private fun pickLevel(l: Int) {
        if (l <= game.save.unlockedLevel) {
            level = l
            lockedMsg = -1f
            updateTabs()
        } else {
            game.audio.play(Sfx.WRONG)
            lockedMsg = time
        }
    }

    private fun updateTabs() {
        for ((i, b) in levelBtns.withIndex()) {
            val l = i + 1
            val open = l <= game.save.unlockedLevel
            b.style = when {
                l == level -> Button.GREEN
                open -> Button.PRIMARY
                else -> Button.GRAY
            }
            b.label = if (open) "Level $l" else "Level $l 🔒"
            b.textSize = 28f
        }
    }

    override fun enter() {
        game.audio.music(Music.BHAJAN)
    }

    override fun onBack(): Boolean {
        game.go(MenuScreen(game))
        return true
    }

    override fun touch(action: Int, x: Float, y: Float) {
        if (action != Touch.UP) return
        for (i in 0 until Stages.COUNT) {
            val (px, py) = pos(i)
            if (hypot(px - x, py - y) < 75f) {
                if (game.save.isUnlocked(i + 1, level)) {
                    game.audio.play(Sfx.CLICK)
                    game.go(StoryScreen(game, i + 1, level))
                } else {
                    game.audio.play(Sfx.WRONG)
                }
                return
            }
        }
    }

    override fun draw(c: Canvas) {
        c.drawRect(0f, 0f, V.W, V.H, P.vgrad(0f, V.H, 0xFF3A0610.toInt(), 0xFF1A0306.toInt()))
        Ui.parchment(c, 30f, 120f, V.W - 30f, V.H - 20f)
        Ui.title(c, "The Journey of Shri Ram", V.W / 2, 95f, 66f)
        val save = game.save
        Ui.text(c, "⭐ ${save.totalStars} / ${save.maxStars}", V.W - 160f, 80f, 38f, Color.WHITE, outline = Color.BLACK)
        if (lockedMsg >= 0f && time - lockedMsg < 2.5f) {
            val need = save.unlockedLevel
            Ui.text(c, "🔒 Finish all 10 stages of Level $need to open Level ${need + 1}", V.W / 2, 860f, 32f, Ui.RED, face = P.serifBold)
        } else if (save.hanumanShakti) {
            Ui.text(c, "🙏 Hanuman Shakti earned", V.W / 2, 860f, 32f, Ui.MAROON, face = P.serifBold)
        }

        // dotted trail
        path.reset()
        for (i in 0 until Stages.COUNT) {
            val (x, y) = pos(i)
            if (i == 0) path.moveTo(x, y) else {
                val (px, py) = pos(i - 1)
                path.quadTo((px + x) / 2, (py + y) / 2 + 60f, x, y)
            }
        }
        val line = P.s(0xFF9C6B2E.toInt(), 8f)
        line.pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 22f), -time * 30f)
        c.drawPath(path, line)
        line.pathEffect = null

        for (i in 0 until Stages.COUNT) {
            val n = i + 1
            val (x, y) = pos(i)
            val unlocked = save.isUnlocked(n, level)
            val current = save.isFrontier(n, level)
            if (current) {
                val pulse = 1f + 0.08f * sin(time * 5f)
                c.drawCircle(x, y, 110f * pulse, P.glow(x, y, 110f * pulse, 0xFFFF9800.toInt(), 0.9f))
            }
            c.drawCircle(x + 4f, y + 8f, 62f, P.f(0x55000000))
            if (unlocked) {
                c.drawCircle(x, y, 62f, P.vgrad(y - 62f, y + 62f, 0xFFFFC94D.toInt(), 0xFFD9480F.toInt()))
                c.drawCircle(x, y, 62f, P.s(Ui.MAROON, 5f))
                c.drawCircle(x, y, 50f, P.s(0xFFFFF3C4.toInt(), 2f))
                Ui.text(c, "$n", x, y + 22f, 64f, Color.WHITE, outline = Ui.MAROON_DARK)
                // stars earned on this level
                val st = save.stars(n, level)
                for (k in 0 until 3) Ui.star(c, x - 36f + k * 36f, y + 80f, 17f, k < st)
            } else {
                c.drawCircle(x, y, 62f, P.vgrad(y - 62f, y + 62f, 0xFF9E9E9E.toInt(), 0xFF5E5E5E.toInt()))
                c.drawCircle(x, y, 62f, P.s(0xFF424242.toInt(), 5f))
                // padlock
                Ui.roundRect(c, x - 22f, y - 6f, x + 22f, y + 28f, 6f, P.f(0xFF37474F.toInt()))
                c.drawArc(x - 15f, y - 34f, x + 15f, y + 4f, 180f, 180f, false, P.s(0xFF37474F.toInt(), 7f))
            }
            Ui.text(c, shortNames[i], x, y - 80f, 26f, if (unlocked) Ui.INK else 0xFF8D8D8D.toInt())
        }
        drawButtons(c)
    }
}
