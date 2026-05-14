package com.example.motionsound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motionsound.ui.theme.MotionSoundTheme
import com.example.motionsound.ui.viewmodel.MotionSoundViewModel
import kotlinx.coroutines.delay

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

@Composable
fun MotionSoundScreen(
    modifier: Modifier = Modifier,
    viewModel: MotionSoundViewModel = viewModel()
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 5 })
    var variationMenuExpanded by remember { mutableStateOf(false) }

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    DisposableEffect(Unit) {
        viewModel.startListening()
        onDispose {
            viewModel.stopListening()
        }
    }

    // Reset movement detection text after a short delay
    LaunchedEffect(viewModel.movementDetected) {
        if (viewModel.movementDetected) {
            delay(1000)
            viewModel.resetMovementDetection()
        }
    }

    @Composable
    fun VariationMenu(variations: Map<String, String>) {
        val selectedVariationText = variations[viewModel.soundVariation] ?: "Bomb"
        Box(modifier = Modifier.padding(top = 20.dp)) {
            Button(onClick = { variationMenuExpanded = true }) {
                Text(selectedVariationText)
            }
            DropdownMenu(
                expanded = variationMenuExpanded,
                onDismissRequest = { variationMenuExpanded = false }
            ) {
                for ((id, text) in variations) {
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            viewModel.soundVariation = id
                            variationMenuExpanded = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun CautionBanner(message: String, modifier: Modifier = Modifier) {
        val colorScheme = MaterialTheme.colorScheme
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.secondaryContainer)
                .drawBehind {
                    val strokeWidth = 4.dp.toPx()
                    drawLine(
                        color = colorScheme.secondary,
                        start = Offset(strokeWidth / 2, 0f),
                        end = Offset(strokeWidth / 2, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "CAUTION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (page) {
                        0 -> "Silent"
                        1 -> "General"
                        2 -> "Gun"
                        3 -> "Throw"
                        4 -> "Speed"
                        else -> "Page $page"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = when (page) {
                        0 -> "Won't play any sound."
                        1 -> "Move the phone around to play sound!"
                        2 -> "Quickly move the phone up and down to play sound and flash the torch!"
                        3 -> "Throw the phone to play sound!"
                        4 -> "Move the phone faster for higher pitch!"
                        else -> "I forgot to add text to this page."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (page == 3) {
                    VariationMenu(mapOf(("" to "Bomb"), ("pipe" to "Pipe"), ("fahh" to "Fahh")))
                }
                if (page == 2 || page == 3) {
                    CautionBanner(
                        message = when (page) {
                            2 -> "Do not use in public."
                            3 -> "I am not responsible for any damage that may occur to your phone."
                            else -> ""
                        },
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val colorScheme = MaterialTheme.colorScheme
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) colorScheme.primary else colorScheme.secondaryContainer
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
            Text(text = if (viewModel.movementDetected) "Detected!" else "")
        }
    }
}
