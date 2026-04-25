package com.example.motionsound.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.motionsound.audio.SoundPlayer
import com.example.motionsound.sensor.MotionSensorManager

class MotionSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val soundPlayer = SoundPlayer(application)
    private val sensorManager = MotionSensorManager(application)

    var movementDetected by mutableStateOf(false)
        private set

    var isLooping by mutableStateOf(false)
        private set

    var currentPage by mutableStateOf(0)
        private set

    init {
        soundPlayer.loadSound("jump", "jump.wav")
        soundPlayer.loadSound("explosion", "explosion.mp3")
        soundPlayer.loadSound("speed", "speed.wav")
    }

    fun onPageChanged(page: Int) {
        currentPage = page
        finishMotion()
    }

    fun startListening() {
        sensorManager.startListening(
            onJumpDetected = {
                finishMotion()
                movementDetected = true
                soundPlayer.playSound("jump")
            },
            onExplosionDetected = {
                finishMotion()
                movementDetected = true
                soundPlayer.playSound("explosion")
            },
            onSpeedDetected = { speed ->
                if (isLooping) {
                    soundPlayer.setLoopSoundRate((speed / 1000f).coerceIn(0.5f, 2.0f))
                } else {
                    finishMotion()
                    movementDetected = true
                    isLooping = true
                    soundPlayer.playSound("speed", loop = -1)
                }
            },
            onMotionStop = {
                finishMotion()
            },
            getCurrentPage = { currentPage }
        )
    }

    private fun finishMotion() {
        soundPlayer.finishLoopSounds()
        movementDetected = false
        isLooping = false
    }

    fun stopListening() {
        sensorManager.stopListening()
    }

    fun resetMovementDetection() {
        if (!isLooping)
            movementDetected = false
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.finishLoopSounds()
        soundPlayer.release()
        sensorManager.stopListening()
    }
}
