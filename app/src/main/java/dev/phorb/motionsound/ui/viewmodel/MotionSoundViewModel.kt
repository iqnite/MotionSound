package dev.phorb.motionsound.ui.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dev.phorb.motionsound.audio.SoundPlayer
import dev.phorb.motionsound.sensor.MotionSensorManager

class MotionSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val soundPlayer = SoundPlayer(application)
    private val sensorManager = MotionSensorManager(application)
    private val cameraManager =
        application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraIdWithFlash: String? = null

    var movementDetected by mutableStateOf(false)
        private set

    var isLooping by mutableStateOf(false)
        private set

    var currentPage by mutableIntStateOf(0)
        private set

    var soundVariation by mutableStateOf("")

    init {
        soundPlayer.loadSound("jump", "jump.wav")
        soundPlayer.loadSound("explosion", "explosion.mp3")
        soundPlayer.loadSound("explosionfahh", "fahh.mp3")
        soundPlayer.loadSound("explosionpipe", "metalpipe.mp3")
        soundPlayer.loadSound("speed", "speed.wav")
        soundPlayer.loadSound("gun", "gun.mp3")

        val cameraList = cameraManager.cameraIdList
        cameraList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            if (characteristics.get(FLASH_INFO_AVAILABLE) == true) {
                cameraIdWithFlash = id
                return@forEach
            }
        }
    }

    fun onPageChanged(page: Int) {
        soundVariation = ""
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
                soundPlayer.playSound("explosion$soundVariation")
            },
            onSpeedDetected = { speed ->
                if (isLooping) {
                    val normalizedSpeed = speed / 1000f
                    soundPlayer.setLoopSoundRate(normalizedSpeed.coerceIn(0.5f, 2.0f))
                    soundPlayer.setLoopSoundVolume((normalizedSpeed).coerceIn(0f, 1.0f))
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
            onGunDetected = {
                finishMotion()
                movementDetected = true
                toggleFlash(true)
                soundPlayer.playSound("gun")
                toggleFlash(false)
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

    fun toggleFlash(state: Boolean = true) {
        cameraIdWithFlash?.let {
            try {
                cameraManager.setTorchMode(it, state)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.finishLoopSounds()
        soundPlayer.release()
        sensorManager.stopListening()
    }
}
