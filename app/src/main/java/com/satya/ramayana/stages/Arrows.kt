package com.satya.ramayana.stages

import android.graphics.Canvas
import com.satya.ramayana.art.Characters
import com.satya.ramayana.engine.V
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Arrow(var x: Float, var y: Float, val angle: Float, val speed: Float, val golden: Boolean) {
    var alive = true
    val tipX get() = x + cos(angle) * 120f
    val tipY get() = y + sin(angle) * 120f
}

/** Ram's quiver: fires straight arrows towards a tapped point. */
class Quiver(private val cooldown: Float = 0.22f) {
    val list = ArrayList<Arrow>()
    private var wait = 0f
    var aim = -0.1f
        private set

    fun canShoot() = wait <= 0f

    fun shoot(fromX: Float, fromY: Float, tx: Float, ty: Float, speed: Float = 1900f, golden: Boolean = false): Arrow? {
        aim = atan2(ty - fromY, tx - fromX)
        if (wait > 0f) return null
        wait = cooldown
        val a = Arrow(fromX - cos(aim) * 40f, fromY - sin(aim) * 40f, aim, speed, golden)
        list.add(a)
        return a
    }

    fun update(dt: Float) {
        wait -= dt
        val it = list.iterator()
        while (it.hasNext()) {
            val a = it.next()
            a.x += cos(a.angle) * a.speed * dt
            a.y += sin(a.angle) * a.speed * dt
            if (!a.alive || a.x > V.W + 200 || a.x < -300 || a.y < -300 || a.y > V.H + 200) it.remove()
        }
    }

    /** Returns the first live arrow whose tip is within [r] of (x,y), and consumes it. */
    fun hit(x: Float, y: Float, r: Float): Arrow? {
        for (a in list) {
            if (!a.alive) continue
            // check tip and mid-shaft so fast arrows don't tunnel through small targets
            if (hypot(a.tipX - x, a.tipY - y) < r || hypot(a.x + cos(a.angle) * 60f - x, a.y + sin(a.angle) * 60f - y) < r) {
                a.alive = false
                return a
            }
        }
        return null
    }

    fun draw(c: Canvas) {
        for (a in list) if (a.alive) Characters.arrow(c, a.x, a.y, a.angle, 120f, a.golden)
    }
}
