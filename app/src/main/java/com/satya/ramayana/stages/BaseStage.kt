package com.satya.ramayana.stages

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.satya.ramayana.art.Characters
import com.satya.ramayana.engine.Button
import com.satya.ramayana.engine.Game
import com.satya.ramayana.engine.Music
import com.satya.ramayana.engine.P
import com.satya.ramayana.engine.Particles
import com.satya.ramayana.engine.Screen
import com.satya.ramayana.engine.Sfx
import com.satya.ramayana.engine.Touch
import com.satya.ramayana.engine.Ui
import com.satya.ramayana.engine.V
import com.satya.ramayana.game.StageInfo
import com.satya.ramayana.game.Stages
import com.satya.ramayana.screens.MapScreen
import com.satya.ramayana.screens.ResultScreen
import kotlin.math.sin

/**
 * Shared stage flow: countdown → play (timer runs) → win / time-up.
 * Nobody is ever hurt: failing simply shows "Time Up" and the stage can be retried.
 *
 * Each stage has 3 levels. A higher level makes the whole game world move faster
 * ([speed]) and asks for a bigger goal ([goalFor]), with only a little extra time.
 */
abstract class BaseStage(game: Game, val info: StageInfo, val level: Int = 1) : Screen(game) {

    enum class State { INTRO, PLAY, PAUSED, WON, LOST }

    var state = State.INTRO
    private var stateTime = 0f
    /** How much faster everything moves on this level. */
    val speed = floatArrayOf(1f, 1.15f, 1.3f)[level - 1]

    /** Time for this level. Percent-goal stages get less time because the faster world finishes sooner. */
    val timeLimit = info.timeLimit * (if (info.percentGoal) floatArrayOf(1f, 0.93f, 0.86f) else floatArrayOf(1f, 1.05f, 1.1f))[level - 1]

    var timeLeft = timeLimit
    var score = 0
    var progress = 0
    abstract val goal: Int
    open val goalLabel = "Goal"

    /** Scale a level-1 goal up for levels 2 and 3. */
    protected fun goalFor(base: Int) = Math.round(base * floatArrayOf(1f, 1.2f, 1.4f)[level - 1])
    open val music = Music.BHAJAN

    /** Seconds of play so far — stages use this to ramp up difficulty. */
    var playTime = 0f

    val fx = Particles()
    private val floaters = ArrayList<Floater>()
    private var failReason = ""
    private var lastTick = 99

    private val hanumanAvailable = info.num > Stages.HANUMAN_STAGE && game.save.hanumanShakti
    private var hanumanUsed = false
    private var hanumanAnim = -1f

    private class Floater(val text: String, var x: Float, var y: Float, val color: Int, var life: Float = 1.2f)

    private val pauseBtn = Button("II", V.W - 60f, 52f, 76f, 64f, Button.SECONDARY) { pauseGame() }
    private val hanumanBtn = Button("", V.W - 110f, V.H - 110f, 150f, 150f, Button.PRIMARY) { useHanuman() }
    private val resumeBtn = Button("Resume", V.W / 2, 420f, 380f, 90f, Button.GREEN) { state = State.PLAY }
    private val restartBtn = Button("Restart Stage", V.W / 2, 530f, 380f, 90f, Button.PRIMARY) {
        game.go(Stages.create(game, info.num, level))
    }
    private val mapBtn = Button("Stage Map", V.W / 2, 640f, 380f, 90f, Button.SECONDARY) { game.go(MapScreen(game)) }

    init {
        buttons.add(pauseBtn)
        buttons.add(hanumanBtn)
        buttons.add(resumeBtn)
        buttons.add(restartBtn)
        buttons.add(mapBtn)
        hanumanBtn.visible = false
        showPauseMenu(false)
    }

    override fun enter() {
        game.save.lastStage = info.num
        game.save.lastLevel = level
        game.audio.music(music)
        game.audio.play(Sfx.CONCH)
    }

    private fun showPauseMenu(show: Boolean) {
        resumeBtn.visible = show
        restartBtn.visible = show
        mapBtn.visible = show
        pauseBtn.visible = !show
    }

    fun pauseGame() {
        if (state == State.PLAY) state = State.PAUSED
    }

    override fun onPause() = pauseGame()

    override fun onBack(): Boolean {
        when (state) {
            State.PLAY -> pauseGame()
            State.PAUSED -> state = State.PLAY
            else -> {}
        }
        return true
    }

    // ------------------------------------------------------------ hooks

    /** Advance game objects. Only called while playing. */
    abstract fun updateGame(dt: Float)

    /** Draw the world (background + objects). */
    abstract fun drawWorld(c: Canvas)

    open fun onGameTouch(action: Int, x: Float, y: Float) {}

    /** Hanuman Shakti effect. Default: +10 seconds. */
    open fun onHanumanPower() {
        addTime(10f, V.W / 2, 300f)
    }

    /** Decorative animation still running after the stage ends. */
    open fun updateAmbient(dt: Float) {}

    // ------------------------------------------------------------ helpers

    fun float(text: String, x: Float, y: Float, color: Int = Ui.GOLD) {
        floaters.add(Floater(text, x, y, color))
    }

    fun penalty(sec: Float, x: Float, y: Float, msg: String? = null) {
        timeLeft -= sec
        game.audio.play(Sfx.WRONG)
        float("-${sec.toInt()}s", x, y, 0xFFFF6E6E.toInt())
        if (msg != null) float(msg, x, y - 50f, Color.WHITE)
    }

    fun addTime(sec: Float, x: Float, y: Float) {
        timeLeft += sec
        float("+${sec.toInt()}s", x, y, 0xFF8CFF9E.toInt())
    }

    fun addProgress(n: Int = 1, x: Float = V.W / 2, y: Float = 300f, points: Int = 100) {
        progress = (progress + n).coerceAtMost(goal)
        score += points * n
        float("+${points * n}", x, y)
    }

    fun fail(reason: String) {
        if (state != State.PLAY) return
        failReason = reason
        state = State.LOST
        stateTime = 0f
        game.audio.play(Sfx.TIMEUP)
    }

    open fun win() {
        if (state != State.PLAY) return
        state = State.WON
        stateTime = 0f
        game.audio.play(Sfx.WIN)
        fx.burst(V.W / 2, V.H / 2, intArrayOf(Ui.GOLD, 0xFFFFFFFF.toInt(), Ui.SAFFRON), 80, 600f, 8f, 100f)
    }

    fun stars(): Int {
        val frac = timeLeft / timeLimit
        return when {
            frac > 0.45f -> 3
            frac > 0.2f -> 2
            else -> 1
        }
    }

    /** Where to go after the win celebration. */
    open fun afterWin() {
        val st = stars()
        val timeBonus = (timeLeft.coerceAtLeast(0f) * 20).toInt()
        game.save.completeLevel(info.num, level, st, score + timeBonus)
        game.go(ResultScreen(game, info.num, level, true, st, score + timeBonus))
    }

    // ------------------------------------------------------------ loop

    override fun update(dt: Float) {
        stateTime += dt
        showPauseMenu(state == State.PAUSED)
        hanumanBtn.visible = hanumanAvailable && !hanumanUsed && state == State.PLAY
        when (state) {
            State.INTRO -> if (stateTime > 2.6f) {
                state = State.PLAY
                stateTime = 0f
            }
            State.PLAY -> {
                playTime += dt
                timeLeft -= dt
                val sec = timeLeft.toInt()
                if (timeLeft < 10f && sec != lastTick && sec >= 0) {
                    lastTick = sec
                    game.audio.play(Sfx.TICK)
                }
                updateGame(dt * speed)
                if (state == State.PLAY) {
                    if (progress >= goal) win()
                    else if (timeLeft <= 0f) {
                        timeLeft = 0f
                        fail("Time Up!")
                    }
                }
            }
            State.PAUSED -> return
            State.WON -> if (stateTime > 2.4f) {
                afterWin()
                stateTime = -999f
            }
            State.LOST -> if (stateTime > 2f) {
                game.go(ResultScreen(game, info.num, level, false, 0, score, failReason))
                stateTime = -999f
            }
        }
        if (state == State.WON || state == State.LOST) updateAmbient(dt)
        if (hanumanAnim >= 0f) {
            hanumanAnim += dt
            if (hanumanAnim > 2.2f) hanumanAnim = -1f
        }
        fx.update(dt)
        val it = floaters.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.life -= dt
            f.y -= 60f * dt
            if (f.life <= 0f) it.remove()
        }
    }

    private fun useHanuman() {
        if (hanumanUsed || state != State.PLAY) return
        hanumanUsed = true
        hanumanAnim = 0f
        game.audio.play(Sfx.POWER)
        fx.burst(V.W / 2, V.H / 2, intArrayOf(0xFFFF9800.toInt(), Ui.GOLD, 0xFFFFFFFF.toInt()), 90, 700f, 8f, 0f)
        onHanumanPower()
    }

    override fun touch(action: Int, x: Float, y: Float) {
        if (state == State.PLAY) onGameTouch(action, x, y)
    }

    override fun draw(c: Canvas) {
        drawWorld(c)
        fx.draw(c)
        for (f in floaters) {
            Ui.text(c, f.text, f.x, f.y, 44f, f.color, outline = 0xCC000000.toInt(), alpha = (f.life / 0.4f).coerceAtMost(1f))
        }
        if (hanumanAnim >= 0f) drawHanumanFlight(c)
        drawHud(c)
        when (state) {
            State.INTRO -> drawIntro(c)
            State.PAUSED -> {
                c.drawRect(0f, 0f, V.W, V.H, P.f(0xAA000000.toInt()))
                Ui.panel(c, V.W / 2 - 300f, 200f, V.W / 2 + 300f, 720f)
                Ui.title(c, "Paused", V.W / 2, 320f, 80f)
            }
            State.WON -> {
                val k = (stateTime / 0.4f).coerceAtMost(1f)
                c.drawRect(0f, 0f, V.W, V.H, P.glow(V.W / 2, V.H / 2, 900f, 0xFFFFE082.toInt(), 0.6f * k))
                Ui.title(c, "Jai Shri Ram!", V.W / 2, V.H / 2 + 30f, 120f * (0.6f + 0.4f * k))
            }
            State.LOST -> {
                c.drawRect(0f, 0f, V.W, V.H, P.f(0x88000000.toInt()))
                Ui.title(c, failReason, V.W / 2, V.H / 2, 110f)
                Ui.text(c, "Don't worry – let's try again!", V.W / 2, V.H / 2 + 80f, 44f, Color.WHITE, outline = Color.BLACK)
            }
            else -> {}
        }
        drawButtons(c)
        if (hanumanBtn.visible) drawHanumanButton(c)
    }

    private fun drawIntro(c: Canvas) {
        c.drawRect(0f, 0f, V.W, V.H, P.f(0x77000000))
        val k = (stateTime / 0.5f).coerceAtMost(1f)
        Ui.text(c, "STAGE ${info.num}  ·  LEVEL $level", V.W / 2, 300f, 56f, Ui.CREAM, outline = Color.BLACK, alpha = k)
        for (s in 0 until Stages.LEVELS) Ui.star(c, V.W / 2 - 60f + s * 60f, 470f, 22f, s < level)
        Ui.title(c, info.title, V.W / 2, 400f, 84f, k)
        val cd = when {
            stateTime < 0.9f -> "Get Ready!"
            stateTime < 1.6f -> "Steady…"
            else -> "Jai Shri Ram!"
        }
        Ui.title(c, cd, V.W / 2, 590f, 96f + 10f * sin(stateTime * 10f))
    }

    private fun drawHud(c: Canvas) {
        // top bar
        c.drawRect(0f, 0f, V.W, 100f, P.vgrad(0f, 100f, 0xDD3A0610.toInt(), 0x003A0610))
        Ui.text(c, "${info.num}. ${info.title}", 26f, 60f, if (info.title.length > 24) 28f else 34f, Ui.CREAM, Paint.Align.LEFT, outline = 0xAA000000.toInt())
        Ui.text(c, "Level $level of ${Stages.LEVELS}", 26f, 96f, 26f, Ui.GOLD, Paint.Align.LEFT, outline = 0xAA000000.toInt())

        // timer bar
        val bx = 610f
        val bw = 400f
        val frac = (timeLeft / timeLimit).coerceIn(0f, 1f)
        Ui.roundRect(c, bx, 30f, bx + bw, 70f, 20f, P.f(0x88000000.toInt()))
        val col = when {
            timeLeft < 10f -> if ((time * 4).toInt() % 2 == 0) 0xFFFF5252.toInt() else 0xFFFF8A80.toInt()
            frac < 0.4f -> 0xFFFFB300.toInt()
            else -> 0xFF66BB6A.toInt()
        }
        if (frac > 0f) Ui.roundRect(c, bx + 4f, 34f, bx + 4f + (bw - 8f) * frac, 66f, 16f, P.f(col))
        Ui.roundRect(c, bx, 30f, bx + bw, 70f, 20f, P.s(Ui.GOLD, 3f))
        Ui.text(c, "⏳ ${timeLeft.coerceAtLeast(0f).toInt()}s", bx + bw / 2, 62f, 32f, Color.WHITE, outline = Color.BLACK)

        // goal counter
        Ui.text(c, "$goalLabel: $progress / $goal", 1450f, 62f, 32f, Ui.GOLD, Paint.Align.RIGHT, outline = 0xCC000000.toInt())
        Ui.text(c, "Score $score", 1440f, 100f, 26f, Ui.CREAM, Paint.Align.RIGHT, outline = 0xCC000000.toInt())
    }

    private fun drawHanumanButton(c: Canvas) {
        val x = hanumanBtn.x
        val y = hanumanBtn.y
        val pulse = 1f + 0.06f * sin(time * 5f)
        c.drawCircle(x, y, 110f * pulse, P.glow(x, y, 110f * pulse, 0xFFFF9800.toInt(), 0.8f))
        c.drawCircle(x, y, 70f, P.vgrad(y - 70f, y + 70f, 0xFFFFB74D.toInt(), 0xFFD84315.toInt()))
        c.drawCircle(x, y, 70f, P.s(Ui.GOLD, 5f))
        Characters.gada(c, x - 6f, y + 52f, 20f)
        Ui.text(c, "Hanuman", x, y + 100f, 26f, Color.WHITE, outline = Color.BLACK)
        Ui.text(c, "Shakti", x, y + 126f, 26f, Ui.GOLD, outline = Color.BLACK)
    }

    private fun drawHanumanFlight(c: Canvas) {
        val k = hanumanAnim / 2.2f
        val x = -200f + (V.W + 400f) * k
        val y = 420f - sin(k * 3.14f) * 160f
        c.drawCircle(x, y - 100f, 260f, P.glow(x, y - 100f, 260f, 0xFFFFB74D.toInt(), 0.7f))
        Characters.vanar(c, x, y, 1.3f, Characters.HANUMAN, time, flying = true)
        val a = if (k < 0.15f) k / 0.15f else if (k > 0.85f) (1f - k) / 0.15f else 1f
        Ui.title(c, "Jai Hanuman!", V.W / 2, 220f, 100f, a)
    }
}
