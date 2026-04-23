package com.example.motionsound.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundPlayer(private val context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<String, Int>()

    fun loadSound(name: String, fileName: String) {
        try {
            val afd = context.assets.openFd(fileName)
            val soundId = soundPool.load(afd, 1)
            soundMap[name] = soundId
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSound(name: String) {
        soundMap[name]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
