package com.oryno.piggy_ledger.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.BuildConfig
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.voice.AudioRecorder
import com.oryno.piggy_ledger.voice.GroqClient
import com.oryno.piggy_ledger.voice.GroqChatRequest
import com.oryno.piggy_ledger.voice.GroqChatMessage
import com.oryno.piggy_ledger.voice.GroqResponseFormat
import com.oryno.piggy_ledger.voice.GroqChatResponse
import com.oryno.piggy_ledger.voice.GroqChoice
import com.oryno.piggy_ledger.voice.ParsedTransaction
import com.oryno.piggy_ledger.voice.TransactionParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Date
import java.util.UUID

sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object Recording : VoiceUiState()
    object Processing : VoiceUiState()
    data class Result(val text: String, val countdown: Int, val parsed: ParsedTransaction) : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}

class VoiceLedgerViewModel(
    private val repository: PiggyLedgerRepository,
    private val context: Context
) : ViewModel() {

    private val audioRecorder = AudioRecorder(context)
    private var recordingFile: File? = null

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    
    private val groqApiKey = BuildConfig.GROQ_API_KEY.takeIf { it.isNotEmpty() } ?: System.getenv("GROQ_API_KEY") ?: ""

    fun startRecording() {
        _uiState.value = VoiceUiState.Recording
        recordingFile = audioRecorder.startRecording()
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _uiState.value = VoiceUiState.Idle
    }

    fun stopAndProcessRecording() {
        audioRecorder.stopRecording()
        
        if (recordingFile == null || !recordingFile!!.exists()) {
            _uiState.value = VoiceUiState.Error("No audio file found")
            return
        }

        _uiState.value = VoiceUiState.Processing
        
        if (groqApiKey.isBlank()) {
            _uiState.value = VoiceUiState.Error("Groq API Key is missing")
            return
        }

        viewModelScope.launch {
            try {
                val fileBody = recordingFile!!.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val multipartFile = MultipartBody.Part.createFormData("file", recordingFile!!.name, fileBody)
                
                val model = "whisper-large-v3".toRequestBody("text/plain".toMediaTypeOrNull())
                val language = "en".toRequestBody("text/plain".toMediaTypeOrNull()) // Could be empty for auto
                val prompt = "Transcription of a financial voice note.".toRequestBody("text/plain".toMediaTypeOrNull())
                val responseFormat = "json".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = GroqClient.api.transcribe(
                    auth = "Bearer $groqApiKey",
                    file = multipartFile,
                    model = model,
                    language = language,
                    prompt = prompt,
                    responseFormat = responseFormat
                )

                val transcribedText = response.text
                if (transcribedText.isBlank() || transcribedText.length < 2) {
                    _uiState.value = VoiceUiState.Error("No speech detected. Please try again.")
                    return@launch
                }

                processTranscript(transcribedText)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = VoiceUiState.Error(e.localizedMessage ?: "Failed to process audio")
            } finally {
                recordingFile?.delete()
            }
        }
    }

    fun processTranscript(text: String) {
        viewModelScope.launch {
            val accounts = repository.allAccounts.first().map { it.name }
            val goals = repository.allGoals.first().map { it.name }
            
            _uiState.value = VoiceUiState.Processing // Show processing during LLM extraction too
            
            try {
                val systemPrompt = """
                    You are a financial transaction parser. Extract details from the text.
                    Accounts available: ${accounts.joinToString(", ")}
                    Goals available: ${goals.joinToString(", ")}
                    
                    Respond ONLY with a JSON object:
                    {
                        "amount": number,
                        "accountName": string or null (match exactly from available accounts),
                        "goalName": string or null (match exactly from available goals),
                        "isExpense": boolean (true for spending/withdrawing, false for adding/earning/deposit)
                    }
                """.trimIndent()

                val request = GroqChatRequest(
                    model = "llama3-8b-8192", // Correct model name
                    messages = listOf(
                        GroqChatMessage("system", systemPrompt),
                        GroqChatMessage("user", text)
                    ),
                    response_format = GroqResponseFormat("json_object")
                )

                val response = GroqClient.api.chatCompletion("Bearer $groqApiKey", request)
                val json = response.choices.first().message.content
                
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(ParsedTransaction::class.java)
                val parsed = adapter.fromJson(json) ?: TransactionParser.parse(text, accounts, goals)
                
                _uiState.value = VoiceUiState.Result(text, 5, parsed)
                startCountdown()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to basic parser
                val parsed = TransactionParser.parse(text, accounts, goals)
                _uiState.value = VoiceUiState.Result(text, 5, parsed)
                startCountdown()
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 5 downTo 1) {
                val currentState = _uiState.value
                if (currentState is VoiceUiState.Result) {
                    _uiState.value = currentState.copy(countdown = i)
                } else {
                    return@launch
                }
                delay(1000)
            }
            // Auto accept
            val currentState = _uiState.value
            if (currentState is VoiceUiState.Result) {
                confirmTransaction()
            }
        }
    }

    fun pauseCountdown() {
        countdownJob?.cancel()
    }

    fun resumeCountdown() {
        val currentState = _uiState.value
        if (currentState is VoiceUiState.Result) {
             startCountdown()
        }
    }

    fun cancelResult() {
        countdownJob?.cancel()
        _uiState.value = VoiceUiState.Idle
    }

    fun updateTarget(accountName: String?, goalName: String?) {
        val currentState = _uiState.value
        if (currentState is VoiceUiState.Result) {
            val updatedParsed = currentState.parsed.copy(accountName = accountName, goalName = goalName)
            _uiState.value = currentState.copy(parsed = updatedParsed)
            pauseCountdown() // Optional: pause when they change it so they have time to confirm
        }
    }

    fun confirmTransaction() {
        countdownJob?.cancel()
        val currentState = _uiState.value
        if (currentState is VoiceUiState.Result) {
            val parsed = currentState.parsed
            
            viewModelScope.launch {
                val accountsList = repository.allAccounts.first()
                val targetAccount = accountsList.find { it.name == parsed.accountName }
                val goalsList = repository.allGoals.first()
                val targetGoal = goalsList.find { it.name == parsed.goalName }
                
                if (targetAccount != null) {
                    val finalAmount = if (parsed.isExpense) -parsed.amount else parsed.amount
                    repository.insertAccountTransaction(
                        com.oryno.piggy_ledger.data.AccountTransaction(
                            account_id = targetAccount.id,
                            amount = finalAmount,
                            merchant = parsed.goalName ?: "Voice Entry",
                            source = "VOICE"
                        )
                    )
                } else if (targetGoal != null) {
                    val finalAmount = if (parsed.isExpense) -parsed.amount else parsed.amount
                    repository.insertTransaction(
                        com.oryno.piggy_ledger.data.Transaction(
                            id = UUID.randomUUID().toString(),
                            goalId = targetGoal.id,
                            amount = finalAmount,
                            note = currentState.text
                        )
                    )
                }
                
                _uiState.value = VoiceUiState.Idle
            }
        }
    }
}
