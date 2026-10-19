package com.satya.ramayana.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.satya.ramayana.art.Characters
import com.satya.ramayana.art.Scenery
import com.satya.ramayana.engine.Button
import com.satya.ramayana.engine.Fireworks
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.Music
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Particles
import com.satya.ramayana.engine.Screen
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.Stages
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Storybook pages before each level: illustration on the left, narration on the right.
 * Pages: a recap of the previous lesson, the story (level 1 only), a "Did you know?"
 * knowledge page and the mission. A narrator reads every page aloud.
 */
class StoryScreen(game: Game, private val stage: Int, private val level: Int = 1) : Screen(game) {
    private val info = Stages.info(stage)

    private enum class Kind { RECAP, STORY, KNOW, MISSION }

    private class Page(val kind: Kind, val text: String)

    private val scene = Stages.create(game, stage, level)
    private val mission = info.mission.replace("{n}", scene.goal.toString())

    private val pages = buildList {
        info.previousLesson(level)?.let { add(Page(Kind.RECAP, it)) }
        if (level == 1) info.story.forEach { add(Page(Kind.STORY, it)) }
        add(Page(Kind.KNOW, info.level(level).knowledge))
        add(Page(Kind.MISSION, mission))
    }
    private var page = 0
    private var pageTime = 0f

    private val nextBtn = Button("Next", 1330f, 800f, 260f, 84f, Button.PRIMARY, "") { next() }
    private val skipBtn = Button("Skip", 1050f, 800f, 200f, 84f, Button.SECONDARY) { start() }
    private val backBtn = Button("Map", 120f, 70f, 180f, 70f, Button.SECONDARY, "◀") { game.go(MapScreen(game)) }
    private val voiceBtn = Button("", 400f, 760f, 300f, 70f, Button.SECONDARY) {
        game.toggleVoice()
        if (game.save.voiceOn) speakPage()
    }
    private val levelBtns = (1..Stages.LEVELS).map { l ->
        Button("Level $l", 1170f + (l - 2) * 235f, 205f, 215f, 60f, Button.PRIMARY) {
            if (l != level) game.go(StoryScreen(game, stage, l))
        }
    }

    init {
        buttons.addAll(listOf(nextBtn, skipBtn, backBtn, voiceBtn))
        buttons.addAll(levelBtns)
        for ((i, btn) in levelBtns.withIndex()) {
            val l = i + 1
            val open = game.save.isUnlocked(stage, l) || l == level
            btn.enabled = open
            btn.style = when {
                l == level -> Button.GREEN
                open -> Button.PRIMARY
                else -> Button.GRAY
            }
            btn.label = if (open) "Level $l" else "Level $l 🔒"
            btn.textSize = 26f
        }
    }

    override fun enter() {
        game.audio.music(Music.BHAJAN)
        speakPage()
    }

    override fun onBack(): Boolean {
        game.go(MapScreen(game))
        return true
    }

    private fun speakPage() {
        val p = pages[page]
        game.narrator.speak(
            when (p.kind) {
                Kind.RECAP -> "Remember what we learnt. ${p.text}"
                Kind.STORY -> p.text
                Kind.KNOW -> "Did you know? ${p.text}"
                Kind.MISSION -> "Level $level. Your mission. ${p.text}"
            }
        )
    }

    /** Characters per second of the typewriter: slower while the narrator reads along. */
    private fun cps() = if (game.save.voiceOn) 14f else 45f

    private fun next() {
        if (pageTime < typedDuration()) {
            pageTime = 99f   // finish the typewriter first
            return
        }
        if (page < pages.size - 1) {
            page++
            pageTime = 0f
            game.audio.play(Sfx.BELL)
            speakPage()
        } else start()
    }

    private fun start() = game.go(Stages.create(game, stage, level))

    private fun typedDuration() = pages[page].text.length / cps()

    override fun touch(action: Int, x: Float, y: Float) {
        if (action == Touch.UP && x > 760f && y > 250f) next()
    }

    override fun update(dt: Float) {
        pageTime += dt
        scene.time = time
        val last = page == pages.size - 1
        nextBtn.label = if (last) "Play!" else "Next"
        nextBtn.icon = "▶"
        nextBtn.style = if (last) Button.GREEN else Button.PRIMARY
        skipBtn.visible = !last
        voiceBtn.label = if (game.save.voiceOn) "Voice: On" else "Voice: Off"
    }

    override fun draw(c: Canvas) {
        c.drawRect(0f, 0f, V.W, V.H, P.vgrad(0f, V.H, 0xFF4A0A14.toInt(), 0xFF1A0306.toInt()))
        // book spine
        Ui.roundRect(c, 30f, 120f, V.W - 30f, V.H - 30f, 24f, P.f(0xFF5D2A12.toInt()))

        // illustration: the live stage scene, framed
        val l = 60f
        val t = 150f
        val w = 680f
        val h = w * V.H / V.W
        c.save()
        c.clipRect(l, t, l + w, t + h)
        c.translate(l, t)
        c.scale(w / V.W, h / V.H)
        scene.drawWorld(c)
        c.restore()
        Ui.roundRect(c, l, t, l + w, t + h, 6f, P.s(Ui.GOLD, 8f))
        Ui.text(c, "📍 ${info.place}", l + w / 2, t + h + 55f, 40f, Ui.GOLD, outline = Color.BLACK)
        Ui.text(c, "Stage ${info.num} of ${Stages.COUNT}  ·  Level $level of ${Stages.LEVELS}", l + w / 2, t + h + 100f, 30f, Ui.CREAM, face = P.sans)
        if (game.narrator.speaking) {
            // little sound waves while the narrator talks
            for (k in 0 until 3) {
                val r = 14f + k * 12f + 6f * sin(time * 8f - k)
                c.drawArc(565f - r, 760f - r, 565f + r, 760f + r, -45f, 90f, false, P.s(Ui.GOLD, 4f))
            }
        }

        // page
        Ui.parchment(c, 780f, 150f, V.W - 60f, 860f)
        Ui.title(c, info.title, 1170f, 100f, 60f)
        val px = 830f
        val maxW = V.W - 60f - 830f - 40f
        val pg = pages[page]
        val shown = pageTime * cps()
        when (pg.kind) {
            Kind.MISSION -> {
                Ui.text(c, "Your Mission", 1170f, 300f, 52f, Ui.MAROON)
                drawWrapped(c, pg.text, px, 370f, 38f, maxW, Ui.INK, 999f)
                if (level > 1) Ui.text(c, "Level $level: everything is faster – stay sharp!", 1170f, 608f, 32f, 0xFFBF360C.toInt())
                Ui.text(c, "⏳ Time: ${scene.timeLimit.toInt()} seconds", 1170f, 660f, 38f, 0xFF1B5E20.toInt())
                if (stage > Stages.HANUMAN_STAGE && game.save.hanumanShakti) {
                    Ui.text(c, "🙏 Use HANUMAN SHAKTI once if you need help!", 1170f, 712f, 30f, 0xFFBF360C.toInt())
                }
            }
            Kind.RECAP -> {
                Ui.text(c, "🌟 Remember what we learnt", 1170f, 300f, 46f, Ui.MAROON)
                drawWrapped(c, "“${pg.text}”", px, 390f, 42f, maxW, 0xFF1B5E20.toInt(), shown)
                if (shown > pg.text.length) {
                    Ui.text(c, "Keep it in your heart for the next adventure!", 1170f, 640f, 30f, Ui.INK, face = P.sans)
                }
            }
            Kind.KNOW -> {
                Ui.text(c, "📖 Did you know?", 1170f, 300f, 46f, Ui.MAROON)
                drawWrapped(c, pg.text, px, 370f, 36f, maxW, Ui.INK, shown)
            }
            Kind.STORY -> drawWrapped(c, pg.text, px, 300f, 40f, maxW, Ui.INK, shown)
        }
        // page dots
        for (k in pages.indices) {
            c.drawCircle(1170f - (pages.size - 1) * 15f + k * 30f, 740f, if (k == page) 10f else 7f, P.f(if (k == page) Ui.MAROON else 0x889C6B2E.toInt()))
        }
        drawButtons(c)
    }

    private fun drawWrapped(c: Canvas, s: String, x: Float, y: Float, size: Float, maxW: Float, color: Int, chars: Float) {
        val lines = Ui.wrap(s, size, maxW, P.sans)
        var remaining = chars.toInt()
        for ((i, line) in lines.withIndex()) {
            if (remaining <= 0) break
            val shown = if (line.length <= remaining) line else line.substring(0, remaining)
            remaining -= line.length + 1
            Ui.text(c, shown, x, y + i * size * 1.45f, size, color, Paint.Align.LEFT, P.sans)
        }
    }
}

/** After a level: crackers and the lesson on success, gentle encouragement on time-up. */
class ResultScreen(
    game: Game, private val stage: Int, private val level: Int, private val success: Boolean,
    private val stars: Int, private val score: Int, private val reason: String = ""
) : Screen(game) {
    private val info = Stages.info(stage)
    private val lesson = info.level(level).lesson
    private val fireworks = Fireworks(if (success) game.audio else null)

    init {
        fireworks.active = success
        fireworks.rate = 0.55f
        if (success) {
            when {
                level < Stages.LEVELS -> buttons.add(Button("Next Level", 1080f, 800f, 380f, 92f, Button.GREEN, "▶") {
                    game.go(StoryScreen(game, stage, level + 1))
                })
                stage < Stages.COUNT -> buttons.add(Button("Next Stage", 1080f, 800f, 380f, 92f, Button.GREEN, "▶") {
                    game.go(StoryScreen(game, stage + 1, 1))
                })
            }
            buttons.add(Button("Replay", 720f, 800f, 260f, 92f, Button.PRIMARY) { game.go(Stages.create(game, stage, level)) })
            buttons.add(Button("Map", 440f, 800f, 220f, 92f, Button.SECONDARY) { game.go(MapScreen(game)) })
        } else {
            buttons.add(Button("Try Again", 940f, 760f, 380f, 92f, Button.GREEN, "↻") { game.go(Stages.create(game, stage, level)) })
            buttons.add(Button("Map", 610f, 760f, 220f, 92f, Button.SECONDARY) { game.go(MapScreen(game)) })
        }
    }

    override fun enter() {
        if (success) {
            game.audio.music(Music.FESTIVE)
            game.audio.play(Sfx.WIN)
            repeat(3) { fireworks.launch() }
            game.narrator.speak("Well done! What we learn: $lesson")
        } else {
            game.audio.music(Music.BHAJAN)
            game.narrator.speak("Time up! Never give up. Try again with patience!")
        }
    }

    override fun onBack(): Boolean {
        game.go(MapScreen(game))
        return true
    }

    override fun update(dt: Float) {
        fireworks.update(dt)
        val shownStars = ((time - 0.6f) / 0.4f).toInt().coerceIn(0, stars)
        if (success && shownStars > lastStar) {
            lastStar = shownStars
            game.audio.play(Sfx.BELL)
        }
    }

    private var lastStar = 0

    override fun draw(c: Canvas) {
        if (success) {
            Scenery.sky(c, 0xFF0D0A2A.toInt(), 0xFF2B1B5A.toInt(), 0xFF5A2A5E.toInt())
            Scenery.stars(c, time)
            Scenery.palace(c, V.W / 2, 900f, 0.8f, time, 1f, 0xFF9C8060.toInt())
        } else {
            Scenery.sky(c, 0xFF3E2A5A.toInt(), 0xFF7A5A8E.toInt(), 0xFFC9A0B8.toInt())
        }
        fireworks.draw(c)

        Ui.panel(c, 260f, 70f, V.W - 260f, if (success) 720f else 680f)
        if (success) {
            val head = if (level < Stages.LEVELS) "Level $level Complete!" else "Stage ${info.num} Complete!"
            Ui.title(c, head, V.W / 2, 170f, 80f)
            val shown = ((time - 0.6f) / 0.4f).toInt().coerceIn(0, stars)
            for (k in 0 until 3) {
                val pop = if (k < shown) 1f + 0.15f * sin((time - 0.6f - k * 0.4f).coerceAtLeast(0f) * 8f) * (1f - ((time - 0.6f - k * 0.4f) / 1f).coerceIn(0f, 1f)) else 1f
                Ui.star(c, V.W / 2 - 110f + k * 110f, 260f, 46f * pop, k < shown)
            }
            Ui.text(c, "Stage ${info.num} · Level $level      Score: $score", V.W / 2, 350f, 40f, Color.WHITE)
            Ui.text(c, "What we learn", V.W / 2, 430f, 38f, Ui.GOLD)
            val lines = Ui.wrap(lesson, 36f, 900f)
            for ((i, l) in lines.withIndex()) Ui.text(c, l, V.W / 2, 480f + i * 46f, 36f, Ui.CREAM, face = P.sans)
            if (stage == Stages.HANUMAN_STAGE && level == 1) {
                val a = 0.75f + 0.25f * sin(time * 4f)
                Ui.text(c, "🙏 You earned HANUMAN SHAKTI!", V.W / 2, 600f, 44f, 0xFFFFB74D.toInt(), alpha = a)
                Ui.text(c, "Use it once in each coming stage when you need help.", V.W / 2, 650f, 30f, Ui.CREAM, face = P.sans)
            } else if (stage == 7 && level == 1) {
                Ui.text(c, "King Sugriv promises his whole army to help Ram!", V.W / 2, 630f, 34f, 0xFFFFE082.toInt(), face = P.sans)
            } else if (level < Stages.LEVELS) {
                Ui.text(c, "Level ${level + 1} is now open – it will be faster!", V.W / 2, 640f, 32f, 0xFFFFE082.toInt(), face = P.sans)
            }
        } else {
            Ui.title(c, if (reason.isEmpty()) "Time Up!" else reason, V.W / 2, 200f, 80f)
            c.drawCircle(V.W / 2, 400f, 150f, P.glow(V.W / 2, 400f, 150f, 0xFFFFE082.toInt(), 0.5f))
            val p = Characters.pose()
            p.namaste = true
            Characters.person(c, V.W / 2, 560f, 0.9f, Characters.RAM, time, p)
            Ui.text(c, "\"Never give up. Try again with patience!\"", V.W / 2, 630f, 38f, Ui.CREAM, face = P.sans)
        }
        drawButtons(c)
    }
}

/** Ram–Sita Vivah celebration after the bow is broken. */
class VivahScreen(game: Game, private val stars: Int, private val score: Int) : Screen(game) {
    private val petals = Particles(700)
    private val fireworks = Fireworks(game.audio)
    private var belled = false
    private val cont = Button("Continue", V.W / 2, 830f, 340f, 86f, Button.GREEN, "▶") {
        game.go(ResultScreen(game, 3, Stages.LEVELS, true, stars, score))
    }

    init {
        buttons.add(cont)
        cont.visible = false
        fireworks.rate = 1.4f
    }

    override fun enter() {
        game.audio.music(Music.VIVAH)
        game.audio.play(Sfx.CONCH)
    }

    override fun update(dt: Float) {
        if (time > 3f && (time * 20).toInt() % 2 == 0) petals.petals(V.W / 2, -10f, 2)
        petals.update(dt)
        fireworks.active = time > 3.5f
        fireworks.update(dt)
        cont.visible = time > 5f
        if (time > 2.4f && !belled) {
            belled = true
            game.audio.play(Sfx.BELL)
        }
    }

    override fun draw(c: Canvas) {
        Scenery.court(c, time)
        fireworks.draw(c)
        // mandap
        val mx = V.W / 2
        for (side in floatArrayOf(-1f, 1f)) {
            val px = mx + side * 330f
            c.drawRect(px - 18f, 220f, px + 18f, 780f, P.vgrad(220f, 780f, 0xFFFFD54F.toInt(), 0xFFB8860B.toInt()))
            for (k in 0 until 14) c.drawCircle(px, 240f + k * 38f, 14f, P.f(if (k % 2 == 0) 0xFFFF9800.toInt() else 0xFFFFEB3B.toInt()))
        }
        Ui.roundRect(c, mx - 380f, 170f, mx + 380f, 240f, 30f, P.vgrad(170f, 240f, 0xFFE53935.toInt(), 0xFF8E0000.toInt()))
        for (k in 0 until 20) {
            val gx = mx - 360f + k * 38f
            c.drawLine(gx, 240f, gx, 300f + (k % 3) * 14f, P.s(0xFFFF9800.toInt(), 6f))
            c.drawCircle(gx, 304f + (k % 3) * 14f, 8f, P.f(0xFFFFEB3B.toInt()))
        }
        Ui.title(c, "॥ राम – सीता विवाह ॥", mx, 130f, 64f)

        // sacred fire
        Scenery.yagna(c, mx, 820f, 0.7f, time, 0.8f)

        // garland exchange
        val sitaGives = ((time - 1f) / 1.2f).coerceIn(0f, 1f)
        val ramGives = ((time - 2.4f) / 1.2f).coerceIn(0f, 1f)
        val rp = Characters.pose()
        rp.holdGarland = sitaGives >= 1f
        rp.armsUp = if (ramGives in 0.01f..0.99f) 0.6f else 0f
        Characters.person(c, mx - 160f, 800f, 1.3f, Characters.RAM, time, rp)
        val sp = Characters.pose()
        sp.holdGarland = ramGives >= 1f
        sp.armsUp = if (sitaGives in 0.01f..0.99f) 0.6f else 0f
        Characters.person(c, mx + 160f, 800f, 1.25f, Characters.SITA, time, sp, facing = -1f)
        if (sitaGives in 0.01f..0.99f) garland(c, mx + 110f - 260f * sitaGives, 520f - sin(sitaGives * PI.toFloat()) * 80f)
        if (ramGives in 0.01f..0.99f) garland(c, mx - 110f + 260f * ramGives, 520f - sin(ramGives * PI.toFloat()) * 80f)

        // families blessing
        Characters.person(c, 280f, 825f, 1.0f, Characters.KING, time)
        Characters.person(c, 1320f, 825f, 1.0f, Characters.KING, time, facing = -1f)
        Characters.person(c, 430f, 835f, 0.95f, Characters.LAKSHMAN, time)
        Characters.person(c, 1170f, 835f, 0.95f, Characters.SAGE, time, facing = -1f)

        petals.draw(c)
        if (time > 3f) {
            val a = ((time - 3f) / 0.8f).coerceAtMost(1f)
            Ui.title(c, "Shubh Vivah!", mx, 345f, 72f, a)
        }
        drawButtons(c)
    }

    private fun garland(c: Canvas, x: Float, y: Float) {
        for (i in 0..14) {
            val a = PI * 2 * i / 15
            c.drawCircle(x + (cos(a) * 34).toFloat(), y + (sin(a) * 42).toFloat(), 8f, P.f(if (i % 2 == 0) 0xFFFF7043.toInt() else 0xFFFFEB3B.toInt()))
        }
    }
}

/** The grand finale: Ram returns to Ayodhya and the city lights up for Diwali. */
class DiwaliScreen(game: Game, private val stars: Int, private val score: Int) : Screen(game) {
    private val fireworks = Fireworks(game.audio)
    private val petals = Particles(500)
    private val again = Button("Stage Map", V.W / 2 - 220f, 840f, 340f, 84f, Button.PRIMARY) { game.go(MapScreen(game)) }
    private val menu = Button("Main Menu", V.W / 2 + 220f, 840f, 340f, 84f, Button.SECONDARY) { game.go(MenuScreen(game)) }

    init {
        buttons.add(again)
        buttons.add(menu)
        fireworks.rate = 0.35f
        fireworks.active = false
    }

    override fun enter() {
        game.audio.music(Music.FESTIVE)
        game.audio.play(Sfx.CONCH)
    }

    override fun onBack(): Boolean {
        game.go(MenuScreen(game))
        return true
    }

    override fun update(dt: Float) {
        fireworks.active = time > 4.5f
        if (time > 4.5f && time - dt <= 4.5f) {
            repeat(5) { fireworks.launch() }
            game.audio.play(Sfx.WIN)
        }
        fireworks.update(dt)
        if (time > 5f && (time * 10).toInt() % 3 == 0) petals.petals(V.W / 2, -10f, 1)
        petals.update(dt)
        again.visible = time > 7f
        menu.visible = time > 7f
    }

    override fun touch(action: Int, x: Float, y: Float) {
        if (action == Touch.DOWN && time > 4.5f) fireworks.launch(x)
    }

    override fun draw(c: Canvas) {
        Scenery.sky(c, 0xFF050318.toInt(), 0xFF1A1040.toInt(), 0xFF3A1E4A.toInt())
        Scenery.stars(c, time, 120, 600f)
        Scenery.moon(c, 1400f, 130f, 40f)

        val lit = ((time - 3.5f) / 3f).coerceIn(0f, 1f)
        // the whole city glows
        c.drawCircle(V.W / 2, 700f, 900f, P.glow(V.W / 2, 700f, 900f, 0xFFFF9800.toInt(), 0.45f * lit))
        Scenery.palace(c, V.W / 2, 760f, 1.05f, time, lit, 0xFFC9A77A.toInt())
        Scenery.ground(c, 760f, 0xFF4A2A1A.toInt(), 0xFF1E0E08.toInt())

        // diyas light up row by row
        val rows = arrayOf(floatArrayOf(560f, 26f, 0.55f), floatArrayOf(760f, 18f, 0.8f), floatArrayOf(880f, 14f, 1f))
        var idx = 0
        for (row in rows) {
            val n = row[1].toInt()
            for (k in 0 until n) {
                val x = 40f + k * (V.W - 80f) / (n - 1)
                val threshold = idx * 0.012f
                Ui.diya(c, x, row[0], row[2], time + k, ((lit - threshold) * 4f).coerceIn(0f, 1f))
                idx++
            }
        }
        if (lit > 0.3f) Scenery.rangoli(c, V.W / 2, 830f, 0.9f, time)

        // Pushpak Viman arriving with Ram, Sita, Lakshman and Hanuman
        val arrive = (time / 3.5f).coerceAtMost(1f)
        val ease = 1f - (1f - arrive) * (1f - arrive)
        val vx = V.W + 300f - (V.W + 300f - 1200f) * ease
        val vy = 120f + 230f * ease
        val vs = 0.55f
        Scenery.pushpak(c, vx, vy, vs, time)
        val base = vy + sin(time * 2f) * 6f - 12f * vs
        Characters.vanar(c, vx - 80f * vs * 1.6f, base, 0.32f, Characters.HANUMAN, time)
        Characters.person(c, vx - 30f, base, 0.3f, Characters.LAKSHMAN, time)
        val rp = Characters.pose(); rp.namaste = arrive >= 1f
        Characters.person(c, vx + 15f, base, 0.32f, Characters.RAM, time, rp)
        Characters.person(c, vx + 55f, base, 0.3f, Characters.SITA, time, facing = -1f)

        fireworks.draw(c)
        petals.draw(c)

        if (time > 3.8f) {
            val a = ((time - 3.8f) / 1f).coerceAtMost(1f)
            Ui.title(c, "शुभ दीपावली", V.W / 2 - 180f, 150f, 110f, a)
            Ui.title(c, "Happy Diwali!  Jai Shri Ram!", V.W / 2 - 180f, 250f, 64f, a)
        } else {
            Ui.text(c, "Shri Ram returns to Ayodhya…", V.W / 2, 120f, 56f, Ui.GOLD, outline = Color.BLACK)
        }
        if (time > 6f) {
            val a = ((time - 6f) / 1f).coerceAtMost(1f)
            Ui.text(c, "You completed the Ramayana!  ⭐ ${game.save.totalStars} / ${game.save.maxStars}   Score $score",
                V.W / 2 - 180f, 320f, 34f, Color.WHITE, outline = Color.BLACK, alpha = a)
            Ui.text(c, "Good always wins over evil – that is why we light diyas on Diwali.",
                V.W / 2 - 220f, 375f, 28f, Ui.CREAM, outline = Color.BLACK, alpha = a)
        }
        drawButtons(c)
    }
}
