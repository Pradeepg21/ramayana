package com.satya.ramayana.stages

import android.graphics.Canvas
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.Stages
import kotlin.math.hypot
import kotlin.random.Random

/** Stage 8 — recruit the vanar and bear army. */
class VanarSenaStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(8), level) {
    override val goal = goalFor(25)
    override val goalLabel = "Warriors"

    private class Leaper(var x: Float, var y: Float, var vx: Float, var vy: Float, val style: Characters.VanarStyle, val worth: Int) {
        var gone = false
    }

    private val leapers = ArrayList<Leaper>()
    private var spawn = 0.3f
    private val armyStyles = ArrayList<Characters.VanarStyle>()

    override fun updateGame(dt: Float) {
        spawn -= dt
        if (spawn <= 0f) {
            spawn = (0.85f - playTime * 0.008f).coerceAtLeast(0.42f)
            val fromLeft = Random.nextBoolean()
            val r = Random.nextFloat()
            val (style, worth) = when {
                r < 0.06f -> Characters.JAMBAVAN to 3
                r < 0.24f + playTime * 0.003f -> Characters.SPY to -1
                r < 0.45f -> Characters.BEAR to 1
                r < 0.72f -> Characters.VANAR to 1
                else -> Characters.VANAR2 to 1
            }
            val vx = (300f + Random.nextFloat() * 160f + playTime * 3f) * (if (fromLeft) 1f else -1f)
            val startX = if (fromLeft) 560f else V.W + 60f
            leapers.add(Leaper(startX, 760f, vx, -(820f + Random.nextFloat() * 260f), style, worth))
        }
        val it = leapers.iterator()
        while (it.hasNext()) {
            val l = it.next()
            l.vy += 1150f * dt
            l.x += l.vx * dt
            l.y += l.vy * dt
            if (l.gone || l.y > V.H + 250f || l.x < 400f || l.x > V.W + 150f) it.remove()
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val l = leapers.lastOrNull { !it.gone && hypot(it.x - x, it.y - 160f - y) < 120f } ?: return
        l.gone = true
        if (l.worth < 0) {
            fx.burst(l.x, l.y - 160f, intArrayOf(0xFF9C27B0.toInt(), 0xFF4A148C.toInt()), 24)
            penalty(3f, l.x, l.y - 300f, "A spy of Ravan!")
        } else {
            game.audio.play(if (l.worth > 1) Sfx.BELL else Sfx.COLLECT)
            fx.sparkle(l.x, l.y - 160f, Ui.GOLD, 14)
            if (l.worth > 1) float("Jambavan joins!", l.x, l.y - 350f, 0xFFFFFFFF.toInt())
            repeat(l.worth) { armyStyles.add(l.style) }
            addProgress(l.worth, l.x, l.y - 300f)
        }
    }

    override fun onHanumanPower() {
        // Hanuman calls his friends: 4 warriors join at once
        repeat(4) { armyStyles.add(if (it % 2 == 0) Characters.VANAR else Characters.BEAR) }
        addProgress(4, V.W / 2, 300f)
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF64B5F6.toInt(), 0xFFB3E5FC.toInt(), 0xFFFFF3D6.toInt())
        Scenery.sun(c, 1400f, 140f, 55f)
        Scenery.clouds(c, time)
        Scenery.mountain(c, 1000f, 620f, 1200f, 400f, 0xFF8D7B68.toInt())
        Scenery.hills(c, 620f, 90f, 0xFF7CB342.toInt(), 5f, color2 = 0xFF558B2F.toInt())
        for (k in 0 until 4) Scenery.tree(c, 650f + k * 300f, 720f, 1.1f, k % 3, time)
        Scenery.ground(c, 700f, 0xFF9CCC65.toInt(), 0xFF689F38.toInt())
        Scenery.grass(c, 710f, 0f, 0xFF33691E.toInt())

        // assembly area with Ram, Lakshman and King Sugriv
        Ui.roundRect(c, -40f, 640f, 520f, 920f, 40f, P.f(0x33FFFFFF))
        Characters.person(c, 110f, 732f, 0.85f, Characters.RAM, time)
        Characters.person(c, 215f, 734f, 0.8f, Characters.LAKSHMAN, time)
        Characters.vanar(c, 330f, 734f, 0.85f, Characters.SUGRIV, time, -1f)
        Characters.vanar(c, 440f, 736f, 0.85f, Characters.HANUMAN, time, -1f)
        // recruited army in neat rows
        for ((i, st) in armyStyles.withIndex()) {
            if (i >= 30) break
            val row = i / 8
            val col = i % 8
            Characters.vanar(c, 50f + col * 58f + (row % 2) * 24f, 800f + row * 32f, 0.38f, st, time + i)
        }
        // flag of the army
        Scenery.flag(c, 470f, 640f, 1.4f, time)

        for (l in leapers) {
            Characters.vanar(c, l.x, l.y, 1.05f, l.style, time, if (l.vx > 0) 1f else -1f, jump = true)
        }

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap the vanars & bears – not the purple spies!", 1000f, 200f, 44f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }
}
