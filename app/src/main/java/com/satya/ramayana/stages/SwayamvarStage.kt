package com.satya.ramayana.stages

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Pose
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.Music
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.Stages
import com.satya.ramayana.screens.VivahScreen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Stage 3 — lift and string Lord Shiva's bow at Sita's swayamvar. */
class SwayamvarStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(3), level) {
    override val goal = goalFor(5)
    override val goalLabel = "Strength"
    override val music = Music.VIVAH

    private val zoneWidths = floatArrayOf(0.26f, 0.21f, 0.17f, 0.14f, 0.11f)
    private val speeds = floatArrayOf(0.7f, 0.85f, 1.0f, 1.15f, 1.3f)
    private var needle = 0f
    private var dir = 1f
    private var zoneCenter = 0.5f
    private var shake = 0f
    private var lift = 0f
    private var flashZone = 0f
    private var broken = -1f
    private val pose = Pose()
    private val path = Path()
    private val rect = RectF()

    private val gx = 1100f
    private val gy = 885f
    private val gr = 185f

    override fun updateGame(dt: Float) {
        val i = progress.coerceAtMost(4)
        needle += dir * speeds[i] * dt
        if (needle > 1f) { needle = 1f; dir = -1f }
        if (needle < 0f) { needle = 0f; dir = 1f }
        shake = (shake - dt * 3f).coerceAtLeast(0f)
        flashZone = (flashZone - dt * 2f).coerceAtLeast(0f)
        lift += ((progress / goal.toFloat()) - lift) * dt * 4f
    }

    override fun updateAmbient(dt: Float) {
        lift += (1f - lift) * dt * 4f
        if (broken >= 0f) broken += dt
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val w = zoneWidths[progress.coerceAtMost(4)]
        if (abs(needle - zoneCenter) <= w / 2) {
            game.audio.play(Sfx.HIT)
            flashZone = 1f
            fx.sparkle(gx, gy - gr, Ui.GOLD, 20)
            addProgress(1, V.W / 2, 450f, 200)
            if (progress < goal) {
                zoneCenter = 0.15f + Random.nextFloat() * 0.7f
            }
        } else {
            shake = 1f
            penalty(2f, V.W / 2, 450f, "Steady… try again!")
        }
    }

    override fun win() {
        super.win()
        broken = 0f
        game.audio.play(Sfx.BREAK)
        fx.burst(800f, 380f, intArrayOf(0xFFFFFFFF.toInt(), 0xFF80D8FF.toInt(), Ui.GOLD), 120, 900f, 9f, 50f)
    }

    override fun afterWin() {
        val st = stars()
        val total = score + (timeLeft.coerceAtLeast(0f) * 20).toInt()
        game.save.completeLevel(info.num, level, st, total)
        game.go(VivahScreen(game, level, st, total))
    }

    override fun drawWorld(c: Canvas) {
        Scenery.court(c, time)

        // King Janak on his throne and visiting kings
        Ui.roundRect(c, 60f, 420f, 300f, 760f, 30f, P.vgrad(420f, 760f, 0xFFFFD54F.toInt(), 0xFFB8860B.toInt()))
        Characters.person(c, 180f, 800f, 1.05f, Characters.KING, time)
        for ((k, kx) in floatArrayOf(380f, 470f).withIndex()) {
            Characters.person(c, kx, 790f - k * 10f, 0.85f, Characters.LAKSHMAN, time + k)
        }

        // Sita waiting with the varmala
        val sp = Characters.pose()
        sp.holdGarland = progress >= goal
        sp.namaste = progress < goal
        Characters.person(c, 1380f, 830f, 1.2f, Characters.SITA, time, sp, facing = -1f)
        if (progress < goal) {
            for (k in 0..10) {
                val a = PI * (k / 10.0)
                c.drawCircle(1376f + (cos(a) * 26).toFloat(), 618f + (sin(a) * 36).toFloat(), 6.5f, P.f(if (k % 2 == 0) 0xFFFF7043.toInt() else 0xFFFFEB3B.toInt()))
            }
        }
        Characters.person(c, 1510f, 820f, 0.95f, Characters.SAGE, time)

        // Ram
        pose.reset()
        pose.armsUp = lift
        val sx = if (shake > 0f) sin(time * 60f) * 8f * shake else 0f
        Characters.person(c, 800f + sx, 830f, 1.35f, Characters.RAM, time, pose)

        drawShivaBow(c, sx)

        // power gauge
        drawGauge(c)
        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap when the needle is in the golden zone!", V.W / 2, 180f, 46f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
    }

    private fun drawShivaBow(c: Canvas, sx: Float) {
        val restY = 650f
        val liftedY = 290f
        val by = restY + (liftedY - restY) * lift
        val bx = 800f + sx + 40f * lift
        val rot = 90f * lift

        if (lift < 0.2f) {
            // stand
            Ui.roundRect(c, 560f, 670f, 1040f, 700f, 10f, P.vgrad(670f, 700f, 0xFF8D6E63.toInt(), 0xFF4E342E.toInt()))
            c.drawRect(600f, 700f, 630f, 760f, P.f(0xFF4E342E.toInt()))
            c.drawRect(970f, 700f, 1000f, 760f, P.f(0xFF4E342E.toInt()))
        }
        c.drawCircle(bx, by, 260f, P.glow(bx, by, 260f, 0xFF80D8FF.toInt(), 0.35f + 0.15f * sin(time * 3f)))

        val halves = if (broken >= 0f) 2 else 1
        for (h in 0 until halves) {
            c.save()
            c.translate(bx, by)
            c.rotate(rot)
            if (broken >= 0f) {
                val k = (broken * 2f).coerceAtMost(1f)
                c.translate(0f, (if (h == 0) -1 else 1) * 120f * k)
                c.rotate((if (h == 0) -1 else 1) * 25f * k)
                c.clipRect(-300f, if (h == 0) -400f else 0f, 300f, if (h == 0) 0f else 400f)
            }
            path.reset()
            path.moveTo(-250f, -30f)
            path.quadTo(0f, 70f, 250f, -30f)
            c.drawPath(path, P.s(0xFF1A237E.toInt(), 22f))
            c.drawPath(path, P.s(0xFF5C6BC0.toInt(), 12f))
            c.drawPath(path, P.s(Ui.GOLD, 3f))
            for (k in -2..2) c.drawCircle(k * 90f, 18f - abs(k) * 12f, 8f, P.f(Ui.GOLD))
            // string tightens as Ram strings the bow
            val sag = 40f * (1f - (progress / goal.toFloat()))
            c.drawLine(-250f, -30f, 0f, -30f - sag, P.s(0xFFFFF8E1.toInt(), 3f))
            c.drawLine(0f, -30f - sag, 250f, -30f, P.s(0xFFFFF8E1.toInt(), 3f))
            // trident emblem
            c.drawLine(0f, 30f, 0f, 70f, P.s(Ui.GOLD, 4f))
            path.reset()
            path.moveTo(-14f, 50f); path.quadTo(-14f, 70f, 0f, 74f); path.quadTo(14f, 70f, 14f, 50f)
            c.drawPath(path, P.s(Ui.GOLD, 4f))
            c.restore()
        }
    }

    private fun drawGauge(c: Canvas) {
        rect.set(gx - gr, gy - gr, gx + gr, gy + gr)
        c.drawArc(rect, 180f, 180f, true, P.f(0xCC2A0A10.toInt()))
        c.drawArc(rect, 180f, 180f, true, P.s(Ui.GOLD, 5f))
        val w = zoneWidths[progress.coerceAtMost(4)]
        val start = 180f + (zoneCenter - w / 2) * 180f
        val glow = 0.7f + 0.3f * flashZone
        rect.inset(18f, 18f)
        c.drawArc(rect, start, w * 180f, true, P.vgrad(gy - gr, gy, 0xFFFFF59D.toInt(), 0xFFFFA000.toInt()).also { it.alpha = (255 * glow).toInt() })
        rect.inset(-18f, -18f)
        val a = PI + needle * PI
        val nx = gx + (cos(a) * (gr - 20f)).toFloat()
        val ny = gy + (sin(a) * (gr - 20f)).toFloat()
        c.drawLine(gx, gy, nx, ny, P.s(0xFFFFFFFF.toInt(), 8f))
        c.drawCircle(nx, ny, 12f, P.f(0xFFE53935.toInt()))
        c.drawCircle(gx, gy, 18f, P.f(Ui.GOLD))
        for (k in 0 until goal) {
            Ui.star(c, gx - (goal - 1) * 28f + k * 56f, gy - gr - 40f, 22f, k < progress)
        }
    }
}
