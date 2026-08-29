package com.oryno.piggy_ledger.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Native Android TextToSpeech helper for Piggy AI Chat.
 * Provides on-device, local speech synthesis with clean prose formatting,
 * rate control, and speaking state tracking.
 */
class NativeTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var currentSpeakingText: String = ""
    private var remainingText: String = ""
    private var currentSpeakingOffset: Int = 0
    private var tickerJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Set speech rate for natural, non-rushed pacing
                engine.setSpeechRate(0.95f)
                
                // Try device default locale or fallback to English/Arabic
                val defaultLocale = Locale.getDefault()
                val result = engine.setLanguage(defaultLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _speakingMessageId.value = utteranceId
                        _isPaused.value = false
                        startTicker()
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        currentSpeakingOffset = start
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_speakingMessageId.value == utteranceId) {
                            _speakingMessageId.value = null
                            _isPaused.value = false
                            stopTicker()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_speakingMessageId.value == utteranceId) {
                            _speakingMessageId.value = null
                            _isPaused.value = false
                            stopTicker()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (_speakingMessageId.value == utteranceId) {
                            _speakingMessageId.value = null
                            _isPaused.value = false
                            stopTicker()
                        }
                    }
                })
                isInitialized = true
            }
        }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                if (!_isPaused.value && _speakingMessageId.value != null) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /**
     * Speaks the given message text using native TTS.
     * Cleans Markdown, formats currencies, and sanitizes prose.
     */
    fun speak(messageId: String, text: String) {
        if (!isInitialized || tts == null) return

        // If clicking the currently speaking message, toggle stop
        if (_speakingMessageId.value == messageId) {
            stop()
            return
        }

        // Stop any active utterance before starting a new one
        stop()

        val cleanProse = sanitizeTextForTts(text)
        if (cleanProse.isBlank()) return

        currentSpeakingText = cleanProse
        remainingText = cleanProse
        currentSpeakingOffset = 0
        _elapsedSeconds.value = 0
        _isPaused.value = false

        // Detect Arabic vs English and adapt language dynamically if supported
        val isArabic = containsArabic(cleanProse)
        if (isArabic) {
            tts?.setLanguage(Locale("ar"))
        } else {
            val deviceLocale = Locale.getDefault()
            val res = tts?.setLanguage(deviceLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }
        tts?.setSpeechRate(0.95f)

        _speakingMessageId.value = messageId
        tts?.speak(cleanProse, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    /**
     * Toggles pause / resume playback for active speech.
     */
    fun togglePauseResume() {
        val msgId = _speakingMessageId.value ?: return
        if (_isPaused.value) {
            // Resume
            _isPaused.value = false
            tts?.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, msgId)
        } else {
            // Pause
            _isPaused.value = true
            tts?.stop()
            if (currentSpeakingOffset < remainingText.length) {
                remainingText = remainingText.substring(currentSpeakingOffset)
            }
            currentSpeakingOffset = 0
        }
    }

    /**
     * Restarts the current audio speech from 00:00.
     */
    fun restart() {
        val msgId = _speakingMessageId.value ?: return
        if (currentSpeakingText.isBlank()) return
        stop()
        remainingText = currentSpeakingText
        currentSpeakingOffset = 0
        _elapsedSeconds.value = 0
        _isPaused.value = false
        _speakingMessageId.value = msgId
        tts?.speak(currentSpeakingText, TextToSpeech.QUEUE_FLUSH, null, msgId)
    }

    /**
     * Stops any currently playing speech.
     */
    fun stop() {
        tts?.stop()
        _speakingMessageId.value = null
        _isPaused.value = false
        _elapsedSeconds.value = 0
        stopTicker()
    }

    /**
     * Releases TTS resources on lifecycle destroy.
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        scope.cancel()
    }

    private fun containsArabic(text: String): Boolean {
        for (char in text) {
            if (Character.UnicodeBlock.of(char) == Character.UnicodeBlock.ARABIC) {
                return true
            }
        }
        return false
    }

    /**
     * Converts markdown and formatted text into clean natural prose suitable for TTS engines.
     * E.g., strips markdown symbols, translates currency codes into spoken words, etc.
     */
    companion object {
        fun sanitizeTextForTts(rawText: String): String {
            var text = rawText

            // Strip code blocks completely
            text = text.replace(Regex("```[\\s\\S]*?```"), "")

            // Strip JSON or UI blocks if present in string
            text = text.replace(Regex("\\{[\\s\\S]*?\\}"), "")

            // Replace common currency acronyms with spoken text
            text = text.replace(Regex("(?i)\\bEGP\\s*([0-9,.]+)"), "$1 Egyptian pounds")
            text = text.replace(Regex("(?i)\\bUSD\\s*([0-9,.]+)"), "$1 US dollars")
            text = text.replace(Regex("(?i)\\bEUR\\s*([0-9,.]+)"), "$1 euros")
            text = text.replace(Regex("(?i)\\bSAR\\s*([0-9,.]+)"), "$1 Saudi riyals")
            text = text.replace(Regex("(?i)\\bAED\\s*([0-9,.]+)"), "$1 UAE dirhams")
            text = text.replace(Regex("\\$([0-9,.]+)"), "$1 dollars")

            // Remove markdown headers
            text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")

            // Remove bold, italics, strikethrough, inline code
            text = text.replace(Regex("[*_~`]{1,3}"), "")

            // Remove markdown links: [text](url) -> text
            text = text.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

            // Remove blockquote markers
            text = text.replace(Regex("(?m)^>\\s*"), "")

            // Remove bullet points / numbering markers at line start
            text = text.replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
            text = text.replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")

            // Remove table pipes and divider lines
            text = text.replace(Regex("\\|"), " ")
            text = text.replace(Regex("[-:]{3,}"), "")

            // Remove extra whitespace / newlines
            text = text.replace(Regex("\\n+"), ". ")
            text = text.replace(Regex("\\s{2,}"), " ")

            return text.trim()
        }
    }
}
