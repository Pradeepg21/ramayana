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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Stage 2 — protect Rishi Vishwamitra's yagna from the flying demons. */
class YagnaStage(game: Game, level: Int = 1) : BaseStage(game, Stages.info(2), level) {
    override val goal = goalFor(14)
    override val goalLabel = "Demons"
    override val music = Music.BATTLE

    private val quiver = Quiver(0.2f)
    private val ramX = 480f
    private val groundY = 830f
    private val pose = Pose()
    private var flame = 5
    private var spawn = 1f
    private var tadakaSpawned = false

    private class Demon(var x: Float, var y: Float, val speed: Float, val color: Int, val big: Boolean) {
        var hp = if (big) 4 else 1
        var phase = Random.nextFloat() * 6f
        var hurt = 0f
        val baseY = y
    }

    private val demons = ArrayList<Demon>()
    private val colors = intArrayOf(0xFF7E57C2.toInt(), 0xFF43A047.toInt(), 0xFF5C6BC0.toInt(), 0xFF8D6E63.toInt(), 0xFF26A69A.toInt())

    override fun updateGame(dt: Float) {
        quiver.update(dt)
        spawn -= dt
        val rate = (2.0f - progress * 0.09f).coerceAtLeast(0.75f)
        if (spawn <= 0f && demons.size < 6) {
            spawn = rate * (0.7f + Random.nextFloat() * 0.6f)
            val sp = 120f + progress * 9f + Random.nextFloat() * 40f
            demons.add(Demon(V.W + 100f, 220f + Random.nextFloat() * 380f, sp, colors[Random.nextInt(colors.size)], false))
        }
        if (progress >= 10 && !tadakaSpawned) {
            tadakaSpawned = true
            demons.add(Demon(V.W + 150f, 400f, 90f, 0xFF4A3B6B.toInt(), true))
            float("Tadaka is coming!", V.W / 2, 220f, 0xFFFFCDD2.toInt())
        }

        val it = demons.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.phase += dt * 2.5f
            d.x -= d.speed * dt
            d.y = d.baseY + sin(d.phase) * 40f
            d.hurt = (d.hurt - dt * 3f).coerceAtLeast(0f)
            val r = if (d.big) 135f else 90f
            if (quiver.hit(d.x, d.y, r) != null) {
                d.hp--
                d.hurt = 1f
                game.audio.play(Sfx.HIT)
                if (d.hp <= 0) {
                    fx.burst(d.x, d.y, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt(), 0xFFB39DDB.toInt()), if (d.big) 70 else 36)
                    addProgress(1, d.x, d.y - 60f, if (d.big) 500 else 100)
                    it.remove()
                    continue
                }
            }
            if (d.x < 330f) {
                // the demon disturbs the fire, then flies away scared of the holy flames
                fx.burst(d.x, d.y, intArrayOf(0xFF9E9E9E.toInt(), 0xFF616161.toInt()), 24, 200f)
                flame--
                game.audio.play(Sfx.WRONG)
                float("The Yagna fire is weaker!", 330f, 300f, 0xFFFFCDD2.toInt())
                if (d.big) tadakaSpawned = false
                it.remove()
                if (flame <= 0) fail("The Yagna fire went out!")
            }
        }
    }

    override fun onGameTouch(action: Int, x: Float, y: Float) {
        if (action != Touch.DOWN) return
        val s = 1.3f
        val h = Characters.bowHand(ramX, groundY, s, kotlin.math.atan2(y - (groundY - 222f * s), x - ramX).coerceIn(-1.3f, 1.1f))
        if (quiver.shoot(h[0], h[1], x, y) != null) game.audio.play(Sfx.ARROW)
    }

    override fun onHanumanPower() {
        super.onHanumanPower()
        flame = 5
    }

    override fun drawWorld(c: Canvas) {
        Scenery.sky(c, 0xFF4A3C8C.toInt(), 0xFFB06AB3.toInt(), 0xFFFFC48C.toInt())
        Scenery.sun(c, 1250f, 520f, 70f, 0xFFFFB74D.toInt())
        Scenery.hills(c, 600f, 130f, 0xFF5E7D4A.toInt(), 2f, color2 = 0xFF3E5B30.toInt())
        Scenery.tree(c, 900f, 700f, 1.1f, 0, time)
        Scenery.tree(c, 1450f, 700f, 1.3f, 2, time)
        Scenery.tree(c, 60f, 690f, 1f, 1, time)
        Scenery.ground(c, 690f, 0xFF7CA35A.toInt(), 0xFF4A6B33.toInt())
        Scenery.hut(c, 700f, 700f, 0.7f)

        // Vishwamitra at the yagna
        val sage = Characters.pose()
        sage.namaste = true
        Characters.person(c, 120f, 830f, 1.05f, Characters.SAGE, time, sage)
        Scenery.yagna(c, 260f, 800f, 1f, time, flame / 5f)
        // flame strength meter
        for (k in 0 until 5) {
            Ui.diya(c, 175f + k * 42f, 860f, 0.6f, time, if (k < flame) 1f else 0f)
        }

        // Lakshman guarding
        val lp = Characters.pose()
        lp.aim = -0.5f
        lp.drawn = false
        Characters.person(c, 650f, 840f, 1.1f, Characters.LAKSHMAN, time, lp)

        for (d in demons) Characters.demon(c, d.x, d.y, if (d.big) 1.8f else 1.05f, d.color, time, d.big, d.hurt)

        pose.reset()
        pose.aim = quiver.aim.coerceIn(-1.3f, 1.1f)
        pose.drawn = quiver.canShoot()
        Characters.person(c, ramX, groundY, 1.3f, Characters.RAM, time, pose)
        quiver.draw(c)
        if (playTime < 4f && state == State.PLAY) {
            Ui.text(c, "Tap the demons before they reach the fire!", 1000f, 180f, 46f, 0xFFFFFFFF.toInt(), outline = 0xFF3A0610.toInt())
        }
        if (flame <= 2 && state == State.PLAY) {
            c.drawRect(0f, 0f, V.W, V.H, P.f(0xFFFF0000.toInt(), 0.08f + 0.05f * sin(time * 6f)))
        }
    }
}
