package com.satya.ramayana.stages

import android.graphics.Canvas
import android.graphics.RectF
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Pose
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.Stages
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Stage 5 — catch the jewels Sita dropped from the Pushpak Viman. */
class SitaSearchStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(5), level) {
    override val goal = goalFor(12)
    override val goalLabel = "Jewels"

    private val groundY = 845f
    private var ramX = V.W / 2
    private var targetX = V.W / 2
    private var facing = 1f
    private val pose = Pose()
    private val rect = RectF()

    private class Item(var x: Float, var y: Float, val kind: Int, val vy: Float) {
        var rot = Random.nextFloat() * 360f
    }

    private val items = ArrayList<Item>()
    private var spawn = 0.8f

    private fun viman() = V.W / 2 + sin(time * 0.45f) * 620f

    override fun updateGame(dt: Float) {
        // follow the finger smoothly
        val dx = targetX - ramX
        if (abs(dx) > 4f) facing = if (dx > 0) 1f else -1f
        ramX += dx * (dt * 9f).coerceAtMost(1f)

        spawn -= dt
        if (spawn <= 0f) {
            spawn = (1.0f - playTime * 0.012f).coerceAtLeast(0.45f)
            val r = Random.nextFloat()
            val kind = when {
                r < 0.55f -> JEWEL
                r < 0.72f -> BERRY
                else -> THORN
            }
            val sp = 230f + playTime * 5f + Random.nextFloat() * 80f
            items.add(Item(viman() + Random.nextFloat() * 120f - 60f, 170f, kind, sp))
        }

        val it = items.iterator()
        while (it.hasNext()) {
            val i = it.next()
            i.y += i.vy * dt
            i.rot += 120f * dt
            val catchY = groundY - 215f
            if (i.y > catchY - 40f && i.y < catchY + 40f && abs(i.x - ramX) < 85f) {
                when (i.kind) {
                    JEWEL -> {
                        game.audio.play(Sfx.COLLECT)
                        fx.sparkle(i.x, i.y, Ui.GOLD, 16)
                        addProgress(1, i.x, i.y - 60f)
                    }
                    BERRY -> {
                        game.audio.play(Sfx.COLLECT)
                        fx.sparkle(i.x, i.y, 0xFFE040FB.toInt(), 10)
                        addTime(2f, i.x, i.y - 60f)
                    }
                    else -> penalty(3f, i.x, i.y - 60f, "Thorns!")
                }
                it.remove()
                continue
            }
            if (i.y > V.H + 40f) it.remove()
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        targetX = x.coerceIn(80f, V.W - 80f)
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF3E6FB0.toInt(), 0xFF9AC3E6.toInt(), 0xFFF3E1B6.toInt())
        Scenery.clouds(c, time, 0.7f)
        Scenery.mountain(c, 300f, 700f, 700f, 330f, 0xFF6D7F8A.toInt())
        Scenery.mountain(c, 1250f, 700f, 800f, 380f, 0xFF5E6F7A.toInt())
        Scenery.hills(c, 700f, 80f, 0xFF6B9A54.toInt(), 4f, color2 = 0xFF4C7A3A.toInt())
        Scenery.tree(c, 100f, 760f, 1.1f, 0, time)
        Scenery.tree(c, 1500f, 760f, 1.2f, 2, time)
        Scenery.hut(c, 1320f, 740f, 0.6f)
        Scenery.ground(c, 740f, 0xFF9A8A55.toInt(), 0xFF6A5C33.toInt())
        Scenery.grass(c, 750f, 0f, 0xFF4E7A2A.toInt())

        // Pushpak Viman far above
        Scenery.pushpak(c, viman(), 150f, 0.35f, time)

        for (i in items) drawItem(c, i)

        // Lakshman watching
        val lp = Characters.pose()
        Characters.person(c, 240f, groundY - 20f, 0.95f, Characters.LAKSHMAN, time, lp)

        pose.reset()
        pose.catchPose = true
        pose.walk = if (abs(targetX - ramX) > 10f) time * 10f else Float.NaN
        Characters.person(c, ramX, groundY, 1.25f, Characters.RAM, time, pose, facing)
        // woven basket held out to catch the jewels
        val bx = ramX + 45f * facing
        val by = groundY - 200f
        val basket = android.graphics.Path()
        basket.moveTo(bx - 70f, by - 20f)
        basket.lineTo(bx + 70f, by - 20f)
        basket.quadTo(bx + 62f, by + 40f, bx, by + 42f)
        basket.quadTo(bx - 62f, by + 40f, bx - 70f, by - 20f)
        basket.close()
        c.drawPath(basket, P.vgrad(by - 20f, by + 42f, 0xFFD7A15A.toInt(), 0xFF8D5A24.toInt()))
        for (k in 1..3) c.drawLine(bx - 66f + k * 4f, by - 20f + k * 15f, bx + 66f - k * 4f, by - 20f + k * 15f, P.s(0xFF6D4219.toInt(), 2.5f))
        rect.set(bx - 72f, by - 30f, bx + 72f, by - 10f)
        c.drawOval(rect, P.f(0xFF5D3A16.toInt()))
        c.drawOval(rect, P.s(0xFFE0B26A.toInt(), 4f))

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Drag left and right to catch the jewels!", V.W / 2, 300f, 48f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }

    private fun drawItem(c: Canvas, i: Item) {
        c.save()
        c.translate(i.x, i.y)
        when (i.kind) {
            JEWEL -> {
                c.drawCircle(0f, 0f, 40f, P.glow(0f, 0f, 40f, 0xFFFFF59D.toInt(), 0.8f))
                c.rotate(i.rot * 0.3f)
                c.drawCircle(0f, 0f, 20f, P.s(Ui.GOLD, 7f))
                c.drawCircle(0f, -20f, 9f, P.f(0xFFE53935.toInt()))
                c.drawCircle(17f, 10f, 6f, P.f(0xFF00C853.toInt()))
                c.drawCircle(-17f, 10f, 6f, P.f(0xFF2979FF.toInt()))
                c.drawCircle(-2f, -22f, 3f, P.f(0xCCFFFFFF.toInt()))
            }
            BERRY -> {
                for ((k, off) in floatArrayOf(-10f, 10f, 0f).withIndex()) c.drawCircle(off, if (k == 2) -12f else 0f, 12f, P.f(0xFF8E24AA.toInt()))
                c.drawCircle(-4f, -3f, 4f, P.f(0x66FFFFFF))
                c.drawLine(0f, -22f, 6f, -32f, P.s(0xFF2E7D32.toInt(), 3f))
            }
            else -> {
                c.rotate(i.rot)
                c.drawLine(-40f, 0f, 40f, 0f, P.s(0xFF4E342E.toInt(), 7f))
                for (k in -3..3) {
                    c.drawLine(k * 11f, 0f, k * 11f + 6f, -14f, P.s(0xFF3E2723.toInt(), 3f))
                    c.drawLine(k * 11f + 4f, 0f, k * 11f - 2f, 14f, P.s(0xFF3E2723.toInt(), 3f))
                }
            }
        }
        c.restore()
    }

    companion object {
        const val JEWEL = 0
        const val BERRY = 1
        const val THORN = 2
    }
}
