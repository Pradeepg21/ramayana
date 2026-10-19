package com.satya.ramayana.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import kotlin.math.sin

/** Backgrounds and props. All coordinates are in the 1600x900 virtual space. */
object Scenery {
    private val path = Path()
    private val rect = RectF()

    fun sky(c: Canvas, top: Int, mid: Int, bottom: Int, h: Float = V.H) {
        c.drawRect(0f, 0f, V.W, h, P.vgrad3(0f, h, top, mid, bottom))
    }

    fun sun(c: Canvas, x: Float, y: Float, r: Float, color: Int = 0xFFFFE082.toInt()) {
        c.drawCircle(x, y, r * 3.2f, P.glow(x, y, r * 3.2f, color, 0.5f))
        c.drawCircle(x, y, r, P.f(color))
        c.drawCircle(x, y, r * 0.8f, P.f(0x55FFFFFF))
    }

    fun moon(c: Canvas, x: Float, y: Float, r: Float) {
        c.drawCircle(x, y, r * 3f, P.glow(x, y, r * 3f, 0xFFFFF8E1.toInt(), 0.35f))
        c.drawCircle(x, y, r, P.f(0xFFFFF8E1.toInt()))
        c.drawCircle(x - r * 0.3f, y - r * 0.2f, r * 0.18f, P.f(0x22000000))
        c.drawCircle(x + r * 0.25f, y + r * 0.3f, r * 0.12f, P.f(0x22000000))
    }

    fun stars(c: Canvas, t: Float, count: Int = 80, maxY: Float = 520f) {
        for (i in 0 until count) {
            val x = ((i * 7919) % 1600).toFloat()
            val y = ((i * 104729) % maxY.toInt()).toFloat()
            val tw = 0.5f + 0.5f * sin(t * 2f + i)
            c.drawCircle(x, y, 1.2f + (i % 3) * 0.6f, P.f(Color.WHITE, 0.3f + 0.7f * tw))
        }
    }

    fun cloud(c: Canvas, x: Float, y: Float, s: Float, alpha: Float = 0.9f) {
        val col = 0xFFFFFFFF.toInt()
        c.drawCircle(x, y, 40 * s, P.f(col, alpha))
        c.drawCircle(x + 45 * s, y - 18 * s, 50 * s, P.f(col, alpha))
        c.drawCircle(x + 95 * s, y, 38 * s, P.f(col, alpha))
        rect.set(x - 40 * s, y, x + 133 * s, y + 30 * s)
        c.drawRoundRect(rect, 30 * s, 30 * s, P.f(col, alpha))
    }

    fun clouds(c: Canvas, t: Float, alpha: Float = 0.85f) {
        for (i in 0 until 4) {
            val x = ((i * 430 + t * (12 + i * 5)) % 1900) - 200
            cloud(c, x, 90f + (i % 3) * 60f, 0.8f + (i % 2) * 0.4f, alpha)
        }
    }

    /** Rolling hills. */
    fun hills(c: Canvas, baseY: Float, amp: Float, color: Int, seed: Float, scroll: Float = 0f, color2: Int = color) {
        path.reset()
        path.moveTo(0f, V.H)
        var x = 0f
        while (x <= V.W + 20f) {
            val wx = x + scroll
            val y = baseY - amp * (0.6f + 0.4f * sin(wx * 0.004f + seed) + 0.25f * sin(wx * 0.011f + seed * 2.1f))
            path.lineTo(x, y)
            x += 20f
        }
        path.lineTo(V.W, V.H)
        path.close()
        c.drawPath(path, P.vgrad(baseY - amp * 1.4f, V.H, color, color2))
    }

    fun ground(c: Canvas, y: Float, c1: Int, c2: Int) {
        c.drawRect(0f, y, V.W, V.H, P.vgrad(y, V.H, c1, c2))
    }

    fun grass(c: Canvas, y: Float, scroll: Float, color: Int) {
        var x = -((scroll % 60f + 60f) % 60f)
        var i = 0
        while (x < V.W + 60f) {
            val h = 14f + ((i * 37) % 11)
            c.drawLine(x, y, x - 6f, y - h, P.s(color, 3f))
            c.drawLine(x + 6f, y, x + 8f, y - h * 0.8f, P.s(color, 3f))
            x += 60f
            i++
        }
    }

    fun tree(c: Canvas, x: Float, y: Float, s: Float, kind: Int = 0, t: Float = 0f) {
        val sway = sin(t * 0.8f + x * 0.01f) * 4f * s
        if (kind == 1) {
            // palm
            path.reset()
            path.moveTo(x - 8 * s, y)
            path.quadTo(x + 10 * s, y - 120 * s, x + sway, y - 240 * s)
            path.lineTo(x + 10 * s + sway, y - 240 * s)
            path.quadTo(x + 22 * s, y - 120 * s, x + 10 * s, y)
            path.close()
            c.drawPath(path, P.f(0xFF7A5230.toInt()))
            val tx = x + 5 * s + sway
            val ty = y - 240 * s
            for (k in 0 until 7) {
                val a = -170f + k * 28f
                c.save()
                c.translate(tx, ty)
                c.rotate(a)
                path.reset()
                path.moveTo(0f, 0f)
                path.quadTo(50 * s, -26 * s, 110 * s, 10 * s)
                path.quadTo(50 * s, -6 * s, 0f, 0f)
                c.drawPath(path, P.f(if (k % 2 == 0) 0xFF2E7D32.toInt() else 0xFF388E3C.toInt()))
                c.restore()
            }
            c.drawCircle(tx, ty + 8 * s, 9 * s, P.f(0xFF6D4C41.toInt()))
            return
        }
        c.drawRect(x - 12 * s, y - 130 * s, x + 12 * s, y, P.vgrad(y - 130 * s, y, 0xFF7B4A26.toInt(), 0xFF4E2C14.toInt()))
        c.drawLine(x, y - 90 * s, x - 40 * s, y - 130 * s, P.s(0xFF6D4223.toInt(), 9 * s))
        c.drawLine(x, y - 100 * s, x + 40 * s, y - 140 * s, P.s(0xFF6D4223.toInt(), 9 * s))
        val g1 = if (kind == 2) 0xFF558B2F.toInt() else 0xFF2E7D32.toInt()
        val g2 = if (kind == 2) 0xFF33691E.toInt() else 0xFF1B5E20.toInt()
        val cx = x + sway
        c.drawCircle(cx - 55 * s, y - 150 * s, 60 * s, P.vgrad(y - 210 * s, y - 90 * s, g1, g2))
        c.drawCircle(cx + 55 * s, y - 155 * s, 62 * s, P.vgrad(y - 217 * s, y - 93 * s, g1, g2))
        c.drawCircle(cx, y - 200 * s, 72 * s, P.vgrad(y - 272 * s, y - 128 * s, g1, g2))
        c.drawCircle(cx - 20 * s, y - 220 * s, 26 * s, P.f(0x22FFFFFF))
        if (kind == 3) {
            // mango tree with fruit
            for (k in 0 until 6) {
                val fx = cx - 70 * s + k * 28 * s
                val fy = y - 140 * s - (k % 3) * 40 * s
                c.drawCircle(fx, fy, 8 * s, P.f(0xFFFFB300.toInt()))
            }
        }
    }

    fun bush(c: Canvas, x: Float, y: Float, s: Float, color: Int = 0xFF2E7D32.toInt()) {
        c.drawCircle(x - 30 * s, y - 20 * s, 30 * s, P.f(color))
        c.drawCircle(x + 30 * s, y - 20 * s, 30 * s, P.f(color))
        c.drawCircle(x, y - 38 * s, 36 * s, P.f(Characters.shade(color, 1.15f)))
        c.drawRect(x - 58 * s, y - 20 * s, x + 58 * s, y, P.f(color))
    }

    fun flag(c: Canvas, x: Float, y: Float, s: Float, t: Float) {
        c.drawLine(x, y, x, y - 70 * s, P.s(0xFF5D4037.toInt(), 3 * s))
        val w = sin(t * 4f + x) * 6f * s
        path.reset()
        path.moveTo(x, y - 70 * s)
        path.quadTo(x + 22 * s, y - 64 * s + w, x + 44 * s, y - 56 * s + w)
        path.lineTo(x, y - 42 * s)
        path.close()
        c.drawPath(path, P.f(Ui.SAFFRON))
    }

    private fun dome(c: Canvas, cx: Float, by: Float, w: Float, color: Int) {
        path.reset()
        path.moveTo(cx - w / 2, by)
        path.cubicTo(cx - w * 0.75f, by - w * 0.55f, cx - w * 0.1f, by - w * 0.75f, cx, by - w * 1.05f)
        path.cubicTo(cx + w * 0.1f, by - w * 0.75f, cx + w * 0.75f, by - w * 0.55f, cx + w / 2, by)
        path.close()
        c.drawPath(path, P.vgrad(by - w, by, Characters.shade(color, 1.2f), color))
        c.drawLine(cx, by - w * 1.05f, cx, by - w * 1.3f, P.s(Ui.GOLD, w * 0.05f))
        c.drawCircle(cx, by - w * 1.32f, w * 0.05f, P.f(Ui.GOLD))
    }

    /** Ayodhya royal palace. */
    fun palace(c: Canvas, cx: Float, by: Float, s: Float, t: Float, lit: Float = 0f, base: Int = 0xFFF3D9A4.toInt()) {
        val wall = base
        val wallDark = Characters.shade(base, 0.8f)
        val domeC = 0xFFF5C26B.toInt()
        // side wings
        for (side in floatArrayOf(-1f, 1f)) {
            val wx = cx + side * 260 * s
            c.drawRect(wx - 150 * s, by - 190 * s, wx + 150 * s, by, P.vgrad(by - 190 * s, by, wall, wallDark))
            for (k in 0 until 5) {
                val ax = wx - 120 * s + k * 60 * s
                arch(c, ax, by - 20 * s, 34 * s, 80 * s, lit)
            }
            c.drawRect(wx - 160 * s, by - 200 * s, wx + 160 * s, by - 186 * s, P.f(Ui.MAROON))
            for (k in 0 until 3) {
                val dx = wx - 100 * s + k * 100 * s
                c.drawRect(dx - 26 * s, by - 236 * s, dx + 26 * s, by - 200 * s, P.f(wall))
                dome(c, dx, by - 236 * s, 60 * s, domeC)
            }
        }
        // central block
        c.drawRect(cx - 140 * s, by - 320 * s, cx + 140 * s, by, P.vgrad(by - 320 * s, by, wall, wallDark))
        c.drawRect(cx - 150 * s, by - 330 * s, cx + 150 * s, by - 316 * s, P.f(Ui.MAROON))
        arch(c, cx, by, 90 * s, 190 * s, lit, door = true)
        for (k in 0 until 3) arch(c, cx - 80 * s + k * 80 * s, by - 220 * s, 36 * s, 70 * s, lit)
        dome(c, cx, by - 330 * s, 200 * s, domeC)
        dome(c, cx - 120 * s, by - 330 * s, 60 * s, domeC)
        dome(c, cx + 120 * s, by - 330 * s, 60 * s, domeC)
        flag(c, cx, by - 330 * s - 260 * s, s * 1.3f, t)
        flag(c, cx - 410 * s, by - 236 * s - 80 * s, s, t)
        flag(c, cx + 410 * s, by - 236 * s - 80 * s, s, t)
    }

    private fun arch(c: Canvas, cx: Float, by: Float, w: Float, h: Float, lit: Float, door: Boolean = false) {
        path.reset()
        path.moveTo(cx - w / 2, by)
        path.lineTo(cx - w / 2, by - h * 0.65f)
        path.quadTo(cx - w / 2, by - h, cx, by - h * 1.05f)
        path.quadTo(cx + w / 2, by - h, cx + w / 2, by - h * 0.65f)
        path.lineTo(cx + w / 2, by)
        path.close()
        val inside = if (lit > 0f) blend(0xFF5A3A28.toInt(), 0xFFFFC46B.toInt(), lit) else 0xFF5A3A28.toInt()
        c.drawPath(path, P.f(inside))
        c.drawPath(path, P.s(Ui.GOLD_DARK, 3f))
        if (door) c.drawLine(cx, by, cx, by - h * 0.95f, P.s(0xFF3E2216.toInt(), 3f))
    }

    fun hut(c: Canvas, x: Float, y: Float, s: Float) {
        c.drawRect(x - 90 * s, y - 110 * s, x + 90 * s, y, P.vgrad(y - 110 * s, y, 0xFFC8A06A.toInt(), 0xFF8D6A3E.toInt()))
        for (k in 0 until 9) c.drawLine(x - 85 * s + k * 21 * s, y - 108 * s, x - 85 * s + k * 21 * s, y, P.s(0x557A5230, 2f))
        path.reset()
        path.moveTo(x - 125 * s, y - 100 * s)
        path.lineTo(x, y - 210 * s)
        path.lineTo(x + 125 * s, y - 100 * s)
        path.close()
        c.drawPath(path, P.vgrad(y - 210 * s, y - 100 * s, 0xFFE0B860.toInt(), 0xFFA6782C.toInt()))
        for (k in 0 until 8) c.drawLine(x, y - 206 * s, x - 120 * s + k * 34 * s, y - 102 * s, P.s(0x55704010, 2f))
        rect.set(x - 28 * s, y - 80 * s, x + 28 * s, y)
        c.drawRoundRect(rect, 20 * s, 20 * s, P.f(0xFF4E3420.toInt()))
    }

    /** Sacred yagna fire in a square kund. [strength] 0..1 controls the flame. */
    fun yagna(c: Canvas, x: Float, y: Float, s: Float, t: Float, strength: Float) {
        if (strength > 0f) c.drawCircle(x, y - 60 * s, 160 * s, P.glow(x, y - 60 * s, 160 * s, 0xFFFF9800.toInt(), 0.5f * strength))
        rect.set(x - 80 * s, y - 34 * s, x + 80 * s, y)
        c.drawRect(rect, P.vgrad(y - 34 * s, y, 0xFFB5651D.toInt(), 0xFF7B3F12.toInt()))
        c.drawRect(x - 90 * s, y - 42 * s, x + 90 * s, y - 32 * s, P.f(0xFFD08B4A.toInt()))
        for (k in 0 until 4) c.drawCircle(x - 60 * s + k * 40 * s, y - 17 * s, 5 * s, P.f(0xFFFFEB3B.toInt()))
        if (strength <= 0f) {
            for (k in 0 until 3) c.drawCircle(x - 20 * s + k * 20 * s, y - 60 * s - (t * 30 % 40) * s - k * 10, 14 * s, P.f(0x55888888))
            return
        }
        for (k in 0 until 5) {
            val fx = x + (k - 2) * 24 * s
            val fh = (70f + 40f * sin(t * 9f + k * 1.7f)) * s * strength * (1.1f - kotlin.math.abs(k - 2) * 0.18f)
            path.reset()
            path.moveTo(fx - 20 * s, y - 40 * s)
            path.quadTo(fx - 18 * s, y - 40 * s - fh * 0.6f, fx + sin(t * 7f + k) * 8 * s, y - 40 * s - fh)
            path.quadTo(fx + 18 * s, y - 40 * s - fh * 0.6f, fx + 20 * s, y - 40 * s)
            path.close()
            c.drawPath(path, P.vgrad(y - 40 * s - fh, y - 40 * s, 0xFFFFF59D.toInt(), 0xFFFF5722.toInt()))
        }
    }

    fun sea(c: Canvas, top: Float, t: Float, bottom: Float = V.H) {
        c.drawRect(0f, top, V.W, bottom, P.vgrad(top, bottom, 0xFF26A6D1.toInt(), 0xFF0B3D6B.toInt()))
        for (row in 0 until 6) {
            val y = top + 20f + row * (bottom - top) / 6f
            path.reset()
            var x = -40f
            val off = (t * (30f + row * 10f)) % 80f
            path.moveTo(x - off, y)
            while (x < V.W + 80f) {
                path.quadTo(x + 20f - off, y - 8f, x + 40f - off, y)
                path.quadTo(x + 60f - off, y + 8f, x + 80f - off, y)
                x += 80f
            }
            c.drawPath(path, P.s(0x55FFFFFF, 2.5f))
        }
    }

    /** Golden city of Lanka on a hill. */
    fun lanka(c: Canvas, cx: Float, by: Float, s: Float, t: Float) {
        path.reset()
        path.moveTo(cx - 420 * s, by)
        path.quadTo(cx - 200 * s, by - 160 * s, cx, by - 170 * s)
        path.quadTo(cx + 220 * s, by - 160 * s, cx + 420 * s, by)
        path.close()
        c.drawPath(path, P.vgrad(by - 170 * s, by, 0xFF6D4C41.toInt(), 0xFF3E2723.toInt()))
        val gold1 = 0xFFFFD54F.toInt()
        val gold2 = 0xFFC79100.toInt()
        for (k in -3..3) {
            val tx = cx + k * 85 * s
            val th = (220 - kotlin.math.abs(k) * 40) * s
            val tb = by - 150 * s + kotlin.math.abs(k) * 25 * s
            c.drawRect(tx - 30 * s, tb - th, tx + 30 * s, tb, P.vgrad(tb - th, tb, gold1, gold2))
            path.reset()
            path.moveTo(tx - 36 * s, tb - th)
            path.lineTo(tx, tb - th - 70 * s)
            path.lineTo(tx + 36 * s, tb - th)
            path.close()
            c.drawPath(path, P.f(0xFFFFB300.toInt()))
            for (w in 0 until 3) c.drawRect(tx - 8 * s, tb - th + 30 * s + w * 50 * s, tx + 8 * s, tb - th + 50 * s + w * 50 * s, P.f(0xFF5D2E00.toInt()))
            flag(c, tx, tb - th - 60 * s, s * 0.8f, t)
        }
        c.drawCircle(cx, by - 260 * s, 300 * s, P.glow(cx, by - 260 * s, 300 * s, 0xFFFFC107.toInt(), 0.18f))
    }

    /** Mountain peak (Rishyamukh / Kishkindha). */
    fun mountain(c: Canvas, cx: Float, by: Float, w: Float, h: Float, color: Int) {
        path.reset()
        path.moveTo(cx - w / 2, by)
        path.lineTo(cx - w * 0.12f, by - h)
        path.lineTo(cx + w * 0.05f, by - h * 0.9f)
        path.lineTo(cx + w * 0.16f, by - h * 1.02f)
        path.lineTo(cx + w / 2, by)
        path.close()
        c.drawPath(path, P.vgrad(by - h, by, Characters.shade(color, 1.25f), color))
    }

    fun lotus(c: Canvas, x: Float, y: Float, s: Float, color: Int = 0xFFFF80AB.toInt()) {
        rect.set(x - 46 * s, y - 6 * s, x + 46 * s, y + 12 * s)
        c.drawOval(rect, P.f(0xFF2E7D32.toInt()))
        for (k in -2..2) {
            c.save()
            c.translate(x, y)
            c.rotate(k * 24f)
            path.reset()
            path.moveTo(0f, 0f)
            path.quadTo(-18 * s, -26 * s, 0f, -52 * s)
            path.quadTo(18 * s, -26 * s, 0f, 0f)
            c.drawPath(path, P.vgrad(-52 * s, 0f, 0xFFFFFFFF.toInt(), color))
            c.restore()
        }
    }

    /** Royal court of Mithila. */
    fun court(c: Canvas, t: Float) {
        c.drawRect(0f, 0f, V.W, V.H, P.vgrad(0f, V.H, 0xFF8E2A2A.toInt(), 0xFF3A0A10.toInt()))
        // pillars
        for (k in 0 until 7) {
            val x = 60f + k * 247f
            c.drawRect(x - 30f, 80f, x + 30f, 720f, P.vgrad(80f, 720f, 0xFFF3D9A4.toInt(), 0xFFB98B4E.toInt()))
            c.drawRect(x - 42f, 690f, x + 42f, 730f, P.f(Ui.GOLD_DARK))
            c.drawRect(x - 42f, 70f, x + 42f, 100f, P.f(Ui.GOLD_DARK))
        }
        // drapes
        for (k in 0 until 6) {
            val x = 60f + k * 247f
            path.reset()
            path.moveTo(x, 100f)
            path.quadTo(x + 123f, 230f + sin(t + k) * 6f, x + 247f, 100f)
            c.drawPath(path, P.s(Ui.SAFFRON, 14f))
            c.drawPath(path, P.s(Ui.GOLD, 3f))
        }
        c.drawRect(0f, 0f, V.W, 70f, P.f(Ui.MAROON_DARK))
        for (k in 0 until 40) c.drawCircle(20f + k * 40f, 70f, 8f, P.f(Ui.GOLD))
        // marble floor
        c.drawRect(0f, 720f, V.W, V.H, P.vgrad(720f, V.H, 0xFFEADFC8.toInt(), 0xFFB9A887.toInt()))
        for (k in 0 until 17) c.drawLine(k * 100f, 720f, k * 140f - 320f, V.H, P.s(0x33000000, 2f))
        // carpet
        path.reset()
        path.moveTo(700f, 720f); path.lineTo(900f, 720f); path.lineTo(1020f, V.H); path.lineTo(580f, V.H); path.close()
        c.drawPath(path, P.f(0xFFB71C1C.toInt()))
        c.drawPath(path, P.s(Ui.GOLD, 4f))
    }

    fun rangoli(c: Canvas, x: Float, y: Float, s: Float, t: Float) {
        val cols = intArrayOf(0xFFE91E63.toInt(), 0xFFFFC107.toInt(), 0xFF00BCD4.toInt(), 0xFF8BC34A.toInt(), 0xFFFF5722.toInt())
        c.save()
        c.translate(x, y)
        c.scale(1f, 0.45f)
        for (ring in 4 downTo 0) {
            val r = (30 + ring * 26) * s
            val petals = 8 + ring * 4
            for (k in 0 until petals) {
                c.save()
                c.rotate(k * 360f / petals + t * (if (ring % 2 == 0) 6f else -6f))
                path.reset()
                path.moveTo(0f, -r + 22 * s)
                path.quadTo(12 * s, -r, 0f, -r - 18 * s)
                path.quadTo(-12 * s, -r, 0f, -r + 22 * s)
                c.drawPath(path, P.f(cols[(ring + k) % cols.size]))
                c.restore()
            }
        }
        c.drawCircle(0f, 0f, 24 * s, P.f(0xFFFFEB3B.toInt()))
        c.drawCircle(0f, 0f, 12 * s, P.f(0xFFE91E63.toInt()))
        c.restore()
    }

    /** Pushpak Viman - the flying chariot. */
    fun pushpak(c: Canvas, x: Float, y: Float, s: Float, t: Float) {
        c.save()
        c.translate(x, y + sin(t * 2f) * 6f)
        c.scale(s, s)
        c.drawCircle(0f, 0f, 220f, P.glow(0f, 0f, 220f, 0xFFFFE082.toInt(), 0.4f))
        path.reset()
        path.moveTo(-200f, 0f)
        path.quadTo(0f, 80f, 200f, 0f)
        path.lineTo(170f, -20f)
        path.lineTo(-170f, -20f)
        path.close()
        c.drawPath(path, P.vgrad(-20f, 60f, 0xFFFFE082.toInt(), 0xFFC68400.toInt()))
        for (k in -3..3) c.drawCircle(k * 50f, 18f, 7f, P.f(0xFFE53935.toInt()))
        // canopy
        c.drawLine(-140f, -20f, -140f, -150f, P.s(Ui.GOLD_DARK, 6f))
        c.drawLine(140f, -20f, 140f, -150f, P.s(Ui.GOLD_DARK, 6f))
        path.reset()
        path.moveTo(-170f, -150f)
        path.quadTo(0f, -260f, 170f, -150f)
        path.close()
        c.drawPath(path, P.vgrad(-260f, -150f, 0xFFFFF59D.toInt(), 0xFFFFB300.toInt()))
        c.drawLine(0f, -230f, 0f, -290f, P.s(Ui.GOLD, 4f))
        flag(c, 0f, -230f, 1f, t)
        c.restore()
    }

    fun blend(a: Int, b: Int, k: Float): Int {
        val t = k.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        )
    }
}
