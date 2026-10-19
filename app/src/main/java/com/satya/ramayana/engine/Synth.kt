package com.satya.ramayana.engine

import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Procedural Indian-classical style instruments and sound effects. */
object Synth {
    const val SR = 22050
    private const val TWO_PI = (2 * PI).toFloat()
    private val rnd = Random(108)

    fun hz(midi: Float) = 440f * 2f.pow((midi - 69f) / 12f)

    private fun len(sec: Float) = (sec * SR).toInt()

    // ------------------------------------------------------------------ instruments

    /** Bansuri (bamboo flute): soft sine with breath and delayed vibrato. */
    private fun flute(buf: FloatArray, start: Int, dur: Float, f: Float, vol: Float) {
        val n = len(dur + 0.12f)
        var ph = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val att = (t / 0.05f).coerceAtMost(1f)
            val rel = ((dur + 0.12f - t) / 0.14f).coerceIn(0f, 1f)
            val vib = if (t > 0.18f) 0.006f * sin(TWO_PI * 5.2f * t) else 0f
            ph += TWO_PI * f * (1f + vib) / SR
            if (ph > TWO_PI) ph -= TWO_PI
            val s = sin(ph) + 0.22f * sin(2 * ph) + 0.07f * sin(3 * ph)
            val breath = (rnd.nextFloat() * 2 - 1) * 0.035f
            buf[idx] += (s + breath) * att * rel * vol
        }
    }

    /** Shehnai: bright reedy tone with strong vibrato. */
    private fun shehnai(buf: FloatArray, start: Int, dur: Float, f: Float, vol: Float) {
        val n = len(dur + 0.08f)
        var ph = 0f
        val amps = floatArrayOf(1f, 0.75f, 0.55f, 0.42f, 0.3f, 0.2f, 0.12f)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val att = (t / 0.03f).coerceAtMost(1f)
            val rel = ((dur + 0.08f - t) / 0.1f).coerceIn(0f, 1f)
            val vib = 0.009f * sin(TWO_PI * 6f * t) * (t / 0.25f).coerceAtMost(1f)
            ph += TWO_PI * f * (1f + vib) / SR
            if (ph > TWO_PI * 64) ph -= TWO_PI * 64
            var s = 0f
            for (k in amps.indices) s += amps[k] * sin(ph * (k + 1))
            buf[idx] += s * 0.32f * att * rel * vol
        }
    }

    /** Karplus-Strong plucked string - used for the tanpura drone. */
    private fun pluck(buf: FloatArray, start: Int, dur: Float, f: Float, vol: Float, feedback: Float = 0.996f) {
        val period = (SR / f).toInt().coerceAtLeast(2)
        val ring = FloatArray(period) { rnd.nextFloat() * 2 - 1 }
        var p = 0
        val n = len(dur)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val nx = (p + 1) % period
            val v = ring[p]
            ring[p] = feedback * 0.5f * (ring[p] + ring[nx])
            p = nx
            val rel = ((n - i).toFloat() / (SR * 0.2f)).coerceAtMost(1f)
            buf[idx] += v * vol * rel
        }
    }

    private fun tablaLow(buf: FloatArray, start: Int, vol: Float) {
        val n = len(0.35f)
        var ph = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val f = 70f + 90f * exp(-t * 18f)
            ph += TWO_PI * f / SR
            buf[idx] += sin(ph) * exp(-t * 9f) * vol
        }
    }

    private fun tablaHigh(buf: FloatArray, start: Int, vol: Float, pitch: Float = 520f) {
        val n = len(0.3f)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val tone = sin(TWO_PI * pitch * t) * exp(-t * 16f) + 0.5f * sin(TWO_PI * pitch * 2.02f * t) * exp(-t * 24f)
            val click = (rnd.nextFloat() * 2 - 1) * exp(-t * 260f)
            buf[idx] += (tone * 0.6f + click * 0.5f) * vol
        }
    }

    private fun dhol(buf: FloatArray, start: Int, vol: Float) {
        val n = len(0.45f)
        var ph = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val f = 52f + 70f * exp(-t * 25f)
            ph += TWO_PI * f / SR
            val slap = (rnd.nextFloat() * 2 - 1) * exp(-t * 90f) * 0.5f
            buf[idx] += (sin(ph) * exp(-t * 7f) + slap) * vol
        }
    }

    private fun stick(buf: FloatArray, start: Int, vol: Float) {
        val n = len(0.12f)
        var lp = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val noise = rnd.nextFloat() * 2 - 1
            lp += (noise - lp) * 0.5f
            buf[idx] += (noise - lp + 0.6f * sin(TWO_PI * 420f * t)) * exp(-t * 45f) * vol
        }
    }

    /** Temple bell with inharmonic partials. */
    private fun bell(buf: FloatArray, start: Int, f: Float, vol: Float, dur: Float = 2f) {
        val ratios = floatArrayOf(1f, 2.0f, 2.76f, 5.4f, 8.93f)
        val amps = floatArrayOf(1f, 0.6f, 0.4f, 0.25f, 0.12f)
        val decays = floatArrayOf(1.6f, 2.4f, 3.2f, 5f, 7f)
        val n = len(dur)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            var s = 0f
            for (k in ratios.indices) s += amps[k] * sin(TWO_PI * f * ratios[k] * t) * exp(-t * decays[k])
            buf[idx] += s * 0.4f * vol * (t / 0.003f).coerceAtMost(1f)
        }
    }

    private fun blip(buf: FloatArray, start: Int, f: Float, dur: Float, vol: Float) {
        val n = len(dur)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / SR
            val env = (t / 0.005f).coerceAtMost(1f) * exp(-t * 6f / dur)
            buf[idx] += (sin(TWO_PI * f * t) + 0.3f * sin(TWO_PI * f * 2 * t)) * env * vol
        }
    }

    // ------------------------------------------------------------------ songs

    private val NOTE = mapOf(
        'S' to 0, 'r' to 1, 'R' to 2, 'g' to 3, 'G' to 4, 'm' to 5, 'M' to 6,
        'P' to 7, 'd' to 8, 'D' to 9, 'n' to 10, 'N' to 11
    )

    /** Parses sargam: `S R G m P D N`, `'` = upper octave, `.` = lower, each `-` holds one more step, `x` = rest. */
    private fun parse(mel: String): List<Pair<Int?, Int>> {
        val out = ArrayList<Pair<Int?, Int>>()
        for (tok in mel.trim().split(Regex("\\s+"))) {
            if (tok.isEmpty() || tok == "|") continue
            val steps = 1 + tok.count { it == '-' }
            val ch = tok[0]
            if (ch == 'x') {
                out.add(null to steps)
                continue
            }
            var semi = NOTE[ch] ?: continue
            semi += 12 * tok.count { it == '\'' }
            semi -= 12 * tok.count { it == '.' }
            out.add(semi to steps)
        }
        return out
    }

    private class Song(
        val sa: Int, val bpm: Float, val melody: String, val shehnai: Boolean,
        val rhythm: String, val dholDrums: Boolean, val bells: Boolean, val harmony: Boolean = false
    )

    private val specs = mapOf(
        Music.BHAJAN to Song(
            sa = 67, bpm = 84f, shehnai = false, dholDrums = false, bells = true,
            melody = """
                G- G- G- m- P--- P- x-
                m- G- R- G- m----- x-
                m- m- m- P- D--- D- x-
                P- m- G- R- G----- x-
                S'- S'- N- D- P--- D- P-
                m- G- m- P- D----- x-
                P- D- P- m- G- R- G- m-
                G- R- S- R- S----- x-
            """,
            rhythm = "D.gtn.t.n.k.Dgn."
        ),
        Music.BATTLE to Song(
            sa = 62, bpm = 132f, shehnai = true, dholDrums = true, bells = false, harmony = true,
            melody = """
                S- r- G- m- P- m- G- r-
                S- r- G- m- P----- x-
                P- d- N- S'- N- d- P- m-
                G- m- P- d- P----- x-
                S'- S'- N- d- P- P- m- G-
                m- m- G- r- S----- x-
                G- G- m- m- P- P- d- d-
                P- m- G- r- S----- x-
            """,
            rhythm = "D.dnD.dnD.dnDdnn"
        ),
        Music.FESTIVE to Song(
            sa = 67, bpm = 138f, shehnai = true, dholDrums = true, bells = true,
            melody = """
                S- G- m- P- P- P- D- P-
                m- G- m- P- G----- x-
                P- D- N- S'- S'- N- D- P-
                D- P- m- G- R----- x-
                G- m- P- D- P- m- G- R-
                S- R- G- m- G- R- S- x-
            """,
            rhythm = "D.nnD.n.D.nnDnDn"
        ),
        Music.VIVAH to Song(
            sa = 67, bpm = 96f, shehnai = true, dholDrums = false, bells = true,
            melody = """
                S- R- G- R- G- m- P---
                m- G- R- G- S----- x-
                G- m- P- D- P- m- G- m-
                P- D- P- m- G----- x-
                P- D- N- S'- N- D- P---
                m- G- R- G- S----- x-
            """,
            rhythm = "D.t.n.D.t.D.n.t."
        )
    )

    fun buildSong(m: Music): FloatArray {
        val spec = specs[m] ?: return FloatArray(0)
        val notes = parse(spec.melody)
        val totalSteps = notes.sumOf { it.second }
        val stepSec = 60f / spec.bpm / 2f
        val stepLen = stepSec * SR
        val buf = FloatArray((totalSteps * stepLen).toInt() + 1)

        // Melody
        var step = 0
        for ((semi, steps) in notes) {
            if (semi != null) {
                val f = hz((spec.sa + semi).toFloat())
                val start = (step * stepLen).toInt()
                val dur = steps * stepSec * 0.92f
                if (spec.shehnai) shehnai(buf, start, dur, f, 0.42f) else flute(buf, start, dur, f, 0.42f)
                if (spec.harmony) flute(buf, start, dur, f / 2f, 0.16f)
            }
            step += steps
        }

        // Tanpura drone: Pa Sa Sa Sa(low), one string every 4 steps.
        val drone = intArrayOf(-17, -12, -12, -24)
        var k = 0
        var s = 0
        while (s < totalSteps) {
            pluck(buf, (s * stepLen).toInt(), stepSec * 7f, hz((spec.sa + drone[k % 4]).toFloat()), 0.16f, 0.997f)
            k++
            s += 4
        }

        // Percussion
        val r = spec.rhythm
        for (i in 0 until totalSteps) {
            val start = (i * stepLen).toInt()
            when (r[i % r.length]) {
                'D' -> if (spec.dholDrums) dhol(buf, start, 0.55f) else {
                    tablaLow(buf, start, 0.45f); tablaHigh(buf, start, 0.22f)
                }
                'd', 'g' -> if (spec.dholDrums) dhol(buf, start, 0.35f) else tablaLow(buf, start, 0.32f)
                'n' -> if (spec.dholDrums) stick(buf, start, 0.3f) else tablaHigh(buf, start, 0.28f)
                't' -> tablaHigh(buf, start, 0.14f, 700f)
                'k' -> stick(buf, start, 0.14f)
            }
        }

        // Temple bells at the start of each long phrase
        if (spec.bells) {
            var b = 0
            while (b < totalSteps) {
                bell(buf, (b * stepLen).toInt(), hz((spec.sa + 24).toFloat()), 0.22f, 2.2f)
                b += 32
            }
        }

        normalize(buf, 0.9f)
        // Smooth loop seam.
        val fadeN = minOf(400, buf.size / 2)
        for (i in 0 until fadeN) {
            val g = i.toFloat() / fadeN
            buf[i] *= g
            buf[buf.size - 1 - i] *= g
        }
        return buf
    }

    private fun normalize(buf: FloatArray, peak: Float) {
        var mx = 0.0001f
        for (v in buf) if (kotlin.math.abs(v) > mx) mx = kotlin.math.abs(v)
        val g = peak / mx
        for (i in buf.indices) buf[i] *= g
    }

    // ------------------------------------------------------------------ effects

    fun buildSfx(map: MutableMap<Sfx, FloatArray>) {
        map[Sfx.CLICK] = FloatArray(len(0.08f)).also { blip(it, 0, 1200f, 0.06f, 0.5f) }

        map[Sfx.ARROW] = FloatArray(len(0.35f)).also { b ->
            var lp = 0f
            for (i in b.indices) {
                val t = i.toFloat() / SR
                val cutoff = 0.08f + 0.5f * (t / 0.35f)
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * cutoff
                b[i] = lp * (t / 0.02f).coerceAtMost(1f) * exp(-t * 9f) * 1.4f
            }
            pluck(b, 0, 0.3f, 180f, 0.5f, 0.99f)
        }

        map[Sfx.HIT] = FloatArray(len(0.6f)).also {
            bell(it, 0, 1320f, 0.6f, 0.6f)
            blip(it, 0, 880f, 0.2f, 0.3f)
        }

        map[Sfx.COLLECT] = FloatArray(len(0.4f)).also {
            blip(it, 0, hz(79f), 0.12f, 0.4f)
            blip(it, len(0.07f), hz(83f), 0.12f, 0.4f)
            blip(it, len(0.14f), hz(86f), 0.25f, 0.45f)
        }

        map[Sfx.CRACKER] = FloatArray(len(1.6f)).also { b ->
            var lp = 0f
            for (i in 0 until len(0.6f)) {
                val t = i.toFloat() / SR
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.12f
                b[i] += (lp * 2.2f + sin(TWO_PI * 55f * t) * 0.8f) * exp(-t * 7f)
            }
            // crackle tail
            repeat(55) {
                val at = len(0.25f + rnd.nextFloat() * 1.25f)
                val amp = 0.2f + rnd.nextFloat() * 0.5f
                for (j in 0 until 70) {
                    if (at + j < b.size) b[at + j] += (rnd.nextFloat() * 2 - 1) * amp * exp(-j / 14f)
                }
            }
        }

        map[Sfx.CONCH] = FloatArray(len(1.8f)).also { b ->
            var ph = 0f
            for (i in b.indices) {
                val t = i.toFloat() / SR
                val f = 196f * (1f + 0.02f * (t / 1.8f)) * (1f + 0.004f * sin(TWO_PI * 5f * t))
                ph += TWO_PI * f / SR
                val env = (t / 0.35f).coerceAtMost(1f) * ((1.8f - t) / 0.4f).coerceIn(0f, 1f)
                val s = sin(ph) + 0.5f * sin(2 * ph) + 0.35f * sin(3 * ph) + 0.2f * sin(4 * ph) + 0.1f * sin(5 * ph)
                b[i] = (s * 0.45f + (rnd.nextFloat() * 2 - 1) * 0.05f) * env
            }
        }

        map[Sfx.WRONG] = FloatArray(len(0.35f)).also {
            blip(it, 0, 330f, 0.15f, 0.45f)
            blip(it, len(0.14f), 247f, 0.2f, 0.45f)
        }

        map[Sfx.TIMEUP] = FloatArray(len(1.4f)).also {
            flute(it, 0, 0.3f, hz(79f), 0.5f)
            flute(it, len(0.33f), 0.3f, hz(76f), 0.5f)
            flute(it, len(0.66f), 0.6f, hz(72f), 0.5f)
        }

        map[Sfx.JUMP] = FloatArray(len(0.2f)).also { b ->
            var ph = 0f
            for (i in b.indices) {
                val t = i.toFloat() / SR
                ph += TWO_PI * (300f + 2200f * t) / SR
                b[i] = sin(ph) * exp(-t * 10f) * 0.45f
            }
        }

        map[Sfx.BELL] = FloatArray(len(2f)).also { bell(it, 0, 880f, 0.8f, 2f) }

        map[Sfx.POWER] = FloatArray(len(1.6f)).also { b ->
            var ph = 0f
            var lp = 0f
            for (i in 0 until len(1.2f)) {
                val t = i.toFloat() / SR
                ph += TWO_PI * (90f + 450f * t * t) / SR
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.2f
                val env = (t / 0.2f).coerceAtMost(1f) * ((1.2f - t) / 0.3f).coerceIn(0f, 1f)
                b[i] = (sin(ph) * 0.5f + 0.3f * sin(ph * 2) + lp * 0.6f) * env
            }
            bell(b, len(1.0f), 1046f, 0.6f, 0.6f)
        }

        map[Sfx.BREAK] = FloatArray(len(2f)).also { b ->
            var lp = 0f
            for (i in b.indices) {
                val t = i.toFloat() / SR
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * (0.04f + 0.3f * exp(-t * 10f))
                b[i] = lp * 3.2f * exp(-t * 1.8f) + (rnd.nextFloat() * 2 - 1) * exp(-t * 40f)
            }
        }

        map[Sfx.WIN] = FloatArray(len(2.6f)).also {
            val seq = floatArrayOf(67f, 71f, 74f, 79f)
            for ((i, nt) in seq.withIndex()) shehnai(it, len(i * 0.16f), if (i == 3) 1.2f else 0.16f, hz(nt), 0.6f)
            dhol(it, 0, 0.5f); dhol(it, len(0.32f), 0.4f); dhol(it, len(0.48f), 0.6f)
            bell(it, len(0.48f), hz(91f), 0.4f, 1.8f)
        }

        map[Sfx.TICK] = FloatArray(len(0.06f)).also { blip(it, 0, 1600f, 0.04f, 0.35f) }

        map[Sfx.SPLASH] = FloatArray(len(0.5f)).also { b ->
            var lp = 0f
            for (i in b.indices) {
                val t = i.toFloat() / SR
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.3f
                b[i] = lp * exp(-t * 7f) * (t / 0.01f).coerceAtMost(1f) * 1.2f
            }
        }

        for (v in map.values) normalize(v, 0.85f)
    }
}
