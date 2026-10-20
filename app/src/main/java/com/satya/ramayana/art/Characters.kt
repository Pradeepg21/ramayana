package com.satya.ramayana.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Ui
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Look(
    val skin: Int,
    val garment: Int,
    val sash: Int,
    val crown: Int = Characters.CROWN_GOLD,
    val female: Boolean = false,
    val beard: Boolean = false,
    val hair: Int = 0xFF16100C.toInt(),
    val tilak: Boolean = true,
    val halo: Boolean = false,
    val mustache: Boolean = false,
    val build: Float = 1f,
    val horns: Boolean = false,
    val mala: Boolean = false,
)

class Pose {
    /** Aim angle in radians (0 = facing direction, negative = up). NaN = no bow. */
    var aim = Float.NaN
    var drawn = true          // bow string pulled with arrow nocked
    var walk = Float.NaN      // walking phase
    var armsUp = 0f           // 0..1 raising arms (lifting / garland)
    var namaste = false
    var catchPose = false
    var holdGarland = false
    var bob = 0f
    var jump = false
    var flying = false
    var blessing = false      // abhaya mudra - raised palm
    var holdGada = false

    fun reset(): Pose {
        aim = Float.NaN; drawn = true; walk = Float.NaN; armsUp = 0f
        namaste = false; catchPose = false; holdGarland = false; bob = 0f
        jump = false; flying = false; blessing = false; holdGada = false
        return this
    }
}

/**
 * Procedurally drawn characters with natural proportions (about 7.5 heads tall),
 * in the style of traditional Ramayana paintings. Each figure is drawn in its own
 * local space with the feet at (0,0); the top of a person's head is at about y = -282.
 * Arms and legs use simple two-bone IK so poses look natural.
 */
object Characters {
    const val CROWN_NONE = 0
    const val CROWN_GOLD = 1
    const val CROWN_SAGE = 2
    const val CROWN_RAVAN = 3
    const val CROWN_SITA = 4

    // skeleton (local units)
    private const val SHOULDER_Y = -222f
    private const val SHOULDER_X = 30f
    private const val UPPER = 54f
    private const val FORE = 50f
    private const val HIP_Y = -136f
    private const val THIGH = 66f
    private const val SHIN = 62f
    const val HEAD_Y = -261f
    /** How far back from the bow hand the string is drawn: to the chin. */
    private const val DRAW_PULL = 112f
    private const val VANAR_K = 0.82f

    val RAM = Look(0xFF4A86E8.toInt(), 0xFFFFB300.toInt(), 0xFFE65100.toInt(), halo = true, mala = true)
    val LAKSHMAN = Look(0xFFF5C99B.toInt(), 0xFFFF8F00.toInt(), 0xFFC62828.toInt())
    val SITA = Look(0xFFFAD3AE.toInt(), 0xFFD81B60.toInt(), 0xFFFFC107.toInt(), CROWN_SITA, female = true, tilak = false, halo = true)
    val SAGE = Look(0xFFD49C68.toInt(), 0xFFF57C00.toInt(), 0xFFFFB74D.toInt(), CROWN_SAGE, beard = true, hair = 0xFFE3E3E3.toInt(), build = 0.92f)
    val KING = Look(0xFFE0AC7C.toInt(), 0xFFFFF8E7.toInt(), 0xFF8E24AA.toInt(), CROWN_GOLD, beard = true, hair = 0xFFEEEEEE.toInt(), mustache = true)
    val RAVAN = Look(0xFF5E4570.toInt(), 0xFF2A0A22.toInt(), 0xFFB71C1C.toInt(), CROWN_RAVAN, tilak = false, mustache = true, build = 1.25f)
    val VIBHISHAN = Look(0xFF8D6E63.toInt(), 0xFF1565C0.toInt(), 0xFFFFC107.toInt(), CROWN_GOLD, mustache = true)

    private const val INK = 0xFF2A140A.toInt()
    private const val INK_W = 2.4f

    private val path = Path()
    private val path2 = Path()
    private val path3 = Path()
    private val rect = RectF()
    private val tmpPose = Pose()
    private val vanarPose = Pose()
    private val demonPose = Pose()
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val demonLooks = HashMap<Int, Look>()

    // joint scratch: elbowX, elbowY, handX, handY / kneeX, kneeY, ankleX, ankleY
    private val armF = FloatArray(4)
    private val armB = FloatArray(4)
    private val legF = FloatArray(4)
    private val legB = FloatArray(4)

    fun pose() = tmpPose.reset()

    /** World position of Ram's bow hand for a person drawn at (x,y) with scale s, facing right. */
    fun bowHand(x: Float, y: Float, s: Float, aim: Float): FloatArray =
        floatArrayOf(x + (SHOULDER_X + cos(aim) * 100f) * s, y + (SHOULDER_Y + sin(aim) * 100f) * s)

    // =============================================================== PERSON

    fun person(
        c: Canvas, x: Float, y: Float, s: Float, look: Look, t: Float, pose: Pose? = null,
        facing: Float = 1f, child: Boolean = false, drawHead: Boolean = true
    ) {
        val p = pose ?: tmpPose.reset()
        c.save()
        c.translate(x, y + p.bob)
        c.scale(s * facing, s)
        if (child) c.scale(0.8f, 0.8f)
        rect.set(-46f, -7f, 46f, 7f)
        c.drawOval(rect, P.f(0x44000000))
        humanoid(c, look, p, t, drawHead, null, child)
        c.restore()
    }

    private fun humanoid(c: Canvas, look: Look, p: Pose, t: Float, drawHead: Boolean, v: VanarStyle?, child: Boolean = false) {
        val b = look.build
        computeLegs(p)
        computeArms(p, t, b)
        val skin = look.skin
        val skinBack = shade(skin, 0.84f)

        if (look.halo && drawHead) halo(c, t)
        if (v != null && !v.bear) tail(c, v, t)
        if (drawHead && v == null) hairBack(c, look, child)
        if (v == null && look.crown != CROWN_SAGE) drapeBack(c, look, t, b)

        // back arm (+ Hanuman's gada resting on his shoulder). When its hand comes in front of the
        // chest it is drawn over the torso instead: for namaste/catching, and for an archer it goes
        // on top of everything so the hand visibly holds the drawn string at the chin.
        val aiming = !p.aim.isNaN()
        val backInFront = aiming || p.namaste || p.catchPose
        if (!backInFront) drawArm(c, -SHOULDER_X * b, armB, skinBack, b, v == null || v.crown, false)
        if (v != null && v.gada) {
            when {
                p.flying -> gada(c, armB[2], armB[3], 160f)
                p.holdGada -> gada(c, armB[2], armB[3], -28f)
            }
        }

        // legs
        if (look.female) {
            for (fx in floatArrayOf(-9f, 7f)) {
                rect.set(fx - 4f, -4f, fx + 14f, 4f)
                c.drawOval(rect, P.f(if (fx < 0) skinBack else skin))
            }
        } else {
            drawLeg(c, -10f, legB, skinBack, b)
            drawLeg(c, 10f, legF, skin, b)
        }

        // lower garment
        when {
            v != null -> langot(c, look, v, b)
            look.female -> saree(c, look, p, t)
            else -> dhoti(c, look, p, t, b)
        }

        // torso
        neck(c, skin)
        if (look.female) torsoFemale(c, look) else torsoMale(c, look, b, v)
        decorations(c, look, p, b, v)
        if (backInFront && !aiming) drawArm(c, -SHOULDER_X * b, armB, skinBack, b, v == null || v.crown, false)

        // head
        if (drawHead) {
            c.save()
            c.translate(0f, HEAD_Y)
            if (child) {
                c.translate(0f, 3f)
                c.scale(1.18f, 1.18f)
            }
            when {
                v == null -> humanHead(c, look, t)
                v.bear -> bearHead(c, v)
                else -> monkeyHead(c, v)
            }
            c.restore()
        }

        // front arm
        drawArm(c, SHOULDER_X * b, armF, skin, b, v == null || v.crown, p.aim.isNaN())
        if (v != null && v.gada && !p.flying && !p.holdGada) gada(c, armF[2], armF[3], 12f)
        if (aiming) {
            bow(c, armF[2], armF[3], p.aim, p.drawn)
            drawArm(c, -SHOULDER_X * b, armB, skinBack, b, v == null || v.crown, false)
        }
    }

    // --------------------------------------------------------------- skeleton

    private fun computeLegs(p: Pose) {
        val tf: Float
        val sf: Float
        val tb: Float
        val sb: Float
        when {
            p.flying -> { tf = -0.15f; sf = -0.25f; tb = -0.38f; sb = -0.45f }
            p.jump -> { tf = 1.0f; sf = -1.7f; tb = 0.45f; sb = -1.3f }
            !p.walk.isNaN() -> {
                tf = sin(p.walk) * 0.42f
                tb = -tf
                sf = -0.55f * max(0f, cos(p.walk))
                sb = -0.55f * max(0f, -cos(p.walk))
            }
            else -> { tf = 0.04f; sf = -0.02f; tb = -0.04f; sb = 0f }
        }
        legPoints(10f, tf, sf, legF)
        legPoints(-10f, tb, sb, legB)
    }

    private fun legPoints(hx: Float, ta: Float, sa: Float, out: FloatArray) {
        out[0] = hx + sin(ta) * THIGH
        out[1] = HIP_Y + cos(ta) * THIGH
        out[2] = out[0] + sin(ta + sa) * SHIN
        out[3] = out[1] + cos(ta + sa) * SHIN
    }

    private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

    private fun computeArms(p: Pose, t: Float, b: Float) {
        val sfx = SHOULDER_X * b
        val sbx = -SHOULDER_X * b
        val sy = SHOULDER_Y
        val sway = sin(t * 1.5f) * 2f
        var fx = sfx + 7f
        var fy = sy + 99f + sway
        // front view: elbows bend outward, mirrored left/right
        var fb = -1f
        var bx = sbx - 7f
        var by = sy + 99f
        var bb = 1f
        when {
            !p.aim.isNaN() -> {
                val dx = cos(p.aim)
                val dy = sin(p.aim)
                fx = sfx + dx * 100f
                fy = sy + dy * 100f
                // the drawing hand pulls the string all the way back to the chin
                val pull = if (p.drawn) DRAW_PULL else 30f
                bx = fx - dx * pull
                by = fy - dy * pull
                fb = 1f
                bb = 1f
            }
            p.flying -> {
                fx = sfx + 40f; fy = sy - 92f; fb = 1f
                bx = sbx + 12f; by = sy + 98f
            }
            p.armsUp > 0f -> {
                val u = p.armsUp
                fx = lerp(fx, sfx + 22f, u); fy = lerp(fy, sy - 98f, u)
                bx = lerp(bx, sbx - 22f, u); by = lerp(by, sy - 98f, u)
                fb = if (u > 0.3f) 1f else -1f
                bb = if (u > 0.3f) -1f else 1f
            }
            p.namaste -> {
                fx = 5f; fy = -186f; fb = -1f
                bx = 3f; by = -186f; bb = 1f
            }
            p.catchPose -> {
                fx = 66f; fy = -150f; fb = 1f
                bx = 46f; by = -148f
            }
            else -> {
                if (!p.walk.isNaN()) {
                    val af = sin(p.walk + PI.toFloat()) * 0.45f
                    fx = sfx + sin(af) * 99f; fy = sy + cos(af) * 99f
                    val ab = sin(p.walk) * 0.45f
                    bx = sbx + sin(ab) * 99f; by = sy + cos(ab) * 99f
                }
                if (p.blessing) { fx = sfx + 30f; fy = sy + 8f; fb = 1f }
                if (p.holdGada) { bx = -6f; by = sy + 34f }
            }
        }
        ik(sfx, sy, fx, fy, fb, armF)
        ik(sbx, sy, bx, by, bb, armB)
        if (!p.aim.isNaN() && p.drawn) {
            // archer's drawing arm: elbow held out at shoulder height, hand at the chin
            armB[0] = sbx - 28f; armB[1] = sy + 14f
            armB[2] = bx; armB[3] = by
        }
    }

    /** Two-bone IK. [bend] picks which side the elbow goes. */
    private fun ik(sx: Float, sy: Float, tx: Float, ty: Float, bend: Float, out: FloatArray) {
        var dx = tx - sx
        var dy = ty - sy
        var d = hypot(dx, dy)
        val maxD = UPPER + FORE - 0.5f
        if (d > maxD) {
            dx *= maxD / d; dy *= maxD / d; d = maxD
        }
        if (d < 1f) d = 1f
        val base = atan2(dy, dx)
        val cosA = ((UPPER * UPPER + d * d - FORE * FORE) / (2 * UPPER * d)).coerceIn(-1f, 1f)
        val ang = base + bend * acos(cosA)
        out[0] = sx + cos(ang) * UPPER
        out[1] = sy + sin(ang) * UPPER
        out[2] = sx + dx
        out[3] = sy + dy
    }

    // --------------------------------------------------------------- body parts

    /** Tapered limb segment: flat colour, a cel-shadow strip and an ink outline. */
    private fun limb(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, w1: Float, w2: Float, color: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = max(0.01f, hypot(dx, dy))
        val ux = dx / len
        val uy = dy / len
        val nx = -uy
        val ny = ux
        path.reset()
        path.moveTo(x1 + nx * w1, y1 + ny * w1)
        path.lineTo(x2 + nx * w2, y2 + ny * w2)
        path.quadTo(x2 + ux * w2 * 1.4f, y2 + uy * w2 * 1.4f, x2 - nx * w2, y2 - ny * w2)
        path.lineTo(x1 - nx * w1, y1 - ny * w1)
        path.quadTo(x1 - ux * w1 * 1.4f, y1 - uy * w1 * 1.4f, x1 + nx * w1, y1 + ny * w1)
        path.close()
        c.drawPath(path, P.f(color))
        // shadow side: always the side facing away from the viewer's light (left / down)
        val side = if (nx + ny * 0.3f > 0f) -1f else 1f
        path2.reset()
        path2.moveTo(x1 + side * nx * w1 * 0.15f, y1 + side * ny * w1 * 0.15f)
        path2.lineTo(x2 + side * nx * w2 * 0.15f, y2 + side * ny * w2 * 0.15f)
        path2.lineTo(x2 + side * nx * w2 * 2f, y2 + side * ny * w2 * 2f)
        path2.lineTo(x1 + side * nx * w1 * 2f, y1 + side * ny * w1 * 2f)
        path2.close()
        c.save()
        c.clipPath(path)
        c.drawPath(path2, P.f(shade(color, 0.8f)))
        c.restore()
        c.drawPath(path, P.s(INK, INK_W))
    }

    /** [mirror] flips the hand so the thumb sits on the other side (left/right hands differ). */
    private fun drawArm(c: Canvas, sx: Float, a: FloatArray, color: Int, b: Float, jewels: Boolean, mirror: Boolean) {
        val sy = SHOULDER_Y
        limb(c, sx, sy, a[0], a[1], 10.5f * b, 7.5f * b, color)
        c.drawCircle(sx, sy + 3f, 11f * b, P.f(color))
        c.drawArc(sx - 11f * b, sy + 3f - 11f * b, sx + 11f * b, sy + 3f + 11f * b, 180f, 180f, false, P.s(INK, INK_W))
        limb(c, a[0], a[1], a[2], a[3], 7.4f * b, 5f * b, color)
        // hand
        val ang = atan2(a[3] - a[1], a[2] - a[0]) * 57.2958f
        c.save()
        c.translate(a[2], a[3])
        c.rotate(ang)
        if (mirror) c.scale(1f, -1f)
        rect.set(-3f, -5.5f, 15f, 5.5f)
        c.drawOval(rect, P.f(color))
        c.drawOval(rect, P.s(INK, INK_W * 0.8f))
        rect.set(1f, -9f, 10f, -3.5f)
        c.drawOval(rect, P.f(color))
        c.drawOval(rect, P.s(INK, INK_W * 0.7f))
        c.drawLine(8f, -1.5f, 13f, -1.5f, P.s(INK, 1f))
        c.drawLine(8f, 1.8f, 13f, 1.8f, P.s(INK, 1f))
        c.restore()
        if (jewels) {
            // bajuband (armlet) and kada (bangle)
            val ax = sx + (a[0] - sx) * 0.38f
            val ay = sy + (a[1] - sy) * 0.38f
            val dx = a[0] - sx
            val dy = a[1] - sy
            val l = max(0.01f, hypot(dx, dy))
            val nx = -dy / l * 10f * b
            val ny = dx / l * 10f * b
            c.drawLine(ax + nx, ay + ny, ax - nx, ay - ny, P.s(INK, 5.5f))
            c.drawLine(ax + nx, ay + ny, ax - nx, ay - ny, P.s(Ui.GOLD, 3.5f))
            c.drawCircle(ax, ay, 2.6f, P.f(0xFFD32F2F.toInt()))
            val wx = a[0] + (a[2] - a[0]) * 0.86f
            val wy = a[1] + (a[3] - a[1]) * 0.86f
            val fdx = a[2] - a[0]
            val fdy = a[3] - a[1]
            val fl = max(0.01f, hypot(fdx, fdy))
            c.drawLine(wx - fdy / fl * 6.5f, wy + fdx / fl * 6.5f, wx + fdy / fl * 6.5f, wy - fdx / fl * 6.5f, P.s(INK, 5f))
            c.drawLine(wx - fdy / fl * 6.5f, wy + fdx / fl * 6.5f, wx + fdy / fl * 6.5f, wy - fdx / fl * 6.5f, P.s(Ui.GOLD, 3f))
        }
    }

    private fun drawLeg(c: Canvas, hipX: Float, l: FloatArray, color: Int, b: Float) {
        limb(c, hipX, HIP_Y, l[0], l[1], 13f * b, 9f * b, color)
        limb(c, l[0], l[1], l[2], l[3], 9.4f * b, 5.2f * b, color)
        rect.set(l[2] - 5f, l[3] - 3f, l[2] + 17f, l[3] + 8f)
        c.drawOval(rect, P.f(color))
        c.drawOval(rect, P.s(INK, INK_W * 0.8f))
        c.drawLine(l[2] - 6f, l[3] - 3f, l[2] + 6f, l[3] - 3f, P.s(Ui.GOLD, 2.5f))
    }

    private fun neck(c: Canvas, skin: Int) {
        rect.set(-7.5f, -252f, 7.5f, -236f)
        c.drawRect(rect, P.f(shade(skin, 0.8f)))
        c.drawLine(-7.5f, -252f, -7.5f, -238f, P.s(INK, INK_W * 0.8f))
        c.drawLine(7.5f, -252f, 7.5f, -238f, P.s(INK, INK_W * 0.8f))
    }

    private fun hShade(l: Float, r: Float, color: Int): Paint {
        shadePaint.shader = LinearGradient(
            l, 0f, r, 0f,
            intArrayOf(shade(color, 0.72f), lighten(color, 0.12f), color, shade(color, 0.68f)),
            floatArrayOf(0f, 0.38f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        return shadePaint
    }

    private fun torsoMale(c: Canvas, look: Look, b: Float, v: VanarStyle?) {
        val skin = look.skin
        path3.reset()
        path3.moveTo(-8f, -244f)
        path3.quadTo(-10f, -232f, -28f * b, -228f)
        path3.quadTo(-38f * b, -225f, -37f * b, -211f)
        path3.quadTo(-34f * b, -199f, -28f * b, -195f)
        path3.quadTo(-21f * b, -180f, -21f * b, -165f)
        path3.quadTo(-23f * b, -150f, -27f * b, -134f)
        path3.lineTo(27f * b, -134f)
        path3.quadTo(23f * b, -150f, 21f * b, -165f)
        path3.quadTo(21f * b, -180f, 28f * b, -195f)
        path3.quadTo(34f * b, -199f, 37f * b, -211f)
        path3.quadTo(38f * b, -225f, 28f * b, -228f)
        path3.quadTo(10f, -232f, 8f, -244f)
        path3.close()
        c.drawPath(path3, P.f(skin))
        c.save()
        c.clipPath(path3)
        if (v != null) {
            rect.set(-17f * b, -212f, 17f * b, -146f)
            c.drawOval(rect, P.f(v.face))
        }
        // cel shadows: the far side of the body and under the chest
        path2.reset()
        path2.moveTo(12f * b, -240f)
        path2.quadTo(4f * b, -190f, 14f * b, -130f)
        path2.lineTo(50f * b, -130f)
        path2.lineTo(50f * b, -240f)
        path2.close()
        c.drawPath(path2, P.f(shade(skin, 0.8f), 0.85f))
        path2.reset()
        path2.moveTo(-27f * b, -205f); path2.quadTo(-14f * b, -186f, -2f, -197f)
        path2.lineTo(-2f, -191f); path2.quadTo(-14f * b, -181f, -27f * b, -199f); path2.close()
        path2.moveTo(27f * b, -205f); path2.quadTo(14f * b, -186f, 2f, -197f)
        path2.lineTo(2f, -191f); path2.quadTo(14f * b, -181f, 27f * b, -199f); path2.close()
        c.drawPath(path2, P.f(shade(skin, 0.78f)))
        c.restore()
        path.reset()
        path.moveTo(-27f * b, -207f); path.quadTo(-14f * b, -188f, -2f, -199f)
        path.moveTo(27f * b, -207f); path.quadTo(14f * b, -188f, 2f, -199f)
        c.drawPath(path, P.s(INK, 1.4f))
        c.drawLine(0f, -192f, 0f, -170f, P.s(INK, 1f, 0.6f))
        if (b > 1.05f) {
            for (k in 0..1) {
                val yy = -184f + k * 10f
                c.drawLine(-10f, yy, -3f, yy + 1f, P.s(INK, 1f, 0.6f))
                c.drawLine(10f, yy, 3f, yy + 1f, P.s(INK, 1f, 0.6f))
            }
        }
        path.reset(); path.moveTo(-2.5f, -159f); path.quadTo(0f, -156f, 2.5f, -159f)
        c.drawPath(path, P.s(INK, 1.2f))
        c.drawPath(path3, P.s(INK, INK_W))
    }

    private fun torsoFemale(c: Canvas, look: Look) {
        val skin = look.skin
        path3.reset()
        path3.moveTo(-7f, -244f)
        path3.quadTo(-9f, -233f, -22f, -229f)
        path3.quadTo(-30f, -226f, -29f, -213f)
        path3.quadTo(-27f, -202f, -22f, -197f)
        path3.quadTo(-16f, -182f, -17f, -166f)
        path3.lineTo(-20f, -156f)
        path3.lineTo(20f, -156f)
        path3.lineTo(17f, -166f)
        path3.quadTo(16f, -182f, 22f, -197f)
        path3.quadTo(27f, -202f, 29f, -213f)
        path3.quadTo(30f, -226f, 22f, -229f)
        path3.quadTo(9f, -233f, 7f, -244f)
        path3.close()
        c.drawPath(path3, P.f(skin))
        c.save()
        c.clipPath(path3)
        c.drawRect(10f, -250f, 40f, -150f, P.f(shade(skin, 0.82f)))
        c.restore()
        c.drawPath(path3, P.s(INK, INK_W))
        // choli
        rect.set(-25f, -218f, 25f, -186f)
        c.drawRoundRect(rect, 9f, 9f, P.f(shade(look.garment, 0.85f)))
        c.drawRoundRect(rect, 9f, 9f, P.s(INK, INK_W * 0.8f))
        c.drawLine(-22f, -188f, 22f, -188f, P.s(Ui.GOLD, 2f))
        // pallu across the body, from the right waist up over the left shoulder
        path.reset()
        path.moveTo(-28f, -229f)
        path.lineTo(-13f, -233f)
        path.quadTo(8f, -200f, 25f, -170f)
        path.lineTo(24f, -156f)
        path.lineTo(10f, -156f)
        path.quadTo(-6f, -190f, -28f, -229f)
        path.close()
        c.drawPath(path, P.f(lighten(look.garment, 0.06f)))
        c.drawPath(path, P.s(INK, INK_W * 0.9f))
        path2.reset()
        path2.moveTo(-13f, -230f); path2.quadTo(7f, -198f, 23f, -170f)
        c.drawPath(path2, P.s(Ui.GOLD, 2.4f))
        for (k in 0..4) c.drawCircle(-12f + k * 6.5f, -214f + k * 11f, 1.6f, P.f(Ui.GOLD))
    }

    private fun drapeBack(c: Canvas, look: Look, t: Float, b: Float) {
        val sw = sin(t * 1.7f) * 4f
        if (look.female) {
            path.reset()
            path.moveTo(-20f, -231f)
            path.quadTo(-46f, -180f, -42f + sw, -108f)
            path.lineTo(-26f + sw, -110f)
            path.quadTo(-30f, -180f, -10f, -229f)
            path.close()
            c.drawPath(path, P.f(shade(look.garment, 0.8f)))
            c.drawPath(path, P.s(INK, INK_W))
            return
        }
        val col = shade(look.sash, 0.85f)
        for (side in floatArrayOf(-1f, 1f)) {
            path.reset()
            path.moveTo(side * 26f * b, -229f)
            path.quadTo(side * 46f * b, -190f, side * 46f * b + sw, -132f)
            path.lineTo(side * 34f * b + sw, -128f)
            path.quadTo(side * 34f * b, -190f, side * 18f * b, -228f)
            path.close()
            c.drawPath(path, P.f(col))
            c.drawPath(path, P.s(INK, INK_W))
        }
    }

    private fun dhoti(c: Canvas, look: Look, p: Pose, t: Float, b: Float) {
        val wy = -164f
        val g = look.garment
        val fo = max(legF[0] + 13f, 18f)
        val bo = min(legB[0] - 13f, -18f)
        val fy = legF[1] + 9f
        val by = legB[1] + 9f
        val cy = min(legF[1], legB[1]) - 2f
        path.reset()
        path.moveTo(-27f * b, wy)
        path.lineTo(27f * b, wy)
        path.quadTo(35f * b, -140f, fo, fy)
        path.quadTo((legF[0] + legB[0]) / 2f, cy, bo, by)
        path.quadTo(-35f * b, -140f, -27f * b, wy)
        path.close()
        c.drawPath(path, P.f(g))
        c.save()
        c.clipPath(path)
        c.drawRect(8f, wy, 60f, 0f, P.f(shade(g, 0.82f)))
        c.restore()
        c.drawPath(path, P.s(INK, INK_W))
        // folds
        for (i in 0..3) {
            val x0 = -18f + i * 12f
            val k = i / 3f
            path2.reset()
            path2.moveTo(x0, wy + 6f)
            path2.quadTo(x0 * 1.1f, -112f, lerp(legB[0], legF[0], k), (fy + by) / 2f - 6f)
            c.drawPath(path2, P.s(shade(g, 0.6f), 1.4f, 0.8f))
        }
        // gold hem
        path2.reset()
        path2.moveTo(fo, fy)
        path2.quadTo((legF[0] + legB[0]) / 2f, cy, bo, by)
        c.drawPath(path2, P.s(Ui.GOLD, 3f))
        // pleated front panel
        val sw = if (p.walk.isNaN()) sin(t * 1.3f) * 1.5f else sin(p.walk) * 5f
        path.reset()
        path.moveTo(-7f, wy + 2f)
        path.lineTo(7f, wy + 2f)
        path.lineTo(10f + sw, -46f)
        path.lineTo(5f + sw, -41f)
        path.lineTo(sw, -46f)
        path.lineTo(-5f + sw, -41f)
        path.lineTo(-10f + sw, -46f)
        path.close()
        c.drawPath(path, P.f(lighten(g, 0.1f)))
        c.drawPath(path, P.s(INK, INK_W * 0.8f))
        c.drawLine(-3f, wy + 4f, -3f + sw, -44f, P.s(shade(g, 0.7f), 1.2f))
        c.drawLine(3f, wy + 4f, 3f + sw, -44f, P.s(shade(g, 0.7f), 1.2f))
        c.drawLine(-7f, wy + 2f, -10f + sw, -46f, P.s(Ui.GOLD, 1.6f))
        c.drawLine(7f, wy + 2f, 10f + sw, -46f, P.s(Ui.GOLD, 1.6f))
        // kamarbandh (jewelled belt) with hanging chains
        Ui.roundRect(c, -28f * b, wy - 6f, 28f * b, wy + 3f, 3f, P.f(Ui.GOLD))
        Ui.roundRect(c, -28f * b, wy - 6f, 28f * b, wy + 3f, 3f, P.s(INK, INK_W * 0.8f))
        c.drawCircle(0f, wy - 1.5f, 3.8f, P.f(0xFFD32F2F.toInt()))
        for (side in floatArrayOf(-1f, 1f)) {
            val ex = side * 13f + sw * 0.3f
            c.drawLine(side * 12f, wy + 3f, ex, wy + 24f, P.s(Ui.GOLD, 1.5f))
            c.drawCircle(ex, wy + 25f, 2.2f, P.f(Ui.GOLD))
        }
    }

    private fun saree(c: Canvas, look: Look, p: Pose, t: Float) {
        val wy = -164f
        val g = look.garment
        val sw = if (p.walk.isNaN()) sin(t * 1.2f) * 1.5f else sin(p.walk) * 6f
        val hem = -2f
        path.reset()
        path.moveTo(-21f, wy)
        path.lineTo(21f, wy)
        path.cubicTo(30f, -120f, 40f + sw, -50f, 44f + sw, hem)
        path.quadTo(sw, hem + 8f, -44f + sw, hem)
        path.cubicTo(-40f + sw, -50f, -30f, -120f, -21f, wy)
        path.close()
        c.drawPath(path, P.f(g))
        c.save()
        c.clipPath(path)
        path3.reset()
        path3.moveTo(10f, wy); path3.cubicTo(18f, -110f, 22f + sw, -50f, 20f + sw, hem + 10f)
        path3.lineTo(80f, hem + 10f); path3.lineTo(80f, wy); path3.close()
        c.drawPath(path3, P.f(shade(g, 0.8f)))
        c.restore()
        c.drawPath(path, P.s(INK, INK_W))
        for (k in 0..5) {
            c.drawLine(2f + k * 2f, wy + 20f, -14f + k * 7f + sw, hem + 3f, P.s(shade(g, 0.66f), 1.4f, 0.7f))
        }
        // buti motifs
        for (k in 0 until 12) {
            val yy = -150f + (k * 37 % 130)
            val half = 22f + (yy - wy) / (hem - wy) * 20f
            val xx = -half + ((k * 53) % 100) / 100f * half * 2f
            c.drawCircle(xx + sw * (yy - wy) / (hem - wy), yy, 1.8f, P.f(Ui.GOLD))
        }
        path2.reset()
        path2.moveTo(-44f + sw, hem); path2.quadTo(sw, hem + 8f, 44f + sw, hem)
        c.drawPath(path2, P.s(Ui.GOLD, 6f))
        path2.reset()
        path2.moveTo(-42f + sw, hem - 10f); path2.quadTo(sw, hem - 2f, 42f + sw, hem - 10f)
        c.drawPath(path2, P.s(Ui.GOLD, 1.5f))
        Ui.roundRect(c, -22f, wy - 5f, 22f, wy + 2f, 3f, P.f(Ui.GOLD))
    }

    private fun langot(c: Canvas, look: Look, v: VanarStyle, b: Float) {
        val wy = -160f
        path.reset()
        path.moveTo(-27f * b, wy)
        path.lineTo(27f * b, wy)
        path.lineTo(25f * b, -126f)
        path.quadTo(0f, -116f, -25f * b, -126f)
        path.close()
        c.drawPath(path, P.f(look.garment))
        c.drawPath(path, P.s(INK, INK_W))
        path.reset()
        path.moveTo(-8f, wy); path.lineTo(8f, wy); path.lineTo(11f, -104f); path.lineTo(-11f, -104f); path.close()
        c.drawPath(path, P.f(lighten(look.garment, 0.08f)))
        c.drawPath(path, P.s(INK, INK_W * 0.8f))
        Ui.roundRect(c, -27f * b, wy - 5f, 27f * b, wy + 2f, 3f, P.f(if (v.crown) Ui.GOLD else 0xFF6D4C41.toInt()))
    }

    private fun decorations(c: Canvas, look: Look, p: Pose, b: Float, v: VanarStyle?) {
        if (v == null && !look.female && look.crown != CROWN_SAGE && !look.horns) {
            // sacred thread
            c.drawLine(-20f * b, -227f, 20f * b, -150f, P.s(0xFFFFF8E1.toInt(), 1.4f, 0.9f))
            // angavastra over the shoulders
            path.reset()
            path.moveTo(-35f * b, -225f)
            path.quadTo(0f, -199f, 35f * b, -225f)
            path.quadTo(0f, -210f, -35f * b, -225f)
            path.close()
            c.drawPath(path, P.f(look.sash))
            c.drawPath(path, P.s(INK, INK_W * 0.8f))
            path2.reset(); path2.moveTo(-33f * b, -223f); path2.quadTo(0f, -201f, 33f * b, -223f)
            c.drawPath(path2, P.s(Ui.GOLD, 1.5f))
        }
        if (look.crown == CROWN_SAGE) {
            beads(c, -12f, -240f, 0f, -150f, 12f, -240f, 18, 2.6f, intArrayOf(0xFF6D3B1F.toInt()))
            return
        }
        val crowned = v == null || v.crown
        if (crowned && !look.horns) {
            path.reset()
            path.moveTo(-9f, -241f); path.quadTo(0f, -224f, 9f, -241f)
            c.drawPath(path, P.s(Ui.GOLD, 2.6f))
            path.reset()
            path.moveTo(-13f, -239f); path.quadTo(0f, -206f, 13f, -239f)
            c.drawPath(path, P.s(Ui.GOLD, 2f))
            c.drawCircle(0f, -214f, 3.6f, P.f(0xFFD32F2F.toInt()))
            c.drawCircle(0f, -214f, 3.6f, P.s(Ui.GOLD, 1.2f))
        }
        if (look.mala) {
            beads(c, -15f, -238f, 0f, -96f, 15f, -238f, 26, 3f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFD54F.toInt(), 0xFFE53935.toInt()))
        }
        if (p.holdGarland || (v != null && v.garland)) {
            beads(c, -20f, -236f, 0f, -140f, 20f, -236f, 18, 5.5f, intArrayOf(0xFFFF7043.toInt(), 0xFFFFEB3B.toInt()))
        }
        if (v != null && v.spy) {
            path.reset()
            path.moveTo(-32f, -230f); path.lineTo(32f, -230f); path.lineTo(42f, -60f)
            path.quadTo(0f, -48f, -42f, -60f); path.close()
            c.drawPath(path, P.f(0xDD4A148C.toInt()))
        }
    }

    /** Draws beads along a quadratic curve - garlands and malas. */
    private fun beads(c: Canvas, x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, n: Int, r: Float, cols: IntArray) {
        for (i in 0..n) {
            val u = i / n.toFloat()
            val a = (1 - u) * (1 - u)
            val bb = 2 * (1 - u) * u
            val cc = u * u
            c.drawCircle(a * x0 + bb * cx + cc * x1, a * y0 + bb * cy + cc * y1, r, P.f(cols[i % cols.size]))
        }
    }

    private fun halo(c: Canvas, t: Float) {
        val hy = HEAD_Y - 8f
        c.drawCircle(0f, hy, 70f, P.glow(0f, hy, 70f, 0xFFFFE082.toInt(), 0.55f))
        c.drawCircle(0f, hy, 44f, P.s(0xFFFFD54F.toInt(), 2f, 0.8f))
        for (k in 0 until 16) {
            val a = k * PI.toFloat() / 8f + t * 0.15f
            c.drawLine(cos(a) * 47f, hy + sin(a) * 47f, cos(a) * 56f, hy + sin(a) * 56f, P.s(0xFFFFE082.toInt(), 2f, 0.6f))
        }
    }

    private fun hairBack(c: Canvas, look: Look, child: Boolean) {
        if (look.crown == CROWN_SAGE) return
        c.save()
        c.translate(0f, HEAD_Y)
        if (child) {
            c.translate(0f, 3f)
            c.scale(1.18f, 1.18f)
        }
        path.reset()
        if (look.female) {
            path.moveTo(-16f, -14f)
            path.cubicTo(-30f, 20f, -27f, 70f, -16f, 112f)
            path.lineTo(-8f, 104f)
            path.lineTo(0f, 118f)
            path.lineTo(8f, 104f)
            path.lineTo(16f, 112f)
            path.cubicTo(27f, 70f, 30f, 20f, 16f, -14f)
        } else {
            // long flowing locks ending in points
            path.moveTo(-16f, -14f)
            path.cubicTo(-30f, 4f, -24f, 24f, -34f, 50f)
            path.lineTo(-24f, 44f)
            path.lineTo(-20f, 58f)
            path.lineTo(-12f, 44f)
            path.lineTo(0f, 52f)
            path.lineTo(12f, 44f)
            path.lineTo(20f, 58f)
            path.lineTo(24f, 44f)
            path.lineTo(34f, 50f)
            path.cubicTo(24f, 24f, 30f, 4f, 16f, -14f)
        }
        path.close()
        c.drawPath(path, P.f(look.hair))
        c.drawPath(path, P.s(INK, INK_W))
        // glossy streak
        path2.reset()
        path2.moveTo(-22f, 0f); path2.quadTo(-26f, 22f, -28f, 40f)
        c.drawPath(path2, P.s(lighten(look.hair, 0.25f), 2.5f, 0.7f))
        c.restore()
    }

    // --------------------------------------------------------------- heads (centred)

    private fun humanHead(c: Canvas, look: Look, t: Float) {
        val skin = look.skin
        val fs = 1.2f   // features turned slightly towards the facing direction
        // ears
        rect.set(-18.5f, -6f, -13f, 7f)
        c.drawOval(rect, P.f(shade(skin, 0.88f))); c.drawOval(rect, P.s(INK, 1.4f))
        rect.set(13f, -6f, 18.5f, 7f)
        c.drawOval(rect, P.f(shade(skin, 0.88f))); c.drawOval(rect, P.s(INK, 1.4f))
        // face: slightly angular anime jaw
        path3.reset()
        path3.moveTo(0f, -21f)
        path3.cubicTo(11f, -21f, 16f, -13f, 16f, -3f)
        path3.lineTo(15f, 5f)
        path3.quadTo(13f, 13f, 6f, 18f)
        path3.lineTo(fs * 0.5f, 21f)
        path3.lineTo(-6f, 18f)
        path3.quadTo(-13f, 13f, -15f, 5f)
        path3.lineTo(-16f, -3f)
        path3.cubicTo(-16f, -13f, -11f, -21f, 0f, -21f)
        path3.close()
        c.drawPath(path3, P.f(skin))
        c.save()
        c.clipPath(path3)
        // cel shadow on the far cheek and under the hairline
        path2.reset()
        path2.moveTo(9f, -22f); path2.quadTo(13f, 0f, 5f, 22f); path2.lineTo(30f, 22f); path2.lineTo(30f, -22f); path2.close()
        c.drawPath(path2, P.f(shade(skin, 0.84f)))
        c.drawRect(-20f, -22f, 20f, -12f, P.f(shade(skin, 0.86f)))
        c.restore()
        c.drawPath(path3, P.s(INK, INK_W * 0.85f))
        // front hair: spiky bangs with a middle parting
        if (look.crown != CROWN_SAGE) {
            path.reset()
            path.moveTo(-16.5f, -1f)
            path.cubicTo(-18f, -18f, -9f, -25f, 0f, -24f)
            path.cubicTo(9f, -25f, 18f, -18f, 16.5f, -1f)
            path.lineTo(13f, -9f)
            path.lineTo(11f, -4f)
            path.lineTo(8f, -14f)
            path.lineTo(3f, -11f)
            path.lineTo(0f, -16f)
            path.lineTo(-3f, -11f)
            path.lineTo(-8f, -14f)
            path.lineTo(-11f, -4f)
            path.lineTo(-13f, -9f)
            path.close()
            c.drawPath(path, P.f(look.hair))
            c.drawPath(path, P.s(INK, INK_W * 0.8f))
            path2.reset(); path2.moveTo(-9f, -20f); path2.quadTo(-4f, -23f, 2f, -21f)
            c.drawPath(path2, P.s(lighten(look.hair, 0.3f), 1.6f, 0.8f))
            if (look.horns) {
                for (k in -3..3) c.drawLine(k * 5f, -20f, k * 8f, -32f - (k and 1) * 5f, P.s(look.hair, 4f))
            }
        } else {
            path.reset()
            path.moveTo(-16f, -3f)
            path.cubicTo(-17f, -20f, -8f, -24f, 0f, -24f)
            path.cubicTo(8f, -24f, 17f, -20f, 16f, -3f)
            path.quadTo(8f, -17f, 0f, -17f)
            path.quadTo(-8f, -17f, -16f, -3f)
            path.close()
            c.drawPath(path, P.f(look.hair))
            c.drawPath(path, P.s(INK, 1.4f))
        }
        // eyebrows: tapered, heroic
        for (side in intArrayOf(-1, 1)) {
            val ex = side * 6.8f + fs
            path.reset()
            if (look.horns || look.crown == CROWN_RAVAN) {
                path.moveTo(ex - side * 1.5f, -6f)
                path.lineTo(ex + side * 6f, -11.5f)
                path.lineTo(ex + side * 5f, -9f)
            } else {
                path.moveTo(ex - side * 4.5f, -7.5f)
                path.quadTo(ex, -11f, ex + side * 6f, -9.5f)
                path.lineTo(ex + side * 5f, -8.6f)
                path.quadTo(ex, -9.6f, ex - side * 4.5f, -7.5f)
            }
            path.close()
            c.drawPath(path, P.f(if (look.beard) 0xFFBDBDBD.toInt() else INK))
            c.drawPath(path, P.s(if (look.beard) 0xFFBDBDBD.toInt() else INK, 1.2f))
        }
        // big expressive anime eyes
        val blink = ((t * 0.7f) % 4f) > 3.87f
        val iris = when {
            look.horns -> 0xFFFFC400.toInt()
            look.female -> 0xFF5D3A1A.toInt()
            else -> 0xFF3A2414.toInt()
        }
        for (side in intArrayOf(-1, 1)) {
            val ex = side * 6.8f + fs
            if (blink) {
                path.reset(); path.moveTo(ex - 5f, -2f); path.quadTo(ex, 0.5f, ex + 5f, -2f)
                c.drawPath(path, P.s(INK, 1.8f))
                continue
            }
            path.reset()
            path.moveTo(ex - 5.6f, -1.6f)
            path.quadTo(ex, -7f, ex + 5.6f, -2.6f)
            path.quadTo(ex + 1f, 2.2f, ex - 5.6f, -1.6f)
            path.close()
            c.drawPath(path, P.f(Color.WHITE))
            c.save()
            c.clipPath(path)
            rect.set(ex - 2.4f, -6.2f, ex + 3.4f, 1.6f)
            c.drawOval(rect, P.f(iris))
            rect.set(ex - 1f, -4.6f, ex + 2f, 0f)
            c.drawOval(rect, P.f(if (look.horns) 0xFFB71C1C.toInt() else Color.BLACK))
            c.drawRect(ex - 6f, -8f, ex + 6f, -4.4f, P.f(0x33000000))
            c.restore()
            c.drawCircle(ex + 1.6f, -3.8f, 1.1f, P.f(Color.WHITE))
            c.drawCircle(ex - 0.6f, -0.6f, 0.5f, P.f(Color.WHITE))
            // thick upper lash line with outer flick
            path2.reset()
            path2.moveTo(ex - 6f, -1.2f)
            path2.quadTo(ex, -7.6f, ex + 6.2f, -2.8f)
            c.drawPath(path2, P.s(INK, if (look.female) 2.4f else 2f))
            if (look.female) {
                val ox = if (side > 0) ex + 6f else ex - 6f
                c.drawLine(ox, if (side > 0) -2.8f else -1.2f, ox + side * 2.2f, -4.8f, P.s(INK, 1.4f))
            }
            c.drawLine(ex - 2f, 1.4f, ex + 3.5f, 0.6f, P.s(INK, 0.8f, 0.7f))
        }
        // nose: a small anime stroke
        path.reset()
        path.moveTo(fs + 1.5f, 1f)
        path.lineTo(fs + 3f, 7f)
        path.lineTo(fs + 0.8f, 7.6f)
        c.drawPath(path, P.s(shade(skin, 0.55f), 1.2f))
        // mouth
        path.reset()
        if (look.horns) {
            path.moveTo(fs - 5f, 12f); path.quadTo(fs, 15f, fs + 5f, 11.5f)
            c.drawPath(path, P.s(INK, 1.4f))
        } else {
            path.moveTo(fs - 3.8f, 12.2f); path.quadTo(fs, 13.6f, fs + 3.8f, 12f)
            c.drawPath(path, P.s(INK, 1.3f))
            path.reset(); path.moveTo(fs - 2f, 14.6f); path.quadTo(fs, 15.6f, fs + 2f, 14.6f)
            c.drawPath(path, P.s(if (look.female) 0xFFD84B5A.toInt() else shade(skin, 0.6f), 1.4f))
        }
        if (look.female) {
            path.reset()
            path.moveTo(fs - 3.4f, 12.3f); path.quadTo(fs, 15.4f, fs + 3.4f, 12.1f); path.close()
            c.drawPath(path, P.f(0xFFE0566A.toInt()))
        }
        if (look.horns) {
            for (side in intArrayOf(-1, 1)) {
                val fx = fs + side * 3.6f
                path.reset(); path.moveTo(fx - 1.3f, 12.6f); path.lineTo(fx, 17.5f); path.lineTo(fx + 1.3f, 12.4f); path.close()
                c.drawPath(path, P.f(Color.WHITE))
                c.drawPath(path, P.s(INK, 0.8f))
                path.reset()
                path.moveTo(side * 9f, -17f)
                path.quadTo(side * 22f, -28f, side * 17f, -44f)
                path.quadTo(side * 14f, -30f, side * 3f, -20f)
                path.close()
                c.drawPath(path, P.f(0xFFFFF3E0.toInt()))
                c.drawPath(path, P.s(INK, 1.4f))
            }
        }
        // mustache
        if (look.mustache) {
            val mc = if (look.beard) look.hair else INK
            for (side in intArrayOf(-1, 1)) {
                path.reset()
                path.moveTo(fs, 9.4f)
                if (look.crown == CROWN_RAVAN) {
                    path.quadTo(fs + side * 7f, 6.5f, fs + side * 13f, 10.5f)
                    path.quadTo(fs + side * 16f, 7f, fs + side * 12f, 4.5f)
                    c.drawPath(path, P.s(mc, 2.8f))
                } else {
                    path.quadTo(fs + side * 6f, 8f, fs + side * 9f, 12f)
                    c.drawPath(path, P.s(mc, 2.2f))
                }
            }
        }
        // beard
        if (look.beard) {
            path.reset()
            path.moveTo(-15f, 2f)
            path.quadTo(-16f, 34f, 0f, 48f)
            path.quadTo(16f, 34f, 15f, 2f)
            path.quadTo(9f, 16f, fs, 17f)
            path.quadTo(-9f, 16f, -15f, 2f)
            path.close()
            c.drawPath(path, P.f(look.hair))
            c.drawPath(path, P.s(INK, 1.4f))
            for (k in -2..2) c.drawLine(k * 4f, 22f, k * 5f, 38f - kotlin.math.abs(k) * 4f, P.s(shade(look.hair, 0.75f), 1f))
        }
        // tilak / bindi
        if (look.tilak && look.crown != CROWN_RAVAN && !look.horns) {
            path.reset()
            path.moveTo(-3f, -18f); path.quadTo(0f, -9f, 3f, -18f)
            c.drawPath(path, P.s(0xFFFFF3E0.toInt(), 1.8f))
            c.drawLine(0f, -17f, 0f, -11f, P.s(0xFFE53935.toInt(), 2f))
        } else if (look.female) {
            c.drawCircle(fs * 0.5f, -11f, 1.9f, P.f(0xFFD50000.toInt()))
        }
        // kundal earrings
        for (side in intArrayOf(-1, 1)) {
            c.drawCircle(side * 16f, 9f, 3.2f, P.f(Ui.GOLD))
            c.drawCircle(side * 16f, 9f, 3.2f, P.s(INK, 1f))
            c.drawCircle(side * 16f, 14f, 1.8f, P.f(0xFFD32F2F.toInt()))
        }
        // headwear
        when (look.crown) {
            CROWN_GOLD -> kireeta(c, 1f, Ui.GOLD, 0xFFD32F2F.toInt())
            CROWN_RAVAN -> {
                kireeta(c, 1.12f, 0xFFD8A030.toInt(), 0xFF00C853.toInt())
                for (side in intArrayOf(-1, 1)) {
                    path.reset()
                    path.moveTo(side * 10f, -22f); path.lineTo(side * 22f, -52f); path.lineTo(side * 18f, -22f); path.close()
                    c.drawPath(path, P.f(0xFFD8A030.toInt()))
                    c.drawPath(path, P.s(INK, 1.2f))
                }
            }
            CROWN_SAGE -> {
                for ((r, y) in listOf(12f to -26f, 9f to -37f, 6f to -45f)) {
                    c.drawCircle(0f, y, r, P.f(look.hair))
                    c.drawCircle(0f, y, r, P.s(INK, 1.2f))
                }
                c.drawLine(-10f, -30f, 10f, -30f, P.s(0xFF6D3B1F.toInt(), 2.5f))
                c.drawLine(-6f, -16f, 6f, -16f, P.s(Color.WHITE, 1.5f))
                c.drawLine(-6f, -13f, 6f, -13f, P.s(Color.WHITE, 1.5f))
                c.drawCircle(0f, -14.5f, 1.5f, P.f(0xFFE53935.toInt()))
            }
            CROWN_SITA -> {
                path.reset(); path.moveTo(-15f, -10f); path.quadTo(0f, -30f, 15f, -10f)
                c.drawPath(path, P.s(Ui.GOLD, 2.4f))
                for (k in -2..2) c.drawCircle(k * 6f, -20f + kotlin.math.abs(k) * 2.5f, 1.8f, P.f(if (k == 0) 0xFFD32F2F.toInt() else Ui.GOLD))
                c.drawLine(0f, -22f, 0f, -14f, P.s(Ui.GOLD, 1.3f))
                c.drawCircle(0f, -13f, 2.4f, P.f(Ui.GOLD))
                path.reset(); path.moveTo(-9f, -18f); path.lineTo(0f, -32f); path.lineTo(9f, -18f); path.close()
                c.drawPath(path, P.f(Ui.GOLD))
                c.drawPath(path, P.s(INK, 1.2f))
                c.drawCircle(0f, -24f, 2.3f, P.f(0xFFD32F2F.toInt()))
                c.drawCircle(fs + 3.5f, 7f, 2.6f, P.s(Ui.GOLD, 1f))
            }
        }
    }

    /** Tall jewelled kireeta mukut. */
    private fun kireeta(c: Canvas, size: Float, gold: Int, gem: Int) {
        val k = size * 0.74f
        val dark = 0xFF8D5A00.toInt()
        for (side in intArrayOf(-1, 1)) {
            path.reset()
            path.moveTo(side * 13f, -15f)
            path.quadTo(side * 27f, -18f, side * 25f, -32f)
            path.quadTo(side * 19f, -25f, side * 11f, -22f)
            path.close()
            c.drawPath(path, P.f(gold))
            c.drawPath(path, P.s(INK, 1.3f))
        }
        path.reset()
        path.moveTo(-16f, -19f)
        path.cubicTo(-15f, -36f * k, -10f, -52f * k, -5.5f, -64f * k)
        path.lineTo(0f, -78f * k)
        path.lineTo(5.5f, -64f * k)
        path.cubicTo(10f, -52f * k, 15f, -36f * k, 16f, -19f)
        path.close()
        c.drawPath(path, P.f(gold))
        c.save()
        c.clipPath(path)
        c.drawRect(3f, -90f * k, 20f, -10f, P.f(shade(gold, 0.82f)))
        c.drawRect(-12f, -90f * k, -6f, -10f, P.f(lighten(gold, 0.35f)))
        c.restore()
        c.drawPath(path, P.s(INK, 1.6f))
        // tiers
        val tiers = floatArrayOf(-32f, -45f, -56f)
        val widths = floatArrayOf(14.5f, 11.5f, 8f)
        for (i in tiers.indices) {
            val y = tiers[i] * k
            c.drawLine(-widths[i] * k, y, widths[i] * k, y, P.s(dark, 1.4f))
            for (d in -1..1) c.drawCircle(d * widths[i] * 0.55f * k, y + 3f, 1.3f, P.f(Color.WHITE))
        }
        rect.set(-17f, -21f, 17f, -13f)
        c.drawRect(rect, P.f(shade(gold, 0.9f)))
        c.drawRect(rect, P.s(INK, 1.3f))
        for (d in -3..3) c.drawCircle(d * 4.5f, -17f, 1.4f, P.f(Color.WHITE))
        rect.set(-4f, -32f * k, 4f, -24f * k)
        c.drawOval(rect, P.f(gem))
        c.drawCircle(-1f, -29.5f * k, 1.1f, P.f(0xAAFFFFFF.toInt()))
        c.drawCircle(0f, -50f * k, 2.4f, P.f(0xFF00A86B.toInt()))
        c.drawCircle(0f, -80f * k, 2.6f, P.f(Color.WHITE))
    }

    private fun monkeyHead(c: Canvas, v: VanarStyle) {
        val fur = v.fur
        val face = v.face
        val fs = 1.2f
        for (side in intArrayOf(-1, 1)) {
            c.drawCircle(side * 19f, -1f, 6.5f, P.f(fur))
            c.drawCircle(side * 19f, -1f, 3.5f, P.f(face))
        }
        for (side in intArrayOf(-1, 1)) c.drawCircle(side * 19f, -1f, 6.5f, P.s(INK, 1.4f))
        rect.set(-19f, -24f, 19f, 17f)
        c.drawOval(rect, P.f(fur))
        c.drawOval(rect, P.s(INK, INK_W * 0.85f))
        // cheek tufts
        for (side in intArrayOf(-1, 1)) {
            path.reset()
            path.moveTo(side * 16f, -2f); path.lineTo(side * 23f, 6f); path.lineTo(side * 15f, 9f); path.close()
            c.drawPath(path, P.f(fur))
        }
        if (v.spy) {
            path.reset()
            path.moveTo(-24f, 14f); path.quadTo(-28f, -34f, 0f, -34f); path.quadTo(28f, -34f, 24f, 14f)
            path.quadTo(0f, -18f, -24f, 14f); path.close()
            c.drawPath(path, P.f(0xFF4A148C.toInt()))
        }
        // face mask
        path.reset()
        path.moveTo(-14f, -5f)
        path.cubicTo(-14f, -16f, -3f, -16f, 0f, -10f)
        path.cubicTo(3f, -16f, 14f, -16f, 14f, -5f)
        path.cubicTo(14f, 4f, 12f, 10f, 10f, 13f)
        path.quadTo(0f, 23f, -10f, 13f)
        path.cubicTo(-12f, 10f, -14f, 4f, -14f, -5f)
        path.close()
        c.drawPath(path, P.f(face))
        c.drawPath(path, P.s(INK, 1.4f))
        rect.set(-9f + fs, 2f, 11f + fs, 21f)
        c.drawOval(rect, P.f(lighten(face, 0.1f)))
        c.drawOval(rect, P.s(INK, 1.3f))
        // brow ridge
        path.reset()
        path.moveTo(-13f, -8f); path.quadTo(-6f, -13f, 0f, -8.5f); path.quadTo(6f, -13f, 13f, -8f)
        c.drawPath(path, P.s(INK, 2.2f))
        // eyes
        for (side in intArrayOf(-1, 1)) {
            val ex = side * 5.8f + fs
            path.reset()
            path.moveTo(ex - 4.2f, -3.5f); path.quadTo(ex, -6.8f, ex + 4.2f, -3.5f); path.quadTo(ex, -0.6f, ex - 4.2f, -3.5f)
            path.close()
            c.drawPath(path, P.f(0xFFFFF8E8.toInt()))
            c.drawCircle(ex + 0.6f, -3.6f, 2f, P.f(if (v.spy) 0xFFD50000.toInt() else 0xFF6D3A10.toInt()))
            c.drawCircle(ex + 0.6f, -3.6f, 0.9f, P.f(Color.BLACK))
            c.drawCircle(ex + 1.1f, -4.2f, 0.5f, P.f(Color.WHITE))
        }
        c.drawCircle(fs - 2f, 8f, 1.2f, P.f(0xFF3E2723.toInt()))
        c.drawCircle(fs + 2f, 8f, 1.2f, P.f(0xFF3E2723.toInt()))
        path.reset(); path.moveTo(fs - 6f, 14f); path.quadTo(fs, 17f, fs + 6f, 14f)
        c.drawPath(path, P.s(0xFF5D2A1A.toInt(), 1.5f))
        if (v.gada) {
            c.drawLine(0f, -22f, 0f, -14f, P.s(0xFFFF5722.toInt(), 2.2f))
        }
        if (v.crown) kireeta(c, 0.85f, Ui.GOLD, if (v.gada) 0xFFD32F2F.toInt() else 0xFF1E88E5.toInt())
    }

    private fun bearHead(c: Canvas, v: VanarStyle) {
        val fur = v.fur
        val fs = 1.2f
        for (side in intArrayOf(-1, 1)) {
            c.drawCircle(side * 14f, -18f, 6.5f, P.f(fur))
            c.drawCircle(side * 14f, -18f, 3.5f, P.f(v.face))
        }
        rect.set(-19f, -22f, 19f, 17f)
        c.drawOval(rect, P.f(fur))
        c.drawOval(rect, P.s(INK, INK_W * 0.85f))
        rect.set(-8f + fs, -1f, 14f + fs, 17f)
        c.drawOval(rect, P.f(v.face))
        c.drawOval(rect, P.s(INK, 1.3f))
        rect.set(1f + fs, 0f, 11f + fs, 6f)
        c.drawOval(rect, P.f(0xFF1A1010.toInt()))
        for (side in intArrayOf(-1, 1)) {
            c.drawCircle(side * 6.5f + fs, -6f, 2.2f, P.f(0xFF1A1010.toInt()))
            c.drawCircle(side * 6.5f + fs + 0.6f, -6.6f, 0.6f, P.f(Color.WHITE))
        }
        path.reset(); path.moveTo(fs, 7f); path.lineTo(fs + 2f, 12f); path.quadTo(fs + 5f, 14f, fs + 8f, 12f)
        c.drawPath(path, P.s(0xFF1A1010.toInt(), 1.3f))
        if (v.beard) {
            path.reset()
            path.moveTo(-14f, 8f); path.quadTo(0f, 44f, 16f, 8f); path.quadTo(2f, 18f, -14f, 8f); path.close()
            c.drawPath(path, P.f(0xFFEEEEEE.toInt()))
        }
        if (v.crown) kireeta(c, 0.8f, Ui.GOLD, 0xFF1E88E5.toInt())
    }

    private fun tail(c: Canvas, v: VanarStyle, t: Float) {
        val wag = sin(t * 2.6f) * 10f
        path.reset()
        path.moveTo(-12f, -146f)
        path.cubicTo(-54f, -138f, -82f + wag, -196f, -60f + wag, -246f)
        path.quadTo(-50f + wag, -266f, -32f + wag, -258f)
        c.drawPath(path, P.s(INK, 11f))
        c.drawPath(path, P.s(shade(v.fur, 0.9f), 7f))
        c.drawCircle(-32f + wag, -258f, 5.5f, P.f(shade(v.fur, 0.75f)))
    }

    // --------------------------------------------------------------- weapons

    /** Bow held at (hx,hy), facing [aim]. */
    private fun bow(c: Canvas, hx: Float, hy: Float, aim: Float, drawn: Boolean) {
        val dx = cos(aim)
        val dy = sin(aim)
        val px = -dy
        val py = dx
        val half = 96f
        val tipX1 = hx + px * half - dx * 16f
        val tipY1 = hy + py * half - dy * 16f
        val tipX2 = hx - px * half - dx * 16f
        val tipY2 = hy - py * half - dy * 16f
        path.reset()
        path.moveTo(tipX1, tipY1)
        path.quadTo(hx + dx * 50f, hy + dy * 50f, tipX2, tipY2)
        c.drawPath(path, P.s(INK, 9f))
        c.drawPath(path, P.s(0xFF6D3A12.toInt(), 6f))
        c.drawPath(path, P.s(Ui.GOLD, 2.2f))
        c.drawCircle(hx, hy, 5f, P.f(0xFF8D1C1C.toInt()))
        val pull = if (drawn) DRAW_PULL else 6f
        val sx = hx - dx * pull
        val sy = hy - dy * pull
        c.drawLine(tipX1, tipY1, sx, sy, P.s(0xFFFFF8E1.toInt(), 1.4f))
        c.drawLine(tipX2, tipY2, sx, sy, P.s(0xFFFFF8E1.toInt(), 1.4f))
        if (drawn) arrow(c, sx, sy, aim, pull + 30f, false)
    }

    fun arrow(c: Canvas, x: Float, y: Float, angle: Float, len: Float = 110f, golden: Boolean = false) {
        val dx = cos(angle)
        val dy = sin(angle)
        val tx = x + dx * len
        val ty = y + dy * len
        if (golden) c.drawCircle(tx, ty, 26f, P.glow(tx, ty, 26f, 0xFFFFE082.toInt()))
        c.drawLine(x, y, tx, ty, P.s(if (golden) 0xFFFFD54F.toInt() else 0xFF8D5524.toInt(), 3.5f))
        path.reset()
        path.moveTo(tx + dx * 15f, ty + dy * 15f)
        path.lineTo(tx - dy * 6f, ty + dx * 6f)
        path.lineTo(tx + dy * 6f, ty - dx * 6f)
        path.close()
        c.drawPath(path, P.f(if (golden) 0xFFFFF59D.toInt() else 0xFFB0BEC5.toInt()))
        for (k in 0..1) {
            val bx = x + dx * (6f + k * 9f)
            val by = y + dy * (6f + k * 9f)
            c.drawLine(bx, by, bx - dx * 11f - dy * 8f, by - dy * 11f + dx * 8f, P.s(0xFFE53935.toInt(), 2.5f))
            c.drawLine(bx, by, bx - dx * 11f + dy * 8f, by - dy * 11f - dx * 8f, P.s(0xFFE53935.toInt(), 2.5f))
        }
    }

    fun gada(c: Canvas, hx: Float, hy: Float, angleDeg: Float) {
        c.save()
        c.translate(hx, hy)
        c.rotate(angleDeg)
        c.drawLine(0f, 14f, 0f, -80f, P.s(0xFF8D6E63.toInt(), 7f))
        c.drawLine(0f, 14f, 0f, -80f, P.s(Ui.GOLD, 2f))
        c.drawCircle(0f, -104f, 28f, P.vgrad(-132f, -76f, 0xFFFFF59D.toInt(), 0xFFD49A00.toInt()))
        for (i in -1..1) c.drawLine(i * 13f, -129f, i * 13f, -79f, P.s(0xFFB8860B.toInt(), 2f))
        c.drawCircle(0f, -104f, 28f, P.s(INK, 2.4f))
        c.drawCircle(0f, -136f, 6f, P.f(Ui.GOLD))
        c.restore()
    }

    // =============================================================== VANARS

    class VanarStyle(
        val fur: Int, val face: Int, val crown: Boolean = false, val gada: Boolean = false,
        val garland: Boolean = false, val bear: Boolean = false, val beard: Boolean = false,
        val spy: Boolean = false
    ) {
        val look = Look(
            skin = fur,
            garment = when {
                gada -> 0xFFC62828.toInt()
                spy -> 0xFF4A148C.toInt()
                bear -> 0xFF5D4037.toInt()
                crown -> 0xFF1565C0.toInt()
                else -> 0xFF8D6E63.toInt()
            },
            sash = 0xFFFFC107.toInt(),
            crown = if (crown) CROWN_GOLD else CROWN_NONE,
            tilak = false,
            build = if (bear) 1.3f else if (gada) 1.25f else 1.12f,
        )
    }

    val HANUMAN = VanarStyle(0xFFD9541E.toInt(), 0xFFF5A06A.toInt(), crown = true, gada = true)
    val SUGRIV = VanarStyle(0xFF8D6E3F.toInt(), 0xFFE2C49A.toInt(), crown = true, garland = true)
    val BALI = VanarStyle(0xFFC08A2A.toInt(), 0xFFF0D3A0.toInt(), crown = true)
    val VANAR = VanarStyle(0xFF8F5E34.toInt(), 0xFFE6C7A0.toInt())
    val VANAR2 = VanarStyle(0xFFA86E36.toInt(), 0xFFF0D3AE.toInt())
    val BEAR = VanarStyle(0xFF4E342E.toInt(), 0xFF8D6E63.toInt(), bear = true)
    val JAMBAVAN = VanarStyle(0xFF5D4037.toInt(), 0xFFA1887F.toInt(), bear = true, beard = true, crown = true)
    val SPY = VanarStyle(0xFF7B3FA0.toInt(), 0xFFC9A0DC.toInt(), spy = true)

    /** Vanar or bear warrior: heroic human body with an animal head. About 230 units tall at scale 1. */
    fun vanar(
        c: Canvas, x: Float, y: Float, s: Float, st: VanarStyle, t: Float,
        facing: Float = 1f, armsUp: Float = 0f, jump: Boolean = false, flying: Boolean = false
    ) {
        val p = vanarPose.reset()
        p.armsUp = armsUp
        p.jump = jump
        p.flying = flying
        if (st.gada && !flying && armsUp < 0.5f) {
            p.blessing = true
            p.holdGada = true
        }
        c.save()
        c.translate(x, y)
        c.scale(s * facing * VANAR_K, s * VANAR_K)
        if (!flying) {
            rect.set(-46f, -7f, 46f, 7f)
            c.drawOval(rect, P.f(0x44000000))
        } else {
            c.rotate(68f, 0f, -140f)
        }
        humanoid(c, st.look, p, t, true, st)
        c.restore()
    }

    // =============================================================== DEMONS

    /** Flying rakshasa, centred on (x,y), facing left towards the yagna. */
    fun demon(c: Canvas, x: Float, y: Float, s: Float, color: Int, t: Float, big: Boolean = false, hurt: Float = 0f) {
        val look = demonLooks.getOrPut(color) {
            Look(
                color, 0xFF3A1030.toInt(), 0xFF7B1FA2.toInt(), CROWN_NONE,
                hair = 0xFF1A0A14.toInt(), tilak = false, horns = true, build = 1.15f
            )
        }
        c.drawCircle(x, y, 110f * s, P.glow(x, y, 110f * s, 0xFF7B1FA2.toInt(), 0.35f))
        val p = demonPose.reset()
        p.flying = true
        c.save()
        c.translate(x, y)
        val k = s * 0.62f
        c.scale(-k, k)
        c.rotate(58f + sin(t * 3f) * 4f)
        c.translate(0f, 150f)
        humanoid(c, look, p, t, true, null)
        c.restore()
        if (big) c.drawCircle(x, y, 120f * s, P.s(0xFFB388FF.toInt(), 3f, 0.5f))
        if (hurt > 0f) c.drawCircle(x, y, 100f * s, P.glow(x, y, 100f * s, Color.WHITE, hurt))
    }

    // =============================================================== RAVAN

    /** Local head positions for Ravan's ten heads (index 0 = main head). */
    val ravanHeads: Array<FloatArray> = Array(10) { i ->
        when {
            i == 0 -> floatArrayOf(0f, HEAD_Y)
            i <= 5 -> floatArrayOf(-34f * i, HEAD_Y + 4f + 2.5f * i)
            else -> floatArrayOf(34f * (i - 5), HEAD_Y + 4f + 2.5f * (i - 5))
        }
    }
    val RAVAN_NAVEL = floatArrayOf(0f, -158f)

    fun ravan(
        c: Canvas, x: Float, y: Float, s: Float, t: Float, heads: FloatArray,
        navelGlow: Float, flash: Float = 0f, armsUp: Float = 0f
    ) {
        c.save()
        c.translate(x, y)
        c.scale(s, s)
        c.drawCircle(0f, -200f, 300f, P.glow(0f, -200f, 300f, 0xFF7B1FA2.toInt(), 0.45f))
        c.restore()

        val p = tmpPose.reset()
        p.armsUp = armsUp
        person(c, x, y, s, RAVAN, t, p, facing = -1f, drawHead = false)

        c.save()
        c.translate(x, y)
        c.scale(s, s)
        for (i in 9 downTo 0) {
            val h = heads[i]
            val hp = ravanHeads[i]
            if (h <= 0.01f) {
                c.drawCircle(hp[0], hp[1], 14f, P.glow(hp[0], hp[1], 14f, 0xFFFFE082.toInt(), 0.6f))
                continue
            }
            c.save()
            c.translate(hp[0], hp[1])
            val k = (if (i == 0) 1f else 0.88f) * h
            c.scale(-k, k)
            humanHead(c, RAVAN, t + i)
            c.restore()
        }
        if (navelGlow > 0f) {
            val pulse = 0.75f + 0.25f * sin(t * 8f)
            val nx = RAVAN_NAVEL[0]
            val ny = RAVAN_NAVEL[1]
            c.drawCircle(nx, ny, 60f * pulse, P.glow(nx, ny, 60f * pulse, 0xFFFFE57F.toInt(), navelGlow))
            c.drawCircle(nx, ny, 9f, P.f(0xFFFFF8E1.toInt(), navelGlow))
            c.drawCircle(nx, ny, 17f * pulse, P.s(0xFFFFC107.toInt(), 3f, navelGlow))
        }
        if (flash > 0f) c.drawCircle(0f, -200f, 340f, P.glow(0f, -200f, 340f, Color.WHITE, flash))
        c.restore()
    }

    // =============================================================== helpers

    fun shade(color: Int, k: Float): Int = Color.argb(
        Color.alpha(color),
        (Color.red(color) * k).toInt().coerceIn(0, 255),
        (Color.green(color) * k).toInt().coerceIn(0, 255),
        (Color.blue(color) * k).toInt().coerceIn(0, 255)
    )

    fun lighten(color: Int, k: Float): Int = Color.argb(
        Color.alpha(color),
        (Color.red(color) + (255 - Color.red(color)) * k).toInt().coerceIn(0, 255),
        (Color.green(color) + (255 - Color.green(color)) * k).toInt().coerceIn(0, 255),
        (Color.blue(color) + (255 - Color.blue(color)) * k).toInt().coerceIn(0, 255)
    )
}
