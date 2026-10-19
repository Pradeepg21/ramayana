package com.satya.ramayana.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

enum class Music { NONE, BHAJAN, BATTLE, FESTIVE, VIVAH }

enum class Sfx { CLICK, ARROW, HIT, COLLECT, CRACKER, CONCH, WRONG, TIMEUP, JUMP, BELL, POWER, BREAK, WIN, TICK, SPLASH }

/**
 * Small software mixer. All music and sound effects are synthesised at start-up
 * (bansuri, shehnai, tanpura, tabla, dhol, temple bells, crackers) so the game
 * needs no audio files.
 */
class AudioEngine {

    @Volatile var enabled = true

    /** Lower the music while the narrator is speaking. */
    @Volatile var duck = false

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var wantMusic = Music.NONE
    @Volatile private var ready = false

    private val queue = ConcurrentLinkedQueue<Sfx>()
    private val sfx = HashMap<Sfx, FloatArray>()
    private val songs = HashMap<Music, FloatArray>()
    private var thread: Thread? = null

    private class Voice(val data: FloatArray, val vol: Float) {
        var pos = 0
    }

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "audio").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun stop() {
        running = false
        try {
            thread?.join(500)
        } catch (_: InterruptedException) {
        }
    }

    fun setPaused(p: Boolean) {
        paused = p
    }

    fun play(s: Sfx) {
        if (enabled && ready && queue.size < 16) queue.add(s)
    }

    fun music(m: Music) {
        wantMusic = m
    }

    private fun loop() {
        Synth.buildSfx(sfx)
        ready = true
        songs[Music.BHAJAN] = Synth.buildSong(Music.BHAJAN)
        val sr = Synth.SR
        val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, 4096) * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            running = false
            return
        }
        track.play()

        val n = 512
        val mix = FloatArray(n)
        val out = ShortArray(n)
        val voices = ArrayList<Voice>()
        var curMusic = Music.NONE
        var musicPos = 0
        var gain = 0f

        while (running) {
            if (paused) {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
                try {
                    Thread.sleep(40)
                } catch (_: InterruptedException) {
                }
                continue
            } else if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }

            // Lazily synthesise the requested song the first time it is needed.
            val want = wantMusic
            if (want != Music.NONE && !songs.containsKey(want)) {
                songs[want] = Synth.buildSong(want)
            }

            while (true) {
                val s = queue.poll() ?: break
                sfx[s]?.let { voices.add(Voice(it, sfxVolume(s))) }
            }
            if (voices.size > 12) voices.subList(0, voices.size - 12).clear()

            java.util.Arrays.fill(mix, 0f)
            val on = enabled
            var song = songs[curMusic]
            val musicVol = if (duck) 0.42f * 0.35f else 0.42f
            for (i in 0 until n) {
                // crossfade between songs
                if (want != curMusic) {
                    gain -= 1f / (0.35f * sr)
                    if (gain <= 0f) {
                        gain = 0f
                        curMusic = want
                        musicPos = 0
                        song = songs[curMusic]
                    }
                } else if (gain < 1f) {
                    gain = (gain + 1f / (0.6f * sr)).coerceAtMost(1f)
                }
                if (song != null && song.isNotEmpty()) {
                    mix[i] += song[musicPos] * gain * musicVol
                    musicPos++
                    if (musicPos >= song.size) musicPos = 0
                }
            }

            val it = voices.iterator()
            while (it.hasNext()) {
                val v = it.next()
                var p = v.pos
                val d = v.data
                var i = 0
                while (i < n && p < d.size) {
                    mix[i] += d[p] * v.vol
                    i++
                    p++
                }
                v.pos = p
                if (p >= d.size) it.remove()
            }

            for (i in 0 until n) {
                var x = if (on) mix[i] else 0f
                // soft limiter
                x = x / (1f + abs(x) * 0.6f)
                out[i] = (x.coerceIn(-1f, 1f) * 30000f).toInt().toShort()
            }
            if (!on) voices.clear()
            track.write(out, 0, n)
        }
        try {
            track.stop()
        } catch (_: Exception) {
        }
        track.release()
    }

    private fun sfxVolume(s: Sfx) = when (s) {
        Sfx.CRACKER -> 0.75f
        Sfx.CLICK, Sfx.TICK -> 0.5f
        Sfx.ARROW -> 0.55f
        else -> 0.8f
    }
}
