package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * TASK 6 (Bonus): produceState & Suspend Source Conversion
 *
 * REQUIREMENTS:
 * 1. FakeRepository with `suspend fun load(url: String): ResultState` (500ms delay; Success if ends with .png, else Error).
 * 2. `loadNetworkImage(url)` with `produceState`, returning states Loading, Success, Error.
 * 3. UI displays all three states using a `when` expression.
 * 4. Show by experiment that setting the same value causes no recomposition.
 * 5. Verify producer restarts & Loading state appears when URL changes.
 * 6. Verify producer cancellation (`finally` block in Logcat) when composable leaves composition.
 *
 * CONFLATION / EQUALITY BEHAVIOR:
 * `produceState` uses a Compose `MutableState` internally. When assigning `value = result`, Compose checks
 * structural equality (`==`) between the new value and existing value.
 * Because `ResultState.Success` and `ResultState.Error` are data classes with structural equality, setting
 * an identical result value does not trigger recomposition.
 */

sealed interface ResultState {
    object Loading : ResultState
    data class Success(val data: String) : ResultState
    data class Error(val message: String) : ResultState
}

object FakeRepository {
    suspend fun load(url: String): ResultState {
        delay(500L)
        return if (url.endsWith(".png")) {
            ResultState.Success("Successfully loaded image from $url")
        } else {
            ResultState.Error("Failed to load: URL must end with .png ($url)")
        }
    }
}

@Composable
fun loadNetworkImage(url: String): State<ResultState> {
    return produceState<ResultState>(initialValue = ResultState.Loading, key1 = url) {
        try {
            value = ResultState.Loading
            Log.d("Task6ProduceState", "Producer started for url: $url")
            val result = FakeRepository.load(url)
            value = result
            Log.d("Task6ProduceState", "Producer loaded result for url: $url -> $result")
        } finally {
            Log.d("Task6ProduceState", "Producer cancelled/disposed for url: $url")
        }
    }
}

@Composable
fun ImageStateDisplayer(url: String) {
    val imageState = loadNetworkImage(url)
    var resultUiRecomposeCount by remember { mutableIntStateOf(0) }

    SideEffect {
        resultUiRecomposeCount++
        Log.d("Task6ProduceState", "ImageStateDisplayer recomposed count: $resultUiRecomposeCount")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Image Loader Result:", style = MaterialTheme.typography.titleSmall)
            Text("Component Recompositions: $resultUiRecomposeCount", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(4.dp))

            when (val state = imageState.value) {
                is ResultState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Loading image...", style = MaterialTheme.typography.bodyMedium)
                }
                is ResultState.Success -> {
                    Text("Success!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(state.data, style = MaterialTheme.typography.bodyMedium)
                }
                is ResultState.Error -> {
                    Text("Error!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun Task6ProduceStateContent() {
    var url by remember { mutableStateOf("avatar.png") }
    var isMounted by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Task 6: Bonus - produceState", style = MaterialTheme.typography.titleLarge)
        Text("Current URL: $url", style = MaterialTheme.typography.titleMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Mount Image Loader Component:")
            Switch(checked = isMounted, onCheckedChange = { isMounted = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { url = "cat.png" }) { Text("Load cat.png (Success)") }
            Button(onClick = { url = "document.txt" }) { Text("Load doc.txt (Error)") }
        }

        // Button to set same URL
        Button(
            onClick = { url = "cat.png" },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Set Same URL ('cat.png')")
        }

        if (isMounted) {
            ImageStateDisplayer(url = url)
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Component unmounted.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
                    Text("Check Logcat for producer cancellation log in finally block.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Verification & Behavior Checklist:", style = MaterialTheme.typography.titleSmall)
                Text("1. URL change: produceState key changes -> previous producer cancels (finally block logged) and new producer starts with Loading state.", style = MaterialTheme.typography.bodySmall)
                Text("2. Same URL clicked: url state doesn't change, so produceState producer is NOT restarted and component does NOT recompose.", style = MaterialTheme.typography.bodySmall)
                Text("3. Unmounting / Leaving screen: produceState coroutine is cancelled, logging 'Producer cancelled/disposed' in Logcat.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
