package com.satya.ramayana.stages

import android.graphics.Canvas
import android.graphics.Path
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
import kotlin.random.Random

/** Stage 4 — the walk into exile: jump over obstacles and reach Panchavati. */
class ForestStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(4), level) {
    override val goal = 100
    override val goalLabel = "Journey"

    private val groundY = 790f
    private val ramX = 470f
    private val target = 11000f
    private var dist = 0f
    private var slow = 0f

    private class Jumper(val x: Float) {
        var y = 0f
        var vy = 0f
        var air = false
        var walk = 0f
        fun jump(power: Float = 1150f) {
            if (!air) { vy = -power; air = true }
        }
        fun update(dt: Float) {
            if (air) {
                vy += 2600f * dt
                y += vy * dt
                if (y >= 0f) { y = 0f; vy = 0f; air = false }
            }
        }
    }

    private val ram = Jumper(ramX)
    private val sita = Jumper(310f)
    private val lakshman = Jumper(160f)

    private class Obstacle(var x: Float, val kind: Int) {
        var hit = false
        val w get() = when (kind) { 0 -> 90f; 1 -> 110f; else -> 150f }
        val h get() = when (kind) { 0 -> 70f; 1 -> 80f; else -> 50f }
    }

    private class Fruit(var x: Float, val y: Float, val kind: Int) {
        var taken = false
    }

    private val obstacles = ArrayList<Obstacle>()
    private val fruits = ArrayList<Fruit>()
    private var nextObstacle = 900f
    private val ramPose = Pose()
    private val path = Path()

    private fun speed() = (340f + playTime * 5f).coerceAtMost(560f) * (if (slow > 0f) 0.4f else 1f)

    override fun updateGame(dt: Float) {
        val v = speed()
        dist += v * dt
        progress = ((dist / target) * 100f).toInt().coerceAtMost(100)
        slow -= dt

        ram.update(dt); sita.update(dt); lakshman.update(dt)
        ram.walk += dt * v / 40f
        sita.walk += dt * v / 40f
        lakshman.walk += dt * v / 40f

        // spawn obstacles ahead (none near the finish)
        nextObstacle -= v * dt
        if (nextObstacle <= 0f && dist < target - 1400f) {
            obstacles.add(Obstacle(V.W + 100f, Random.nextInt(3)))
            val gap = (720f - playTime * 6f).coerceAtLeast(430f)
            nextObstacle = gap + Random.nextFloat() * 380f
            if (Random.nextFloat() < 0.6f) {
                fruits.add(Fruit(V.W + 100f + 200f + Random.nextFloat() * 120f, groundY - 140f - Random.nextFloat() * 140f, Random.nextInt(3)))
            }
        }

        val it = obstacles.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o.x -= v * dt
            // followers hop automatically
            if (o.x - sita.x in 40f..90f) sita.jump()
            if (o.x - lakshman.x in 40f..90f) lakshman.jump()
            if (!o.hit && o.x - o.w / 2 < ramX + 22f && o.x + o.w / 2 > ramX - 22f && -ram.y < o.h - 10f) {
                o.hit = true
                slow = 1.1f
                game.audio.play(Sfx.WRONG)
                float("Ouch! Careful!", ramX, groundY - 330f, 0xFFFFCDD2.toInt())
            }
            if (o.x < -200f) it.remove()
        }
        val fi = fruits.iterator()
        while (fi.hasNext()) {
            val f = fi.next()
            f.x -= v * dt
            val ry = groundY + ram.y - 200f
            if (!f.taken && kotlin.math.abs(f.x - ramX) < 60f && kotlin.math.abs(f.y - ry) < 170f) {
                f.taken = true
                game.audio.play(Sfx.COLLECT)
                fx.sparkle(f.x, f.y, Ui.GOLD, 12)
                addTime(1f, f.x, f.y - 40f)
                score += 50
            }
            if (f.x < -100f || f.taken) fi.remove()
        }
        if (progress >= 100) progress = goal
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action == Touch.DOWN && !ram.air) {
            ram.jump()
            game.audio.play(Sfx.JUMP)
        }
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF87CEEB.toInt(), 0xFFB9E4F5.toInt(), 0xFFE8F5D0.toInt())
        Scenery.sun(c, 1300f, 130f, 55f)
        Scenery.clouds(c, time + dist * 0.02f)
        Scenery.hills(c, 520f, 140f, 0xFF7FB77E.toInt(), 0.5f, dist * 0.15f, 0xFF5E9A5D.toInt())
        Scenery.hills(c, 610f, 100f, 0xFF4E8A4A.toInt(), 2.5f, dist * 0.3f, 0xFF3B6E38.toInt())

        // mid-layer trees
        val spacing = 330f
        val off = (dist * 0.55f) % spacing
        for (i in -1..6) {
            val tx = i * spacing - off
            val idx = ((dist * 0.55f) / spacing).toInt() + i
            Scenery.tree(c, tx, 700f, 0.9f + (idx % 3) * 0.12f, if (idx % 4 == 0) 1 else (idx % 3), time)
        }
        Scenery.ground(c, 700f, 0xFF8D7B4A.toInt(), 0xFF5D4E2C.toInt())
        // path
        c.drawRect(0f, 760f, V.W, 830f, P.vgrad(760f, 830f, 0xFFC9A877.toInt(), 0xFFA88958.toInt()))
        Scenery.grass(c, 760f, dist, 0xFF4E7A2A.toInt())

        // Panchavati hut at the end
        val hutX = ramX + (target - dist) + 500f
        if (hutX < V.W + 300f) {
            Scenery.hut(c, hutX, 770f, 1.1f)
            Scenery.tree(c, hutX + 200f, 770f, 1f, 1, time)
            Ui.text(c, "Panchavati", hutX, 520f, 40f, Ui.GOLD, outline = 0xFF3A0610.toInt())
        }

        for (o in obstacles) drawObstacle(c, o)
        for (f in fruits) drawFruit(c, f)

        // Lakshman, Sita, Ram walking
        val lp = Characters.pose(); lp.walk = lakshman.walk
        Characters.person(c, lakshman.x, groundY + lakshman.y, 1.12f, Characters.LAKSHMAN, time, lp)
        val sp = Characters.pose(); sp.walk = sita.walk
        Characters.person(c, sita.x, groundY + sita.y, 1.12f, Characters.SITA, time, sp)
        ramPose.reset()
        ramPose.walk = if (ram.air) 0.6f else ram.walk
        val stumble = if (slow > 0f) kotlin.math.sin(time * 30f) * 4f else 0f
        Characters.person(c, ramX + stumble, groundY + ram.y, 1.25f, Characters.RAM, time, ramPose)

        // journey bar
        val bx = 400f
        val bw = 800f
        Ui.roundRect(c, bx, 850f, bx + bw, 876f, 13f, P.f(0x88000000.toInt()))
        Ui.roundRect(c, bx, 850f, bx + bw * (progress / 100f), 876f, 13f, P.f(Ui.GOLD))
        Ui.text(c, "Ayodhya", bx - 80f, 872f, 26f, 0xFFFFFFFF.toInt(), outline = 0xFF000000.toInt())
        Ui.text(c, "Panchavati", bx + bw + 100f, 872f, 26f, 0xFFFFFFFF.toInt(), outline = 0xFF000000.toInt())
        c.drawCircle(bx + bw * (progress / 100f), 863f, 16f, P.f(0xFF4A86E0.toInt()))

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap to jump!", V.W / 2, 200f, 56f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }

    private fun drawObstacle(c: Canvas, o: Obstacle) {
        val x = o.x
        val y = groundY
        when (o.kind) {
            0 -> {
                path.reset()
                path.moveTo(x - 45f, y); path.lineTo(x - 36f, y - 50f); path.lineTo(x - 5f, y - 72f)
                path.lineTo(x + 30f, y - 58f); path.lineTo(x + 45f, y); path.close()
                c.drawPath(path, P.vgrad(y - 72f, y, 0xFFB0A49A.toInt(), 0xFF6D625A.toInt()))
                c.drawLine(x - 20f, y - 40f, x + 10f, y - 52f, P.s(0x55FFFFFF, 3f))
            }
            1 -> {
                Scenery.bush(c, x, y, 0.75f, 0xFF33691E.toInt())
                for (k in 0 until 8) {
                    val tx = x - 45f + k * 13f
                    c.drawLine(tx, y - 50f, tx + 4f, y - 74f, P.s(0xFF3E2723.toInt(), 3f))
                }
            }
            else -> {
                Ui.roundRect(c, x - 75f, y - 50f, x + 75f, y, 25f, P.vgrad(y - 50f, y, 0xFF8D6E63.toInt(), 0xFF4E342E.toInt()))
                c.drawCircle(x + 60f, y - 25f, 22f, P.f(0xFFD7B98E.toInt()))
                c.drawCircle(x + 60f, y - 25f, 12f, P.s(0xFF8D6E63.toInt(), 2f))
            }
        }
    }

    private fun drawFruit(c: Canvas, f: Fruit) {
        val bob = kotlin.math.sin(time * 4f + f.x * 0.01f) * 6f
        c.drawCircle(f.x, f.y + bob, 34f, P.glow(f.x, f.y + bob, 34f, 0xFFFFF59D.toInt(), 0.6f))
        val col = when (f.kind) { 0 -> 0xFFFFB300.toInt(); 1 -> 0xFFE53935.toInt(); else -> 0xFF8E24AA.toInt() }
        c.drawCircle(f.x, f.y + bob, 17f, P.f(col))
        c.drawCircle(f.x - 5f, f.y + bob - 6f, 5f, P.f(0x66FFFFFF))
        c.drawLine(f.x, f.y + bob - 16f, f.x + 6f, f.y + bob - 26f, P.s(0xFF2E7D32.toInt(), 4f))
    }
}
