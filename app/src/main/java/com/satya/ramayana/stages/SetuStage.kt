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

/** Stage 9 — write Ram's name on falling stones so they float and form the Setu. */
class SetuStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(9), level) {
    override val goal = goalFor(20)
    override val goalLabel = "Stones"

    private val seaY = 640f
    private val bridgeStart = 230f
    private val bridgeEnd = 1330f
    private val stoneW get() = (bridgeEnd - bridgeStart) / goal

    private class Stone(var x: Float, var y: Float, val vy: Float) {
        var named = false
        var flyT = 0f
        var sinking = -1f
        var sx = 0f
        var sy = 0f
    }

    private val stones = ArrayList<Stone>()
    private var spawn = 0.5f

    private fun slotX(i: Int) = bridgeStart + stoneW * (i + 0.5f)

    override fun updateGame(dt: Float) {
        spawn -= dt
        if (spawn <= 0f) {
            spawn = (1.15f - playTime * 0.012f).coerceAtLeast(0.5f)
            val sp = 150f + playTime * 3.5f + Random.nextFloat() * 60f
            stones.add(Stone(380f + Random.nextFloat() * 900f, 230f, sp))
        }
        val it = stones.iterator()
        while (it.hasNext()) {
            val s = it.next()
            when {
                s.named -> {
                    s.flyT += dt * 2.2f
                    if (s.flyT >= 1f) {
                        it.remove()
                        game.audio.play(Sfx.SPLASH)
                        addProgress(1, slotX(progress), seaY - 80f)
                        fx.sparkle(slotX(progress - 1), seaY, 0xFFFFFFFF.toInt(), 10)
                    }
                }
                s.sinking >= 0f -> {
                    s.sinking += dt
                    s.y += 60f * dt
                    if (s.sinking > 1f) it.remove()
                }
                else -> {
                    s.y += s.vy * dt
                    if (s.y > seaY + 10f) {
                        s.sinking = 0f
                        game.audio.play(Sfx.SPLASH)
                        fx.burst(s.x, seaY, intArrayOf(0xFFFFFFFF.toInt(), 0xFFB3E5FC.toInt()), 16, 260f, 5f, 600f)
                    }
                }
            }
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val s = stones.firstOrNull { !it.named && it.sinking < 0f && hypot(it.x - x, it.y - y) < 90f } ?: return
        s.named = true
        s.sx = s.x
        s.sy = s.y
        game.audio.play(Sfx.BELL)
        fx.sparkle(s.x, s.y, Ui.GOLD, 14)
    }

    override fun onHanumanPower() {
        // every falling stone gets Ram's name at once
        for (s in stones) if (!s.named && s.sinking < 0f) { s.named = true; s.sx = s.x; s.sy = s.y }
        addTime(5f, V.W / 2, 300f)
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF1E88E5.toInt(), 0xFF81D4FA.toInt(), 0xFFFFF3E0.toInt(), seaY)
        Scenery.sun(c, 1100f, 160f, 60f)
        Scenery.clouds(c, time)
        // Lanka on the horizon
        Scenery.lanka(c, 1480f, seaY + 10f, 0.55f, time)
        Scenery.sea(c, seaY, time)

        // shore of Rameswaram
        c.drawOval(-200f, seaY - 40f, 330f, V.H + 200f, P.vgrad(seaY - 40f, V.H, 0xFFE6C88F.toInt(), 0xFFB89A5E.toInt()))
        Scenery.tree(c, 60f, seaY + 10f, 0.9f, 1, time)
        val rp = Characters.pose(); rp.namaste = true
        Characters.person(c, 150f, seaY + 150f, 0.9f, Characters.RAM, time, rp)
        Characters.vanar(c, 255f, seaY + 175f, 0.72f, Characters.HANUMAN, time)

        // built bridge
        for (i in 0 until progress) {
            val x = slotX(i)
            val bob = sin(time * 2f + i) * 3f
            stoneShape(c, x, seaY + 8f + bob, stoneW * 0.55f, true)
        }

        // vanars on the cliff dropping stones
        for (k in 0 until 4) {
            Characters.vanar(c, 450f + k * 260f, 240f + sin(time * 2f + k) * 4f, 0.55f, if (k % 2 == 0) Characters.VANAR else Characters.BEAR, time + k, armsUp = 1f)
        }

        for (s in stones) {
            if (s.named) {
                val tx = slotX(progress)
                val t = s.flyT
                val x = s.sx + (tx - s.sx) * t
                val y = s.sy + (seaY - s.sy) * t - sin(t * 3.14f) * 120f
                stoneShape(c, x, y, 52f, true)
            } else {
                val a = if (s.sinking >= 0f) 1f - s.sinking else 1f
                c.save()
                c.clipRect(0f, 0f, V.W, seaY + 20f)
                stoneShape(c, s.x, s.y, 52f, false, a)
                c.restore()
            }
        }

        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap the stones before they reach the water!", V.W / 2, 330f, 46f, 0xFFFFFFFF.toInt(), outline = 0xFF0D47A1.toInt())
        }
    }

    private fun stoneShape(c: Canvas, x: Float, y: Float, r: Float, named: Boolean, alpha: Float = 1f) {
        if (named) c.drawCircle(x, y, r * 1.7f, P.glow(x, y, r * 1.7f, 0xFFFFE082.toInt(), 0.6f * alpha))
        Ui.roundRect(c, x - r, y - r * 0.7f, x + r, y + r * 0.7f, r * 0.45f,
            P.vgrad(y - r, y + r, 0xFFB0BEC5.toInt(), 0xFF607D8B.toInt()).also { it.alpha = (255 * alpha).toInt() })
        c.drawCircle(x - r * 0.4f, y - r * 0.25f, r * 0.12f, P.f(0x33000000, alpha))
        if (named) Ui.text(c, "राम", x, y + r * 0.25f, r * 0.75f, 0xFFD50000.toInt(), outline = 0xFFFFFFFF.toInt(), alpha = alpha)
    }
}
