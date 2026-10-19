package com.satya.ramayana.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import com.satya.ramayana.art.Promo
import java.io.File
import android.view.MotionEvent
import android.view.View

/**
 * Hosts the game loop on the UI thread, driven by vsync through
 * [postInvalidateOnAnimation] so drawing is hardware accelerated.
 * Everything is drawn into a fixed virtual canvas of [V.W] x [V.H] that is
 * scaled and letter-boxed to fit the real screen.
 */
class GameView(context: Context) : View(context) {

    val game = Game(context.applicationContext)

    private var running = false
    private var last = 0L

    // Debug-only screenshot: after [shotDelay] seconds the next frame is also rendered to [shotFile].
    private var shotFile: File? = null
    private var shotW = 0
    private var shotH = 0
    private var shotDelay = 0f
    private var played = 0f

    fun requestShot(file: File, w: Int, h: Int, delay: Float) {
        shotFile = file
        shotW = w
        shotH = h
        shotDelay = delay
        played = 0f
    }

    init {
        isFocusable = true
        keepScreenOn = true
    }

    fun resume() {
        game.audio.setPaused(false)
        running = true
        last = System.nanoTime()
        postInvalidateOnAnimation()
    }

    fun pause() {
        running = false
        game.onPause()
        game.audio.setPaused(true)
    }

    fun release() {
        running = false
        game.release()
    }

    fun onBack(): Boolean = game.onBack()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        game.setViewport(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
        last = now
        if (running) game.update(dt)
        game.render(canvas)
        played += dt
        shotFile?.let { f ->
            if (played >= shotDelay) {
                shotFile = null
                game.setViewport(shotW, shotH)
                Promo.export(f, shotW, shotH, shotW.toFloat(), shotH.toFloat()) { game.render(it) }
                game.setViewport(width, height)
            }
        }
        if (running) postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> Touch.DOWN
            MotionEvent.ACTION_MOVE -> Touch.MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> Touch.UP
            else -> return true
        }
        val idx = if (action == Touch.MOVE) 0 else event.actionIndex
        game.touch(action, event.getX(idx), event.getY(idx))
        return true
    }
}

object Touch {
    const val DOWN = 0
    const val MOVE = 1
    const val UP = 2
}

/** Virtual resolution. */
object V {
    const val W = 1600f
    const val H = 900f
}
