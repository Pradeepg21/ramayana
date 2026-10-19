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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Stage 7 — help Sugriv by striking Bali during their duel. */
class BaliStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(7), level) {
    override val goal = goalFor(6)
    override val goalLabel = "Strikes"
    override val music = Music.BATTLE

    private val quiver = Quiver(0.35f)
    private val ramX = 230f
    private val groundY = 830f
    private val pose = Pose()

    private var phi = 0f
    private var cx = 1000f
    private var baliX = 0f
    private var baliY = 0f
    private var sugX = 0f
    private var sugY = 0f
    private var baliHurt = 0f
    private var sugHurt = 0f
    private var defeated = -1f

    override fun updateGame(dt: Float) {
        quiver.update(dt)
        val sp = 0.75f + progress * 0.15f
        phi += dt * sp
        cx = 1050f + sin(time * 0.4f) * 160f
        val d = 110f + 70f * abs(sin(time * 1.1f))
        baliX = cx + cos(phi) * d
        sugX = cx - cos(phi) * d
        baliY = groundY - abs(sin(time * 3.3f)) * 50f
        sugY = groundY - abs(sin(time * 3.3f + 1.5f)) * 50f
        baliHurt = (baliHurt - dt * 3f).coerceAtLeast(0f)
        sugHurt = (sugHurt - dt * 3f).coerceAtLeast(0f)

        // whoever is "in front" (closer to the viewer) is hit first
        val baliFront = sin(phi) > 0f
        val order = if (baliFront) listOf(true, false) else listOf(false, true)
        for (isBali in order) {
            val x = if (isBali) baliX else sugX
            val y = (if (isBali) baliY else sugY) - (if (isBali) 233f else 214f)
            if (quiver.hit(x, y, 100f) != null) {
                if (isBali) {
                    baliHurt = 1f
                    game.audio.play(Sfx.HIT)
                    fx.burst(x, y, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt()), 30)
                    addProgress(1, x, y - 150f, 150)
                } else {
                    sugHurt = 1f
                    penalty(4f, x, y - 150f, "That's Sugriv! Look for the garland")
                }
            }
        }
    }

    override fun updateAmbient(dt: Float) {
        if (state == State.WON) {
            if (defeated < 0f) defeated = 0f
            defeated += dt
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val h = Characters.bowHand(ramX, groundY, 1.3f, kotlin.math.atan2(y - (groundY - 222f * 1.3f), x - ramX).coerceIn(-1.3f, 1.1f))
        if (quiver.shoot(h[0], h[1], x, y) != null) game.audio.play(Sfx.ARROW)
    }

    override fun onHanumanPower() {
        super.onHanumanPower()
        // Hanuman holds Bali still for a moment: one sure strike
        baliHurt = 1f
        addProgress(1, baliX, baliY - 330f, 150)
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF5D9CEC.toInt(), 0xFFA8D8F0.toInt(), 0xFFFFE9B0.toInt())
        Scenery.clouds(c, time)
        Scenery.mountain(c, 1100f, 640f, 1100f, 420f, 0xFF8D7B68.toInt())
        Scenery.mountain(c, 500f, 640f, 700f, 300f, 0xFF7A6A58.toInt())
        // Kishkindha caves
        c.drawOval(1180f, 480f, 1320f, 620f, P.f(0xFF3E2723.toInt()))
        Scenery.hills(c, 650f, 60f, 0xFF8BC34A.toInt(), 3f, color2 = 0xFF689F38.toInt())
        Scenery.ground(c, 690f, 0xFFB7A16C.toInt(), 0xFF7D6A3E.toInt())
        Scenery.tree(c, 1500f, 720f, 1f, 1, time)

        // the duel
        val baliA = if (defeated >= 0f) (1f - defeated / 1.5f).coerceIn(0f, 1f) else 1f
        val drawBali = {
            if (baliA > 0f) {
                if (baliA < 1f) c.drawCircle(baliX, baliY - 233f, 200f, P.glow(baliX, baliY - 233f, 200f, 0xFFFFFFFF.toInt(), 1f - baliA))
                c.save()
                c.translate(baliX, baliY)
                c.scale(baliA, baliA)
                Characters.vanar(c, 0f, 0f, 1.45f, Characters.BALI, time, if (sugX > baliX) 1f else -1f, armsUp = 0.75f + 0.25f * sin(time * 4f))
                c.restore()
                if (baliHurt > 0f) c.drawCircle(baliX, baliY - 233f, 90f, P.glow(baliX, baliY - 233f, 90f, 0xFFFFFFFF.toInt(), baliHurt))
            }
        }
        val drawSug = {
            Characters.vanar(c, sugX, sugY, 1.3f, Characters.SUGRIV, time, if (baliX > sugX) 1f else -1f, armsUp = if (defeated >= 0f) 1f else 0.75f + 0.25f * sin(time * 4f + 2f))
            if (sugHurt > 0f) c.drawCircle(sugX, sugY - 214f, 80f, P.glow(sugX, sugY - 214f, 80f, 0xFFFF8A80.toInt(), sugHurt))
        }
        if (sin(phi) > 0f) { drawSug(); drawBali() } else { drawBali(); drawSug() }

        // Bali's strength bar
        val frac = 1f - progress / goal.toFloat()
        Ui.roundRect(c, 600f, 130f, 1000f, 160f, 15f, P.f(0x88000000.toInt()))
        if (frac > 0f) Ui.roundRect(c, 603f, 133f, 603f + 394f * frac, 157f, 12f, P.f(0xFFFFA000.toInt()))
        Ui.text(c, "Bali", 560f, 157f, 32f, 0xFFFFFFFF.toInt(), outline = 0xFF000000.toInt())

        // Ram behind a tree with Hanuman & Lakshman
        Characters.vanar(c, 85f, groundY, 1.0f, Characters.HANUMAN, time)
        pose.reset()
        pose.aim = quiver.aim.coerceIn(-1.3f, 1.0f)
        pose.drawn = quiver.canShoot()
        Characters.person(c, ramX, groundY, 1.3f, Characters.RAM, time, pose)
        Scenery.tree(c, 380f, 830f, 1.25f, 0, time)
        Scenery.bush(c, 370f, 830f, 1.1f)
        quiver.draw(c)

        if (playTime < 5f && state == State.PLAY) {
            Ui.text(c, "Tap BALI – Sugriv wears the flower garland!", V.W / 2, 240f, 46f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
        if (defeated >= 0f) {
            Ui.text(c, "Sugriv is King of Kishkindha!", V.W / 2, 260f, 50f, Ui.GOLD, outline = 0xFF3A0610.toInt())
        }
    }
}
