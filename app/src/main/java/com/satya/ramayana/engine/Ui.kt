package com.satya.ramayana.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shared paints. Only ever touched from the game-loop thread. */
object P {
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG)
    val shaded = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    val sansBold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    val sans: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    fun f(color: Int, alpha: Float = 1f): Paint {
        fill.shader = null
        fill.color = color
        if (alpha < 1f) fill.alpha = (Color.alpha(color) * alpha.coerceIn(0f, 1f)).toInt()
        return fill
    }

    fun s(color: Int, width: Float, alpha: Float = 1f): Paint {
        line.shader = null
        line.color = color
        line.strokeWidth = width
        if (alpha < 1f) line.alpha = (Color.alpha(color) * alpha.coerceIn(0f, 1f)).toInt()
        return line
    }

    fun vgrad(top: Float, bottom: Float, c1: Int, c2: Int): Paint {
        shaded.shader = LinearGradient(0f, top, 0f, bottom, c1, c2, Shader.TileMode.CLAMP)
        shaded.alpha = 255
        return shaded
    }

    fun vgrad3(top: Float, bottom: Float, c1: Int, c2: Int, c3: Int): Paint {
        shaded.shader = LinearGradient(
            0f, top, 0f, bottom, intArrayOf(c1, c2, c3), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        shaded.alpha = 255
        return shaded
    }

    fun glow(x: Float, y: Float, r: Float, color: Int, alpha: Float = 1f): Paint {
        val a = (Color.alpha(color) * alpha.coerceIn(0f, 1f)).toInt()
        val inner = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        shaded.shader = RadialGradient(x, y, r.coerceAtLeast(1f), inner, inner and 0x00FFFFFF, Shader.TileMode.CLAMP)
        shaded.alpha = 255
        return shaded
    }
}

object Ui {
    const val GOLD = 0xFFFFC93C.toInt()
    const val GOLD_DARK = 0xFFB8860B.toInt()
    const val SAFFRON = 0xFFFF8F1F.toInt()
    const val MAROON = 0xFF6B1020.toInt()
    const val MAROON_DARK = 0xFF3A0610.toInt()
    const val CREAM = 0xFFFFF4DC.toInt()
    const val PARCHMENT = 0xFFF5E2B8.toInt()
    const val INK = 0xFF3B2410.toInt()
    const val GREEN = 0xFF2E9E48.toInt()
    const val RED = 0xFFD8342C.toInt()

    private val tmpRect = RectF()

    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.CENTER, face: Typeface = P.serifBold,
        outline: Int = 0, outlineW: Float = size * 0.12f, alpha: Float = 1f
    ) {
        val p = P.text
        p.textSize = size
        p.typeface = face
        p.textAlign = align
        p.shader = null
        if (outline != 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = outlineW
            p.strokeJoin = Paint.Join.ROUND
            p.color = outline
            p.alpha = (Color.alpha(outline) * alpha).toInt()
            c.drawText(s, x, y, p)
        }
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = (Color.alpha(color) * alpha).toInt()
        c.drawText(s, x, y, p)
    }

    /** Big golden heading with gradient fill and dark outline. */
    fun title(c: Canvas, s: String, x: Float, y: Float, size: Float, alpha: Float = 1f) {
        val p = P.text
        p.textSize = size
        p.typeface = P.serifBold
        p.textAlign = Paint.Align.CENTER
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = size * 0.16f
        p.strokeJoin = Paint.Join.ROUND
        p.color = MAROON_DARK
        p.alpha = (255 * alpha).toInt()
        c.drawText(s, x, y, p)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, y - size * 0.85f, 0f, y + size * 0.1f,
            intArrayOf(0xFFFFF6C8.toInt(), GOLD, 0xFFE07B00.toInt()), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        p.alpha = (255 * alpha).toInt()
        c.drawText(s, x, y, p)
        p.shader = null
    }

    fun measure(s: String, size: Float, face: Typeface = P.serifBold): Float {
        P.text.textSize = size
        P.text.typeface = face
        return P.text.measureText(s)
    }

    fun wrap(s: String, size: Float, maxW: Float, face: Typeface = P.sans): List<String> {
        val out = ArrayList<String>()
        for (para in s.split("\n")) {
            var cur = ""
            for (word in para.split(" ")) {
                val trial = if (cur.isEmpty()) word else "$cur $word"
                if (measure(trial, size, face) > maxW && cur.isNotEmpty()) {
                    out.add(cur)
                    cur = word
                } else cur = trial
            }
            out.add(cur)
        }
        return out
    }

    /** Ornate royal panel: maroon body with double gold border and corner lotuses. */
    fun panel(c: Canvas, l: Float, t: Float, r: Float, b: Float, alpha: Float = 1f) {
        tmpRect.set(l + 6, t + 10, r + 6, b + 10)
        c.drawRoundRect(tmpRect, 28f, 28f, P.f(0x66000000, alpha))
        tmpRect.set(l, t, r, b)
        val g = P.vgrad(t, b, 0xFF8A1A2A.toInt(), MAROON_DARK)
        g.alpha = (255 * alpha).toInt()
        c.drawRoundRect(tmpRect, 28f, 28f, g)
        c.drawRoundRect(tmpRect, 28f, 28f, P.s(GOLD, 6f, alpha))
        tmpRect.inset(14f, 14f)
        c.drawRoundRect(tmpRect, 18f, 18f, P.s(GOLD_DARK, 2.5f, alpha))
        for ((cx, cy) in listOf(l to t, r to t, l to b, r to b)) {
            c.drawCircle(cx, cy, 16f, P.f(GOLD, alpha))
            c.drawCircle(cx, cy, 8f, P.f(MAROON, alpha))
        }
    }

    /** Parchment page used by the story book. */
    fun parchment(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        tmpRect.set(l + 8, t + 12, r + 8, b + 12)
        c.drawRoundRect(tmpRect, 20f, 20f, P.f(0x55000000))
        tmpRect.set(l, t, r, b)
        c.drawRoundRect(tmpRect, 20f, 20f, P.vgrad(t, b, 0xFFFBEFD0.toInt(), 0xFFEBCF95.toInt()))
        c.drawRoundRect(tmpRect, 20f, 20f, P.s(0xFF9C6B2E.toInt(), 4f))
        tmpRect.inset(16f, 16f)
        c.drawRoundRect(tmpRect, 12f, 12f, P.s(0x889C6B2E.toInt(), 2f))
    }

    fun roundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, p: Paint) {
        tmpRect.set(l, t, r, b)
        c.drawRoundRect(tmpRect, rad, rad, p)
    }

    fun star(c: Canvas, cx: Float, cy: Float, r: Float, filled: Boolean, alpha: Float = 1f) {
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = -PI / 2 + i * PI / 5
            val x = cx + (cos(a) * rad).toFloat()
            val y = cy + (sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) {
            c.drawPath(path, P.vgrad(cy - r, cy + r, 0xFFFFF3A0.toInt(), 0xFFFF9800.toInt()).also { it.alpha = (255 * alpha).toInt() })
        } else {
            c.drawPath(path, P.f(0x553A2010, alpha))
        }
        c.drawPath(path, P.s(MAROON_DARK, r * 0.12f, alpha))
    }

    /** A small oil lamp (diya). */
    fun diya(c: Canvas, x: Float, y: Float, s: Float, t: Float, lit: Float = 1f) {
        if (lit > 0f) {
            c.drawCircle(x, y - 14 * s, 46 * s, P.glow(x, y - 14 * s, 46 * s, 0xFFFFB030.toInt(), 0.55f * lit))
        }
        val path = Path()
        path.moveTo(x - 22 * s, y - 6 * s)
        path.quadTo(x, y + 22 * s, x + 22 * s, y - 6 * s)
        path.quadTo(x + 30 * s, y - 10 * s, x + 32 * s, y - 14 * s)
        path.lineTo(x + 18 * s, y - 10 * s)
        path.close()
        c.drawPath(path, P.vgrad(y - 10 * s, y + 14 * s, 0xFFE08A3A.toInt(), 0xFF8A3A10.toInt()))
        c.drawLine(x - 20 * s, y - 6 * s, x + 20 * s, y - 6 * s, P.s(0xFFFFD27A.toInt(), 2.5f * s))
        if (lit > 0f) {
            val flick = 1f + 0.12f * sin(t * 13f + x * 0.1f)
            val fp = Path()
            fp.moveTo(x + 22 * s, y - 14 * s)
            fp.quadTo(x + 12 * s, y - 24 * s * flick, x + 24 * s, y - 40 * s * flick)
            fp.quadTo(x + 34 * s, y - 24 * s * flick, x + 26 * s, y - 14 * s)
            fp.close()
            c.drawPath(fp, P.vgrad(y - 40 * s, y - 14 * s, 0xFFFFF6B0.toInt(), 0xFFFF7A00.toInt()).also { it.alpha = (255 * lit).toInt() })
        }
    }
}

class Button(
    var label: String,
    var x: Float, var y: Float, var w: Float, var h: Float,
    var style: Int = PRIMARY,
    var icon: String = "",
    val onClick: () -> Unit
) {
    var visible = true
    var enabled = true
    var pressed = false
    var textSize = h * 0.42f

    fun contains(px: Float, py: Float) = px >= x - w / 2 && px <= x + w / 2 && py >= y - h / 2 && py <= y + h / 2

    fun draw(c: Canvas, t: Float) {
        val s = if (pressed) 0.95f else 1f
        val hw = w / 2 * s
        val hh = h / 2 * s
        val l = x - hw
        val r = x + hw
        val top = y - hh
        val b = y + hh
        Ui.roundRect(c, l + 4, top + 8, r + 4, b + 8, hh, P.f(0x66000000))
        val (c1, c2) = when (style) {
            PRIMARY -> 0xFFFFB13B.toInt() to 0xFFD9480F.toInt()
            SECONDARY -> 0xFF9C2A3C.toInt() to 0xFF4A0B16.toInt()
            GREEN -> 0xFF5BC86A.toInt() to 0xFF1E7A34.toInt()
            else -> 0xFF8A8A8A.toInt() to 0xFF4A4A4A.toInt()
        }
        val alpha = if (enabled) 1f else 0.5f
        Ui.roundRect(c, l, top, r, b, hh, P.vgrad(top, b, c1, c2).also { it.alpha = (255 * alpha).toInt() })
        Ui.roundRect(c, l + 6, top + 4, r - 6, top + hh * 0.9f, hh * 0.8f, P.f(0x33FFFFFF))
        Ui.roundRect(c, l, top, r, b, hh, P.s(Ui.GOLD, 4f, alpha))
        val label = if (icon.isEmpty()) label else "$icon  $label"
        // shrink long labels so they always fit inside the button
        val size = textSize * minOf(1f, (w - h * 0.6f) / Ui.measure(label, textSize).coerceAtLeast(1f))
        Ui.text(
            c, label, x, y + size * 0.36f, size * s, Color.WHITE,
            outline = 0xAA3A0610.toInt(), outlineW = size * 0.14f, alpha = alpha
        )
        if (style == PRIMARY && enabled) {
            // gentle shimmer sweep
            val sweep = ((t * 0.5f) % 1.6f) / 1.6f
            val sx = l + (r - l) * sweep
            c.drawCircle(sx, y - hh * 0.35f, hh * 0.35f, P.glow(sx, y - hh * 0.35f, hh * 0.5f, Color.WHITE, 0.25f))
        }
    }

    companion object {
        const val PRIMARY = 0
        const val SECONDARY = 1
        const val GREEN = 2
        const val GRAY = 3
    }
}
