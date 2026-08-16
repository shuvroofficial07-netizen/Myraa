package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

enum class MyraaState {
    IDLE, WAKE_DETECTED, LISTENING, PROCESSING, EXECUTING, SPEAKING, ERROR
}

class MyraaForegroundService : Service(), TextToSpeech.OnInitListener {

    private val CHANNEL_ID = "MyraaServiceChannel"
    private val NOTIFICATION_ID = 1

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    
    private var currentState = MyraaState.IDLE
    private var isListening = false
    private var isServiceRunning = false

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
        startForeground(NOTIFICATION_ID, buildNotification("Assistant is active. Listening for 'Hey MYRAA'."))
        
        if (currentState == MyraaState.IDLE && !isListening) {
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("Myraa", "Language not supported")
            } else {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        currentState = MyraaState.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d("Myraa", "TTS Done")
                        currentState = MyraaState.IDLE
                        // Restart listening after speaking
                        handler.post { startListening() }
                    }
                    override fun onError(utteranceId: String?) {
                        currentState = MyraaState.IDLE
                        handler.post { startListening() }
                    }
                })
            }
        } else {
            Log.e("Myraa", "TTS Initialization failed")
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                    if (currentState != MyraaState.SPEAKING && isServiceRunning) {
                        // Restart listening automatically
                        handler.postDelayed({ startListening() }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        val spokenText = matches[0].lowercase(Locale.getDefault())
                        Log.d("Myraa", "Heard: $spokenText")
                        processCommand(spokenText)
                    } else {
                        if (currentState != MyraaState.SPEAKING && isServiceRunning) {
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
        if (!isServiceRunning || currentState == MyraaState.SPEAKING || speechRecognizer == null) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        
        try {
            speechRecognizer?.startListening(intent)
            currentState = MyraaState.LISTENING
        } catch (e: Exception) {
            Log.e("Myraa", "Error starting listening", e)
        }
    }

    private fun processCommand(command: String) {
        currentState = MyraaState.PROCESSING
        
        // Very basic wake word and command routing
        val containsWakeWord = command.contains("hey myraa") || command.contains("myraa")
        
        if (containsWakeWord || currentState == MyraaState.WAKE_DETECTED) {
            
            if (command == "hey myraa" || command == "myraa") {
                currentState = MyraaState.WAKE_DETECTED
                speak("Yes, I'm listening.")
                return
            }

            // Command processing
            if (command.contains("battery")) {
                val batteryPct = getBatteryPercentage()
                speak("Your battery is at $batteryPct percent.")
            } else if (command.contains("time")) {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                speak("It is $time.")
            } else if (command.contains("open youtube")) {
                speak("Opening YouTube.")
                openApp("com.google.android.youtube")
            } else {
                speak("I heard you say: ${command.replace("hey myraa", "").trim()}. I am a prototype.")
            }
        } else {
            // Not a wake word, ignore and restart listening
            currentState = MyraaState.IDLE
            startListening()
        }
    }

    private fun speak(text: String) {
        if (tts != null && !text.isEmpty()) {
            currentState = MyraaState.SPEAKING
            // Stop listening while speaking to avoid hearing ourselves
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
    
    private fun openApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            speak("I could not find that app.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Myraa", "Service destroyed")
        isServiceRunning = false
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
