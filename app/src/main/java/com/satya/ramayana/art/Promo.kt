package com.satya.ramayana.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Store artwork: the launcher icon layers and the Play Store feature graphic.
 * Rendered to PNG files by a debug-only hook (see MainActivity) so the art is
 * drawn by the same engine as the game.
 */
object Promo {
    private val path = Path()

    /** Icon art is drawn in a 1000x1000 space. Adaptive icons keep the middle 610 visible. */
    private const val S = 1000f

    /** One launcher-icon design: a background layer and a foreground layer kept inside the safe zone. */
    class IconStyle(val background: (Canvas) -> Unit, val foreground: (Canvas) -> Unit)

    val icons = listOf(
        IconStyle(::sunburst, ::blessing),
        IconStyle(::sunburst, ::archer),
        IconStyle(::nightSky, ::skyArcher),
        IconStyle(::medallion, ::emblem),
    )

    private fun sunburst(c: Canvas) {
        c.drawRect(0f, 0f, S, S, P.vgrad3(0f, S, 0xFFFFC65C.toInt(), 0xFFF2780C.toInt(), 0xFF9C1C24.toInt()))
        rays(c, S / 2, 430f, 0x1FFFF3C4)
        c.drawCircle(S / 2, 430f, 420f, P.glow(S / 2, 430f, 420f, 0xFFFFF3C4.toInt(), 0.9f))
    }

    private fun rays(c: Canvas, cx: Float, cy: Float, color: Int) {
        for (k in 0 until 24) {
            val a0 = k * 2 * PI / 24
            val a1 = a0 + PI / 24
            path.reset()
            path.moveTo(cx, cy)
            path.lineTo(cx + (cos(a0) * 1200).toFloat(), cy + (sin(a0) * 1200).toFloat())
            path.lineTo(cx + (cos(a1) * 1200).toFloat(), cy + (sin(a1) * 1200).toFloat())
            path.close()
            c.drawPath(path, P.f(color))
        }
    }

    private fun nightSky(c: Canvas) {
        c.drawRect(0f, 0f, S, S, P.vgrad3(0f, S, 0xFF140C3A.toInt(), 0xFF3B1A6B.toInt(), 0xFFB8406A.toInt()))
        val r = kotlin.random.Random(7)
        repeat(45) { i ->
            c.drawCircle(r.nextFloat() * S, r.nextFloat() * 620f, 2.5f + (i % 3) * 1.5f, P.f(Color.WHITE, 0.4f + (i % 4) * 0.15f))
        }
        c.drawCircle(470f, 470f, 380f, P.glow(470f, 470f, 380f, 0xFFFFD27A.toInt(), 0.55f))
    }

    private fun medallion(c: Canvas) {
        c.drawRect(0f, 0f, S, S, P.vgrad(0f, S, 0xFF8E1B2B.toInt(), 0xFF3A0610.toInt()))
        rays(c, S / 2, S / 2, 0x14FFC93C)
        c.drawCircle(S / 2, S / 2, 330f, P.glow(S / 2, S / 2, 330f, 0xFFFFB13B.toInt(), 0.75f))
        c.drawCircle(S / 2, S / 2, 318f, P.s(Ui.GOLD, 14f))
        c.drawCircle(S / 2, S / 2, 292f, P.s(0xFFFFE9A8.toInt(), 4f))
        for (k in 0 until 36) {
            val a = k * 2 * PI / 36
            c.drawCircle(S / 2 + (cos(a) * 340).toFloat(), S / 2 + (sin(a) * 340).toFloat(), 7f, P.f(Ui.GOLD))
        }
    }

    /** Shri Ram giving his blessing, framed as a close bust. */
    private fun blessing(c: Canvas) {
        val s = 3.3f
        val p = Characters.pose()
        p.blessing = true
        Characters.person(c, S / 2, 470f + 261f * s, s, Characters.RAM, 0.8f, p)
    }

    /** Ram drawing his bow towards the sky, arrow ready. */
    private fun archer(c: Canvas) {
        val s = 2.15f
        val aim = -0.6f
        val x = 400f
        val y = 445f + 261f * s
        val p = Characters.pose()
        p.aim = aim
        p.drawn = true
        Characters.person(c, x, y, s, Characters.RAM, 0.8f, p)
        val h = Characters.bowHand(x, y, s, aim)
        c.drawCircle(h[0] + cos(aim) * 80f, h[1] + sin(aim) * 80f, 90f, P.glow(h[0] + cos(aim) * 80f, h[1] + sin(aim) * 80f, 90f, 0xFFFFF3C4.toInt(), 0.9f))
    }

    /** Ram aiming high into the night sky with a glowing golden arrow. */
    private fun skyArcher(c: Canvas) {
        val s = 2.0f
        val aim = -1.0f
        val x = 420f
        val y = 500f + 261f * s
        val p = Characters.pose()
        p.aim = aim
        p.drawn = true
        Characters.person(c, x, y, s, Characters.RAM, 0.8f, p)
        val h = Characters.bowHand(x, y, s, aim)
        val tx = h[0] + cos(aim) * 150f
        val ty = h[1] + sin(aim) * 150f
        c.drawCircle(tx, ty, 70f, P.glow(tx, ty, 70f, 0xFFFFE082.toInt(), 1f))
        c.drawCircle(tx, ty, 10f, P.f(0xFFFFF8E1.toInt()))
    }

    /** Ram's portrait in front of a great golden bow and arrow, like a royal seal. */
    private fun emblem(c: Canvas) {
        // the bow, its string and the arrow, crossing behind Ram
        val dx = 0.7071f
        val dy = -0.7071f
        val cx = 430f
        val cy = 570f
        val half = 300f
        val t1x = cx - dy * half; val t1y = cy + dx * half
        val t2x = cx + dy * half; val t2y = cy - dx * half
        path.reset()
        path.moveTo(t1x, t1y)
        path.quadTo(cx + dx * 380f, cy + dy * 380f, t2x, t2y)
        c.drawLine(t1x, t1y, t2x, t2y, P.s(0xFFFFF8E1.toInt(), 5f))
        c.drawPath(path, P.s(0xFF3A0610.toInt(), 40f))
        c.drawPath(path, P.s(0xFFB8860B.toInt(), 28f))
        c.drawPath(path, P.s(Ui.GOLD, 14f))
        c.drawCircle(t1x, t1y, 18f, P.f(Ui.GOLD))
        c.drawCircle(t2x, t2y, 18f, P.f(Ui.GOLD))
        c.save()
        c.translate(cx - dx * 130f, cy - dy * 130f)
        c.scale(5f, 5f)
        Characters.arrow(c, 0f, 0f, kotlin.math.atan2(dy, dx), 118f, golden = true)
        c.restore()
        val s = 2.75f
        val p = Characters.pose()
        p.namaste = true
        Characters.person(c, 470f, 455f + 261f * s, s, Characters.RAM, 0.8f, p)
    }

    /** The design used for the launcher and the Play Store. */
    var chosen = 1

    fun icon(c: Canvas, style: IconStyle = icons[chosen]) {
        style.background(c)
        style.foreground(c)
    }

    /** Feature graphic, drawn in a 1600 x 781 space (the 1024x500 aspect). */
    const val FEATURE_H = 781f

    fun feature(c: Canvas) {
        val h = FEATURE_H
        val t = 1.3f
        Scenery.sky(c, 0xFF2B1B5A.toInt(), 0xFFC2477A.toInt(), 0xFFFFB46B.toInt(), h)
        Scenery.stars(c, t, 45, 260f)
        Scenery.sun(c, 1110f, 330f, 95f, 0xFFFFC46B.toInt())
        Scenery.hills(c, 560f, 60f, 0xFF5B2E5E.toInt(), 1f, color2 = 0xFF3B1A40.toInt())
        Scenery.palace(c, 1110f, 655f, 0.8f, t, 0.75f)
        Scenery.ground(c, 650f, 0xFF6B3A2A.toInt(), 0xFF2E1610.toInt())
        for (k in 0 until 12) Ui.diya(c, 60f + k * 135f, 755f, 0.85f, t + k)

        // Lakshman, Ram, Sita and Hanuman
        Characters.person(c, 905f, 715f, 1.0f, Characters.LAKSHMAN, t)
        val sp = Characters.pose()
        sp.namaste = true
        Characters.person(c, 1345f, 712f, 1.02f, Characters.SITA, t, sp, facing = -1f)
        c.drawCircle(1100f, 330f, 300f, P.glow(1100f, 330f, 300f, 0xFFFFE082.toInt(), 0.35f))
        val p = Characters.pose()
        p.aim = -0.85f
        p.drawn = true
        Characters.person(c, 1080f, 730f, 1.25f, Characters.RAM, t, p)
        Characters.vanar(c, 1500f, 735f, 1.05f, Characters.HANUMAN, t, facing = -1f)

        // title block
        val tx = 420f
        c.drawCircle(tx, 250f, 330f, P.glow(tx, 250f, 330f, 0xFFFFC107.toInt(), 0.32f))
        Ui.title(c, "रामायण", tx, 270f, 190f)
        Ui.text(c, "RAMAYANA", tx, 368f, 76f, Ui.CREAM, outline = Ui.MAROON_DARK)
        Ui.text(c, "The Journey of Shri Ram", tx, 440f, 48f, Ui.GOLD, outline = Ui.MAROON_DARK)
        Ui.text(c, "10 stages  ·  30 levels  ·  stories read aloud", tx, 520f, 34f, Color.WHITE, face = P.sansBold, outline = 0xCC2A0A10.toInt())
    }

    // ---------------------------------------------------------------- export

    /** Renders [draw] (working in a [vw] x [vh] space) into a [w] x [h] PNG. */
    fun export(file: File, w: Int, h: Int, vw: Float, vh: Float, transparent: Boolean = false, draw: (Canvas) -> Unit) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (!transparent) c.drawColor(Color.BLACK)
        c.scale(w / vw, h / vh)
        draw(c)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }

    /** All the store art at the sizes needed by Play and the launcher. */
    fun exportAll(dir: File) {
        dir.mkdirs()
        for ((i, style) in icons.withIndex()) {
            export(File(dir, "icon${i}_1024.png"), 1024, 1024, S, S) { icon(it, style) }
            export(File(dir, "icon${i}_bg_1024.png"), 1024, 1024, S, S) { style.background(it) }
            export(File(dir, "icon${i}_fg_1024.png"), 1024, 1024, S, S, transparent = true) { style.foreground(it) }
        }
        export(File(dir, "feature_2048x1000.png"), 2048, 1000, V.W, FEATURE_H) { feature(it) }
    }
}
