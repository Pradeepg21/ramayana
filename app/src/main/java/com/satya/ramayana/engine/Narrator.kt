package com.satya.ramayana.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Reads the story aloud with the phone's text-to-speech in an Indian-English voice.
 * Music is ducked while the narrator speaks.
 */
class Narrator(context: Context, private val audio: AudioEngine) {

    @Volatile private var ready = false
    @Volatile var speaking = false
        private set
    var enabled = true
        set(v) {
            field = v
            if (!v) stop()
        }

    private var pending: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) onReady()
    }

    private fun onReady() {
        val indian = Locale("en", "IN")
        val ok = tts.setLanguage(indian)
        if (ok == TextToSpeech.LANG_MISSING_DATA || ok == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.UK)
        // a warm storyteller: a little slower than normal speech
        tts.setSpeechRate(0.88f)
        tts.setPitch(1.0f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = setSpeaking(true)
            override fun onDone(id: String?) = setSpeaking(false)
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = setSpeaking(false)
            override fun onStop(id: String?, interrupted: Boolean) = setSpeaking(false)
        })
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    private fun setSpeaking(s: Boolean) {
        speaking = s
        audio.duck = s
    }

    /** Speak [text], replacing anything being read now. */
    fun speak(text: String) {
        if (!enabled) return
        if (!ready) {
            pending = text
            return
        }
        tts.speak(clean(text), TextToSpeech.QUEUE_FLUSH, null, "story")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
        setSpeaking(false)
    }

    fun release() {
        stop()
        tts.shutdown()
    }

    /** Make the text pleasant to hear: Hindi words spelt out, symbols and emoji removed. */
    private fun clean(s: String): String {
        var t = s
        for ((hi, en) in SPOKEN) t = t.replace(hi, en)
        t = t.replace("–", ", ").replace("…", ". ")
        // drop emoji and any remaining Devanagari the English voice cannot read
        return t.filter { it.code < 0x0900 || (it.code in 0x0980 until 0xD800 && it.code !in 0x2190..0x2BFF) }
            .replace(Regex("\\s+"), " ").trim()
    }

    private companion object {
        val SPOKEN = listOf(
            "राम" to "Ram", "भक्ति" to "Bhakti", "सीता" to "Sita",
            "ahankar" to "ahankaar", "Vanvas" to "Vanvaas", "yojanas" to "yojanaas",
        )
    }
}
