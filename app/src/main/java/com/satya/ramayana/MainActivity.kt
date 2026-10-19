package com.satya.ramayana

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.satya.ramayana.art.Promo
import com.satya.ramayana.engine.GameView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        gameView = GameView(this)
        setContentView(gameView)
        hideSystemBars()

        // Debug builds only: `adb shell am start -n com.satya.ramayana/.MainActivity --es screen stage5`
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            intent.getStringExtra("screen")?.let { gameView.game.debugOpen(it) }
            // `--es shot 3840x2160 --ef delay 6` saves a high-resolution frame; `--ez promo true` saves store art
            val files = getExternalFilesDir(null) ?: filesDir
            intent.getStringExtra("shot")?.split("x")?.let { (w, h) ->
                gameView.requestShot(File(files, "shot.png"), w.toInt(), h.toInt(), intent.getFloatExtra("delay", 5f))
            }
            if (intent.getBooleanExtra("promo", false)) Promo.exportAll(File(files, "promo"))
            intent.getIntExtra("progress", -1).let { if (it >= 0) gameView.game.debugProgress = it }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!gameView.onBack()) finish()
            }
        })
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        gameView.pause()
        super.onPause()
    }

    override fun onDestroy() {
        gameView.release()
        super.onDestroy()
    }
}
