package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
 * TASK 2: Snackbar & rememberCoroutineScope
 *
 * WHY LaunchedEffect DOES NOT WORK HERE:
 * `LaunchedEffect` is a Composable function meant to run side-effects triggered by Composition lifecycle
 * (e.g., when entering Composition or when specified keys change).
 *
 * Button click listeners (`onClick: () -> Unit`) are regular non-composable callback functions.
 * Calling Composable functions like `LaunchedEffect` inside non-composable callbacks is a syntax/compiler error.
 *
 * To launch an asynchronous job (such as showing a Snackbar) from a user interaction callback while binding its
 * lifetime to the Composable container, `rememberCoroutineScope()` must be used. It provides a `CoroutineScope`
 * tied to the Composition lifecycle that is automatically cancelled when the Composable leaves Composition.
 *
 * OBSERVED BEHAVIOR ON SECOND TAP:
 * When tapping the button a second time before the first snackbar finishes:
 * `SnackbarHostState.showSnackbar()` suspends until the current snackbar completes or is dismissed,
 * queuing the new snackbar. Each button tap launches a coroutine on `scope`, which waits sequentially to display.
 *
 * LIFECYCLE BEHAVIOR WHEN NAVIGATING AWAY:
 * When navigating away (e.g. clicking 'Back' to return to MenuScreen), `Task2SnackbarContent` leaves Composition,
 * which automatically cancels the `rememberCoroutineScope`. All active snackbar coroutines are immediately cancelled,
 * preventing any leaks, crashes, or leftover snackbars.
 */

@Composable
fun Task2SnackbarContent() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var clickCount by remember { mutableIntStateOf(0) }
    var lastTapInfo by remember { mutableStateOf("No taps yet.") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Task 2: Snackbar & rememberCoroutineScope", style = MaterialTheme.typography.titleLarge)

            Button(onClick = {
                clickCount++
                lastTapInfo = "Tapped #$clickCount - launched snackbar coroutine on scope."
                Log.d("Task2Snackbar", lastTapInfo)
                scope.launch {
                    snackbarHostState.showSnackbar("Snackbar message #$clickCount")
                }
            }) {
                Text("Show Snackbar (Tap #$clickCount)")
            }

            Text("Last action: $lastTapInfo", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Checklist & Verification:", style = MaterialTheme.typography.titleSmall)
                    Text("1. Navigating away while snackbar is visible: scope is cancelled, app doesn't crash, no snackbar leaks.", style = MaterialTheme.typography.bodySmall)
                    Text("2. Second tap before snackbar finishes: SnackbarHostState queues the second snackbar so it displays after the first.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
