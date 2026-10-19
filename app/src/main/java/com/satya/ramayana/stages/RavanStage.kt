package com.satya.ramayana.stages

import android.graphics.Canvas
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
import com.satya.ramayana.screens.DiwaliScreen
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Stage 10 — the final battle. Break Ravan's pride, then strike the Amrit in his navel. */
class RavanStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(10), level) {
    override val goal = 100
    override val goalLabel = "Victory %"
    override val music = Music.BATTLE

    private val quiver = Quiver(0.2f)
    private val ramX = 300f
    private val groundY = 840f
    private val pose = Pose()

    private val ravanS = 1.6f
    private var ravanX = 1200f
    private var ravanY = 760f
    private val bodyY get() = ravanY - 30f
    private val heads = FloatArray(10) { 1f }
    private val regrow = FloatArray(10)
    private var ego = 100f
    private var navelOpen = false
    private var navelHits = 0
    private var hintTime = -1f
    private var ravanFlash = 0f
    private var defeat = -1f
    private var shieldFlash = 0f

    private class Fireball(var x: Float, var y: Float, val vx: Float, val vy: Float)

    private val fireballs = ArrayList<Fireball>()
    private var fireTimer = 2.5f

    override fun updateGame(dt: Float) {
        quiver.update(dt)
        ravanX = 1200f + sin(time * 0.5f) * 60f
        ravanY = 790f + sin(time * 0.9f) * 25f
        ravanFlash = (ravanFlash - dt * 3f).coerceAtLeast(0f)
        shieldFlash = (shieldFlash - dt * 2f).coerceAtLeast(0f)

        // heads grow back - Ravan's boon
        val regrowTime = (3.6f - playTime * 0.012f).coerceAtLeast(2.4f)
        for (i in 0 until 10) {
            if (heads[i] < 1f) {
                regrow[i] += dt
                if (regrow[i] > regrowTime) heads[i] = (heads[i] + dt * 2f).coerceAtMost(1f)
            }
        }

        // fireballs (Ravan's astras)
        fireTimer -= dt
        if (fireTimer <= 0f) {
            fireTimer = (2.4f - playTime * 0.02f).coerceAtLeast(1.0f) * (0.7f + Random.nextFloat() * 0.6f)
            val sx = ravanX - 80f
            val sy = ravanY - 220f
            val tx = ramX + 20f
            val ty = groundY - 200f + Random.nextFloat() * 60f
            val a = atan2(ty - sy, tx - sx)
            val sp = 260f + playTime * 2.5f + Random.nextFloat() * 60f
            fireballs.add(Fireball(sx, sy, cos(a) * sp, sin(a) * sp))
        }
        val it = fireballs.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.x += f.vx * dt
            f.y += f.vy * dt
            fx.add(f.x, f.y, -f.vx * 0.2f + Random.nextFloat() * 40 - 20, -f.vy * 0.2f - 30f, 0.35f, 0xFFFF6D00.toInt(), 7f)
            if (quiver.hit(f.x, f.y, 55f) != null) {
                game.audio.play(Sfx.HIT)
                fx.burst(f.x, f.y, intArrayOf(0xFFFFAB40.toInt(), 0xFFFFFFFF.toInt()), 24)
                score += 50
                it.remove()
                continue
            }
            if (f.x < ramX + 70f) {
                shieldFlash = 1f
                fx.burst(f.x, f.y, intArrayOf(0xFF80D8FF.toInt(), 0xFFFFFFFF.toInt()), 20, 300f)
                penalty(3f, ramX + 80f, groundY - 330f, "Ram's shield holds!")
                it.remove()
            }
        }

        // arrows vs navel and heads
        if (navelOpen) {
            val nx = ravanX + Characters.RAVAN_NAVEL[0] * ravanS
            val ny = bodyY + Characters.RAVAN_NAVEL[1] * ravanS
            if (quiver.hit(nx, ny, 40f) != null) {
                navelHits++
                ravanFlash = 1f
                game.audio.play(Sfx.BREAK)
                fx.burst(nx, ny, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt(), 0xFFFFF59D.toInt()), 60, 600f)
                ego -= 14f
                float("Amrit dries up!", nx, ny - 80f)
            }
        }
        for (i in 0 until 10) {
            if (heads[i] < 0.9f) continue
            val hp = Characters.ravanHeads[i]
            val hx = ravanX + hp[0] * ravanS
            val hy = bodyY + hp[1] * ravanS
            if (quiver.hit(hx, hy, 24f * ravanS) != null) strikeHead(i)
        }

        if (!navelOpen && ego <= 40f) {
            navelOpen = true
            hintTime = 0f
            game.audio.play(Sfx.CONCH)
        }
        if (hintTime >= 0f) hintTime += dt
        progress = (100f - ego).toInt().coerceIn(0, 100)
        if (ego <= 0f) progress = goal
    }

    private fun strikeHead(i: Int) {
        heads[i] = 0f
        regrow[i] = 0f
        ravanFlash = 0.4f
        game.audio.play(Sfx.HIT)
        val hp = Characters.ravanHeads[i]
        val hx = ravanX + hp[0] * ravanS
        val hy = bodyY + hp[1] * ravanS
        fx.burst(hx, hy, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt(), 0xFFCE93D8.toInt()), 26)
        if (ego > 40f) ego = (ego - 6f).coerceAtLeast(40f)
        score += 100
    }

    override fun updateAmbient(dt: Float) {
        if (state == State.WON) {
            if (defeat < 0f) defeat = 0f
            defeat += dt
            fireballs.clear()
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val h = Characters.bowHand(ramX, groundY, 1.3f, kotlin.math.atan2(y - (groundY - 222f * 1.3f), x - ramX).coerceIn(-1.3f, 1.1f))
        if (quiver.shoot(h[0], h[1], x, y, 2000f, golden = navelOpen) != null) game.audio.play(Sfx.ARROW)
    }

    override fun onHanumanPower() {
        for (f in fireballs) fx.burst(f.x, f.y, intArrayOf(0xFFFFAB40.toInt(), 0xFFFFFFFF.toInt()), 20)
        fireballs.clear()
        var n = 0
        for (i in 0 until 10) if (heads[i] >= 0.9f && n < 4) { strikeHead(i); n++ }
        addTime(5f, V.W / 2, 300f)
    }

    override fun afterWin() {
        if (level < Stages.LEVELS) return super.afterWin()
        val st = stars()
        val total = score + (timeLeft.coerceAtLeast(0f) * 20).toInt()
        game.save.completeLevel(info.num, level, st, total)
        game.go(DiwaliScreen(game, st, total))
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF311B4A.toInt(), 0xFFB23A48.toInt(), 0xFFFF9E57.toInt())
        if (defeat >= 0f) {
            // dawn breaks as evil is defeated
            c.drawRect(0f, 0f, V.W, V.H, P.f(0xFFFFE9A8.toInt(), (defeat / 2f).coerceAtMost(0.6f)))
        }
        Scenery.sun(c, 820f, 560f, 80f, 0xFFFFB74D.toInt())
        Scenery.lanka(c, 1250f, 640f, 0.9f, time)
        Scenery.hills(c, 660f, 50f, 0xFF4E342E.toInt(), 2f, color2 = 0xFF3E2723.toInt())
        Scenery.ground(c, 680f, 0xFF6D4C41.toInt(), 0xFF3E2723.toInt())

        // vanar army cheering in the back
        for (k in 0 until 6) {
            Characters.vanar(c, 40f + k * 90f, 700f + (k % 2) * 16f, 0.5f, if (k % 3 == 0) Characters.BEAR else Characters.VANAR, time + k, armsUp = if ((time * 2 + k).toInt() % 2 == 0) 1f else 0f)
        }

        // Ravan on his chariot
        if (defeat < 0f || defeat < 2.5f) {
            val a = if (defeat >= 0f) (1f - defeat / 2.2f).coerceIn(0f, 1f) else 1f
            c.save()
            c.translate(ravanX, ravanY)
            c.scale(a.coerceAtLeast(0.01f), a.coerceAtLeast(0.01f))
            c.translate(-ravanX, -ravanY)
            Ui.roundRect(c, ravanX - 190f, ravanY - 30f, ravanX + 190f, ravanY + 40f, 20f, P.vgrad(ravanY - 30f, ravanY + 40f, 0xFF424242.toInt(), 0xFF212121.toInt()))
            c.drawCircle(ravanX - 120f, ravanY + 50f, 46f, P.s(Ui.GOLD_DARK, 10f))
            c.drawCircle(ravanX + 120f, ravanY + 50f, 46f, P.s(Ui.GOLD_DARK, 10f))
            Characters.ravan(c, ravanX, ravanY - 30f, ravanS, time, heads, if (navelOpen) 1f else 0f, ravanFlash, armsUp = 0.12f + 0.12f * sin(time * 2f))
            c.restore()
            if (defeat >= 0f) {
                val k = (defeat / 2f).coerceAtMost(1f)
                c.drawCircle(ravanX, ravanY - 250f, 500f * k, P.glow(ravanX, ravanY - 250f, 500f * k, 0xFFFFFFFF.toInt(), 1f - k * 0.5f))
            }
        }

        for (f in fireballs) {
            c.drawCircle(f.x, f.y, 46f, P.glow(f.x, f.y, 46f, 0xFFFF6D00.toInt(), 0.9f))
            c.drawCircle(f.x, f.y, 18f, P.f(0xFFFFF176.toInt()))
        }

        // Ram's side
        Characters.vanar(c, 85f, groundY + 10f, 1.05f, Characters.HANUMAN, time)
        Characters.person(c, 190f, groundY - 5f, 1.05f, Characters.LAKSHMAN, time)
        if (navelOpen) Characters.person(c, 490f, groundY + 20f, 0.95f, Characters.VIBHISHAN, time)
        pose.reset()
        pose.aim = quiver.aim.coerceIn(-1.3f, 1.0f)
        pose.drawn = quiver.canShoot()
        Characters.person(c, ramX, groundY, 1.3f, Characters.RAM, time, pose)
        if (shieldFlash > 0f) {
            c.drawCircle(ramX + 20f, groundY - 200f, 230f, P.glow(ramX + 20f, groundY - 200f, 230f, 0xFF80D8FF.toInt(), shieldFlash))
        }
        quiver.draw(c)

        if (playTime < 5f && state == State.PLAY) {
            Ui.text(c, "Shoot Ravan's heads! Tap fireballs to stop them.", V.W / 2, 200f, 44f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
        if (hintTime in 0f..6f && state == State.PLAY) {
            Ui.panel(c, 330f, 160f, 1270f, 300f, (hintTime / 0.3f).coerceAtMost(1f))
            Ui.text(c, "Vibhishan: \"Prabhu, aim at his glowing navel!\"", V.W / 2, 220f, 42f, Ui.GOLD)
            Ui.text(c, "The Amrit there keeps Ravan strong.", V.W / 2, 270f, 34f, Ui.CREAM)
        }
        if (navelOpen && hintTime > 6f && state == State.PLAY) {
            val nx = ravanX + Characters.RAVAN_NAVEL[0] * ravanS
            val ny = ravanY - 30f + Characters.RAVAN_NAVEL[1] * ravanS
            Ui.text(c, "▼ Amrit ▼", nx, ny - 60f + sin(time * 6f) * 6f, 30f, Ui.GOLD, outline = 0xFF000000.toInt())
        }
        if (defeat >= 0f) {
            Ui.title(c, "Victory of Good over Evil!", V.W / 2, 200f, 80f, (defeat / 0.8f).coerceAtMost(1f))
        }
    }
}
