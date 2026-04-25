package com.example.motionsound.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class MotionSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var listener: SensorEventListener? = null

    fun startListening(
        onJumpDetected: () -> Unit,
        onExplosionDetected: () -> Unit,
        onSpeedDetected: (Float) -> Unit,
        onMotionStop: () -> Unit,
        getCurrentPage: () -> Int
    ) {
        listener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private var isMoving = false
            private var movementStartTime: Long = 0
            private var lastPage = -1
            private var smoothedSpeed = 0f
            private val SMOOTHING_FACTOR = 0.2f
            private val MOVEMENT_THRESHOLD = 200f
            private val EXPLOSION_MOVE_SPEED_THRESHOLD = 800f
            private val STOP_SPEED_THRESHOLD = 40f
            private val EXPLOSION_MIN_THROW_DURATION = 500

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val currentTime = System.currentTimeMillis()
                    if ((currentTime - lastUpdate) > 100) {
                        val diffTime = (currentTime - lastUpdate)
                        lastUpdate = currentTime

                        val deltaX = x - lastX
                        val deltaY = y - lastY
                        val deltaZ = z - lastZ
                        val rawSpeed =
                            sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000

                        // Apply EMA smoothing
                        smoothedSpeed = (smoothedSpeed * (1 - SMOOTHING_FACTOR)) + (rawSpeed * SMOOTHING_FACTOR)

                        val currentPage = getCurrentPage()
                        // Reset state when switching pages
                        if (currentPage != lastPage) {
                            if (isMoving) {
                                onMotionStop()
                            }
                            isMoving = false
                            lastPage = currentPage
                            smoothedSpeed = 0f
                        }

                        if (currentPage == 0 && smoothedSpeed > MOVEMENT_THRESHOLD) {
                            onJumpDetected()
                        } else if (currentPage == 1) {
                            if (isMoving) {
                                if (smoothedSpeed < STOP_SPEED_THRESHOLD && currentTime - movementStartTime > EXPLOSION_MIN_THROW_DURATION) {
                                    onExplosionDetected()
                                    isMoving = false
                                }
                                if (smoothedSpeed > EXPLOSION_MOVE_SPEED_THRESHOLD) {
                                    isMoving = false
                                    movementStartTime = currentTime
                                }
                            } else if (smoothedSpeed > EXPLOSION_MOVE_SPEED_THRESHOLD) {
                                isMoving = true
                                movementStartTime = currentTime
                            }
                        } else if (currentPage == 2) {
                            if (smoothedSpeed > MOVEMENT_THRESHOLD) {
                                isMoving = true
                                onSpeedDetected(smoothedSpeed)
                            } else if (isMoving && smoothedSpeed > STOP_SPEED_THRESHOLD) {
                                // Keep updating speed even if it drops below starting threshold
                                onSpeedDetected(smoothedSpeed)
                            } else if (smoothedSpeed < STOP_SPEED_THRESHOLD) {
                                if (isMoving) {
                                    onMotionStop()
                                    isMoving = false
                                }
                            }
                        }

                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stopListening() {
        listener?.let {
            sensorManager.unregisterListener(it)
        }
    }
}
