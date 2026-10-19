package com.satya.ramayana.engine

import android.graphics.Canvas
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Particle {
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var life = 0f; var maxLife = 1f
    var color = 0; var size = 4f
    var gravity = 0f; var drag = 0f
    var kind = DOT
    var spin = 0f; var angle = 0f

    companion object {
        const val DOT = 0
        const val SPARK = 1     // streak in direction of travel
        const val PETAL = 2     // flower petal that flutters
        const val RING = 3      // expanding ring
    }
}

class Particles(private val cap: Int = 900) {
    val list = ArrayList<Particle>()
    private val petalPath = Path()

    fun add(
        x: Float, y: Float, vx: Float, vy: Float, life: Float, color: Int, size: Float,
        gravity: Float = 0f, kind: Int = Particle.DOT, drag: Float = 0f
    ): Particle? {
        if (list.size >= cap) return null
        val p = Particle()
        p.x = x; p.y = y; p.vx = vx; p.vy = vy
        p.life = life; p.maxLife = life
        p.color = color; p.size = size; p.gravity = gravity; p.kind = kind; p.drag = drag
        p.spin = Random.nextFloat() * 6f - 3f
        p.angle = Random.nextFloat() * 6.28f
        list.add(p)
        return p
    }

    /** Radial burst — used when an enemy is defeated (vanishes into light). */
    fun burst(x: Float, y: Float, colors: IntArray, count: Int = 30, speed: Float = 380f, size: Float = 6f, gravity: Float = 200f) {
        repeat(count) {
            val a = Random.nextFloat() * 6.283f
            val sp = speed * (0.3f + Random.nextFloat() * 0.7f)
            add(
                x, y, cos(a) * sp, sin(a) * sp, 0.5f + Random.nextFloat() * 0.6f,
                colors[Random.nextInt(colors.size)], size * (0.6f + Random.nextFloat() * 0.8f),
                gravity, Particle.SPARK, 1.6f
            )
        }
        add(x, y, 0f, 0f, 0.45f, colors[0], 20f, kind = Particle.RING)
    }

    fun sparkle(x: Float, y: Float, color: Int, count: Int = 8) {
        repeat(count) {
            add(
                x + Random.nextFloat() * 30 - 15, y + Random.nextFloat() * 30 - 15,
                Random.nextFloat() * 80 - 40, -40f - Random.nextFloat() * 80, 0.6f + Random.nextFloat() * 0.4f,
                color, 3f + Random.nextFloat() * 4f, -20f
            )
        }
    }

    fun petals(x: Float, y: Float, count: Int, spread: Float = 1600f) {
        val cols = intArrayOf(0xFFFF4F7B.toInt(), 0xFFFFB300.toInt(), 0xFFFF7043.toInt(), 0xFFFFFFFF.toInt(), 0xFFE91E63.toInt())
        repeat(count) {
            add(
                x + Random.nextFloat() * spread - spread / 2, y - Random.nextFloat() * 200,
                Random.nextFloat() * 60 - 30, 60f + Random.nextFloat() * 80, 6f + Random.nextFloat() * 3f,
                cols[Random.nextInt(cols.size)], 9f + Random.nextFloat() * 6f, 10f, Particle.PETAL
            )
        }
    }

    fun update(dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
                continue
            }
            if (p.drag > 0f) {
                val k = 1f / (1f + p.drag * dt)
                p.vx *= k; p.vy *= k
            }
            p.vy += p.gravity * dt
            if (p.kind == Particle.PETAL) p.vx += sin(p.life * 3f + p.angle) * 60f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.angle += p.spin * dt
        }
    }

    fun draw(c: Canvas) {
        for (p in list) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            when (p.kind) {
                Particle.DOT -> c.drawCircle(p.x, p.y, p.size, P.f(p.color, a))
                Particle.SPARK -> {
                    c.drawLine(p.x, p.y, p.x - p.vx * 0.04f, p.y - p.vy * 0.04f, P.s(p.color, p.size * 0.8f, a))
                    c.drawCircle(p.x, p.y, p.size * 0.7f, P.f(0xFFFFFFFF.toInt(), a * 0.8f))
                }
                Particle.RING -> {
                    val r = p.size + (1f - a) * 90f
                    c.drawCircle(p.x, p.y, r, P.s(p.color, 6f * a, a))
                }
                Particle.PETAL -> {
                    c.save()
                    c.translate(p.x, p.y)
                    c.rotate(p.angle * 57.3f)
                    petalPath.reset()
                    petalPath.moveTo(0f, -p.size)
                    petalPath.quadTo(p.size * 0.8f, 0f, 0f, p.size)
                    petalPath.quadTo(-p.size * 0.8f, 0f, 0f, -p.size)
                    c.drawPath(petalPath, P.f(p.color, minOf(1f, a * 3f)))
                    c.restore()
                }
            }
        }
    }
}

/** Fireworks / crackers show: rockets that rise and burst into coloured sparks. */
class Fireworks(private val audio: AudioEngine?) {
    val particles = Particles(1400)
    private val rockets = ArrayList<FloatArray>() // x, y, vx, vy, fuse, colorIndex
    private var timer = 0f
    var rate = 0.8f
    var active = true

    private val palettes = arrayOf(
        intArrayOf(0xFFFFD54F.toInt(), 0xFFFFF59D.toInt(), 0xFFFF8F00.toInt()),
        intArrayOf(0xFFFF5252.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt()),
        intArrayOf(0xFF69F0AE.toInt(), 0xFFB9F6CA.toInt(), 0xFFFFFF8D.toInt()),
        intArrayOf(0xFF40C4FF.toInt(), 0xFF80D8FF.toInt(), 0xFFFFFFFF.toInt()),
        intArrayOf(0xFFE040FB.toInt(), 0xFFFF80AB.toInt(), 0xFFFFD740.toInt())
    )

    fun launch(x: Float = 150f + Random.nextFloat() * (V.W - 300f)) {
        rockets.add(floatArrayOf(x, V.H, Random.nextFloat() * 80 - 40, -(700f + Random.nextFloat() * 260f), 0.9f + Random.nextFloat() * 0.5f, Random.nextInt(palettes.size).toFloat()))
    }

    fun update(dt: Float) {
        if (active) {
            timer -= dt
            if (timer <= 0f) {
                timer = rate * (0.5f + Random.nextFloat())
                launch()
            }
        }
        val it = rockets.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r[0] += r[2] * dt
            r[1] += r[3] * dt
            r[3] += 260f * dt
            r[4] -= dt
            particles.add(r[0], r[1], Random.nextFloat() * 30 - 15, 40f, 0.4f, 0xFFFFE0A0.toInt(), 3f)
            if (r[4] <= 0f) {
                explode(r[0], r[1], palettes[r[5].toInt()])
                it.remove()
            }
        }
        particles.update(dt)
    }

    private fun explode(x: Float, y: Float, pal: IntArray) {
        audio?.play(Sfx.CRACKER)
        val n = 70
        val sp = 260f + Random.nextFloat() * 160f
        for (i in 0 until n) {
            val a = i * 6.283f / n
            val s = sp * (0.85f + Random.nextFloat() * 0.3f)
            particles.add(x, y, cos(a) * s, sin(a) * s, 1.2f + Random.nextFloat() * 0.6f, pal[i % pal.size], 5f, 90f, Particle.SPARK, 1.2f)
        }
        repeat(25) {
            val a = Random.nextFloat() * 6.283f
            val s = Random.nextFloat() * sp * 0.6f
            particles.add(x, y, cos(a) * s, sin(a) * s, 1.6f, 0xFFFFFFFF.toInt(), 2.5f, 60f, Particle.DOT, 1f)
        }
    }

    fun draw(c: Canvas) {
        for (r in rockets) {
            c.drawCircle(r[0], r[1], 5f, P.f(0xFFFFF3C0.toInt()))
        }
        particles.draw(c)
    }
}
