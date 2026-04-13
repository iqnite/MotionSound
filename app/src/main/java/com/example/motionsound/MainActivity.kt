package com.example.motionsound

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.motionsound.ui.theme.MotionSoundTheme
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotionSoundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MotionSoundScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Preview
@Composable
fun MotionSoundScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var movementDetected by remember { mutableStateOf(false) }

    val soundPool = remember {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()
    }

    @Composable
    fun getSound(fileName: String): Int {
        return remember(fileName) {
            val assetManager = context.assets
            val afd = assetManager.openFd(fileName)
            soundPool.load(afd, 1)
        }
    }

    val jumpSoundId = getSound("jump.wav")
    val explosionSoundId = getSound("explosion.mp3")
    val soundIds =
        remember(jumpSoundId) { mapOf("jump" to jumpSoundId, "explosion" to explosionSoundId) }

    val pagerState = rememberPagerState(pageCount = {
        2
    })

    fun playSound(soundId: String) {
        soundIds[soundId]?.let { id ->
            soundPool.play(id, 1f, 1f, 0, 0, 1f)
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
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
                        val speed =
                            sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000
                        val speedX = sqrt(deltaX * deltaX) / diffTime * 10000
                        val speedY = sqrt(deltaY * deltaY) / diffTime * 10000
                        val speedZ = sqrt(deltaZ * deltaZ) / diffTime * 10000

                        var detected = false
                        if (pagerState.currentPage == 0 && speed > MOVEMENT_THRESHOLD) {
                            detected = true
                            playSound("jump")
                        } else if (pagerState.currentPage == 1) {
                            if (isMoving) {
                                if (speed < EXPLOSION_STOP_SPEED_THRESHOLD && currentTime - movementStartTime > EXPLOSION_MIN_THROW_DURATION) {
                                    detected = true
                                    isMoving = false
                                    playSound("explosion")
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
                        movementDetected = detected

                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
            soundPool.release()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when (page) {
                        0 -> "General movement"
                        1 -> "Bomb"
                        else -> "Page $page"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (movementDetected) "Moving!" else "Move the phone to play sound")
        }
    }
}
