package app.studyscroll.narration

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class NarrationState {
    object Idle : NarrationState()
    data class Playing(val lessonId: String) : NarrationState()
    data class Paused(val lessonId: String) : NarrationState()
    data class Error(val message: String) : NarrationState()
}

class NarrationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _state = MutableStateFlow<NarrationState>(NarrationState.Idle)
    val state: StateFlow<NarrationState> = _state.asStateFlow()

    private var currentLessonId: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    currentLessonId?.let {
                        _state.value = NarrationState.Playing(it)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = NarrationState.Idle
                    currentLessonId = null
                }

                override fun onError(utteranceId: String?) {
                    _state.value = NarrationState.Error("Text-to-speech playback error.")
                    currentLessonId = null
                }
            })
            isInitialized = true
        } else {
            _state.value = NarrationState.Error("Text-to-speech initialization failed on this device.")
        }
    }

    fun speak(lessonId: String, text: String, speechRate: Float = 1.0f) {
        if (!isInitialized) {
            _state.value = NarrationState.Error("Text-to-speech is initializing. Please tap again in a moment.")
            return
        }

        if (_state.value is NarrationState.Playing && currentLessonId == lessonId) {
            stop()
            return
        }

        stop()
        currentLessonId = lessonId
        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        val utteranceId = "lesson_$lessonId"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _state.value = NarrationState.Idle
        currentLessonId = null
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
