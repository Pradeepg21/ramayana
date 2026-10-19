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

    fun iconBackground(c: Canvas) {
        c.drawRect(0f, 0f, S, S, P.vgrad3(0f, S, 0xFFFFC65C.toInt(), 0xFFF2780C.toInt(), 0xFF9C1C24.toInt()))
        // sun rays fanning out from behind Ram's head
        val cx = S / 2
        val cy = 430f
        for (k in 0 until 24) {
            val a0 = k * 2 * PI / 24
            val a1 = a0 + PI / 24
            path.reset()
            path.moveTo(cx, cy)
            path.lineTo(cx + (cos(a0) * 900).toFloat(), cy + (sin(a0) * 900).toFloat())
            path.lineTo(cx + (cos(a1) * 900).toFloat(), cy + (sin(a1) * 900).toFloat())
            path.close()
            c.drawPath(path, P.f(0x1FFFF3C4))
        }
        c.drawCircle(cx, cy, 420f, P.glow(cx, cy, 420f, 0xFFFFF3C4.toInt(), 0.9f))
    }

    fun iconForeground(c: Canvas) {
        // Shri Ram giving his blessing, framed as a close bust inside the adaptive-icon safe zone
        val s = 3.3f
        val p = Characters.pose()
        p.blessing = true
        Characters.person(c, S / 2, 470f + 261f * s, s, Characters.RAM, 0.8f, p)
    }

    fun icon(c: Canvas) {
        iconBackground(c)
        iconForeground(c)
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
        export(File(dir, "icon_1024.png"), 1024, 1024, S, S) { icon(it) }
        export(File(dir, "icon_bg_1024.png"), 1024, 1024, S, S) { iconBackground(it) }
        export(File(dir, "icon_fg_1024.png"), 1024, 1024, S, S, transparent = true) { iconForeground(it) }
        export(File(dir, "feature_2048x1000.png"), 2048, 1000, V.W, FEATURE_H) { feature(it) }
    }
}
