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
import kotlin.math.sin
import kotlin.random.Random

/** Stage 6 — fill Hanuman's devotion by tapping lotuses carrying Ram's name. */
class HanumanStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(6), level) {
    override val goal = goalFor(15)
    override val goalLabel = "Devotion"

    private val spots = arrayOf(
        floatArrayOf(240f, 600f), floatArrayOf(470f, 680f), floatArrayOf(720f, 620f), floatArrayOf(980f, 690f),
        floatArrayOf(1230f, 610f), floatArrayOf(1440f, 700f), floatArrayOf(350f, 810f), floatArrayOf(620f, 830f),
        floatArrayOf(900f, 820f), floatArrayOf(1170f, 830f), floatArrayOf(1400f, 840f)
    )

    private class Bloom(val spot: Int, val ram: Boolean, val life: Float) {
        var age = 0f
        var gone = false
    }

    private val blooms = ArrayList<Bloom>()
    private var spawn = 0.4f
    private val flyers = ArrayList<FloatArray>() // x, y, t

    override fun updateGame(dt: Float) {
        spawn -= dt
        val maxActive = if (progress < 6) 2 else 3
        if (spawn <= 0f && blooms.size < maxActive) {
            spawn = 0.35f + Random.nextFloat() * 0.4f
            val free = spots.indices.filter { s -> blooms.none { it.spot == s } }
            if (free.isNotEmpty()) {
                val life = (1.7f - progress * 0.05f).coerceAtLeast(0.95f)
                blooms.add(Bloom(free.random(), Random.nextFloat() < 0.7f, life))
            }
        }
        val it = blooms.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            if (b.age > b.life || b.gone) it.remove()
        }
        val fi = flyers.iterator()
        while (fi.hasNext()) {
            val f = fi.next()
            f[2] += dt * 1.6f
            if (f[2] >= 1f) fi.remove()
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val b = blooms.firstOrNull { !it.gone && hypot(spots[it.spot][0] - x, spots[it.spot][1] - 30f - y) < 85f } ?: return
        b.gone = true
        val sx = spots[b.spot][0]
        val sy = spots[b.spot][1]
        if (b.ram) {
            game.audio.play(Sfx.BELL)
            fx.sparkle(sx, sy - 30f, Ui.GOLD, 16)
            addProgress(1, sx, sy - 90f)
            flyers.add(floatArrayOf(sx, sy - 30f, 0f))
        } else {
            penalty(2f, sx, sy - 90f, "No Ram naam!")
        }
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFFFFB74D.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFF8E1.toInt())
        Scenery.sun(c, 800f, 260f, 90f, 0xFFFFF59D.toInt())
        Scenery.mountain(c, 800f, 520f, 1000f, 380f, 0xFF8D6E63.toInt())
        Scenery.mountain(c, 200f, 540f, 600f, 250f, 0xFF795548.toInt())
        Scenery.mountain(c, 1400f, 540f, 600f, 280f, 0xFF6D4C41.toInt())
        Scenery.hills(c, 540f, 40f, 0xFF7CB342.toInt(), 1f, color2 = 0xFF558B2F.toInt())
        // pond
        c.drawRect(0f, 540f, V.W, V.H, P.vgrad(540f, V.H, 0xFF4FC3F7.toInt(), 0xFF0277BD.toInt()))
        for (k in 0 until 7) {
            val y = 570f + k * 48f
            c.drawLine(0f, y, V.W, y + sin(time + k) * 3f, P.s(0x33FFFFFF, 2f))
        }
        // lily pads
        for (s in spots) {
            c.drawOval(s[0] - 70f, s[1] - 14f, s[0] + 70f, s[1] + 22f, P.f(0xFF388E3C.toInt()))
            c.drawLine(s[0], s[1] + 4f, s[0] + 50f, s[1] - 6f, P.s(0xFF1B5E20.toInt(), 3f))
        }

        // Hanuman on the rock, with Ram & Lakshman greeting
        Ui.roundRect(c, 650f, 440f, 950f, 560f, 60f, P.vgrad(440f, 560f, 0xFFA1887F.toInt(), 0xFF5D4037.toInt()))
        val glow = progress / goal.toFloat()
        c.drawCircle(800f, 330f, 140f + 120f * glow, P.glow(800f, 330f, 140f + 120f * glow, 0xFFFFB300.toInt(), 0.3f + 0.6f * glow))
        Characters.vanar(c, 800f, 480f, 1.3f, Characters.HANUMAN, time, armsUp = if (progress >= goal) 1f else 0f)
        val rp = Characters.pose(); rp.namaste = true
        Characters.person(c, 560f, 520f, 0.82f, Characters.RAM, time, rp)
        Characters.person(c, 455f, 525f, 0.76f, Characters.LAKSHMAN, time)

        // devotion meter
        val mx = 1000f
        Ui.roundRect(c, mx, 230f, mx + 40f, 480f, 20f, P.f(0x88000000.toInt()))
        val h = 242f * glow
        if (h > 0f) Ui.roundRect(c, mx + 4f, 476f - h, mx + 36f, 476f, 16f, P.vgrad(234f, 476f, 0xFFFFF59D.toInt(), 0xFFFF6F00.toInt()))
        Ui.roundRect(c, mx, 230f, mx + 40f, 480f, 20f, P.s(Ui.GOLD, 3f))
        Ui.text(c, "भक्ति", mx + 20f, 220f, 34f, Ui.MAROON, outline = 0xFFFFFFFF.toInt())

        for (b in blooms) {
            val s = spots[b.spot]
            val k = when {
                b.age < 0.2f -> b.age / 0.2f
                b.age > b.life - 0.2f -> (b.life - b.age) / 0.2f
                else -> 1f
            }.coerceIn(0f, 1f)
            if (b.ram) c.drawCircle(s[0], s[1] - 30f, 90f * k, P.glow(s[0], s[1] - 30f, 90f * k, 0xFFFFF176.toInt(), 0.8f))
            Scenery.lotus(c, s[0], s[1], 1.3f * k, if (b.ram) 0xFFFF4081.toInt() else 0xFFF8BBD0.toInt())
            if (b.ram) Ui.text(c, "राम", s[0], s[1] - 32f, 42f * k, 0xFFB71C1C.toInt(), outline = 0xFFFFFFFF.toInt())
        }
        for (f in flyers) {
            val t = f[2]
            val x = f[0] + (800f - f[0]) * t
            val y = f[1] + (330f - f[1]) * t - sin(t * 3.14f) * 120f
            Ui.text(c, "राम", x, y, 36f, 0xFFFFD54F.toInt(), outline = 0xFFB71C1C.toInt())
        }

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap only lotuses with राम!", V.W / 2, 160f, 52f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }
}
