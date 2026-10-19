package com.satya.ramayana.engine

import android.graphics.Canvas

abstract class Screen(val game: Game) {

    /** Seconds since this screen was created (advanced by [Game]). */
    var time = 0f

    protected val buttons = ArrayList<Button>()

    open fun enter() {}
    open fun exit() {}
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)

    /** Touches that did not land on a button. */
    open fun touch(action: Int, x: Float, y: Float) {}

    open fun onPause() {}

    /** Return true if back was handled. */
    open fun onBack(): Boolean = false

    fun handleTouch(action: Int, x: Float, y: Float) {
        when (action) {
            Touch.DOWN -> {
                val b = buttons.lastOrNull { it.visible && it.enabled && it.contains(x, y) }
                if (b != null) {
                    b.pressed = true
                    return
                }
            }
            Touch.MOVE -> if (buttons.any { it.pressed }) {
                buttons.forEach { if (it.pressed && !it.contains(x, y)) it.pressed = false }
                return
            }
            Touch.UP -> {
                val b = buttons.firstOrNull { it.pressed }
                if (b != null) {
                    buttons.forEach { it.pressed = false }
                    if (b.contains(x, y)) {
                        game.audio.play(Sfx.CLICK)
                        b.onClick()
                    }
                    return
                }
            }
        }
        touch(action, x, y)
    }

    protected fun drawButtons(c: Canvas) {
        for (b in buttons) if (b.visible) b.draw(c, time)
    }
}
