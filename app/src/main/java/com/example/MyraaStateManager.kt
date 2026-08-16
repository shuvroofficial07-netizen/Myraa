package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class MyraaState {
    IDLE, WAKE_DETECTED, LISTENING, PROCESSING, EXECUTING, SPEAKING, ERROR
}

enum class MyraaEmotion {
    CALM, HAPPY, PLAYFUL, FOCUSED, CURIOUS, THINKING, CONFUSED, CONCERNED, ERROR
}

object MyraaStateManager {
    private val _myraaState = MutableStateFlow(MyraaState.IDLE)
    val myraaState: StateFlow<MyraaState> = _myraaState

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText

    private val _emotion = MutableStateFlow(MyraaEmotion.CALM)
    val emotion: StateFlow<MyraaEmotion> = _emotion
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    fun updateState(state: MyraaState) {
        _myraaState.value = state
    }

    fun updateSpokenText(text: String) {
        _spokenText.value = text
    }

    fun updateEmotion(emotion: MyraaEmotion) {
        _emotion.value = emotion
    }
    
    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }

    fun updateAudioLevel(level: Float) {
        _audioLevel.value = level
    }
}

