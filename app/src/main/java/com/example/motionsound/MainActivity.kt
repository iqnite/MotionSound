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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
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
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })
    var variationMenuExpanded by remember { mutableStateOf(false) }
    var selectedVariation by remember { mutableStateOf("Bomb") }

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
    fun variationMenu(variations: Map<String, String>) {
        Box(modifier = Modifier.padding(top = 100.dp)) {
            Button(onClick = { variationMenuExpanded = true }) {
                Text(selectedVariation)
            }
            DropdownMenu(
                expanded = variationMenuExpanded,
                onDismissRequest = { variationMenuExpanded = false }
            ) {
                for ((id, text) in variations) {
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            selectedVariation = text
                            viewModel.soundVariation = id
                            variationMenuExpanded = false
                        }
                    )
                }
            }
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
                        0 -> "Silent"
                        1 -> "General movement"
                        2 -> "Throw"
                        3 -> "Speed"
                        else -> "Page $page"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                if (page == 2) {
                    variationMenu(mapOf(("" to "Bomb"), ("fahh" to "Fahh")))
                }
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
            Text(text = if (viewModel.movementDetected) "Moving!" else "Move the phone to play sound")
        }
    }
}
