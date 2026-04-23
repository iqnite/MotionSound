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

    var currentPage by mutableStateOf(0)
        private set

    init {
        soundPlayer.loadSound("jump", "jump.wav")
        soundPlayer.loadSound("explosion", "explosion.mp3")
    }

    fun onPageChanged(page: Int) {
        currentPage = page
    }

    fun startListening() {
        sensorManager.startListening(
            onJumpDetected = {
                movementDetected = true
                soundPlayer.playSound("jump")
            },
            onExplosionDetected = {
                movementDetected = true
                soundPlayer.playSound("explosion")
            },
            getCurrentPage = { currentPage }
        )
    }

    fun stopListening() {
        sensorManager.stopListening()
    }

    fun resetMovementDetection() {
        movementDetected = false
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.release()
        sensorManager.stopListening()
    }
}
