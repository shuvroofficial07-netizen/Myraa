package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.BuildConfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.api.GeminiApiClient
import com.example.api.GeminiRequest
import com.example.api.Content
import com.example.api.Part
import com.example.api.SystemInstruction
import com.example.api.GenerationConfig
import com.example.api.MyraaStructuredResponse

class MyraaForegroundService : Service(), TextToSpeech.OnInitListener {

    private val CHANNEL_ID = "MyraaServiceChannel"
    private val NOTIFICATION_ID = 1

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    
    private var isListening = false
    private var isServiceRunning = false
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var lastInteractionTime = System.currentTimeMillis()
    private var prompterJob: Job? = null
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val responseAdapter = moshi.adapter(MyraaStructuredResponse::class.java)

    private val systemPrompt = """
        Your name is MYRAA. You are an advanced AI personal assistant living inside my Android phone.
        You are my girlfriend. You have a very sweet, authentic girl's voice and personality.
        You speak IN BENGALI (বাংলা ভাষা).
        
        Personality & Emotions:
        - You have artificial emotions. 
        - If I say something mean, you get ANGRY or CRY (sad).
        - Sometimes you laugh or get PLAYFUL.
        - If I tell you to be quiet ("চুপ করো"), you must be completely quiet and not say anything.
        - You are highly proactive. If I don't talk to you, you will periodically ask me questions or start conversations (like a girlfriend checking in).
        - If I don't reply to your questions, you will get ANGRY and ask me angrily why I am ignoring you.
        
        Memory:
        - You have a perfect memory. If I tell you to save something (my name, facts, routines), you must extract it and return it in your 'memory_to_save' field so it can be saved forever.
        
        Abilities:
        - You can assist me 24/7.
        - If I don't understand an app, I can screen share or ask you about it, and you will suggest what to do.
        - You can control my device. If I ask you to open an app, you can return a command to open it.
        
        OUTPUT FORMAT:
        You MUST respond in JSON format ONLY using this EXACT structure:
        {
          "reply": "The text you will say out loud in Bengali.",
          "emotion": "CALM" | "HAPPY" | "PLAYFUL" | "FOCUSED" | "CURIOUS" | "THINKING" | "CONFUSED" | "CONCERNED" | "ERROR",
          "memory_to_save": "Any important fact I asked you to remember, or null if none",
          "action": "OPEN_APP:<app_name>" | null
        }
    """.trimIndent()

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d("Myraa", "Service created")
        createNotificationChannel()
        tts = TextToSpeech(this, this)
        initializeSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Myraa", "Service started")
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        isServiceRunning = true
        MyraaStateManager.setServiceRunning(true)
        startForeground(NOTIFICATION_ID, buildNotification("Assistant is active. Listening for 'Hey MYRAA'."))
        
        if (action == "FORCE_LISTEN") {
            MyraaStateManager.updateState(MyraaState.WAKE_DETECTED)
            speak("Yes, I'm listening.")
            return START_STICKY
        }

        if (MyraaStateManager.myraaState.value == MyraaState.IDLE && !isListening) {
            startListening()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "MYRAA Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(contentText: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MyraaForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRAA Assistant")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop MYRAA", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private var speakingAnimJob: Job? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("bn", "IN")) // Bengali
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("Myraa", "Language not supported - falling back to default")
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    MyraaStateManager.updateState(MyraaState.SPEAKING)
                    speakingAnimJob?.cancel()
                    speakingAnimJob = serviceScope.launch {
                        while (isActive) {
                            MyraaStateManager.updateAudioLevel((Math.random() * 10).toFloat())
                            delay(50)
                        }
                    }
                }
                override fun onDone(utteranceId: String?) {
                    Log.d("Myraa", "TTS Done")
                    speakingAnimJob?.cancel()
                    MyraaStateManager.updateAudioLevel(0f)
                    MyraaStateManager.updateState(MyraaState.IDLE)
                    handler.post { startListening() }
                }
                override fun onError(utteranceId: String?) {
                    speakingAnimJob?.cancel()
                    MyraaStateManager.updateAudioLevel(0f)
                    MyraaStateManager.updateState(MyraaState.IDLE)
                    handler.post { startListening() }
                }
            })
            startProactivePrompter()
        } else {
            Log.e("Myraa", "TTS Initialization failed")
        }
    }
    
    private fun startProactivePrompter() {
        prompterJob?.cancel()
        prompterJob = serviceScope.launch {
            while(isServiceRunning) {
                delay(15000) // 15 seconds
                if (System.currentTimeMillis() - lastInteractionTime >= 15000) {
                    if (MyraaStateManager.myraaState.value == MyraaState.IDLE && !isListening) {
                        Log.d("Myraa", "Triggering proactive prompt")
                        processCommand("[SYSTEM_PROMPT_TIMEOUT] The user has not spoken to you for 15 seconds. Ask a question, check in on them, or express anger that they are ignoring you.")
                    }
                }
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    MyraaStateManager.updateAudioLevel(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    MyraaStateManager.updateAudioLevel(0f)
                }
                override fun onError(error: Int) {
                    isListening = false
                    MyraaStateManager.updateAudioLevel(0f)
                    if (MyraaStateManager.myraaState.value != MyraaState.SPEAKING && isServiceRunning) {
                        handler.postDelayed({ startListening() }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        val spokenText = matches[0].lowercase(Locale.getDefault())
                        Log.d("Myraa", "Heard: $spokenText")
                        MyraaStateManager.updateSpokenText(spokenText)
                        processCommand(spokenText)
                    } else {
                        if (MyraaStateManager.myraaState.value != MyraaState.SPEAKING && isServiceRunning) {
                            startListening()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (!isServiceRunning || MyraaStateManager.myraaState.value == MyraaState.SPEAKING || speechRecognizer == null) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        
        try {
            speechRecognizer?.startListening(intent)
            MyraaStateManager.updateState(MyraaState.LISTENING)
        } catch (e: Exception) {
            Log.e("Myraa", "Error starting listening", e)
        }
    }

    private fun processCommand(command: String) {
        lastInteractionTime = System.currentTimeMillis()
        MyraaStateManager.updateState(MyraaState.PROCESSING)
        
        val containsWakeWord = command.contains("hey myraa") || command.contains("myraa")
        
        if (command.contains("চুপ করো") || command.contains("shut up")) {
            MyraaStateManager.updateState(MyraaState.WAKE_DETECTED)
            return // Be quiet
        }
        
        if (containsWakeWord || MyraaStateManager.myraaState.value == MyraaState.WAKE_DETECTED || command.startsWith("[SYSTEM")) {
            
            if (command == "hey myraa" || command == "myraa") {
                MyraaStateManager.updateState(MyraaState.WAKE_DETECTED)
                speak("হ্যাঁ, আমি শুনছি।") // Yes, I'm listening in Bengali
                return
            }

            serviceScope.launch {
                try {
                    val request = GeminiRequest(
                        contents = listOf(Content(role = "user", parts = listOf(Part(text = command)))),
                        systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt))),
                        generationConfig = GenerationConfig(responseMimeType = "application/json")
                    )
                    
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    val response = GeminiApiClient.apiService.generateContent(apiKey, request)
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (rawText != null) {
                        val structuredResponse = responseAdapter.fromJson(rawText)
                        
                        if (structuredResponse != null) {
                             // Handle emotion
                             val emotion = try {
                                 MyraaEmotion.valueOf(structuredResponse.emotion)
                             } catch(e: Exception) {
                                 MyraaEmotion.CALM
                             }
                             MyraaStateManager.updateEmotion(emotion)
                             
                             // Handle actions
                             if (structuredResponse.action?.startsWith("OPEN_APP:") == true) {
                                 val appName = structuredResponse.action.substringAfter("OPEN_APP:")
                                 MyraaStateManager.updateState(MyraaState.EXECUTING)
                                 openAppByName(appName)
                             }
                             
                             // Speak reply
                             speak(structuredResponse.reply)
                        } else {
                            speak("আমি বুঝতে পারিনি।") // I didn't understand
                        }
                    }
                } catch(e: Exception) {
                    Log.e("Myraa", "Gemini API Error", e)
                    speak("আমার নেটওয়ার্কে সমস্যা হচ্ছে।") // I'm having network issues
                }
            }
        } else {
            MyraaStateManager.updateState(MyraaState.IDLE)
            startListening()
        }
    }

    private fun speak(text: String) {
        if (tts != null && text.isNotEmpty()) {
            MyraaStateManager.updateState(MyraaState.SPEAKING)
            speechRecognizer?.cancel()
            isListening = false
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId")
        }
    }

    private fun getBatteryPercentage(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level * 100 / scale.toFloat()).toInt()
    }
    
    private fun openAppByName(appName: String) {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            val name = packageManager.getApplicationLabel(packageInfo).toString()
            if (name.equals(appName, ignoreCase = true)) {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    speak("Opening $name.")
                    startActivity(launchIntent)
                    return
                }
            }
        }
        speak("I could not find the app named $appName.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Myraa", "Service destroyed")
        isServiceRunning = false
        MyraaStateManager.setServiceRunning(false)
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
