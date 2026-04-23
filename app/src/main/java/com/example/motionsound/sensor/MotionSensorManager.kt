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
        getCurrentPage: () -> Int
    ) {
        listener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private var isMoving = false
            private var movementStartTime: Long = 0
            private val MOVEMENT_THRESHOLD = 800f
            private val EXPLOSION_MOVE_SPEED_THRESHOLD = 900f
            private val EXPLOSION_STOP_SPEED_THRESHOLD = 10f
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
                        val speed = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000

                        val currentPage = getCurrentPage()
                        if (currentPage == 0 && speed > MOVEMENT_THRESHOLD) {
                            onJumpDetected()
                        } else if (currentPage == 1) {
                            if (isMoving) {
                                if (speed < EXPLOSION_STOP_SPEED_THRESHOLD && currentTime - movementStartTime > EXPLOSION_MIN_THROW_DURATION) {
                                    onExplosionDetected()
                                    isMoving = false
                                }
                                if (speed > EXPLOSION_MOVE_SPEED_THRESHOLD) {
                                    isMoving = false
                                    movementStartTime = currentTime
                                }
                            } else if (speed > EXPLOSION_MOVE_SPEED_THRESHOLD) {
                                isMoving = true
                                movementStartTime = currentTime
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
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        listener?.let {
            sensorManager.unregisterListener(it)
        }
    }
}
