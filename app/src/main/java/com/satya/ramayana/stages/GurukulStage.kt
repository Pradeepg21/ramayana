package com.satya.ramayana.stages

import android.graphics.Canvas
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.game.Stages
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Stage 1 — young Ram learns archery at Guru Vashishtha's gurukul. */
class GurukulStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(1), level) {
    override val goal = goalFor(8)
    override val goalLabel = "Targets"

    private val quiver = Quiver(0.3f)
    private val ramX = 250f
    private val groundY = 800f
    private val ramScale = 1.35f
    private val pose = com.satya.ramayana.art.Pose()

    private class Target(var bx: Float, var by: Float, var phase: Float, var amp: Float, var horizontal: Boolean) {
        var x = bx
        var y = by
        var pop = 0f
        var visible = 1f
    }

    private val targets = ArrayList<Target>()

    init {
        repeat(3) { targets.add(newTarget(it)) }
    }

    private fun newTarget(slot: Int): Target {
        val bx = 820f + slot * 230f + Random.nextFloat() * 60f
        val by = 360f + Random.nextFloat() * 220f
        return Target(bx, by, Random.nextFloat() * 6f, 60f + Random.nextFloat() * 70f, Random.nextBoolean()).also { it.visible = 0f }
    }

    private fun speed() = 0.9f + progress * 0.18f

    override fun updateGame(dt: Float) {
        quiver.update(dt)
        for ((i, t) in targets.withIndex()) {
            t.phase += dt * speed()
            if (t.horizontal) {
                t.x = t.bx + sin(t.phase) * t.amp
                t.y = t.by
            } else {
                t.x = t.bx
                t.y = t.by + sin(t.phase) * t.amp
            }
            t.visible = (t.visible + dt * 3f).coerceAtMost(1f)
            if (t.visible >= 1f && quiver.hit(t.x, t.y, 58f) != null) {
                game.audio.play(Sfx.HIT)
                fx.burst(t.x, t.y, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt(), 0xFFE53935.toInt()), 34)
                addProgress(1, t.x, t.y - 60f)
                targets[i] = newTarget(i)
            }
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val s = ramScale * 0.8f
        val h = Characters.bowHand(ramX, groundY, s, kotlin.math.atan2(y - (groundY - 222f * s), x - ramX).coerceIn(-1.3f, 1.1f))
        if (quiver.shoot(h[0], h[1], x, y) != null) game.audio.play(Sfx.ARROW)
    }


    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF7EC8F2.toInt(), 0xFFBDE6F7.toInt(), 0xFFFFF1C9.toInt())
        Scenery.sun(c, 1350f, 140f, 60f)
        Scenery.clouds(c, time)
        Scenery.hills(c, 560f, 120f, 0xFF9CCC65.toInt(), 1f, color2 = 0xFF7CB342.toInt())
        Scenery.hills(c, 650f, 90f, 0xFF66BB6A.toInt(), 3f, color2 = 0xFF43A047.toInt())
        Scenery.tree(c, 120f, 640f, 1.2f, 0, time)
        Scenery.hut(c, 520f, 680f, 0.9f)
        Scenery.tree(c, 1500f, 700f, 1.1f, 3, time)
        Scenery.ground(c, 680f, 0xFF8BC34A.toInt(), 0xFF558B2F.toInt())
        Scenery.grass(c, 690f, 0f, 0xFF33691E.toInt())

        // Guru Vashishtha watching
        val g = Characters.pose()
        g.namaste = true
        Characters.person(c, 600f, 760f, 1.0f, Characters.SAGE, time, g, facing = 1f)
        // young Lakshman cheering
        val l = Characters.pose()
        l.armsUp = if (progress > 0 && ((time * 2).toInt() % 2 == 0)) 0.6f else 0f
        Characters.person(c, 105f, 810f, 1.2f, Characters.LAKSHMAN, time, l, child = true)

        // targets on wooden stands
        for (t in targets) {
            val a = t.visible
            c.drawLine(t.x, t.y + 50f, t.x, 720f, P.s(0xFF6D4C41.toInt(), 8f, a))
            c.drawLine(t.x - 30f, 720f, t.x + 30f, 720f, P.s(0xFF5D4037.toInt(), 8f, a))
            val cols = intArrayOf(0xFFFFFFFF.toInt(), 0xFFD32F2F.toInt(), 0xFFFFFFFF.toInt(), 0xFFD32F2F.toInt(), Ui.GOLD)
            for (k in cols.indices) c.drawCircle(t.x, t.y, (55f - k * 11f) * a, P.f(cols[k]))
            c.drawCircle(t.x, t.y, 55f * a, P.s(0xFF5D4037.toInt(), 4f))
        }

        // young Ram with his bow
        pose.reset()
        pose.aim = quiver.aim.coerceIn(-1.3f, 1.1f)
        pose.drawn = quiver.canShoot()
        Characters.person(c, ramX, groundY, ramScale, Characters.RAM, time, pose, child = true)
        quiver.draw(c)

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap on a target to shoot!", 1000f, 200f, 48f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }
}
