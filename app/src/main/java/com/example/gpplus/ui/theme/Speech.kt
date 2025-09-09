package com.example.gpplus.ui.theme



import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.gpplus.VoiceState

// ----------------------------------------------------------
//                SPEECH RECOGNITION CONTROLLER
// ----------------------------------------------------------
data class AsrState(
    val state: VoiceState = VoiceState.Idle,
    val partialText: String = "",
    val error: String? = null,
    val audioLevel01: Float = 0f
)

class AsrController(private val recognizer: SpeechRecognizer) {
    private var onPartial: (String) -> Unit = {}
    private var onFinal: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var onRms: (Float) -> Unit = {}

    fun setCallbacks(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
        onRms: (Float) -> Unit
    ) { this.onPartial = onPartial; this.onFinal = onFinal; this.onError = onError; this.onRms = onRms }

    fun attachListener() {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { onRms((rmsdB.coerceIn(0f, 10f)) / 10f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onError("ASR error: $error") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onFinal(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) onPartial(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun start() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    fun stop() = recognizer.stopListening()
    fun destroy() = recognizer.destroy()
}


